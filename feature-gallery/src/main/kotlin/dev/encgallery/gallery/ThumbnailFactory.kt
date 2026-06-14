package dev.encgallery.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

object ThumbnailFactory {

    private const val TAG = "ThumbnailFactory"

    const val TARGET_LONG_EDGE_PX = 1024

    private const val JPEG_QUALITY = 88

    fun generateAtImport(context: Context, uri: Uri, mime: String): ByteArray? {
        return try {
            val bitmap: Bitmap? = when {
                mime.startsWith("image/") -> decodeImageFromUri(context, uri)
                mime.startsWith("video/") -> decodeVideoFrameFromUri(context, uri)
                else -> {
                    EncLog.w(TAG, "generateAtImport: unexpected mime $mime")
                    null
                }
            }
            bitmap?.let {
                try {
                    encodeJpeg(it)
                } finally {
                    it.recycle()
                }
            }
        } catch (t: Throwable) {
            EncLog.w(TAG, "generateAtImport($uri): ${t.javaClass.simpleName}: ${t.message}")
            null
        }
    }

    fun generateFromBlob(
        context: Context,
        blobFile: File,
        keystore: KeystoreAesGcm
    ): ByteArray? {
        val tempPlain = File(context.cacheDir, "thumbgen_${blobFile.nameWithoutExtension}.tmp")
        return try {
            EncryptedFileBlob(keystore).decryptToFile(blobFile, tempPlain)

            val bitmap: Bitmap? = decodeImageFromFile(tempPlain)
                ?: decodeVideoFrameFromFile(tempPlain)
            bitmap?.let {
                try {
                    encodeJpeg(it)
                } finally {
                    it.recycle()
                }
            }
        } catch (t: Throwable) {
            EncLog.w(TAG, "generateFromBlob(${blobFile.name}): ${t.javaClass.simpleName}: ${t.message}")
            null
        } finally {

            if (tempPlain.exists() && !tempPlain.delete()) {
                EncLog.w(TAG, "could not delete temp plaintext ${tempPlain.name}")
            }
        }
    }

    private fun decodeImageFromUri(context: Context, uri: Uri): Bitmap? {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return decodeImageWithTargetSize(source)
    }

    private fun decodeImageFromFile(file: File): Bitmap? {

        val source = ImageDecoder.createSource(ByteBuffer.wrap(file.readBytes()))
        return decodeImageWithTargetSize(source)
    }

    private fun decodeImageWithTargetSize(source: ImageDecoder.Source): Bitmap? = try {

        val intermediate = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            decoder.setTargetSampleSize(sampleSizeFor(longest, TARGET_LONG_EDGE_PX))

            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
        val finalBmp = downscaleTo(intermediate, TARGET_LONG_EDGE_PX)
        if (finalBmp !== intermediate) intermediate.recycle()
        finalBmp
    } catch (t: Throwable) {
        EncLog.w(TAG, "ImageDecoder failed: ${t.javaClass.simpleName}: ${t.message}")
        null
    }

    private fun sampleSizeFor(longest: Int, target: Int): Int {
        if (longest <= target) return 1
        var s = 1
        var best = 1
        var bestDelta = Int.MAX_VALUE
        while (true) {
            val resultLong = longest / s
            val delta = kotlin.math.abs(resultLong - target)
            if (delta < bestDelta) {
                bestDelta = delta
                best = s
            }

            if (resultLong <= target) break
            s *= 2
        }
        return best
    }

    private fun downscaleTo(src: Bitmap, longEdge: Int): Bitmap {
        var cur = src
        while (maxOf(cur.width, cur.height) > longEdge * 2) {
            val next = Bitmap.createScaledBitmap(
                cur,
                (cur.width / 2).coerceAtLeast(1),
                (cur.height / 2).coerceAtLeast(1),
                  true
            )
            if (cur !== src) cur.recycle()
            cur = next
        }
        val longest = maxOf(cur.width, cur.height)
        if (longest <= longEdge) return cur
        val scale = longEdge.toFloat() / longest
        val out = Bitmap.createScaledBitmap(
            cur,
            (cur.width * scale).toInt().coerceAtLeast(1),
            (cur.height * scale).toInt().coerceAtLeast(1),
              true
        )
        if (cur !== src) cur.recycle()
        return out
    }

    private fun decodeVideoFrameFromUri(context: Context, uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            getScaledFrameOrFallback(retriever)
        } catch (t: Throwable) {
            EncLog.w(TAG, "MediaMetadataRetriever (URI) failed: ${t.javaClass.simpleName}: ${t.message}")
            null
        } finally {

            try { retriever.release() } catch (_: Throwable) {   }
        }
    }

    private fun decodeVideoFrameFromFile(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            getScaledFrameOrFallback(retriever)
        } catch (t: Throwable) {
            EncLog.w(TAG, "MediaMetadataRetriever (File) failed: ${t.javaClass.simpleName}: ${t.message}")
            null
        } finally {
            try { retriever.release() } catch (_: Throwable) {   }
        }
    }

    private fun getScaledFrameOrFallback(retriever: MediaMetadataRetriever): Bitmap? {
        val scaled = retriever.getScaledFrameAtTime(
            0L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            TARGET_LONG_EDGE_PX,
            TARGET_LONG_EDGE_PX
        )
        if (scaled != null) return scaled

        val full = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return null
        return scaleBitmapToFit(full, TARGET_LONG_EDGE_PX).also {
            if (it !== full) full.recycle()
        }
    }

    private fun scaleBitmapToFit(src: Bitmap, longEdge: Int): Bitmap {
        val w = src.width
        val h = src.height
        val longest = maxOf(w, h)
        if (longest <= longEdge) return src
        val scale = longEdge.toFloat() / longest
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH,   true)
    }

    private fun encodeJpeg(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream(  32 * 1024)

        val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        check(ok) { "Bitmap.compress returned false" }
        return out.toByteArray()
    }

    fun decodeJpeg(jpegBytes: ByteArray, maxLongEdgePx: Int = Int.MAX_VALUE): Bitmap? = try {
        if (maxLongEdgePx == Int.MAX_VALUE) {
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longest > 0 && longest / (sample * 2) >= maxLongEdgePx) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, opts)
        }
    } catch (t: Throwable) {
        EncLog.w(TAG, "decodeJpeg: ${t.javaClass.simpleName}: ${t.message}")
        null
    }
}
