package dev.encgallery.gallery

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob

enum class WallpaperTarget { HOME, LOCK, BOTH }

object WallpaperOps {
    private const val TAG = "WallpaperOps"

    fun setFromEntry(
        context: Context,
        entry: VaultEntry,
        target: WallpaperTarget,
        keystore: KeystoreAesGcm
    ): Boolean {
        return try {
            val plain = EncryptedFileBlob(keystore).decryptToBytes(entry.blobFile)
            val bitmap = BitmapFactory.decodeByteArray(plain, 0, plain.size)
                ?: run {
                    EncLog.w(TAG, "decodeByteArray returned null for ${entry.uuid}")
                    return false
                }
            val ok = setBitmap(context, bitmap, target)
            if (ok) EncLog.i(TAG, "wallpaper set (whole-image): entry=${entry.uuid} target=$target")
            ok
        } catch (t: Throwable) {
            EncLog.e(
                TAG,
                "setFromEntry failed for ${entry.uuid}: ${t.javaClass.simpleName}: ${t.message}"
            )
            false
        }
    }

    fun setBitmap(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperTarget
    ): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context.applicationContext)
            val flags = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH ->
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            wm.setBitmap(bitmap, null, true, flags)
            EncLog.i(
                TAG,
                "wallpaper set (cropped ${bitmap.width}x${bitmap.height}) target=$target"
            )
            true
        } catch (t: Throwable) {
            EncLog.e(
                TAG,
                "setBitmap failed: ${t.javaClass.simpleName}: ${t.message}"
            )
            false
        }
    }
}
