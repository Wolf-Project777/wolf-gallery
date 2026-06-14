package dev.encgallery.gallery

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayInputStream
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive

class PhotoImporter(private val keystore: KeystoreAesGcm) {

    private val blob = EncryptedFileBlob(keystore)
    private val entries = EntriesRepository(keystore)

    sealed class Progress {

        data class Started(val total: Int) : Progress()

        data class FileStarted(
            val index: Int,
            val total: Int,
            val displayName: String
        ) : Progress()

        data class FileSucceeded(
            val index: Int,
            val total: Int,
            val displayName: String,
            val sizeBytes: Long,
            val durationMs: Long
        ) : Progress()

        data class FileFailed(
            val index: Int,
            val total: Int,
            val displayName: String,
            val reason: String
        ) : Progress()

        data class Done(
            val total: Int,
            val successful: Int,
            val failed: Int
        ) : Progress()
    }

    suspend fun importAll(
        context: Context,
        uris: List<Uri>,
        targetAlbumUuid: String = VaultPaths.IMPORTED_ALBUM_UUID,
        onProgress: suspend (Progress) -> Unit
    ) {
        val total = uris.size
        onProgress(Progress.Started(total))
        EncLog.i(TAG, "import batch starting: $total file(s) → album=$targetAlbumUuid")

        val vaultDir = VaultPaths.albumDir(context, targetAlbumUuid)
        var succeeded = 0
        var failed = 0

        uris.forEachIndexed { index, uri ->

            coroutineContext.ensureActive()

            val displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "<unknown>"
            val humanIndex = index + 1
            onProgress(Progress.FileStarted(humanIndex, total, displayName))

            val mime = context.contentResolver.getType(uri)
            if (!SupportedMimeTypes.isAccepted(mime)) {
                failed++
                val reason = "unsupported MIME: ${mime ?: "<null>"}"

                EncLog.w(
                    TAG,
                    "rejected file ${humanIndex}/${total} before encrypt: $reason"
                )
                onProgress(Progress.FileFailed(humanIndex, total, displayName, reason))
                return@forEachIndexed
            }

            val names = VaultPaths.newBlobFile(vaultDir)
            val started = System.currentTimeMillis()
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("ContentResolver.openInputStream returned null for $uri")
                stream.use { src ->

                    blob.encryptEnvelope(src, names.blob)
                }
                val duration = System.currentTimeMillis() - started
                succeeded++
                EncLog.i(
                    TAG,
                    "imported ${humanIndex}/${total} → ${names.blob.name} (${names.blob.length()} bytes, ${duration}ms)"
                )

                val thumbDuration = generateAndStoreThumbnail(
                    context = context,
                    uri = uri,
                    mime = mime!!,
                    thumbFile = names.thumb
                )
                if (thumbDuration != null) {
                    EncLog.i(
                        TAG,
                        "thumbnail ${humanIndex}/${total} → ${names.thumb.name} (${names.thumb.length()} bytes, ${thumbDuration}ms)"
                    )
                }

                entries.saveImported(
                    blobFile = names.blob,
                    uuid = names.uuid,
                    originalFilename = displayName.takeIf { it != "<unknown>" }
                )

                onProgress(
                    Progress.FileSucceeded(humanIndex, total, displayName, names.blob.length(), duration)
                )
            } catch (e: CancellationException) {

                cleanupBlobPair(names.blob, names.thumb)
                EncLog.i(TAG, "import cancelled mid-batch on file ${humanIndex}/${total}")
                throw e
            } catch (t: Throwable) {

                cleanupBlobPair(names.blob, names.thumb)
                failed++
                val reason = "${t.javaClass.simpleName}: ${t.message ?: "<no message>"}"
                EncLog.e(TAG, "import failed for file ${humanIndex}/${total}: $reason")
                onProgress(Progress.FileFailed(humanIndex, total, displayName, reason))
            }
        }

        onProgress(Progress.Done(total, succeeded, failed))
        EncLog.i(TAG, "import batch complete: $succeeded ok, $failed failed (of $total)")
    }

    private fun generateAndStoreThumbnail(
        context: Context,
        uri: Uri,
        mime: String,
        thumbFile: java.io.File
    ): Long? {
        val started = System.currentTimeMillis()

        val uuid = thumbFile.nameWithoutExtension
        return try {
            val jpegBytes = ThumbnailFactory.generateAtImport(context, uri, mime)
            if (jpegBytes == null) {
                EncLog.w(TAG, "no thumb generated for $uuid (decoder returned null)")
                null
            } else {

                blob.encryptEnvelope(ByteArrayInputStream(jpegBytes), thumbFile)
                System.currentTimeMillis() - started
            }
        } catch (t: Throwable) {

            if (thumbFile.exists() && !thumbFile.delete()) {
                EncLog.w(TAG, "could not delete partial thumb ${thumbFile.name}")
            }
            EncLog.w(
                TAG,
                "thumb generation for $uuid failed: ${t.javaClass.simpleName}: ${t.message}"
            )
            null
        }
    }

    private fun cleanupBlobPair(blobFile: java.io.File, thumbFile: java.io.File) {
        if (blobFile.exists() && !blobFile.delete()) {
            EncLog.w(TAG, "could not delete partial blob ${blobFile.name}")
        }
        if (thumbFile.exists() && !thumbFile.delete()) {
            EncLog.w(TAG, "could not delete partial thumb ${thumbFile.name}")
        }
    }

    companion object {
        private const val TAG = "PhotoImporter"

        private fun queryDisplayName(context: Context, uri: Uri): String? = try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
