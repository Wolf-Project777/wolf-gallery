package dev.encgallery.gallery

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

object ThumbnailMigration {

    private const val TAG = "ThumbnailMigration"

    const val CURRENT_GEN = 6

    suspend fun regenerateAll(context: Context, keystore: KeystoreAesGcm): Int {
        val entries = VaultIndex.listAllEntries(context)
        var done = 0
        for (entry in entries) {
            currentCoroutineContext().ensureActive()

            if (ThumbnailLoader.ensureThumbBytes(context, entry, keystore, forceRegen = true) != null) {
                done++
            }
        }
        EncLog.i(TAG, "regenerated $done/${entries.size} thumbnails at gen $CURRENT_GEN")
        return done
    }
}
