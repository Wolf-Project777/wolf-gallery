package dev.encgallery.crypto

import android.content.Context
import dev.encgallery.nativec.NativeCrypto
import java.io.File

class VerifierStore(context: Context) {
    private val authDir: File = File(context.filesDir, "auth")
    private val file: File = File(authDir, "verifier.bin")
    private val tempFile: File = File(authDir, "verifier.bin.tmp")

    fun exists(): Boolean = file.exists() && file.length().toInt() == EXPECTED_SIZE

    fun write(salt: ByteArray, verifier: ByteArray) {
        require(salt.size == SALT_LEN) {
            "salt must be $SALT_LEN bytes, got ${salt.size}"
        }
        require(verifier.size == VERIFIER_LEN) {
            "verifier must be $VERIFIER_LEN bytes, got ${verifier.size}"
        }
        authDir.mkdirs()
        tempFile.outputStream().use { out ->
            out.write(salt)
            out.write(verifier)
            out.fd.sync()
        }
        if (!tempFile.renameTo(file)) {

            tempFile.delete()
            error("verifier rename failed: ${tempFile.absolutePath} → ${file.absolutePath}")
        }
    }

    fun read(): Stored? {
        if (!exists()) return null
        val all = file.readBytes()
        if (all.size != EXPECTED_SIZE) return null
        val salt = all.copyOfRange(0, SALT_LEN)
        val verifier = all.copyOfRange(SALT_LEN, EXPECTED_SIZE)

        NativeCrypto.secureZero(all)
        return Stored(salt = salt, verifier = verifier)
    }

    fun delete() {
        file.delete()
        tempFile.delete()
    }

    class Stored(val salt: ByteArray, val verifier: ByteArray)

    companion object {
        const val SALT_LEN = 16
        const val VERIFIER_LEN = 32
        const val EXPECTED_SIZE: Int = SALT_LEN + VERIFIER_LEN
    }
}
