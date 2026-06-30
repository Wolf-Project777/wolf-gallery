package dev.encgallery.crypto

import android.content.Context
import dev.encgallery.nativec.NativeCrypto
import java.io.File

class VerifierStore(context: Context) {
    private val authDir: File = File(context.filesDir, "auth")
    private val file: File = File(authDir, "verifier.bin")
    private val tempFile: File = File(authDir, "verifier.bin.tmp")

    fun exists(): Boolean {
        if (!file.exists()) return false
        val len = file.length().toInt()
        return len == V1_SIZE || len == V2_SIZE || len == V3_SIZE
    }

    fun writeV3(lockMethod: LockMethod, salt: ByteArray, verifier: ByteArray) {
        require(salt.size == SALT_LEN) {
            "salt must be $SALT_LEN bytes, got ${salt.size}"
        }
        require(verifier.size == VERIFIER_LEN) {
            "verifier must be $VERIFIER_LEN bytes, got ${verifier.size}"
        }
        authDir.mkdirs()
        tempFile.outputStream().use { out ->
            out.write(VERSION_3.toInt())
            out.write(methodToByte(lockMethod).toInt())
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
        if (!file.exists()) return null
        val all = file.readBytes()
        return when (all.size) {
            V1_SIZE -> {
                val salt = all.copyOfRange(0, SALT_LEN)
                val verifier = all.copyOfRange(SALT_LEN, V1_SIZE)
                NativeCrypto.secureZero(all)
                Stored(version = 1, lockMethod = null, salt = salt, verifier = verifier)
            }
            V2_SIZE -> {
                if (all[0] != VERSION_2) {
                    NativeCrypto.secureZero(all)
                    null
                } else {
                    val salt = all.copyOfRange(1, 1 + SALT_LEN)
                    val verifier = all.copyOfRange(1 + SALT_LEN, V2_SIZE)
                    NativeCrypto.secureZero(all)
                    Stored(version = 2, lockMethod = null, salt = salt, verifier = verifier)
                }
            }
            V3_SIZE -> {
                if (all[0] != VERSION_3) {
                    NativeCrypto.secureZero(all)
                    null
                } else {
                    val method = methodFromByte(all[1])
                    val salt = all.copyOfRange(2, 2 + SALT_LEN)
                    val verifier = all.copyOfRange(2 + SALT_LEN, V3_SIZE)
                    NativeCrypto.secureZero(all)
                    Stored(version = 3, lockMethod = method, salt = salt, verifier = verifier)
                }
            }
            else -> {
                NativeCrypto.secureZero(all)
                null
            }
        }
    }

    fun delete() {
        file.delete()
        tempFile.delete()
    }

    class Stored(
        val version: Int,
        val lockMethod: LockMethod?,
        val salt: ByteArray,
        val verifier: ByteArray
    )

    companion object {
        const val SALT_LEN = 16
        const val VERIFIER_LEN = 32
        const val VERSION_2: Byte = 2
        const val VERSION_3: Byte = 3
        const val V1_SIZE: Int = SALT_LEN + VERIFIER_LEN
        const val V2_SIZE: Int = 1 + SALT_LEN + VERIFIER_LEN
        const val V3_SIZE: Int = 2 + SALT_LEN + VERIFIER_LEN

        fun methodToByte(method: LockMethod): Byte = when (method) {
            LockMethod.PIN -> 0
            LockMethod.PASSWORD -> 1
        }

        fun methodFromByte(b: Byte): LockMethod = when (b.toInt()) {
            1 -> LockMethod.PASSWORD
            else -> LockMethod.PIN
        }
    }
}
