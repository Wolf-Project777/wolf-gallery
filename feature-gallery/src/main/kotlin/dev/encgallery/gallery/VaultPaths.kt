package dev.encgallery.gallery

import android.content.Context
import java.io.File
import java.util.UUID

object VaultPaths {

    private const val VAULT_SUBDIR = "encgallery"

    private const val ALBUMS_SUBDIR = "albums"

    private const val GROUPS_SUBDIR = "groups"

    private const val TRASH_SUBDIR = "trash"

    const val BLOB_SUFFIX = ".blob"

    const val THUMB_SUFFIX = ".thumb"

    const val ENTRY_META_SUFFIX = ".entrymeta"

    const val ALBUM_META_FILE = "meta.enc"

    const val TRASH_META_SUFFIX = ".trashmeta"

    const val TRASH_ALBUM_PREFIX = "album-"

    const val TRASH_GROUP_PREFIX = "group-"

    const val GROUP_META_FILE = "group_meta.enc"

    const val ALBUM_COVER_THUMB_FILE = "cover.thumb"

    const val IMPORTED_ALBUM_UUID = "00000000-0000-0000-0000-000000000001"

    fun vaultDir(context: Context): File =
        File(context.filesDir, VAULT_SUBDIR).apply {
            if (!exists()) mkdirs()
        }

    fun albumsRoot(context: Context): File =
        File(vaultDir(context), ALBUMS_SUBDIR).apply {
            if (!exists()) mkdirs()
        }

    fun albumDir(context: Context, albumUuid: String): File =
        File(albumsRoot(context), albumUuid).apply {
            if (!exists()) mkdirs()
        }

    fun albumCoverThumbFile(context: Context, albumUuid: String): File =
        File(albumDir(context, albumUuid), ALBUM_COVER_THUMB_FILE)

    fun groupsRoot(context: Context): File =
        File(vaultDir(context), GROUPS_SUBDIR).apply {
            if (!exists()) mkdirs()
        }

    fun groupDir(context: Context, groupUuid: String): File =
        File(groupsRoot(context), groupUuid).apply {
            if (!exists()) mkdirs()
        }

    fun trashDir(context: Context): File =
        File(vaultDir(context), TRASH_SUBDIR).apply {
            if (!exists()) mkdirs()
        }

    fun trashBlobFile(context: Context, entryUuid: String): File =
        File(trashDir(context), "$entryUuid$BLOB_SUFFIX")

    fun trashThumbFile(context: Context, entryUuid: String): File =
        File(trashDir(context), "$entryUuid$THUMB_SUFFIX")

    fun trashMetaFile(context: Context, entryUuid: String): File =
        File(trashDir(context), "$entryUuid$TRASH_META_SUFFIX")

    fun trashAlbumDir(context: Context, albumUuid: String): File =
        File(trashDir(context), "$TRASH_ALBUM_PREFIX$albumUuid")

    fun trashAlbumMetaFile(context: Context, albumUuid: String): File =
        File(trashDir(context), "$TRASH_ALBUM_PREFIX$albumUuid$TRASH_META_SUFFIX")

    fun trashGroupDir(context: Context, groupUuid: String): File =
        File(trashDir(context), "$TRASH_GROUP_PREFIX$groupUuid")

    fun trashGroupMetaFile(context: Context, groupUuid: String): File =
        File(trashDir(context), "$TRASH_GROUP_PREFIX$groupUuid$TRASH_META_SUFFIX")

    fun newBlobFile(vaultDir: File): BlobNames {
        val uuid = UUID.randomUUID().toString()
        return BlobNames(
            uuid = uuid,
            blob = File(vaultDir, "$uuid$BLOB_SUFFIX"),
            thumb = File(vaultDir, "$uuid$THUMB_SUFFIX")
        )
    }

    data class BlobNames(val uuid: String, val blob: File, val thumb: File)

    fun thumbForBlob(blobFile: File): File =
        File(blobFile.parentFile, "${blobFile.nameWithoutExtension}$THUMB_SUFFIX")

    fun entryMetaForBlob(blobFile: File): File =
        File(blobFile.parentFile, "${blobFile.nameWithoutExtension}$ENTRY_META_SUFFIX")

    fun trashEntryMetaFile(context: Context, entryUuid: String): File =
        File(trashDir(context), "$entryUuid$ENTRY_META_SUFFIX")
}
