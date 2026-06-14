package dev.encgallery.gallery

import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayInputStream
import java.io.File

class EntriesRepository(private val keystore: KeystoreAesGcm) {

    private val blob = EncryptedFileBlob(keystore)

    fun getMeta(blobFile: File): EntryMeta? {
        val metaFile = VaultPaths.entryMetaForBlob(blobFile)
        if (!metaFile.exists()) return null
        val expectedUuid = blobFile.nameWithoutExtension
        return try {
            val bytes = blob.decryptToBytes(metaFile)
            val parsed = EntryMeta.fromJsonBytes(bytes, expectedUuid)
            if (parsed == null) {
                EncLog.w(TAG, "entrymeta $expectedUuid failed to parse or uuid-mismatch")
            }
            parsed
        } catch (t: Throwable) {
            EncLog.w(
                TAG,
                "entrymeta $expectedUuid decrypt failed: ${t.javaClass.simpleName}: ${t.message}"
            )
            null
        }
    }

    fun setMeta(blobFile: File, meta: EntryMeta): Boolean {
        val metaFile = VaultPaths.entryMetaForBlob(blobFile)
        return try {

            blob.encryptEnvelope(ByteArrayInputStream(meta.toJsonBytes()), metaFile)
            EncLog.i(
                TAG,
                "entrymeta saved for ${meta.uuid} (name-len=${meta.originalFilename?.length ?: 0})"
            )
            true
        } catch (t: Throwable) {
            EncLog.w(
                TAG,
                "entrymeta save failed for ${meta.uuid}: ${t.javaClass.simpleName}: ${t.message}"
            )
            false
        }
    }

    fun saveImported(
        blobFile: File,
        uuid: String,
        originalFilename: String?,
        importedAt: Long = System.currentTimeMillis()
    ): Boolean = setMeta(
        blobFile,
        EntryMeta(uuid = uuid, originalFilename = originalFilename, importedAt = importedAt)
    )

    fun rename(blobFile: File, originalFilename: String?): Boolean {
        val expectedUuid = blobFile.nameWithoutExtension
        val existing = getMeta(blobFile)
        val updated = if (existing != null) {
            existing.copy(originalFilename = originalFilename)
        } else {
            EntryMeta(
                uuid = expectedUuid,
                originalFilename = originalFilename,
                importedAt = System.currentTimeMillis()
            )
        }
        return setMeta(blobFile, updated)
    }

    fun deleteMeta(blobFile: File): Boolean {
        val metaFile = VaultPaths.entryMetaForBlob(blobFile)
        if (!metaFile.exists()) return true
        val ok = metaFile.delete()
        if (!ok) EncLog.w(TAG, "entrymeta delete failed for ${blobFile.nameWithoutExtension}")
        return ok
    }

    fun batchResolve(entries: List<VaultEntry>): Map<String, String?> {
        if (entries.isEmpty()) return emptyMap()
        val out = HashMap<String, String?>(entries.size)
        for (entry in entries) {
            out[entry.uuid] = getMeta(entry.blobFile)?.originalFilename
        }
        return out
    }

    companion object {
        private const val TAG = "EntriesRepository"
    }
}
