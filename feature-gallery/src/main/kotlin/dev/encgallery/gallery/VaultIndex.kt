package dev.encgallery.gallery

import android.content.Context
import dev.encgallery.logging.EncLog
import java.io.File

data class VaultEntry(
    val uuid: String,
    val albumUuid: String,
    val blobFile: File,
    val thumbFile: File,
    val blobSizeBytes: Long,
    val mtimeMillis: Long
) {

    val hasThumbnail: Boolean get() = thumbFile.exists()
}

object VaultIndex {

    private const val TAG = "VaultIndex"

    fun listEntriesInAlbum(context: Context, albumUuid: String): List<VaultEntry> {
        val dir = File(VaultPaths.albumsRoot(context), albumUuid)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val all = dir.listFiles() ?: run {
            EncLog.w(TAG, "albumDir.listFiles() returned null at ${dir.absolutePath}")
            return emptyList()
        }

        return all
            .asSequence()
            .filter { it.isFile && it.name.endsWith(VaultPaths.BLOB_SUFFIX) }
            .map { blob ->
                VaultEntry(
                    uuid = blob.nameWithoutExtension,
                    albumUuid = albumUuid,
                    blobFile = blob,
                    thumbFile = VaultPaths.thumbForBlob(blob),
                    blobSizeBytes = blob.length(),
                    mtimeMillis = blob.lastModified()
                )
            }
            .sortedByDescending { it.mtimeMillis }
            .toList()
    }

    fun listAllEntries(context: Context): List<VaultEntry> {
        val root = VaultPaths.albumsRoot(context)
        val albumDirs = root.listFiles { f -> f.isDirectory } ?: return emptyList()

        return albumDirs
            .asSequence()
            .flatMap { albumDir ->
                listEntriesInAlbum(context, albumDir.name).asSequence()
            }
            .sortedByDescending { it.mtimeMillis }
            .toList()
    }

    fun migrateFlatToAlbums(context: Context): Int {
        val vaultRoot = VaultPaths.vaultDir(context)
        val flatBlobs = vaultRoot.listFiles { f ->
            f.isFile && f.name.endsWith(VaultPaths.BLOB_SUFFIX)
        } ?: return 0

        if (flatBlobs.isEmpty()) return 0

        val targetDir = VaultPaths.albumDir(context, VaultPaths.IMPORTED_ALBUM_UUID)
        var moved = 0
        flatBlobs.forEach { blob ->
            val target = File(targetDir, blob.name)
            if (target.exists()) {

                if (!blob.delete()) {
                    EncLog.w(TAG, "stale flat blob ${blob.name} could not be deleted")
                }
            } else if (blob.renameTo(target)) {
                moved++

                val thumbSource = VaultPaths.thumbForBlob(blob)
                if (thumbSource.exists()) {
                    val thumbTarget = File(targetDir, thumbSource.name)
                    if (!thumbSource.renameTo(thumbTarget)) {
                        EncLog.w(TAG, "thumb move failed for ${thumbSource.name}")
                    }
                }
            } else {
                EncLog.w(TAG, "renameTo failed for ${blob.name} → ${targetDir.name}/")
            }
        }
        if (moved > 0) {
            EncLog.i(TAG, "migrated $moved flat blob(s) into Imported album")
        }
        return moved
    }
}
