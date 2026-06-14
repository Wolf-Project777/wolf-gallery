package dev.encgallery.gallery

import dev.encgallery.logging.EncLog
import java.io.File

object MimeSniffer {

    private const val TAG = "MimeSniffer"

    const val HEADER_BYTES = 16

    private val VIDEO_EXTENSIONS =
        setOf("mp4", "m4v", "mkv", "webm", "3gp", "3gpp", "mov", "avi")

    private const val LIKELY_VIDEO_MIN_BYTES = 150L * 1024 * 1024

    fun isLikelyVideo(name: String?, blobSizeBytes: Long): Boolean {
        val ext = name?.substringAfterLast('.', "")?.lowercase()
        if (ext != null && ext.isNotEmpty() && ext in VIDEO_EXTENSIONS) return true
        return blobSizeBytes > LIKELY_VIDEO_MIN_BYTES
    }

    fun sniff(file: File): String? = try {
        file.inputStream().use { stream ->
            val header = ByteArray(HEADER_BYTES)
            val n = stream.read(header)
            if (n < 8) null else match(header, n)
        }
    } catch (t: Throwable) {
        EncLog.w(TAG, "sniff(file) failed: ${t.javaClass.simpleName}")
        null
    }

    fun sniff(bytes: ByteArray): String? =
        if (bytes.size < 8) null else match(bytes, minOf(bytes.size, HEADER_BYTES))

    private fun match(header: ByteArray, readBytes: Int): String? {

        if (readBytes >= 3 &&
            header[0] == 0xFF.toByte() &&
            header[1] == 0xD8.toByte() &&
            header[2] == 0xFF.toByte()
        ) return "image/jpeg"

        if (readBytes >= 8 &&
            header[0] == 0x89.toByte() &&
            header[1] == 0x50.toByte() &&
            header[2] == 0x4E.toByte() &&
            header[3] == 0x47.toByte() &&
            header[4] == 0x0D.toByte() &&
            header[5] == 0x0A.toByte() &&
            header[6] == 0x1A.toByte() &&
            header[7] == 0x0A.toByte()
        ) return "image/png"

        if (readBytes >= 6 &&
            header[0] == 'G'.code.toByte() &&
            header[1] == 'I'.code.toByte() &&
            header[2] == 'F'.code.toByte() &&
            header[3] == '8'.code.toByte() &&
            (header[4] == '7'.code.toByte() || header[4] == '9'.code.toByte()) &&
            header[5] == 'a'.code.toByte()
        ) return "image/gif"

        if (readBytes >= 2 &&
            header[0] == 'B'.code.toByte() &&
            header[1] == 'M'.code.toByte()
        ) return "image/bmp"

        if (readBytes >= 12 &&
            header[0] == 'R'.code.toByte() &&
            header[1] == 'I'.code.toByte() &&
            header[2] == 'F'.code.toByte() &&
            header[3] == 'F'.code.toByte() &&
            header[8] == 'W'.code.toByte() &&
            header[9] == 'E'.code.toByte() &&
            header[10] == 'B'.code.toByte() &&
            header[11] == 'P'.code.toByte()
        ) return "image/webp"

        if (readBytes >= 12 &&
            header[4] == 'f'.code.toByte() &&
            header[5] == 't'.code.toByte() &&
            header[6] == 'y'.code.toByte() &&
            header[7] == 'p'.code.toByte()
        ) {
            val brand = String(header, 8, 4, Charsets.ISO_8859_1)
            return when (brand) {
                "heic", "heix", "hevc", "hevx", "mif1", "msf1", "heim", "heis", "hevm", "hevs"
                    -> "image/heic"
                "avif", "avis" -> "image/avif"
                "isom", "iso2", "iso3", "iso4", "iso5", "iso6",
                "mp41", "mp42", "M4V ", "M4VH", "M4VP" -> "video/mp4"
                "3gp4", "3gp5", "3gp6", "3gp7", "3g2a", "3g2b", "3g2c" -> "video/3gpp"

                else -> "video/mp4"
            }
        }

        if (readBytes >= 4 &&
            header[0] == 0x1A.toByte() &&
            header[1] == 0x45.toByte() &&
            header[2] == 0xDF.toByte() &&
            header[3] == 0xA3.toByte()
        ) {

            return "video/x-matroska"
        }

        return null
    }
}
