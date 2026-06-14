package dev.encgallery.gallery

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

class AlbumGroupsRepository(private val keystore: KeystoreAesGcm) {

    private val blob = EncryptedFileBlob(keystore)

    fun listGroups(context: Context): List<AlbumGroupMeta> {
        val root = VaultPaths.groupsRoot(context)
        val dirs = root.listFiles { f -> f.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { dir ->
            val metaFile = File(dir, VaultPaths.GROUP_META_FILE)
            if (!metaFile.exists()) return@mapNotNull null
            loadMeta(metaFile, expectedUuid = dir.name)
        }.sortedByDescending { it.modifiedAt }
    }

    fun getGroup(context: Context, groupUuid: String): AlbumGroupMeta? {
        val dir = File(VaultPaths.groupsRoot(context), groupUuid)
        val metaFile = File(dir, VaultPaths.GROUP_META_FILE)
        if (!metaFile.exists()) return null
        return loadMeta(metaFile, expectedUuid = groupUuid)
    }

    fun create(
        context: Context,
        name: String,
        albumUuids: List<String> = emptyList()
    ): AlbumGroupMeta {
        val uuid = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val meta = AlbumGroupMeta(
            uuid = uuid,
            name = name,
            createdAt = now,
            modifiedAt = now,
            albumUuids = albumUuids.distinct()
        )
        val dir = VaultPaths.groupDir(context, uuid)
        saveMeta(File(dir, VaultPaths.GROUP_META_FILE), meta)

        EncLog.i(
            TAG,
            "created group uuid=$uuid (name-len=${name.length}) with ${albumUuids.size} album(s)"
        )
        return meta
    }

    fun rename(context: Context, groupUuid: String, newName: String): AlbumGroupMeta? {
        val current = getGroup(context, groupUuid) ?: return null
        val updated = current.copy(name = newName, modifiedAt = System.currentTimeMillis())
        val metaFile = File(VaultPaths.groupDir(context, groupUuid), VaultPaths.GROUP_META_FILE)
        saveMeta(metaFile, updated)
        EncLog.i(TAG, "renamed group $groupUuid (new-name-len=${newName.length})")
        return updated
    }

    fun setPinned(context: Context, groupUuid: String, pinned: Boolean): AlbumGroupMeta? {
        val current = getGroup(context, groupUuid) ?: return null
        if (current.pinned == pinned) return current
        val updated = current.copy(pinned = pinned)
        val metaFile = File(VaultPaths.groupDir(context, groupUuid), VaultPaths.GROUP_META_FILE)
        saveMeta(metaFile, updated)
        EncLog.i(TAG, "set group $groupUuid pinned=$pinned")
        return updated
    }

    fun addAlbumsToGroup(
        context: Context,
        groupUuid: String,
        albumUuids: List<String>
    ): AlbumGroupMeta? {
        val current = getGroup(context, groupUuid) ?: return null
        val newList = (current.albumUuids + albumUuids).distinct()
        val updated = current.copy(
            albumUuids = newList,
            modifiedAt = System.currentTimeMillis()
        )
        val metaFile = File(VaultPaths.groupDir(context, groupUuid), VaultPaths.GROUP_META_FILE)
        saveMeta(metaFile, updated)
        EncLog.i(
            TAG,
            "added ${albumUuids.size} album(s) to group $groupUuid (new size=${newList.size})"
        )
        return updated
    }

    fun removeAlbumsFromGroup(
        context: Context,
        groupUuid: String,
        albumUuids: List<String>
    ): AlbumGroupMeta? {
        val current = getGroup(context, groupUuid) ?: return null
        val toRemove = albumUuids.toSet()
        val newList = current.albumUuids.filter { it !in toRemove }
        if (newList.size == current.albumUuids.size) return current
        val updated = current.copy(
            albumUuids = newList,
            modifiedAt = System.currentTimeMillis()
        )
        val metaFile = File(VaultPaths.groupDir(context, groupUuid), VaultPaths.GROUP_META_FILE)
        saveMeta(metaFile, updated)
        EncLog.i(
            TAG,
            "removed ${albumUuids.size - newList.size + (current.albumUuids.size - newList.size)} album(s) from group $groupUuid"
        )
        return updated
    }

    fun moveAlbumsToGroup(
        context: Context,
        destGroupUuid: String,
        albumUuids: List<String>
    ): AlbumGroupMeta? {
        val toMove = albumUuids.toSet()

        listGroups(context).forEach { group ->
            if (group.uuid == destGroupUuid) return@forEach
            if (group.albumUuids.any { it in toMove }) {
                removeAlbumsFromGroup(context, group.uuid, albumUuids)
            }
        }
        return addAlbumsToGroup(context, destGroupUuid, albumUuids)
    }

    fun ungroupAlbums(context: Context, albumUuids: List<String>) {
        val toRemove = albumUuids.toSet()
        listGroups(context).forEach { group ->
            if (group.albumUuids.any { it in toRemove }) {
                removeAlbumsFromGroup(context, group.uuid, albumUuids)
            }
        }
    }

    fun deleteGroup(context: Context, groupUuid: String): Boolean {
        val dir = File(VaultPaths.groupsRoot(context), groupUuid)
        if (!dir.exists()) return false
        val ok = dir.deleteRecursively()
        EncLog.i(TAG, "deleted group $groupUuid (albums released to top-level), ok=$ok")
        return ok
    }

    private fun loadMeta(file: File, expectedUuid: String): AlbumGroupMeta? = try {
        val bytes = blob.decryptToBytes(file)
        val parsed = AlbumGroupMeta.fromJsonBytes(bytes, expectedUuid)
        if (parsed == null) {
            EncLog.w(TAG, "group meta $file failed to parse or uuid-mismatch")
        }
        parsed
    } catch (t: Throwable) {
        EncLog.w(TAG, "group meta $file decrypt failed: ${t.javaClass.simpleName}: ${t.message}")
        null
    }

    private fun saveMeta(file: File, meta: AlbumGroupMeta) {
        blob.encrypt(ByteArrayInputStream(meta.toJsonBytes()), file)
    }

    companion object {
        private const val TAG = "AlbumGroupsRepo"
    }
}
