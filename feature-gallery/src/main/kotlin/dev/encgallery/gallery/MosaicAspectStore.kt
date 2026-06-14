package dev.encgallery.gallery

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import java.io.ByteArrayInputStream
import java.io.File

object MosaicAspectStore {

    private const val TAG = "MosaicAspectStore"
    private const val FILE_NAME = "mosaic_aspects.enc"

    @Volatile private var loaded = false
    @Volatile private var dirty = false

    private fun file(context: Context): File =
        File(VaultPaths.vaultDir(context), FILE_NAME)

    fun loadInto(context: Context, keystore: KeystoreAesGcm) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val f = file(context)
            if (f.exists()) {
                try {
                    val text = String(EncryptedFileBlob(keystore).decryptToBytes(f), Charsets.UTF_8)
                    var n = 0
                    for (line in text.lineSequence()) {
                        val eq = line.indexOf('=')
                        if (eq <= 0) continue
                        val uuid = line.substring(0, eq)
                        val ratio = line.substring(eq + 1).toFloatOrNull() ?: continue
                        if (ratio.isFinite() && ratio > 0f) {

                            GallerySession.mosaicAspectRatios.putIfAbsent(uuid, ratio)
                            n++
                        }
                    }
                    EncLog.d(TAG, "loaded $n aspect ratios")
                } catch (t: Throwable) {
                    EncLog.w(TAG, "load failed: ${t.javaClass.simpleName}: ${t.message}")
                }
            }
            loaded = true
        }
    }

    fun put(uuid: String, ratio: Float) {
        if (!ratio.isFinite() || ratio <= 0f) return

        if (GallerySession.mosaicAspectRatios.putIfAbsent(uuid, ratio) == null) {
            dirty = true
        }
    }

    fun forget(uuid: String) {
        if (GallerySession.mosaicAspectRatios.remove(uuid) != null) dirty = true
    }

    fun backfill(context: Context, keystore: KeystoreAesGcm, entries: List<VaultEntry>) {
        var added = 0
        for (entry in entries) {
            if (GallerySession.mosaicAspectRatios.containsKey(entry.uuid)) continue
            val ratio = try {
                ThumbnailLoader.aspectRatioOf(context, entry, keystore)
            } catch (t: Throwable) {
                null
            } ?: continue
            put(entry.uuid, ratio)
            added++
        }
        if (added > 0) {
            EncLog.d(TAG, "backfilled $added missing aspect ratios")
            flush(context, keystore)
        }
    }

    fun flush(context: Context, keystore: KeystoreAesGcm) {
        if (!dirty) return
        synchronized(this) {
            if (!dirty) return

            val sb = StringBuilder()
            for ((uuid, ratio) in GallerySession.mosaicAspectRatios) {
                if (uuid.isEmpty() || !ratio.isFinite() || ratio <= 0f) continue
                sb.append(uuid).append('=').append(ratio).append('\n')
            }
            val bytes = sb.toString().toByteArray(Charsets.UTF_8)
            val target = file(context)
            val tmp = File(target.parentFile, "$FILE_NAME.tmp")
            try {
                EncryptedFileBlob(keystore).encryptEnvelope(ByteArrayInputStream(bytes), tmp)
                if (!tmp.renameTo(target)) {
                    target.delete()
                    if (!tmp.renameTo(target)) {
                        EncLog.w(TAG, "could not place aspect store")
                        tmp.delete()
                        return
                    }
                }
                dirty = false
                EncLog.d(TAG, "flushed ${GallerySession.mosaicAspectRatios.size} aspect ratios")
            } catch (t: Throwable) {
                EncLog.w(TAG, "flush failed: ${t.javaClass.simpleName}: ${t.message}")
                if (tmp.exists()) tmp.delete()
            }
        }
    }

    fun resetLoaded() {
        loaded = false
        dirty = false
    }
}
