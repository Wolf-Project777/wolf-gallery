package dev.encgallery.session

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.crypto.VaultDataKey
import dev.encgallery.crypto.VerifierStore
import dev.encgallery.featuresettings.SettingsSession
import dev.encgallery.gallery.GallerySession
import dev.encgallery.gallery.ThumbnailLoader
import dev.encgallery.gallery.VaultPaths
import dev.encgallery.logging.EncLog
import dev.encgallery.settings.AppSettings

fun wipeVault(ctx: Context) {
    EncLog.w("Wipe", "wipeVault triggered")

    val lockTimeoutBefore = AppSettings.lockTimeoutMinutes.value

    VerifierStore(ctx).delete()
    KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS).delete()

    VaultDataKey.reset()

    dev.encgallery.gallery.ShareMemoryStore.clear()

    val vaultDir = VaultPaths.vaultDir(ctx)
    val files = vaultDir.listFiles()
    if (files == null) {
        EncLog.w("Wipe", "vaultDir.listFiles() returned null at ${vaultDir.absolutePath}")
    } else {
        var deleted = 0
        var failed = 0
        for (f in files) {
            if (f.deleteRecursively()) deleted++ else {
                failed++
                EncLog.w("Wipe", "could not delete vault entry ${f.name}")
            }
        }
        EncLog.w("Wipe", "vault contents: deleted=$deleted top-level entries, failed=$failed")
    }

    ThumbnailLoader.clearCache()

    GallerySession.reset()

    SettingsSession.reset()

    AppSettings.wipeVaultPrefs()
    SessionState.unlocked.value = false
    SessionState.backgroundedAt = null

    val lockTimeoutAfter = AppSettings.lockTimeoutMinutes.value
    EncLog.i(
        "Wipe",
        "lockTimeoutMinutes preservation: before=$lockTimeoutBefore, after=$lockTimeoutAfter"
    )

    EncLog.w("Wipe", "vault wiped")
}
