package dev.encgallery.gallery

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import dev.encgallery.logging.EncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceBucket(
    val id: String,
    val name: String,
    val count: Int,
    val coverUri: Uri
)

object DeviceMediaBuckets {

    private const val TAG = "DeviceMediaBuckets"

    private class Acc(
        val id: String,
        var name: String,
        var count: Int,
        var cover: Uri,
        var recency: Long
    )

    suspend fun listBuckets(context: Context): List<DeviceBucket> = withContext(Dispatchers.IO) {
        val acc = HashMap<String, Acc>()
        foldInto(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, acc)
        foldInto(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, acc)
        acc.values
            .map { DeviceBucket(it.id, it.name, it.count, it.cover) }
            .sortedByDescending { b -> acc[b.id]?.recency ?: 0L }
            .also { EncLog.i(TAG, "listBuckets: ${it.size} bucket(s)") }
    }

    private fun foldInto(context: Context, contentUri: Uri, acc: HashMap<String, Acc>) {
        val proj = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        try {
            context.contentResolver.query(
                contentUri, proj, null, null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { c ->
                val idI = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val bidI = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val bnI = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val dmI = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val bid = c.getString(bidI) ?: continue
                    val id = c.getLong(idI)
                    val date = if (c.isNull(dmI)) 0L else c.getLong(dmI)
                    val itemUri = ContentUris.withAppendedId(contentUri, id)
                    val name = c.getString(bnI)?.takeIf { it.isNotBlank() } ?: bid
                    val e = acc[bid]
                    if (e == null) {
                        acc[bid] = Acc(bid, name, 1, itemUri, date)
                    } else {
                        e.count++
                        if (date >= e.recency) {
                            e.recency = date
                            e.cover = itemUri
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            EncLog.w(TAG, "bucket query failed for $contentUri: ${t.javaClass.simpleName}")
        }
    }

    suspend fun bucketItemUris(context: Context, bucketId: String): List<Uri> =
        withContext(Dispatchers.IO) {
            val out = ArrayList<Uri>()
            collect(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, bucketId, out)
            collect(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, bucketId, out)
            out
        }

    private fun collect(context: Context, contentUri: Uri, bucketId: String, out: MutableList<Uri>) {
        try {
            context.contentResolver.query(
                contentUri,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.BUCKET_ID} = ?",
                arrayOf(bucketId),
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { c ->
                val idI = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                while (c.moveToNext()) {
                    out.add(ContentUris.withAppendedId(contentUri, c.getLong(idI)))
                }
            }
        } catch (t: Throwable) {
            EncLog.w(TAG, "item query failed for $contentUri/$bucketId: ${t.javaClass.simpleName}")
        }
    }

    suspend fun loadThumbnail(context: Context, uri: Uri, sizePx: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
                } else {
                    decodeDownsampled(context, uri, sizePx)
                }
            } catch (t: Throwable) {
                null
            }
        }

    private fun decodeDownsampled(context: Context, uri: Uri, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        var halfW = bounds.outWidth / 2
        var halfH = bounds.outHeight / 2
        while (halfW >= sizePx && halfH >= sizePx) {
            sample *= 2; halfW /= 2; halfH /= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }
}
