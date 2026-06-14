package dev.encgallery.gallery

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object BlobMigrator {

    private const val TAG = "BlobMigrator"

    private const val MAX_AUTO_MIGRATE_BYTES = 64L * 1024 * 1024

    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val failed = ConcurrentHashMap.newKeySet<String>()

    suspend fun migrateOnOpen(context: Context, entry: VaultEntry) {
        val uuid = entry.uuid
        if (uuid in failed) return
        if (entry.blobSizeBytes > MAX_AUTO_MIGRATE_BYTES) return
        if (!inFlight.add(uuid)) return
        try {
            withContext(Dispatchers.IO) {
                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                val blob = EncryptedFileBlob(keystore)
                val scratch = File(context.cacheDir, "blob_migrate").apply { mkdirs() }

                val ok = ThumbnailLoader.gated {
                    blob.migrateV1ToEnvelope(entry.blobFile, scratch)
                }
                if (!ok) {
                    failed.add(uuid)
                    EncLog.w(TAG, "on-open migrate did not complete for $uuid (kept v1)")
                }
            }
        } catch (t: Throwable) {
            failed.add(uuid)
            EncLog.w(TAG, "on-open migrate threw for $uuid: ${t.javaClass.simpleName}")
        } finally {
            inFlight.remove(uuid)
        }
    }
}
