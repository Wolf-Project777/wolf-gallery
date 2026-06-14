package dev.encgallery.gallery

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayInputStream
import java.io.File

class TrashRepository(private val keystore: KeystoreAesGcm) {

    private val blob = EncryptedFileBlob(keystore)

    fun listTrash(context: Context): List<TrashEntry> =
        listAllRecords(context).filterIsInstance<TrashEntry>()

    fun listAllRecords(context: Context): List<TrashRecord> {
        val dir = VaultPaths.trashDir(context)
        val all = dir.listFiles() ?: return emptyList()

        val entryRecords = mutableListOf<TrashRecord>()
        val albumRecords = mutableListOf<TrashRecord>()
        val groupRecords = mutableListOf<TrashRecord>()

        val albumOriginalGroupUuids = mutableMapOf<String, MutableList<TrashAlbum>>()

        for (f in all) {
            when {

                f.isFile && f.name.endsWith(VaultPaths.BLOB_SUFFIX) &&
                        !f.name.startsWith(VaultPaths.TRASH_ALBUM_PREFIX) &&
                        !f.name.startsWith(VaultPaths.TRASH_GROUP_PREFIX) -> {
                    val uuid = f.nameWithoutExtension
                    val metaFile = VaultPaths.trashMetaFile(context, uuid)
                    if (!metaFile.exists()) {
                        EncLog.w(TAG, "trash blob $uuid has no .trashmeta — skipping")
                        continue
                    }
                    val meta = loadMeta(metaFile, expectedUuid = uuid) ?: continue
                    if (meta.kind != TrashKind.ENTRY) {
                        EncLog.w(TAG, "trash blob $uuid has non-ENTRY meta kind=${meta.kind} — skipping")
                        continue
                    }
                    entryRecords += TrashEntry(
                        meta = meta,
                        blobFile = f,
                        thumbFile = VaultPaths.trashThumbFile(context, uuid),
                        blobSizeBytes = f.length()
                    )
                }

                f.isDirectory && f.name.startsWith(VaultPaths.TRASH_ALBUM_PREFIX) -> {
                    val uuid = f.name.removePrefix(VaultPaths.TRASH_ALBUM_PREFIX)
                    val metaFile = VaultPaths.trashAlbumMetaFile(context, uuid)
                    if (!metaFile.exists()) {
                        EncLog.w(TAG, "trash album $uuid has no .trashmeta — skipping")
                        continue
                    }
                    val meta = loadMeta(metaFile, expectedUuid = uuid) ?: continue
                    if (meta.kind != TrashKind.ALBUM) {
                        EncLog.w(TAG, "trash album $uuid has non-ALBUM meta kind=${meta.kind} — skipping")
                        continue
                    }
                    val (entryCount, totalSize) = scanAlbumDir(f)
                    val record = TrashAlbum(
                        meta = meta,
                        albumDir = f,
                        entryCount = entryCount,
                        totalSizeBytes = totalSize
                    )
                    albumRecords += record
                    meta.originalGroupUuid?.let { gid ->
                        albumOriginalGroupUuids.getOrPut(gid) { mutableListOf() } += record
                    }
                }

                f.isDirectory && f.name.startsWith(VaultPaths.TRASH_GROUP_PREFIX) -> {
                    val uuid = f.name.removePrefix(VaultPaths.TRASH_GROUP_PREFIX)
                    val metaFile = VaultPaths.trashGroupMetaFile(context, uuid)
                    if (!metaFile.exists()) {
                        EncLog.w(TAG, "trash group $uuid has no .trashmeta — skipping")
                        continue
                    }
                    val meta = loadMeta(metaFile, expectedUuid = uuid) ?: continue
                    if (meta.kind != TrashKind.GROUP) {
                        EncLog.w(TAG, "trash group $uuid has non-GROUP meta kind=${meta.kind} — skipping")
                        continue
                    }

                    groupRecords += TrashGroup(
                        meta = meta,
                        groupDir = f,
                        albumCount = 0
                    )
                }

                else -> Unit
            }
        }

        val patchedGroups = groupRecords.map { rec ->
            val g = rec as TrashGroup
            val count = albumOriginalGroupUuids[g.uuid]?.size ?: 0
            g.copy(albumCount = count)
        }

        return (entryRecords + albumRecords + patchedGroups)
            .sortedByDescending { it.trashedAt }
    }

    private fun scanAlbumDir(dir: File): Pair<Int, Long> {
        val children = dir.listFiles() ?: return 0 to 0L
        var entries = 0
        var total = 0L
        for (c in children) {
            if (c.isFile) {
                total += c.length()
                if (c.name.endsWith(VaultPaths.BLOB_SUFFIX)) entries++
            }
        }
        return entries to total
    }

    fun moveToTrash(
        context: Context,
        entry: VaultEntry,
        originalAlbumNameHint: String?
    ): Boolean {
        val uuid = entry.uuid
        val now = System.currentTimeMillis()
        val meta = TrashMeta(
            uuid = uuid,
            kind = TrashKind.ENTRY,
            originalAlbumUuid = entry.albumUuid,
            originalAlbumNameHint = originalAlbumNameHint,
            originalMtime = entry.mtimeMillis,
            trashedAt = now
        )

        val destBlob = VaultPaths.trashBlobFile(context, uuid)
        val destThumb = VaultPaths.trashThumbFile(context, uuid)
        val destMeta = VaultPaths.trashMetaFile(context, uuid)
        val destEntryMeta = VaultPaths.trashEntryMetaFile(context, uuid)

        if (destBlob.exists() || destMeta.exists()) {

            EncLog.w(TAG, "trash slot $uuid already in use — aborting moveToTrash")
            return false
        }

        val tStart = System.currentTimeMillis()
        return try {
            saveMeta(destMeta, meta)

            if (!entry.blobFile.renameTo(destBlob)) {
                destMeta.delete()
                EncLog.w(TAG, "renameTo blob failed for $uuid → trash; meta cleaned up")
                return false
            }

            val thumbMoved = if (entry.thumbFile.exists()) {
                if (!entry.thumbFile.renameTo(destThumb)) {
                    EncLog.w(TAG, "renameTo thumb failed for $uuid → trash (continuing)")
                    false
                } else true
            } else false

            val srcEntryMeta = VaultPaths.entryMetaForBlob(entry.blobFile)
            if (srcEntryMeta.exists() && !srcEntryMeta.renameTo(destEntryMeta)) {
                EncLog.w(TAG, "renameTo entrymeta failed for $uuid → trash (continuing)")
            }

            val took = System.currentTimeMillis() - tStart
            EncLog.i(
                TAG,
                "moved $uuid to trash from album=${entry.albumUuid} " +
                    "(took=${took}ms blob=${destBlob.length()}B thumb=${if (thumbMoved) "ok" else "absent"})"
            )
            true
        } catch (t: Throwable) {
            EncLog.w(TAG, "moveToTrash($uuid) failed: ${t.javaClass.simpleName}: ${t.message}")

            destMeta.delete()
            false
        }
    }

    fun restore(context: Context, trashEntry: TrashEntry): String? {
        val uuid = trashEntry.uuid
        val targetAlbumUuid = if (
            File(VaultPaths.albumsRoot(context), trashEntry.meta.originalAlbumUuid).exists()
        ) {
            trashEntry.meta.originalAlbumUuid
        } else {
            VaultPaths.IMPORTED_ALBUM_UUID
        }

        val targetDir = VaultPaths.albumDir(context, targetAlbumUuid)
        val destBlob = File(targetDir, "$uuid${VaultPaths.BLOB_SUFFIX}")
        val destThumb = File(targetDir, "$uuid${VaultPaths.THUMB_SUFFIX}")
        val destEntryMeta = File(targetDir, "$uuid${VaultPaths.ENTRY_META_SUFFIX}")

        if (destBlob.exists()) {
            EncLog.w(TAG, "restore target $uuid already exists in $targetAlbumUuid — aborting")
            return null
        }

        val tStart = System.currentTimeMillis()
        return try {
            if (!trashEntry.blobFile.renameTo(destBlob)) {
                EncLog.w(TAG, "renameTo blob failed restoring $uuid → $targetAlbumUuid")
                return null
            }

            destBlob.setLastModified(trashEntry.meta.originalMtime)
            val thumbMoved = if (trashEntry.thumbFile.exists()) {
                if (!trashEntry.thumbFile.renameTo(destThumb)) {
                    EncLog.w(TAG, "renameTo thumb failed for restore $uuid (continuing)")
                    false
                } else true
            } else false

            val trashEntryMeta = VaultPaths.trashEntryMetaFile(context, uuid)
            if (trashEntryMeta.exists() && !trashEntryMeta.renameTo(destEntryMeta)) {
                EncLog.w(TAG, "renameTo entrymeta failed for restore $uuid (continuing)")
            }

            VaultPaths.trashMetaFile(context, uuid).delete()

            val took = System.currentTimeMillis() - tStart
            val onDisk = destBlob.exists()
            EncLog.i(
                TAG,
                "restored $uuid → album=$targetAlbumUuid " +
                    "(took=${took}ms blob=${if (onDisk) "${destBlob.length()}B" else "MISSING"} " +
                    "thumb=${if (thumbMoved) "ok" else "absent"})"
            )
            if (!onDisk) {
                EncLog.w(TAG, "restore($uuid) renameTo returned true but destBlob is missing!")
            }
            targetAlbumUuid
        } catch (t: Throwable) {
            EncLog.w(TAG, "restore($uuid) failed: ${t.javaClass.simpleName}: ${t.message}")
            null
        }
    }

    fun moveAlbumToTrash(
        context: Context,
        albumMeta: AlbumMeta,
        originalGroupUuid: String?
    ): Boolean {
        val uuid = albumMeta.uuid
        if (uuid == VaultPaths.IMPORTED_ALBUM_UUID) {

            EncLog.w(TAG, "refusing to trash system Imported album")
            return false
        }
        val now = System.currentTimeMillis()
        val meta = TrashMeta(
            uuid = uuid,
            kind = TrashKind.ALBUM,
            originalAlbumUuid = uuid,
            originalAlbumNameHint = null,
            originalMtime = 0L,
            trashedAt = now,
            originalGroupUuid = originalGroupUuid,
            originalName = albumMeta.name,
            albumUuids = emptyList()
        )

        val srcDir = File(VaultPaths.albumsRoot(context), uuid)
        val destDir = VaultPaths.trashAlbumDir(context, uuid)
        val destMeta = VaultPaths.trashAlbumMetaFile(context, uuid)

        if (!srcDir.exists()) {
            EncLog.w(TAG, "moveAlbumToTrash: album $uuid not on disk — aborting")
            return false
        }
        if (destDir.exists() || destMeta.exists()) {
            EncLog.w(TAG, "trash slot album-$uuid already in use — aborting")
            return false
        }

        val tStart = System.currentTimeMillis()
        return try {
            saveMeta(destMeta, meta)
            if (!srcDir.renameTo(destDir)) {
                destMeta.delete()
                EncLog.w(TAG, "renameTo failed for album $uuid → trash; meta cleaned up")
                return false
            }

            val (entryCount, totalBytes) = scanAlbumDir(destDir)
            val took = System.currentTimeMillis() - tStart
            EncLog.i(
                TAG,
                "moved album $uuid to trash " +
                    "(originalGroup=${originalGroupUuid ?: "top-level"} " +
                    "entries=$entryCount totalBytes=$totalBytes took=${took}ms)"
            )
            true
        } catch (t: Throwable) {
            EncLog.w(TAG, "moveAlbumToTrash($uuid) failed: ${t.javaClass.simpleName}: ${t.message}")
            destMeta.delete()
            false
        }
    }

    fun restoreAlbum(context: Context, trashAlbum: TrashAlbum): String? {
        val uuid = trashAlbum.uuid
        val targetDir = File(VaultPaths.albumsRoot(context), uuid)
        if (targetDir.exists()) {
            EncLog.w(TAG, "restoreAlbum: live dir already exists for $uuid — aborting")
            return null
        }

        val tStart = System.currentTimeMillis()
        return try {
            if (!trashAlbum.albumDir.renameTo(targetDir)) {
                EncLog.w(TAG, "renameTo failed restoring album $uuid")
                return null
            }

            VaultPaths.trashAlbumMetaFile(context, uuid).delete()

            val reattached = trashAlbum.meta.originalGroupUuid?.let { groupUuid ->
                val groupDirLive = File(VaultPaths.groupsRoot(context), groupUuid)
                if (groupDirLive.exists()) {
                    val groupsRepo = AlbumGroupsRepository(keystore)
                    groupsRepo.addAlbumsToGroup(
                        context,
                        groupUuid,
                        listOf(uuid)
                    )
                    "$groupUuid"
                } else "group-in-trash"
            } ?: "top-level"

            val (entryCount, totalBytes) = scanAlbumDir(targetDir)
            val onDisk = targetDir.exists() && targetDir.isDirectory
            val took = System.currentTimeMillis() - tStart
            EncLog.i(
                TAG,
                "restored album $uuid → $reattached " +
                    "(entries=$entryCount totalBytes=$totalBytes " +
                    "dir=${if (onDisk) "ok" else "MISSING"} took=${took}ms)"
            )
            if (!onDisk) {
                EncLog.w(TAG, "restoreAlbum($uuid) renameTo returned true but targetDir is missing!")
            }
            uuid
        } catch (t: Throwable) {
            EncLog.w(TAG, "restoreAlbum($uuid) failed: ${t.javaClass.simpleName}: ${t.message}")
            null
        }
    }

    fun moveGroupToTrash(context: Context, group: AlbumGroupMeta): Boolean {
        val uuid = group.uuid
        val now = System.currentTimeMillis()
        val meta = TrashMeta(
            uuid = uuid,
            kind = TrashKind.GROUP,
            originalAlbumUuid = uuid,
            originalAlbumNameHint = null,
            originalMtime = 0L,
            trashedAt = now,
            originalGroupUuid = null,
            originalName = group.name,
            albumUuids = group.albumUuids
        )

        val srcDir = File(VaultPaths.groupsRoot(context), uuid)
        val destDir = VaultPaths.trashGroupDir(context, uuid)
        val destMeta = VaultPaths.trashGroupMetaFile(context, uuid)

        if (!srcDir.exists()) {
            EncLog.w(TAG, "moveGroupToTrash: group $uuid not on disk — aborting")
            return false
        }
        if (destDir.exists() || destMeta.exists()) {
            EncLog.w(TAG, "trash slot group-$uuid already in use — aborting")
            return false
        }

        val tStart = System.currentTimeMillis()
        try {
            saveMeta(destMeta, meta)
            if (!srcDir.renameTo(destDir)) {
                destMeta.delete()
                EncLog.w(TAG, "renameTo failed for group $uuid → trash; meta cleaned up")
                return false
            }
        } catch (t: Throwable) {
            EncLog.w(TAG, "moveGroupToTrash($uuid) failed: ${t.javaClass.simpleName}: ${t.message}")
            destMeta.delete()
            return false
        }

        val albumsRepo = AlbumsRepository(keystore)
        var trashedCount = 0
        var skippedCount = 0
        group.albumUuids.forEach { albumUuid ->
            val albumMeta = albumsRepo.getAlbum(context, albumUuid)
            if (albumMeta == null) {

                skippedCount++
                return@forEach
            }
            if (moveAlbumToTrash(context, albumMeta, originalGroupUuid = uuid)) {
                trashedCount++
            } else {
                skippedCount++
            }
        }

        val took = System.currentTimeMillis() - tStart
        EncLog.i(
            TAG,
            "moved group $uuid to trash with $trashedCount/${group.albumUuids.size} member album(s) " +
                "(skipped=$skippedCount took=${took}ms)"
        )
        return true
    }

    fun restoreGroup(context: Context, trashGroup: TrashGroup): String? {
        val uuid = trashGroup.uuid
        val targetDir = File(VaultPaths.groupsRoot(context), uuid)
        if (targetDir.exists()) {
            EncLog.w(TAG, "restoreGroup: live dir already exists for $uuid — aborting")
            return null
        }

        val tStart = System.currentTimeMillis()
        try {
            if (!trashGroup.groupDir.renameTo(targetDir)) {
                EncLog.w(TAG, "renameTo failed restoring group $uuid")
                return null
            }
            VaultPaths.trashGroupMetaFile(context, uuid).delete()
        } catch (t: Throwable) {
            EncLog.w(TAG, "restoreGroup($uuid) failed: ${t.javaClass.simpleName}: ${t.message}")
            return null
        }

        val savedAlbumUuids = trashGroup.meta.albumUuids
        val groupsRepo = AlbumGroupsRepository(keystore)
        val trashByAlbumUuid: Map<String, TrashAlbum> =
            listAllRecords(context)
                .filterIsInstance<TrashAlbum>()
                .filter { it.meta.originalGroupUuid == uuid }
                .associateBy { it.uuid }

        var restored = 0
        var reattached = 0
        var missing = 0
        savedAlbumUuids.forEach { albumUuid ->
            val trashRecord = trashByAlbumUuid[albumUuid]
            if (trashRecord != null) {
                if (restoreAlbum(context, trashRecord) != null) {

                    restored++
                } else {
                    missing++
                }
            } else {

                val liveAlbumDir = File(VaultPaths.albumsRoot(context), albumUuid)
                if (liveAlbumDir.exists()) {
                    groupsRepo.addAlbumsToGroup(context, uuid, listOf(albumUuid))
                    reattached++
                } else {
                    missing++
                }
            }
        }

        val took = System.currentTimeMillis() - tStart
        val onDisk = targetDir.exists() && targetDir.isDirectory
        EncLog.i(
            TAG,
            "restored group $uuid: restored=$restored, reattached=$reattached, missing=$missing " +
                "(saved-list size=${savedAlbumUuids.size} dir=${if (onDisk) "ok" else "MISSING"} took=${took}ms)"
        )
        if (!onDisk) {
            EncLog.w(TAG, "restoreGroup($uuid) renameTo returned true but targetDir is missing!")
        }
        return uuid
    }

    fun purgePermanently(context: Context, trashEntry: TrashEntry): Boolean {
        val uuid = trashEntry.uuid
        var ok = true
        if (trashEntry.blobFile.exists() && !trashEntry.blobFile.delete()) {
            EncLog.w(TAG, "purge blob failed for $uuid")
            ok = false
        }
        if (trashEntry.thumbFile.exists() && !trashEntry.thumbFile.delete()) {
            EncLog.w(TAG, "purge thumb failed for $uuid (non-fatal)")
        }
        val metaFile = VaultPaths.trashMetaFile(context, uuid)
        if (metaFile.exists() && !metaFile.delete()) {
            EncLog.w(TAG, "purge meta failed for $uuid")
            ok = false
        }

        val entryMetaFile = VaultPaths.trashEntryMetaFile(context, uuid)
        if (entryMetaFile.exists() && !entryMetaFile.delete()) {
            EncLog.w(TAG, "purge entrymeta failed for $uuid (non-fatal)")
        }
        if (ok) EncLog.i(TAG, "purged $uuid permanently from trash")
        return ok
    }

    fun purgeAlbumPermanently(context: Context, trashAlbum: TrashAlbum): Boolean {
        val uuid = trashAlbum.uuid
        var ok = true
        if (trashAlbum.albumDir.exists() && !trashAlbum.albumDir.deleteRecursively()) {
            EncLog.w(TAG, "purge album dir failed for $uuid")
            ok = false
        }
        val metaFile = VaultPaths.trashAlbumMetaFile(context, uuid)
        if (metaFile.exists() && !metaFile.delete()) {
            EncLog.w(TAG, "purge album meta failed for $uuid")
            ok = false
        }
        if (ok) EncLog.i(TAG, "purged album $uuid permanently from trash")
        return ok
    }

    fun purgeGroupPermanently(context: Context, trashGroup: TrashGroup): Boolean {
        val uuid = trashGroup.uuid
        var ok = true
        if (trashGroup.groupDir.exists() && !trashGroup.groupDir.deleteRecursively()) {
            EncLog.w(TAG, "purge group dir failed for $uuid")
            ok = false
        }
        val metaFile = VaultPaths.trashGroupMetaFile(context, uuid)
        if (metaFile.exists() && !metaFile.delete()) {
            EncLog.w(TAG, "purge group meta failed for $uuid")
            ok = false
        }
        if (ok) EncLog.i(TAG, "purged group $uuid permanently from trash")
        return ok
    }

    fun purgePermanentlyRecord(context: Context, record: TrashRecord): Boolean = when (record) {
        is TrashEntry -> purgePermanently(context, record)
        is TrashAlbum -> purgeAlbumPermanently(context, record)
        is TrashGroup -> purgeGroupPermanently(context, record)
    }

    fun purgeOlderThan(
        context: Context,
        retentionMillis: Long = TrashMeta.TRASH_RETENTION_MILLIS
    ): Int {
        val now = System.currentTimeMillis()
        val cutoff = now - retentionMillis
        var purged = 0
        listAllRecords(context).forEach { record ->
            if (record.trashedAt < cutoff) {
                if (purgePermanentlyRecord(context, record)) purged++
            }
        }
        if (purged > 0) {
            EncLog.i(TAG, "auto-purged $purged trashed record(s) older than ${retentionMillis}ms")
        }
        return purged
    }

    fun emptyTrash(context: Context): Int {
        var purged = 0
        listAllRecords(context).forEach { record ->
            if (purgePermanentlyRecord(context, record)) purged++
        }
        EncLog.i(TAG, "emptied trash, purged=$purged")
        return purged
    }

    private fun loadMeta(file: File, expectedUuid: String): TrashMeta? = try {
        val bytes = blob.decryptToBytes(file)
        val parsed = TrashMeta.fromJsonBytes(bytes, expectedUuid)
        if (parsed == null) {
            EncLog.w(TAG, "trash meta $file failed to parse or uuid-mismatch")
        }
        parsed
    } catch (t: Throwable) {
        EncLog.w(TAG, "trash meta $file decrypt failed: ${t.javaClass.simpleName}: ${t.message}")
        null
    }

    private fun saveMeta(file: File, meta: TrashMeta) {
        blob.encrypt(ByteArrayInputStream(meta.toJsonBytes()), file)
    }

    companion object {
        private const val TAG = "TrashRepo"
    }
}
