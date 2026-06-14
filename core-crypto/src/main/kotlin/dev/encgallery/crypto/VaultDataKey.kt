package dev.encgallery.crypto

import android.content.Context
import dev.encgallery.nativec.NativeCrypto
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object VaultDataKey {

    private const val DEK_FILE = "vault.dek"
    private const val DEK_SIZE_BYTES = 32
    private const val GCM_IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    @Volatile private var appContext: Context? = null
    @Volatile private var cached: ByteArray? = null
    private val lock = Any()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun secretKey(keystore: KeystoreAesGcm): SecretKeySpec {
        cached?.let { return SecretKeySpec(it, "AES") }
        synchronized(lock) {
            cached?.let { return SecretKeySpec(it, "AES") }
            val ctx = appContext ?: error("VaultDataKey.init() was not called")
            val f = File(ctx.filesDir, DEK_FILE)
            val dek = if (f.exists()) {
                keystore.decrypt(f.readBytes())
            } else {
                val fresh = ByteArray(DEK_SIZE_BYTES)
                SecureRandom().nextBytes(fresh)
                val wrapped = keystore.encrypt(fresh)

                val tmp = File(ctx.filesDir, "$DEK_FILE.tmp")
                tmp.writeBytes(wrapped)
                if (!tmp.renameTo(f)) {
                    f.delete()
                    tmp.renameTo(f)
                }
                fresh
            }
            cached = dek
            return SecretKeySpec(dek, "AES")
        }
    }

    fun wipe() {
        synchronized(lock) {
            cached?.let { NativeCrypto.secureZero(it) }
            cached = null
        }
    }

    fun reset() {
        wipe()
        appContext?.let { File(it.filesDir, DEK_FILE).delete() }
    }

    fun newEncryptCipher(key: SecretKeySpec): Pair<Cipher, ByteArray> {
        val iv = ByteArray(GCM_IV_LEN)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher to iv
    }

    fun newDecryptCipher(key: SecretKeySpec, iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher
    }
}
