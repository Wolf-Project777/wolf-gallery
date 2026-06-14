package dev.encgallery.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object ThumbnailLoader {

    private const val TAG = "ThumbnailLoader"

    private const val MAX_CONCURRENT_KEYSTORE_OPS = 6
    private val keystoreGate = java.util.concurrent.Semaphore(MAX_CONCURRENT_KEYSTORE_OPS)

    private inline fun <T> withKeystoreGate(block: () -> T): T {
        keystoreGate.acquire()
        return try {
            block()
        } finally {
            keystoreGate.release()
        }
    }

    internal fun <T> gated(block: () -> T): T = withKeystoreGate(block)

    private const val CACHE_BUDGET_BYTES = 128 * 1024 * 1024

    const val GRID_DISPLAY_PX = 512

    private fun gridKey(uuid: String): String = "$uuid@g"

    private val cache = object : LruCache<String, Bitmap>(CACHE_BUDGET_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun clearCache() {
        cache.evictAll()
        EncLog.d(TAG, "cache cleared")
    }

    fun loadOrGenerate(
        context: Context,
        entry: VaultEntry,
        keystore: KeystoreAesGcm,
        gridSized: Boolean = false
    ): Bitmap? {
        val key = if (gridSized) gridKey(entry.uuid) else entry.uuid
        cache.get(key)?.let { return it }

        val jpegBytes = withKeystoreGate {
            ensureThumbBytes(context, entry, keystore, forceRegen = false)
        }
        if (jpegBytes == null) return null
        val maxEdge = if (gridSized) GRID_DISPLAY_PX else Int.MAX_VALUE
        val bitmap = ThumbnailFactory.decodeJpeg(jpegBytes, maxEdge) ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    fun aspectRatioOf(context: Context, entry: VaultEntry, keystore: KeystoreAesGcm): Float? {
        val jpeg = withKeystoreGate {
            ensureThumbBytes(context, entry, keystore, forceRegen = false)
        } ?: return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        if (w <= 0 || h <= 0) return null
        return w.toFloat() / h
    }

    private val writeLocks = ConcurrentHashMap<String, Any>()

    private fun lockFor(uuid: String): Any = writeLocks.getOrPut(uuid) { Any() }

    internal fun ensureThumbBytes(
        context: Context,
        entry: VaultEntry,
        keystore: KeystoreAesGcm,
        forceRegen: Boolean
    ): ByteArray? {
        if (!forceRegen && entry.hasThumbnail) {
            decryptThumb(entry, keystore)?.let { return it }
            EncLog.w(TAG, "thumb ${entry.uuid} unreadable; regenerating")
        }
        synchronized(lockFor(entry.uuid)) {

            if (!forceRegen && entry.hasThumbnail) {
                decryptThumb(entry, keystore)?.let { return it }
            }
            val regenerated = ThumbnailFactory.generateFromBlob(
                context = context,
                blobFile = entry.blobFile,
                keystore = keystore
            ) ?: return null
            writeThumbAtomic(entry, keystore, regenerated)
            forget(entry.uuid)
            return regenerated
        }
    }

    private fun writeThumbAtomic(entry: VaultEntry, keystore: KeystoreAesGcm, jpeg: ByteArray) {
        val tmp = File(entry.thumbFile.parentFile, entry.thumbFile.name + ".tmp")
        try {

            EncryptedFileBlob(keystore).encryptEnvelope(ByteArrayInputStream(jpeg), tmp)
            if (!tmp.renameTo(entry.thumbFile)) {

                entry.thumbFile.delete()
                if (!tmp.renameTo(entry.thumbFile)) {
                    EncLog.w(TAG, "could not place regenerated thumb for ${entry.uuid}")
                    tmp.delete()
                }
            }
        } catch (t: Throwable) {
            EncLog.w(TAG, "persist thumb ${entry.uuid} failed: ${t.javaClass.simpleName}")
            if (tmp.exists()) tmp.delete()
        }
    }

    fun forget(uuid: String) {
        cache.remove(uuid)
        cache.remove(gridKey(uuid))
    }

    fun loadAlbumCover(
        context: Context,
        albumUuid: String,
        keystore: KeystoreAesGcm,
        gridSized: Boolean = false
    ): Bitmap? {
        val key = if (gridSized) "cover:$albumUuid@g" else "cover:$albumUuid"
        cache.get(key)?.let { return it }

        val coverFile = VaultPaths.albumCoverThumbFile(context, albumUuid)
        if (!coverFile.exists()) return null

        val jpegBytes = try {
            withKeystoreGate { EncryptedFileBlob(keystore).decryptToBytes(coverFile) }
        } catch (t: Throwable) {
            EncLog.w(
                TAG,
                "decrypt cover.thumb for $albumUuid failed: ${t.javaClass.simpleName}: ${t.message}"
            )
            return null
        }

        val maxEdge = if (gridSized) GRID_DISPLAY_PX else Int.MAX_VALUE
        val bitmap = ThumbnailFactory.decodeJpeg(jpegBytes, maxEdge) ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    fun forgetAlbumCover(albumUuid: String) {
        cache.remove("cover:$albumUuid")
        cache.remove("cover:$albumUuid@g")
    }

    private fun decryptThumb(entry: VaultEntry, keystore: KeystoreAesGcm): ByteArray? = try {
        EncryptedFileBlob(keystore).decryptToBytes(entry.thumbFile)
    } catch (t: Throwable) {
        EncLog.w(
            TAG,
            "decrypt thumb ${entry.thumbFile.name} failed: ${t.javaClass.simpleName}: ${t.message}"
        )
        null
    }

    fun loadTrashThumb(
        @Suppress("UNUSED_PARAMETER") context: Context,
        trashEntry: TrashEntry,
        keystore: KeystoreAesGcm
    ): Bitmap? {
        cache.get(trashEntry.uuid)?.let { return it }
        if (!trashEntry.hasThumbnail) return null
        val jpegBytes = try {
            withKeystoreGate { EncryptedFileBlob(keystore).decryptToBytes(trashEntry.thumbFile) }
        } catch (t: Throwable) {
            EncLog.w(
                TAG,
                "decrypt trash thumb ${trashEntry.thumbFile.name} failed: ${t.javaClass.simpleName}: ${t.message}"
            )
            return null
        }
        val bitmap = ThumbnailFactory.decodeJpeg(jpegBytes) ?: return null
        cache.put(trashEntry.uuid, bitmap)
        return bitmap
    }

    fun loadTrashAlbumCover(
        @Suppress("UNUSED_PARAMETER") context: Context,
        trashAlbum: TrashAlbum,
        keystore: KeystoreAesGcm
    ): Bitmap? {
        val key = "trashCover:${trashAlbum.uuid}"
        cache.get(key)?.let { return it }

        val coverFile = java.io.File(trashAlbum.albumDir, VaultPaths.ALBUM_COVER_THUMB_FILE)
        val sourceFile = if (coverFile.exists()) {
            coverFile
        } else {

            trashAlbum.albumDir.listFiles { f ->
                f.isFile && f.name.endsWith(VaultPaths.THUMB_SUFFIX)
            }?.firstOrNull() ?: return null
        }

        val jpegBytes = try {
            withKeystoreGate { EncryptedFileBlob(keystore).decryptToBytes(sourceFile) }
        } catch (t: Throwable) {
            EncLog.w(
                TAG,
                "decrypt trash album cover ${trashAlbum.uuid} (${sourceFile.name}) failed: ${t.javaClass.simpleName}: ${t.message}"
            )
            return null
        }
        val bitmap = ThumbnailFactory.decodeJpeg(jpegBytes) ?: return null
        cache.put(key, bitmap)
        return bitmap
    }
}
