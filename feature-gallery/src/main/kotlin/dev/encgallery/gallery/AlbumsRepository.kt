package dev.encgallery.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

class AlbumsRepository(private val keystore: KeystoreAesGcm) {

    private val blob = EncryptedFileBlob(keystore)

    fun listAlbums(context: Context): List<AlbumMeta> {
        val root = VaultPaths.albumsRoot(context)
        val dirs = root.listFiles { f -> f.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { dir ->
            val metaFile = File(dir, VaultPaths.ALBUM_META_FILE)
            if (!metaFile.exists()) {

                return@mapNotNull null
            }
            loadMeta(metaFile, expectedUuid = dir.name)
        }.sortedByDescending { it.modifiedAt }
    }

    fun getAlbum(context: Context, albumUuid: String): AlbumMeta? {
        val dir = File(VaultPaths.albumsRoot(context), albumUuid)
        val metaFile = File(dir, VaultPaths.ALBUM_META_FILE)
        if (!metaFile.exists()) return null
        return loadMeta(metaFile, expectedUuid = albumUuid)
    }

    fun create(context: Context, name: String): AlbumMeta {
        val uuid = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val meta = AlbumMeta(
            uuid = uuid,
            name = name,
            createdAt = now,
            modifiedAt = now,
            coverEntryUuid = null,
            coverCropRect = null
        )
        val dir = VaultPaths.albumDir(context, uuid)
        saveMeta(File(dir, VaultPaths.ALBUM_META_FILE), meta)

        EncLog.i(TAG, "created album uuid=$uuid (name-len=${name.length})")
        return meta
    }

    fun ensureImportedAlbum(context: Context, defaultName: String): AlbumMeta {
        val existing = getAlbum(context, VaultPaths.IMPORTED_ALBUM_UUID)
        if (existing != null) return existing

        val dir = VaultPaths.albumDir(context, VaultPaths.IMPORTED_ALBUM_UUID)
        val metaFile = File(dir, VaultPaths.ALBUM_META_FILE)
        val repairing = metaFile.exists()
        val now = System.currentTimeMillis()
        val meta = AlbumMeta(
            uuid = VaultPaths.IMPORTED_ALBUM_UUID,
            name = defaultName,
            createdAt = now,
            modifiedAt = now,
            coverEntryUuid = null,
            coverCropRect = null
        )
        saveMeta(metaFile, meta)
        EncLog.i(
            TAG,
            if (repairing) {
                "repaired corrupt/unreadable Imported album meta uuid=${VaultPaths.IMPORTED_ALBUM_UUID}"
            } else {
                "created default Imported album uuid=${VaultPaths.IMPORTED_ALBUM_UUID}"
            }
        )
        return meta
    }

    fun rename(context: Context, albumUuid: String, newName: String): AlbumMeta? {
        val current = getAlbum(context, albumUuid) ?: return null
        val updated = current.copy(name = newName, modifiedAt = System.currentTimeMillis())
        val metaFile = File(VaultPaths.albumDir(context, albumUuid), VaultPaths.ALBUM_META_FILE)
        saveMeta(metaFile, updated)
        EncLog.i(TAG, "renamed album $albumUuid (new-name-len=${newName.length})")
        return updated
    }

    fun touch(context: Context, albumUuid: String): AlbumMeta? {
        val current = getAlbum(context, albumUuid) ?: return null
        val updated = current.copy(modifiedAt = System.currentTimeMillis())
        val metaFile = File(VaultPaths.albumDir(context, albumUuid), VaultPaths.ALBUM_META_FILE)
        saveMeta(metaFile, updated)
        return updated
    }

    fun setPinned(context: Context, albumUuid: String, pinned: Boolean): AlbumMeta? {
        val current = getAlbum(context, albumUuid) ?: return null
        if (current.pinned == pinned) return current
        val updated = current.copy(pinned = pinned)
        val metaFile = File(VaultPaths.albumDir(context, albumUuid), VaultPaths.ALBUM_META_FILE)
        saveMeta(metaFile, updated)
        EncLog.i(TAG, "set album $albumUuid pinned=$pinned")
        return updated
    }

    fun setCover(
        context: Context,
        albumUuid: String,
        coverEntryUuid: String,
        cropRect: NormalizedRect?
    ): AlbumMeta? {
        val current = getAlbum(context, albumUuid) ?: return null
        val updated = current.copy(
            coverEntryUuid = coverEntryUuid,
            coverCropRect = cropRect,
            modifiedAt = System.currentTimeMillis()
        )
        val metaFile = File(VaultPaths.albumDir(context, albumUuid), VaultPaths.ALBUM_META_FILE)
        saveMeta(metaFile, updated)
        EncLog.i(TAG, "set cover album=$albumUuid entry=$coverEntryUuid crop=$cropRect")
        return updated
    }

    fun setCustomCover(
        context: Context,
        albumUuid: String,
        coverEntry: VaultEntry,
        cropRect: NormalizedRect
    ): AlbumMeta? {
        if (getAlbum(context, albumUuid) == null) {
            EncLog.w(TAG, "setCustomCover: album $albumUuid not found")
            return null
        }
        if (coverEntry.albumUuid != albumUuid) {
            EncLog.w(TAG, "setCustomCover: entry ${coverEntry.uuid} not in album $albumUuid")
            return null
        }

        val coverThumbFile = VaultPaths.albumCoverThumbFile(context, albumUuid)
        try {
            val thumbJpeg = renderCoverThumbnail(coverEntry, cropRect)
            blob.encrypt(ByteArrayInputStream(thumbJpeg), coverThumbFile)
        } catch (t: Throwable) {
            if (coverThumbFile.exists() && !coverThumbFile.delete()) {
                EncLog.w(TAG, "setCustomCover cleanup: cover.thumb delete failed for $albumUuid")
            }
            EncLog.e(
                TAG,
                "setCustomCover render failed for album=$albumUuid entry=${coverEntry.uuid}: ${t.javaClass.simpleName}: ${t.message}"
            )
            return null
        }

        return setCover(context, albumUuid, coverEntry.uuid, cropRect)
    }

    fun clearCover(context: Context, albumUuid: String): AlbumMeta? {
        val current = getAlbum(context, albumUuid) ?: return null
        val coverThumbFile = VaultPaths.albumCoverThumbFile(context, albumUuid)
        if (coverThumbFile.exists() && !coverThumbFile.delete()) {
            EncLog.w(TAG, "clearCover: failed to delete cover.thumb for $albumUuid")

        }
        val updated = current.copy(
            coverEntryUuid = null,
            coverCropRect = null,
            modifiedAt = System.currentTimeMillis()
        )
        val metaFile = File(VaultPaths.albumDir(context, albumUuid), VaultPaths.ALBUM_META_FILE)
        saveMeta(metaFile, updated)
        EncLog.i(TAG, "cleared cover for album=$albumUuid")
        return updated
    }

    private fun renderCoverThumbnail(
        entry: VaultEntry,
        cropRect: NormalizedRect
    ): ByteArray {
        val plain = blob.decryptToBytes(entry.blobFile)
        val source = ImageDecoder.createSource(ByteBuffer.wrap(plain))
        val cropped: Bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val srcW = info.size.width
            val srcH = info.size.height

            val cx = (cropRect.x * srcW).toInt().coerceAtLeast(0)
            val cy = (cropRect.y * srcH).toInt().coerceAtLeast(0)
            val cw = (cropRect.w * srcW).toInt().coerceAtLeast(1)
            val ch = (cropRect.h * srcH).toInt().coerceAtLeast(1)

            val targetEdge = TARGET_LONG_EDGE_PX

            val cropEdgePx = maxOf(cw, ch)
            val scale = targetEdge.toFloat() / cropEdgePx
            val scaledW = (srcW * scale).toInt().coerceAtLeast(1)
            val scaledH = (srcH * scale).toInt().coerceAtLeast(1)
            decoder.setTargetSize(scaledW, scaledH)

            val tx = (cx * scale).toInt().coerceAtLeast(0)
            val ty = (cy * scale).toInt().coerceAtLeast(0)
            val tw = (cw * scale).toInt().coerceAtLeast(1)
            val th = (ch * scale).toInt().coerceAtLeast(1)
            decoder.setCrop(android.graphics.Rect(tx, ty, tx + tw, ty + th))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return try {
            val out = ByteArrayOutputStream(  32 * 1024)
            val ok = cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            check(ok) { "Bitmap.compress returned false" }
            out.toByteArray()
        } finally {
            cropped.recycle()
        }
    }

    fun deletePermanently(context: Context, albumUuid: String): Boolean {
        val dir = File(VaultPaths.albumsRoot(context), albumUuid)
        if (!dir.exists()) return false
        val ok = dir.deleteRecursively()
        EncLog.i(TAG, "deleted album $albumUuid permanently, ok=$ok")
        return ok
    }

    private fun loadMeta(file: File, expectedUuid: String): AlbumMeta? = try {
        val bytes = blob.decryptToBytes(file)
        val parsed = AlbumMeta.fromJsonBytes(bytes, expectedUuid)
        if (parsed == null) {
            EncLog.w(TAG, "meta file $file failed to parse or uuid-mismatch")
        }
        parsed
    } catch (t: Throwable) {
        EncLog.w(TAG, "meta file $file decrypt failed: ${t.javaClass.simpleName}: ${t.message}")
        null
    }

    private fun saveMeta(file: File, meta: AlbumMeta) {

        blob.encrypt(ByteArrayInputStream(meta.toJsonBytes()), file)
    }

    companion object {
        private const val TAG = "AlbumsRepository"

        private const val TARGET_LONG_EDGE_PX = ThumbnailFactory.TARGET_LONG_EDGE_PX

        private const val JPEG_QUALITY = 75
    }
}
