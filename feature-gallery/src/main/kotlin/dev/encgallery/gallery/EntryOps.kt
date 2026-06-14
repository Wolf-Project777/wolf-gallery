package dev.encgallery.gallery

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

object EntryOps {

    private const val TAG = "EntryOps"

    fun moveEntry(
        context: Context,
        entry: VaultEntry,
        destAlbumUuid: String
    ): Boolean {
        if (entry.albumUuid == destAlbumUuid) {
            EncLog.w(TAG, "moveEntry: src == dest (${entry.uuid}), no-op")
            return false
        }

        val destDir = VaultPaths.albumDir(context, destAlbumUuid)
        val destBlob = File(destDir, "${entry.uuid}${VaultPaths.BLOB_SUFFIX}")
        val destThumb = File(destDir, "${entry.uuid}${VaultPaths.THUMB_SUFFIX}")
        val destEntryMeta = File(destDir, "${entry.uuid}${VaultPaths.ENTRY_META_SUFFIX}")

        if (destBlob.exists()) {

            EncLog.w(
                TAG,
                "moveEntry: dest blob ${destBlob.name} already exists in $destAlbumUuid"
            )
            return false
        }

        val blobOk = entry.blobFile.renameTo(destBlob)
        if (!blobOk) {
            EncLog.w(
                TAG,
                "moveEntry: renameTo failed for ${entry.uuid} → $destAlbumUuid"
            )
            return false
        }

        if (entry.thumbFile.exists()) {
            if (!entry.thumbFile.renameTo(destThumb)) {
                EncLog.w(
                    TAG,
                    "moveEntry: thumb renameTo failed for ${entry.uuid}; main blob already moved"
                )
            }
        }

        val srcEntryMeta = VaultPaths.entryMetaForBlob(entry.blobFile)
        if (srcEntryMeta.exists()) {
            if (!srcEntryMeta.renameTo(destEntryMeta)) {
                EncLog.w(
                    TAG,
                    "moveEntry: entrymeta renameTo failed for ${entry.uuid}"
                )
            }
        }

        EncLog.i(
            TAG,
            "moved ${entry.uuid}: ${entry.albumUuid} → $destAlbumUuid"
        )
        return true
    }

    fun copyEntry(
        context: Context,
        entry: VaultEntry,
        destAlbumUuid: String,
        keystore: KeystoreAesGcm
    ): String? {
        val newUuid = UUID.randomUUID().toString()
        val destDir = VaultPaths.albumDir(context, destAlbumUuid)
        val destBlob = File(destDir, "$newUuid${VaultPaths.BLOB_SUFFIX}")
        val destThumb = File(destDir, "$newUuid${VaultPaths.THUMB_SUFFIX}")
        val destEntryMeta = File(destDir, "$newUuid${VaultPaths.ENTRY_META_SUFFIX}")
        val blob = EncryptedFileBlob(keystore)
        val entries = EntriesRepository(keystore)

        return try {
            val plain = blob.decryptToBytes(entry.blobFile)
            blob.encrypt(ByteArrayInputStream(plain), destBlob)

            if (entry.thumbFile.exists()) {
                val thumbPlain = blob.decryptToBytes(entry.thumbFile)
                blob.encrypt(ByteArrayInputStream(thumbPlain), destThumb)
            }

            val srcMeta = entries.getMeta(entry.blobFile)
            if (srcMeta != null) {
                entries.setMeta(
                    destBlob,
                    srcMeta.copy(uuid = newUuid)
                )
            }

            EncLog.i(
                TAG,
                "copied ${entry.uuid} → $newUuid: ${entry.albumUuid} → $destAlbumUuid"
            )
            newUuid
        } catch (t: Throwable) {

            if (destBlob.exists() && !destBlob.delete()) {
                EncLog.w(TAG, "copyEntry cleanup: blob delete failed ${destBlob.name}")
            }
            if (destThumb.exists() && !destThumb.delete()) {
                EncLog.w(TAG, "copyEntry cleanup: thumb delete failed ${destThumb.name}")
            }
            if (destEntryMeta.exists() && !destEntryMeta.delete()) {
                EncLog.w(TAG, "copyEntry cleanup: entrymeta delete failed ${destEntryMeta.name}")
            }
            EncLog.e(
                TAG,
                "copyEntry failed for ${entry.uuid} → $destAlbumUuid: ${t.javaClass.simpleName}: ${t.message}"
            )
            null
        }
    }
}
