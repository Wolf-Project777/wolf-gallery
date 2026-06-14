package dev.encgallery.gallery

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import java.io.File

enum class EntryBatchOp { MOVE, COPY }

data class EntryBatchOutcome(
    val renamed: Map<String, String>,
    val removed: Set<String>
)

object EntryConflictResolver {

    private const val TAG = "EntryConflictResolver"

    suspend fun run(
        context: Context,
        incoming: List<VaultEntry>,
        destAlbumUuid: String,
        op: EntryBatchOp,
        keystore: KeystoreAesGcm,
        ask: suspend (fileName: String, suggested: String, nameTaken: (String) -> Boolean) -> FileConflictResolution,
        onProgress: (done: Int, total: Int) -> Unit
    ): EntryBatchOutcome {
        val entriesRepo = EntriesRepository(keystore)
        val total = incoming.size
        val renamed = HashMap<String, String>()
        val removed = HashSet<String>()

        val destEntries = VaultIndex.listEntriesInAlbum(context, destAlbumUuid)
        val destByUuid = destEntries.associateBy { it.uuid }
        val nameToUuid = HashMap<String, String>()
        for (e in destEntries) {
            val nm = entriesRepo.getMeta(e.blobFile)?.originalFilename
            if (nm != null) nameToUuid[nm.trim().lowercase()] = e.uuid
        }

        val taken = nameToUuid.keys.toMutableSet()
        val nameTaken: (String) -> Boolean = { cand -> taken.contains(cand.trim().lowercase()) }

        var blanket: FileConflictChoice? = null

        incoming.forEachIndexed { idx, entry ->
            val rawName = entriesRepo.getMeta(entry.blobFile)?.originalFilename
            val key = rawName?.trim()?.lowercase()
            val collides = rawName != null && key != null && taken.contains(key)

            if (collides) {
                val choice: FileConflictChoice
                val typedName: String?
                val pinned = blanket
                if (pinned != null) {
                    choice = pinned
                    typedName = null
                } else {
                    val suggested = nextFreeConflictName(rawName!!, taken)
                    val res = ask(rawName, suggested, nameTaken)
                    if (res.applyToAll && res.choice != FileConflictChoice.CANCEL) {
                        blanket = res.choice
                    }
                    choice = res.choice
                    typedName = res.newName
                }
                when (choice) {
                    FileConflictChoice.CANCEL -> {
                        EncLog.i(TAG, "batch $op cancelled at ${idx + 1}/$total")
                        return EntryBatchOutcome(renamed, removed)
                    }
                    FileConflictChoice.SKIP -> {

                    }
                    FileConflictChoice.REPLACE -> {
                        nameToUuid[key]?.let { existingUuid ->
                            destByUuid[existingUuid]?.let { existing ->
                                entriesRepo.deleteMeta(existing.blobFile)
                                deleteEntryOnDisk(existing)
                                removed.add(existingUuid)
                            }
                        }
                        perform(context, entry, destAlbumUuid, op, keystore, renameTo = null, entriesRepo)

                    }
                    FileConflictChoice.RENAME -> {
                        val finalName = typedName?.takeIf { it.isNotBlank() }
                            ?: nextFreeConflictName(rawName!!, taken)
                        val landed = perform(context, entry, destAlbumUuid, op, keystore, renameTo = finalName, entriesRepo)
                        if (landed != null) renamed[landed] = finalName
                        taken.add(finalName.trim().lowercase())
                    }
                }
            } else {
                perform(context, entry, destAlbumUuid, op, keystore, renameTo = null, entriesRepo)
                if (key != null) taken.add(key)
            }
            onProgress(idx + 1, total)
        }
        return EntryBatchOutcome(renamed, removed)
    }

    private fun perform(
        context: Context,
        entry: VaultEntry,
        destAlbumUuid: String,
        op: EntryBatchOp,
        keystore: KeystoreAesGcm,
        renameTo: String?,
        entriesRepo: EntriesRepository
    ): String? {
        val landedUuid: String? = when (op) {
            EntryBatchOp.MOVE ->
                if (EntryOps.moveEntry(context, entry, destAlbumUuid)) entry.uuid else null
            EntryBatchOp.COPY ->
                EntryOps.copyEntry(context, entry, destAlbumUuid, keystore)
        }
        if (renameTo != null && landedUuid != null) {
            val destBlob = File(
                VaultPaths.albumDir(context, destAlbumUuid),
                "$landedUuid${VaultPaths.BLOB_SUFFIX}"
            )
            entriesRepo.rename(destBlob, renameTo)
        }
        return landedUuid
    }

}

fun nextFreeConflictName(original: String, taken: Set<String>): String {
    val dot = original.lastIndexOf('.')
    val stem = if (dot > 0) original.substring(0, dot) else original
    val ext = if (dot > 0) original.substring(dot) else ""
    var n = 1
    while (true) {
        val candidate = "$stem +$n$ext"
        if (!taken.contains(candidate.trim().lowercase())) return candidate
        n++
    }
}
