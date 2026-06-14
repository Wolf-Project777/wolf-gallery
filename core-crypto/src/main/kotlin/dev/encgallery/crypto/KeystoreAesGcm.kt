package dev.encgallery.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import dev.encgallery.nativec.NativeCrypto
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

class KeystoreAesGcm(private val alias: String) {

    enum class HardwareTier { STRONGBOX, TEE, SOFTWARE }

    private val keystore: KeyStore by lazy {
        KeyStore.getInstance(PROVIDER).apply { load(null) }
    }

    fun exists(): Boolean = keystore.containsAlias(alias)

    fun delete() {
        if (keystore.containsAlias(alias)) {
            keystore.deleteEntry(alias)
        }
    }

    fun ensureExists(context: Context, strongBoxPreferred: Boolean = true): HardwareTier {
        if (exists()) return inspectTier()

        val strongBoxAvailable = strongBoxPreferred &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_STRONGBOX_KEYSTORE
            )

        if (strongBoxAvailable) {
            try {
                generateKey(strongBox = true)
                return inspectTier()
            } catch (_: StrongBoxUnavailableException) {

            }
        }

        generateKey(strongBox = false)
        return inspectTier()
    }

    fun inspectTier(): HardwareTier {
        require(exists()) { "key '$alias' does not exist — call ensureExists first" }
        val key = keystore.getKey(alias, null) as SecretKey
        val factory = SecretKeyFactory.getInstance(key.algorithm, PROVIDER)
        val info = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            when (info.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> HardwareTier.STRONGBOX
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> HardwareTier.TEE
                KeyProperties.SECURITY_LEVEL_SOFTWARE -> HardwareTier.SOFTWARE
                else -> HardwareTier.SOFTWARE
            }
        } else {

            @Suppress("DEPRECATION")
            if (info.isInsideSecureHardware) HardwareTier.TEE else HardwareTier.SOFTWARE
        }
    }

    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadKey())
        val iv = cipher.iv
        check(iv.size == GCM_IV_LEN) {
            "unexpected IV length ${iv.size} — Keystore should produce $GCM_IV_LEN-byte IVs"
        }
        val ctAndTag = cipher.doFinal(plaintext)

        return iv + ctAndTag
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size >= GCM_IV_LEN + GCM_TAG_BYTES) {
            "blob too short: ${blob.size} (need at least ${GCM_IV_LEN + GCM_TAG_BYTES})"
        }
        val iv = blob.copyOfRange(0, GCM_IV_LEN)
        val ctAndTag = blob.copyOfRange(GCM_IV_LEN, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, loadKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ctAndTag)
    }

    fun newEncryptCipher(): Pair<Cipher, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, loadKey())
        val iv = cipher.iv
        check(iv.size == GCM_IV_LEN) {
            "unexpected IV length ${iv.size} — Keystore should produce $GCM_IV_LEN-byte IVs"
        }
        return cipher to iv
    }

    fun newDecryptCipher(iv: ByteArray): Cipher {
        require(iv.size == GCM_IV_LEN) {
            "iv must be $GCM_IV_LEN bytes, was ${iv.size}"
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, loadKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher
    }

    private fun generateKey(strongBox: Boolean) {
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

            .setRandomizedEncryptionRequired(true)

        if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(true)
        }

        gen.init(builder.build())
        gen.generateKey()
    }

    private fun loadKey(): SecretKey =
        keystore.getKey(alias, null) as? SecretKey
            ?: error("key '$alias' is missing or not a SecretKey")

    companion object {

        const val PRODUCTION_ALIAS = "enc_gallery_master_v1"

        private const val TEST_ALIAS = "enc_gallery_test_aes_v1"

        private const val PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LEN = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8

        fun runSelfTest(context: Context, log: (String) -> Unit): Boolean {
            val test = KeystoreAesGcm(TEST_ALIAS)

            test.delete()
            var allPass = true

            try {

                val tier = test.ensureExists(context, strongBoxPreferred = true)
                log("(1) key generated — hardware tier: $tier")
                if (tier == HardwareTier.SOFTWARE) {
                    log("(1) WARN — SOFTWARE-backed key. This device has no TEE/StrongBox.")
                    allPass = false
                }

                val msg = "Hello, EncGallery! Привет 🔐".toByteArray(Charsets.UTF_8)
                val msgCopy = msg.copyOf()
                val blobMsg = test.encrypt(msg)
                val recoveredMsg = test.decrypt(blobMsg)
                val rt1 = recoveredMsg.contentEquals(msgCopy)
                log("(2) round-trip ${msg.size}-byte UTF-8: $rt1")
                if (!rt1) allPass = false

                val rand = ByteArray(1024).also { java.security.SecureRandom().nextBytes(it) }
                val randCopy = rand.copyOf()
                val blobRand = test.encrypt(rand)
                val recoveredRand = test.decrypt(blobRand)
                val rt2 = recoveredRand.contentEquals(randCopy)
                log("(3) round-trip 1 KiB random: $rt2")
                if (!rt2) allPass = false

                val ctOnly = blobRand.copyOfRange(GCM_IV_LEN, blobRand.size - GCM_TAG_BYTES)
                val plainSlice = randCopy.copyOfRange(0, ctOnly.size)
                val ctDiffers = !ctOnly.contentEquals(plainSlice)
                log("(4) ciphertext differs from plaintext: $ctDiffers")
                if (!ctDiffers) allPass = false
                NativeCrypto.secureZero(ctOnly)
                NativeCrypto.secureZero(plainSlice)

                val blobA = test.encrypt(msgCopy)
                val blobB = test.encrypt(msgCopy)
                val ivA = blobA.copyOfRange(0, GCM_IV_LEN)
                val ivB = blobB.copyOfRange(0, GCM_IV_LEN)
                val ivsDiffer = !ivA.contentEquals(ivB)
                val blobsDiffer = !blobA.contentEquals(blobB)
                log("(5) IV differs across two encrypts: $ivsDiffer; full blobs differ: $blobsDiffer")
                if (!ivsDiffer || !blobsDiffer) allPass = false

                val tamperedCt = blobMsg.copyOf()
                val flipAt = GCM_IV_LEN + (tamperedCt.size - GCM_IV_LEN) / 2
                tamperedCt[flipAt] = (tamperedCt[flipAt].toInt() xor 0x01).toByte()
                val ctTamperRejected = try {
                    test.decrypt(tamperedCt)
                    log("(6) FAIL — tampered ciphertext was accepted")
                    false
                } catch (e: Exception) {
                    log("(6) tampered ciphertext rejected: ${e.javaClass.simpleName}")
                    true
                }
                if (!ctTamperRejected) allPass = false

                val tamperedIv = blobMsg.copyOf()
                tamperedIv[0] = (tamperedIv[0].toInt() xor 0x01).toByte()
                val ivTamperRejected = try {
                    test.decrypt(tamperedIv)
                    log("(7) FAIL — tampered IV was accepted")
                    false
                } catch (e: Exception) {
                    log("(7) tampered IV rejected: ${e.javaClass.simpleName}")
                    true
                }
                if (!ivTamperRejected) allPass = false

                NativeCrypto.secureZero(msg)
                NativeCrypto.secureZero(msgCopy)
                NativeCrypto.secureZero(recoveredMsg)
                NativeCrypto.secureZero(rand)
                NativeCrypto.secureZero(randCopy)
                NativeCrypto.secureZero(recoveredRand)
            } catch (t: Throwable) {
                log("self-test threw: ${t.javaClass.simpleName}: ${t.message}")
                allPass = false
            } finally {
                test.delete()
                log("(z) test key alias deleted")
            }

            return allPass
        }
    }
}
