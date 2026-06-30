package dev.encgallery.storage

import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.crypto.VaultDataKey
import dev.encgallery.logging.EncLog
import dev.encgallery.nativec.NativeCrypto
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher

class EncryptedFileBlob(private val keystore: KeystoreAesGcm) {

    fun encrypt(input: InputStream, output: File) {
        val (cipher, iv) = keystore.newEncryptCipher()
        encryptStream(input, output, VERSION_1, cipher, iv)
    }

    fun encryptEnvelope(input: InputStream, output: File) {
        val key = VaultDataKey.secretKey()
        val (cipher, iv) = VaultDataKey.newEncryptCipher(key)
        encryptStream(input, output, VERSION_2, cipher, iv)
    }

    private fun encryptStream(
        input: InputStream,
        output: File,
        version: Byte,
        cipher: Cipher,
        iv: ByteArray
    ) {
        val tmp = File(output.parentFile, "${output.name}.tmp")
        try {
            val fos = tmp.outputStream()
            BufferedOutputStream(fos, STREAM_BUFFER_BYTES).use { out ->
                out.write(MAGIC)
                out.write(byteArrayOf(version))
                out.write(RESERVED_ZEROS)
                out.write(iv)

                val buf = ByteArray(STREAM_BUFFER_BYTES)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    val ct = cipher.update(buf, 0, n)
                    if (ct != null && ct.isNotEmpty()) out.write(ct)
                }

                val finalCt = cipher.doFinal()
                if (finalCt.isNotEmpty()) out.write(finalCt)
                out.flush()

                fos.fd.sync()
            }
            java.nio.file.Files.move(
                tmp.toPath(), output.toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
        } catch (t: Throwable) {

            if (tmp.exists() && !tmp.delete()) {
                EncLog.w(TAG, "encrypt() failed AND temp ${tmp.name} could not be deleted")
            }
            throw t
        }
    }

    fun decryptToFile(input: File, output: File) {
        try {
            BufferedOutputStream(output.outputStream(), STREAM_BUFFER_BYTES).use { sink ->
                decryptInto(input, sink)
            }
        } catch (t: Throwable) {
            if (!output.delete() && output.exists()) {
                EncLog.w(
                    TAG,
                    "decryptToFile() failed AND partial output ${output.name} could not be deleted"
                )
            }
            throw t
        }
    }

    fun decryptToStream(input: File, output: OutputStream) {
        decryptInto(input, output)
    }

    fun decryptToBytes(input: File): ByteArray {

        val sink = ByteArrayOutputStream()
        decryptInto(input, sink)
        return sink.toByteArray()
    }

    private fun decryptInto(input: File, sink: OutputStream) {
        val raw = BufferedInputStream(input.inputStream(), STREAM_BUFFER_BYTES)
        try {
            val data = DataInputStream(raw)
            val (version, iv) = parseHeaderAndReadIv(data, input.name)

            val cipher = when (version) {
                VERSION_1 -> keystore.newDecryptCipher(iv)
                VERSION_2 -> VaultDataKey.newDecryptCipher(VaultDataKey.secretKey(), iv)
                else -> throw BlobFormatException(
                    "blob ${input.name} unsupported version $version"
                )
            }
            val buf = ByteArray(STREAM_BUFFER_BYTES)
            while (true) {

                val n = data.read(buf)
                if (n < 0) break
                val pt = cipher.update(buf, 0, n)
                if (pt != null && pt.isNotEmpty()) sink.write(pt)
            }
            try {
                val finalPt = cipher.doFinal()
                if (finalPt.isNotEmpty()) sink.write(finalPt)
            } catch (e: AEADBadTagException) {
                throw IOException("auth tag verification failed for ${input.name}", e)
            } catch (e: BadPaddingException) {

                throw IOException("auth tag verification failed for ${input.name}", e)
            }
        } finally {
            raw.close()
        }
    }

    private fun parseHeaderAndReadIv(data: DataInputStream, name: String): Pair<Byte, ByteArray> {
        val magic = ByteArray(MAGIC.size)
        try {
            data.readFully(magic)
        } catch (e: EOFException) {
            throw BlobFormatException("blob $name truncated before magic")
        }
        if (!magic.contentEquals(MAGIC)) {
            throw BlobFormatException(
                "blob $name bad magic: expected EGBL, got ${magic.toHex()}"
            )
        }

        val version = data.readByte()
        if (version != VERSION_1 && version != VERSION_2) {
            throw BlobFormatException(
                "blob $name unsupported version $version (this build understands v1 and v2)"
            )
        }

        val reserved = ByteArray(RESERVED_ZEROS.size)
        data.readFully(reserved)
        if (!reserved.all { it == 0.toByte() }) {
            throw BlobFormatException(
                "blob $name reserved bytes are non-zero — possibly written by future build"
            )
        }

        val iv = ByteArray(IV_LEN_BYTES)
        data.readFully(iv)
        return version to iv
    }

    fun migrateV1ToEnvelope(blobFile: File, scratchDir: File): Boolean {
        val version = try {
            readVersionByte(blobFile)
        } catch (t: Throwable) {
            EncLog.w(TAG, "migrate: unreadable header ${blobFile.name}: ${t.javaClass.simpleName}")
            return false
        }
        if (version == VERSION_2) return true
        if (version != VERSION_1) return false

        if (!scratchDir.exists()) scratchDir.mkdirs()
        val plainTmp = File(scratchDir, "mig_${blobFile.nameWithoutExtension}.plain")
        val v2Tmp = File(blobFile.parentFile, "${blobFile.name}.v2tmp")
        try {
            decryptToFile(blobFile, plainTmp)
            val shaPlain = sha256OfFile(plainTmp)
            plainTmp.inputStream().use { encryptEnvelope(it, v2Tmp) }
            val shaV2 = sha256OfDecryptedBlob(v2Tmp)
            if (!shaPlain.contentEquals(shaV2)) {
                EncLog.w(TAG, "migrate VERIFY MISMATCH ${blobFile.name} — keeping v1")
                return false
            }
            return try {
                java.nio.file.Files.move(
                    v2Tmp.toPath(), blobFile.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
                EncLog.i(TAG, "migrated ${blobFile.name} v1→v2 (verified)")
                true
            } catch (t: Throwable) {
                EncLog.w(TAG, "migrate atomic-move failed ${blobFile.name}: ${t.javaClass.simpleName} — keeping v1")
                false
            }
        } catch (t: Throwable) {
            EncLog.w(TAG, "migrate v1→v2 failed ${blobFile.name}: ${t.javaClass.simpleName}: ${t.message} — v1 intact")
            return false
        } finally {
            plainTmp.delete()
            if (v2Tmp.exists()) v2Tmp.delete()
        }
    }

    private fun readVersionByte(file: File): Byte {
        DataInputStream(BufferedInputStream(file.inputStream())).use { data ->
            val magic = ByteArray(MAGIC.size)
            data.readFully(magic)
            if (!magic.contentEquals(MAGIC)) {
                throw BlobFormatException("blob ${file.name} bad magic")
            }
            return data.readByte()
        }
    }

    private fun sha256OfFile(file: File): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(file.inputStream(), STREAM_BUFFER_BYTES).use { ins ->
            val buf = ByteArray(STREAM_BUFFER_BYTES)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest()
    }

    private fun sha256OfDecryptedBlob(blob: File): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val sink = object : OutputStream() {
            override fun write(b: Int) { md.update(b.toByte()) }
            override fun write(b: ByteArray, off: Int, len: Int) { md.update(b, off, len) }
        }
        decryptInto(blob, sink)
        return md.digest()
    }

    class BlobFormatException(message: String) : IOException(message)

    companion object {
        private const val TAG = "EncFileBlob"

        private val MAGIC = byteArrayOf(0x45, 0x47, 0x42, 0x4C)

        private const val VERSION_1: Byte = 0x01

        private const val VERSION_2: Byte = 0x02

        private val RESERVED_ZEROS = byteArrayOf(0x00, 0x00, 0x00)

        private const val IV_LEN_BYTES = 12

        private const val TAG_BYTES = 16

        const val HEADER_BYTES =
            4   + 1   + 3   + IV_LEN_BYTES

        const val OVERHEAD_BYTES = HEADER_BYTES + TAG_BYTES

        private const val STREAM_BUFFER_BYTES = 256 * 1024

        private const val SELF_TEST_ALIAS = "enc_gallery_test_blob_v1"

        fun runSelfTest(
            context: android.content.Context,
            tempDir: File,
            log: (String) -> Unit
        ): Boolean {
            val test = KeystoreAesGcm(SELF_TEST_ALIAS)
            test.delete()
            val plainFile = File(tempDir, "blob_selftest_plain.bin")
            val encFile = File(tempDir, "blob_selftest_enc.bin")
            val decFile = File(tempDir, "blob_selftest_dec.bin")
            val tamperedFile = File(tempDir, "blob_selftest_tampered.bin")
            val badMagicFile = File(tempDir, "blob_selftest_badmagic.bin")
            val badVersionFile = File(tempDir, "blob_selftest_badver.bin")

            val plaintextSize = 2 * 1024 * 1024
            var allPass = true

            try {
                test.ensureExists(context, strongBoxPreferred = true)

                val plaintextBytes = ByteArray(plaintextSize)
                SecureRandom().nextBytes(plaintextBytes)
                plainFile.writeBytes(plaintextBytes)
                val plainSha = sha256(plaintextBytes)
                log("plaintext: ${plaintextSize / 1024} KiB, sha256=${plainSha.toHex(8)}…")

                val tEnc = System.currentTimeMillis()
                val blob = EncryptedFileBlob(test)
                plainFile.inputStream().use { src ->
                    blob.encrypt(src, encFile)
                }
                val encDuration = System.currentTimeMillis() - tEnc
                val expectedSize = plaintextSize.toLong() + OVERHEAD_BYTES
                val sizeOk = encFile.length() == expectedSize
                log(
                    "(1) encrypted size=${encFile.length()} bytes (expected $expectedSize), " +
                        "took ${encDuration}ms, ok=$sizeOk"
                )
                if (!sizeOk) allPass = false

                val headerBytes = encFile.inputStream().use { it.readNBytesCompat(HEADER_BYTES) }
                val magicOk = headerBytes.copyOfRange(0, 4).contentEquals(MAGIC)
                val versionOk = headerBytes[4] == VERSION_1
                log("(1) header: magic=${magicOk}, version=$versionOk")
                if (!magicOk || !versionOk) allPass = false

                val ctSlice = encFile.inputStream().use {
                    it.skip(HEADER_BYTES.toLong())
                    it.readNBytesCompat(256)
                }
                val ptSlice = plaintextBytes.copyOfRange(0, 256)
                val ctDiffers = !ctSlice.contentEquals(ptSlice)
                log("(2) ciphertext bytes differ from plaintext at same offset: $ctDiffers")
                if (!ctDiffers) allPass = false

                val tDec = System.currentTimeMillis()
                blob.decryptToFile(encFile, decFile)
                val decDuration = System.currentTimeMillis() - tDec
                val decSha = sha256(decFile.readBytes())
                val rtOk = plainSha.contentEquals(decSha)
                log("(3) round-trip SHA-256 match: $rtOk, decrypt took ${decDuration}ms")
                if (!rtOk) allPass = false

                encFile.copyTo(tamperedFile, overwrite = true)
                val flipAt = HEADER_BYTES + 1024
                java.io.RandomAccessFile(tamperedFile, "rw").use {
                    it.seek(flipAt.toLong())
                    val b = it.read()
                    it.seek(flipAt.toLong())
                    it.write(b xor 0x01)
                }
                val tamperDest = File(tempDir, "blob_selftest_tampered_dec.bin")
                val tamperRejected = try {
                    blob.decryptToFile(tamperedFile, tamperDest)
                    log("(4) FAIL — tampered blob was accepted")
                    false
                } catch (e: Exception) {
                    log("(4) tampered blob rejected: ${e.javaClass.simpleName}")
                    true
                } finally {
                    tamperDest.delete()
                }
                if (!tamperRejected) allPass = false

                encFile.copyTo(badMagicFile, overwrite = true)
                java.io.RandomAccessFile(badMagicFile, "rw").use {
                    it.seek(0)
                    it.write(0x00)
                }
                val badMagicDest = File(tempDir, "blob_selftest_badmagic_dec.bin")
                val badMagicRejected = try {
                    blob.decryptToFile(badMagicFile, badMagicDest)
                    log("(5) FAIL — bad-magic blob was accepted")
                    false
                } catch (e: BlobFormatException) {
                    log("(5) bad magic rejected: ${e.message}")
                    true
                } catch (e: Exception) {
                    log("(5) FAIL — bad magic threw wrong type: ${e.javaClass.simpleName}")
                    false
                } finally {
                    badMagicDest.delete()
                }
                if (!badMagicRejected) allPass = false

                encFile.copyTo(badVersionFile, overwrite = true)
                java.io.RandomAccessFile(badVersionFile, "rw").use {
                    it.seek(4)
                    it.write(0x99)
                }
                val badVerDest = File(tempDir, "blob_selftest_badver_dec.bin")
                val badVersionRejected = try {
                    blob.decryptToFile(badVersionFile, badVerDest)
                    log("(6) FAIL — wrong-version blob was accepted")
                    false
                } catch (e: BlobFormatException) {
                    log("(6) wrong version rejected: ${e.message}")
                    true
                } catch (e: Exception) {
                    log("(6) FAIL — wrong version threw wrong type: ${e.javaClass.simpleName}")
                    false
                } finally {
                    badVerDest.delete()
                }
                if (!badVersionRejected) allPass = false

                NativeCrypto.secureZero(plaintextBytes)
            } catch (t: Throwable) {
                log("self-test threw: ${t.javaClass.simpleName}: ${t.message}")
                allPass = false
            } finally {
                listOf(plainFile, encFile, decFile, tamperedFile, badMagicFile, badVersionFile)
                    .forEach { it.delete() }
                test.delete()
                log("(z) test files removed, test key alias deleted")
            }

            return allPass
        }

        private fun sha256(bytes: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(bytes)

        private fun ByteArray.toHex(maxBytes: Int = size): String {
            val n = minOf(size, maxBytes)
            val sb = StringBuilder(n * 2)
            for (i in 0 until n) sb.append("%02x".format(this[i].toInt() and 0xFF))
            return sb.toString()
        }

        private fun InputStream.readNBytesCompat(n: Int): ByteArray {
            val buf = ByteArray(n)
            var off = 0
            while (off < n) {
                val r = read(buf, off, n - off)
                if (r < 0) return buf.copyOf(off)
                off += r
            }
            return buf
        }
    }
}
