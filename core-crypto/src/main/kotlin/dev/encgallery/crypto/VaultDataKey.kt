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
    private const val GCM_IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    @Volatile private var appContext: Context? = null
    @Volatile private var cached: ByteArray? = null
    private val lock = Any()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isPrimed(): Boolean = cached != null

    private fun dekFile(ctx: Context): File = File(ctx.filesDir, DEK_FILE)

    fun createAndPrime(keystore: KeystoreAesGcm, kek: ByteArray) {
        val ctx = appContext ?: error("VaultDataKey.init() was not called")
        synchronized(lock) {
            val res = DekVault.createV2(keystore, kek)
            try {
                atomicWrite(dekFile(ctx), res.fileBytes)
                cached?.let { NativeCrypto.secureZero(it) }
                cached = res.dek
            } catch (t: Throwable) {
                NativeCrypto.secureZero(res.dek)
                throw t
            }
        }
    }

    fun unlockAndPrime(keystore: KeystoreAesGcm, kek: ByteArray): Boolean {
        val ctx = appContext ?: error("VaultDataKey.init() was not called")
        synchronized(lock) {
            val f = dekFile(ctx)
            if (!f.exists()) {
                val res = DekVault.createV2(keystore, kek)
                try {
                    atomicWrite(f, res.fileBytes)
                    cached?.let { NativeCrypto.secureZero(it) }
                    cached = res.dek
                } catch (t: Throwable) {
                    NativeCrypto.secureZero(res.dek)
                    throw t
                }
                return false
            }

            val bytes = f.readBytes()
            if (DekVault.isV2(bytes)) {
                val dek = DekVault.loadV2(keystore, kek, bytes)
                cached?.let { NativeCrypto.secureZero(it) }
                cached = dek
                return false
            }

            val res = DekVault.migrateV1toV2(keystore, kek, bytes)
            try {
                val check = DekVault.loadV2(keystore, kek, res.fileBytes)
                val ok = check.contentEquals(res.dek)
                NativeCrypto.secureZero(check)
                if (!ok) error("v1→v2 migration self-verify failed — not committing")
                atomicWrite(f, res.fileBytes)
                cached?.let { NativeCrypto.secureZero(it) }
                cached = res.dek
                return true
            } catch (t: Throwable) {
                NativeCrypto.secureZero(res.dek)
                throw t
            }
        }
    }

    fun secretKey(): SecretKeySpec {
        val c = cached ?: error("VaultDataKey not primed — vault must be unlocked first")
        return SecretKeySpec(c, "AES")
    }

    fun changeSecret(
        keystore: KeystoreAesGcm,
        newKek: ByteArray,
        newSalt: ByteArray,
        newVerifier: ByteArray,
        newLockMethod: LockMethod
    ) {
        val ctx = appContext ?: error("VaultDataKey.init() was not called")
        synchronized(lock) {
            val dek = cached ?: error("VaultDataKey not primed — vault must be unlocked first")
            PasswordChange.apply(ctx.filesDir, keystore, dek, newKek, newSalt, newVerifier, newLockMethod)
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
        appContext?.let { dekFile(it).delete() }
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

    private fun atomicWrite(f: File, bytes: ByteArray) {
        val tmp = File(f.parentFile, "${f.name}.tmp")
        tmp.outputStream().use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        if (!tmp.renameTo(f)) {
            f.delete()
            if (!tmp.renameTo(f)) {
                tmp.delete()
                error("dek file rename failed: ${tmp.absolutePath} → ${f.absolutePath}")
            }
        }
    }
}
