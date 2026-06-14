package dev.encgallery.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection

object EntryShare {

    private const val TAG = "EntryShare"
    private const val SHARE_DIR_NAME = "share"
    private const val STALE_AGE_MS = 10 * 60 * 1000L

    private const val SHARE_RAM_CAP_BYTES = 200L * 1024 * 1024
    private const val BLOB_OVERHEAD_BYTES = 36L

    suspend fun share(
        context: Context,
        entry: VaultEntry,
        keystore: KeystoreAesGcm,
    ) {
        val prepared = withContext(Dispatchers.IO) { prepare(context, entry, keystore) } ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = prepared.mime
            putExtra(Intent.EXTRA_STREAM, prepared.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.viewer_share_chooser))
        )
    }

    suspend fun shareMultiple(
        context: Context,
        entries: List<VaultEntry>,
        keystore: KeystoreAesGcm,
    ) {
        if (entries.isEmpty()) return
        val (uris, commonMime) = withContext(Dispatchers.IO) {
            val mimeSet = mutableSetOf<String>()
            val list = ArrayList<Uri>(entries.size)
            entries.forEach { entry ->
                val p = try {
                    prepare(context, entry, keystore)
                } catch (t: Throwable) {
                    EncLog.w(TAG, "shareMultiple: skipping ${entry.uuid} (${t.javaClass.simpleName}: ${t.message})")
                    null
                }
                if (p != null) {
                    mimeSet.add(p.mime)
                    list.add(p.uri)
                }
            }
            val mime = commonMimeOf(mimeSet)
            EncLog.i(TAG, "shareMultiple: prepared ${list.size}/${entries.size} files, mime=$mime")
            list to mime
        }

        if (uris.isEmpty()) return
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = commonMime
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.viewer_share_chooser))
        )
    }

    private data class Prepared(val uri: Uri, val mime: String)

    private fun prepare(context: Context, entry: VaultEntry, keystore: KeystoreAesGcm): Prepared? {
        val blob = EncryptedFileBlob(keystore)
        val plaintextSize = (entry.blobFile.length() - BLOB_OVERHEAD_BYTES).coerceAtLeast(0)
        return if (plaintextSize <= SHARE_RAM_CAP_BYTES) {
            val bytes = blob.decryptToBytes(entry.blobFile)
            val mime = sniffBytes(bytes)
            val name = recipientName(entry, keystore, mime)
            val token = ShareMemoryStore.register(name, mime, bytes)
            EncLog.i(TAG, "share '${entry.uuid}' in-RAM: ${bytes.size} bytes, mime=$mime, name-len=${name.length}")
            Prepared(ShareContentProvider.uriFor(context.packageName, token), mime)
        } else {
            prepareTempFallback(context, entry, keystore, blob)
        }
    }

    private fun prepareTempFallback(
        context: Context,
        entry: VaultEntry,
        keystore: KeystoreAesGcm,
        blob: EncryptedFileBlob,
    ): Prepared {
        val shareDir = File(context.cacheDir, SHARE_DIR_NAME).apply { mkdirs() }
        cleanupStale(shareDir)
        val placeholder = File(shareDir, "${entry.uuid}-${System.currentTimeMillis()}")
        blob.decryptToFile(entry.blobFile, placeholder)
        val mime = MimeSniffer.sniff(placeholder) ?: "application/octet-stream"
        val name = recipientName(entry, keystore, mime)
        val finalFile = File(shareDir, name)
        val renamed = if (placeholder.renameTo(finalFile)) finalFile else placeholder
        val authority = "${context.packageName}.fileprovider"
        EncLog.i(TAG, "share '${entry.uuid}' >cap temp fallback: ${renamed.length()} bytes, mime=$mime")
        return Prepared(FileProvider.getUriForFile(context, authority, renamed), mime)
    }

    private fun sniffBytes(bytes: ByteArray): String {
        MimeSniffer.sniff(bytes)?.let { return it }
        return try {
            ByteArrayInputStream(bytes).buffered().use { URLConnection.guessContentTypeFromStream(it) }
        } catch (t: Throwable) {
            EncLog.w(TAG, "MIME sniff fallback failed: ${t.javaClass.simpleName}")
            null
        } ?: "application/octet-stream"
    }

    private fun recipientName(entry: VaultEntry, keystore: KeystoreAesGcm, mime: String): String {
        val ext = mimeToExtension(mime) ?: "bin"
        val original = EntriesRepository(keystore).getMeta(entry.blobFile)
            ?.originalFilename
            ?.let(::sanitizeForShare)
        return original ?: "WolfGallery-${entry.uuid.take(8)}.$ext"
    }

    private fun commonMimeOf(mimeSet: Set<String>): String = when {
        mimeSet.isEmpty() -> "application/octet-stream"
        mimeSet.size == 1 -> mimeSet.first()
        mimeSet.all { it.startsWith("image/") } -> "image/*"
        mimeSet.all { it.startsWith("video/") } -> "video/*"
        else -> "*/*"
    }

    private fun cleanupStale(shareDir: File) {
        val cutoff = System.currentTimeMillis() - STALE_AGE_MS
        var removed = 0
        shareDir.listFiles()?.forEach { f ->
            if (f.lastModified() < cutoff) {
                secureDeleteFile(f)
                removed++
            }
        }
        if (removed > 0) EncLog.d(TAG, "cleaned up $removed stale share temp file(s)")
    }

    private fun sanitizeForShare(name: String): String? {
        val basename = name.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = basename.filter { it != ' ' && it != '\r' && it != '\n' && it.code != 0 }
            .trim()
            .take(200)
        return cleaned.takeIf { it.isNotEmpty() }
    }

    private fun mimeToExtension(mime: String): String? = when (mime.lowercase()) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/heic" -> "heic"
        "image/heif" -> "heif"
        "image/avif" -> "avif"
        "image/bmp" -> "bmp"
        "video/mp4" -> "mp4"
        "video/x-matroska" -> "mkv"
        "video/webm" -> "webm"
        "video/3gpp" -> "3gp"
        else -> null
    }
}
