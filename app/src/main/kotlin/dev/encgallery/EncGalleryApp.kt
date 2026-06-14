package dev.encgallery

import android.app.Application
import dev.encgallery.crypto.VaultDataKey
import dev.encgallery.logging.EncLog
import dev.encgallery.settings.AppSettings

class EncGalleryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        EncLog.init(filesDir = filesDir, enabled = false)
        AppSettings.init(this)

        VaultDataKey.init(this)

        EncLog.i("App", "EncGalleryApp.onCreate — process started")
        EncLog.d("App", "filesDir=$filesDir")
        EncLog.d("App", "loggingEnabled=${AppSettings.loggingEnabled.value}")
        EncLog.d("App", "screenshotsAllowed=${AppSettings.screenshotsAllowed.value}")
    }
}
