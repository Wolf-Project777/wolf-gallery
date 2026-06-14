package dev.encgallery.gallery

import android.content.Context
import android.media.MediaDataSource
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.nativec.NativeCrypto
import dev.encgallery.storage.EncryptedFileBlob
import java.io.File
import java.io.RandomAccessFile

internal const val VIDEO_RAM_CAP_BYTES: Long = 200L * 1024 * 1024

private const val BLOB_OVERHEAD_BYTES: Long = 36L

private fun playbackTempDir(context: Context): File =
    File(context.cacheDir, "vid_playback").apply { mkdirs() }

internal fun secureDeleteFile(file: File) {
    if (!file.exists()) return
    try {
        RandomAccessFile(file, "rw").use { raf ->
            val len = raf.length()
            val zeros = ByteArray(256 * 1024)
            var written = 0L
            raf.seek(0)
            while (written < len) {
                val n = minOf(zeros.size.toLong(), len - written).toInt()
                raf.write(zeros, 0, n)
                written += n
            }
            raf.fd.sync()
        }
    } catch (t: Throwable) {
        EncLog.w("VideoPlayback", "zero-overwrite failed for ${file.name}: ${t.javaClass.simpleName}")
    }
    if (!file.delete() && file.exists()) {
        EncLog.w("VideoPlayback", "could not delete playback temp ${file.name}")
    }
}

internal fun sweepPlaybackTemps(context: Context) {
    val dir = File(context.cacheDir, "vid_playback")
    if (dir.isDirectory) dir.listFiles()?.forEach { secureDeleteFile(it) }
}

internal class InMemoryMediaDataSource(private var data: ByteArray?) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        val d = data ?: return -1
        if (position >= d.size) return -1

        val start = position.toInt()
        val n = minOf(size, d.size - start)
        System.arraycopy(d, start, buffer, offset, n)
        return n
    }

    override fun getSize(): Long = data?.size?.toLong() ?: 0L

    override fun close() {
        data?.let { NativeCrypto.secureZero(it) }
        data = null
    }
}

internal sealed interface VideoSource {
    fun close()

    class InMemory(val dataSource: InMemoryMediaDataSource) : VideoSource {
        override fun close() {
            try { dataSource.close() } catch (_: Throwable) {}
        }
    }

    class TempFile(val file: File) : VideoSource {
        override fun close() = secureDeleteFile(file)
    }
}

internal fun decryptVideoForPlayback(
    context: Context,
    blobFile: File,
    keystore: KeystoreAesGcm,
): VideoSource {
    val blob = EncryptedFileBlob(keystore)
    val plaintextSize = (blobFile.length() - BLOB_OVERHEAD_BYTES).coerceAtLeast(0)
    return if (plaintextSize <= VIDEO_RAM_CAP_BYTES) {
        VideoSource.InMemory(InMemoryMediaDataSource(blob.decryptToBytes(blobFile)))
    } else {
        val out = File(playbackTempDir(context), "play_${blobFile.nameWithoutExtension}.bin")
        blob.decryptToFile(blobFile, out)
        EncLog.i("VideoPlayback", "video > ${VIDEO_RAM_CAP_BYTES / (1024 * 1024)}MB → temp fallback (zeroed on close)")
        VideoSource.TempFile(out)
    }
}
