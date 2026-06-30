package dev.encgallery.crypto

import dev.encgallery.nativec.NativeCrypto
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object KekWrap {

    const val KEK_LEN = 32
    const val VERIFIER_LEN = 32
    const val SALT_LEN = 16

    private const val DERIVE_LEN = KEK_LEN + VERIFIER_LEN
    private const val GCM_IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    class Derived(val kek: ByteArray, val verifier: ByteArray) : AutoCloseable {
        override fun close() {
            NativeCrypto.secureZero(kek)
            NativeCrypto.secureZero(verifier)
        }
    }

    fun derive(passwordPlain: ByteArray, salt: ByteArray, params: Argon2idParams): Derived {
        require(salt.size == SALT_LEN) { "salt must be $SALT_LEN bytes, was ${salt.size}" }
        val out = NativeCrypto.argon2idHashRaw(
            passwordPlain,
            salt,
            params.memoryKib,
            params.iterations,
            params.parallelism,
            DERIVE_LEN
        ) ?: error("argon2idHashRaw returned null")
        try {
            val kek = out.copyOfRange(0, KEK_LEN)
            val verifier = out.copyOfRange(KEK_LEN, DERIVE_LEN)
            return Derived(kek, verifier)
        } finally {
            NativeCrypto.secureZero(out)
        }
    }

    fun wrap(kek: ByteArray, dek: ByteArray): ByteArray {
        require(kek.size == KEK_LEN) { "kek must be $KEK_LEN bytes, was ${kek.size}" }
        val iv = ByteArray(GCM_IV_LEN)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(kek, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        val ctAndTag = cipher.doFinal(dek)
        return iv + ctAndTag
    }

    fun unwrap(kek: ByteArray, blob: ByteArray): ByteArray {
        require(kek.size == KEK_LEN) { "kek must be $KEK_LEN bytes, was ${kek.size}" }
        require(blob.size >= GCM_IV_LEN + GCM_TAG_BYTES) {
            "blob too short: ${blob.size} (need at least ${GCM_IV_LEN + GCM_TAG_BYTES})"
        }
        val iv = blob.copyOfRange(0, GCM_IV_LEN)
        val ctAndTag = blob.copyOfRange(GCM_IV_LEN, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(kek, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        return cipher.doFinal(ctAndTag)
    }

    fun runSelfTest(log: (String) -> Unit): Boolean {
        var allPass = true
        val params = Argon2idParams(memoryKib = 8 * 1024, iterations = 2, parallelism = 1)

        var d1: Derived? = null
        var d3: Derived? = null
        var dWrong: Derived? = null
        var dNew: Derived? = null
        var dek: ByteArray? = null
        var dekCopy: ByteArray? = null
        var recovered: ByteArray? = null
        var dekFromOld: ByteArray? = null
        var newRecovered: ByteArray? = null

        try {
            val pw = "correct horse battery staple".toByteArray(Charsets.UTF_8)
            val wrongPw = "Tr0ub4dor&3".toByteArray(Charsets.UTF_8)
            val newPw = "a brand new passphrase".toByteArray(Charsets.UTF_8)
            val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
            val salt2 = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
            val newSalt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }

            d1 = derive(pw, salt, params)
            val d2 = derive(pw, salt, params)
            val deterministic =
                d1.kek.contentEquals(d2.kek) && d1.verifier.contentEquals(d2.verifier)
            log("(1) derive deterministic (same pw+salt): $deterministic")
            if (!deterministic) allPass = false
            d2.close()

            val domainSep = !d1.kek.contentEquals(d1.verifier)
            log("(2) KEK != verifier (domain separation): $domainSep")
            if (!domainSep) allPass = false

            d3 = derive(pw, salt2, params)
            val saltSensitive = !d1.kek.contentEquals(d3.kek)
            log("(3) different salt yields different KEK: $saltSensitive")
            if (!saltSensitive) allPass = false

            dek = ByteArray(32).also { SecureRandom().nextBytes(it) }
            dekCopy = dek.copyOf()

            val blob = wrap(d1.kek, dek)
            recovered = unwrap(d1.kek, blob)
            val roundTrip = recovered.contentEquals(dekCopy)
            log("(4) wrap/unwrap round-trip recovers DEK: $roundTrip")
            if (!roundTrip) allPass = false

            val blobB = wrap(d1.kek, dekCopy)
            val ivA = blob.copyOfRange(0, GCM_IV_LEN)
            val ivB = blobB.copyOfRange(0, GCM_IV_LEN)
            val randomized = !ivA.contentEquals(ivB) && !blob.contentEquals(blobB)
            log("(5) two wraps differ (random IV): $randomized")
            if (!randomized) allPass = false

            dWrong = derive(wrongPw, salt, params)
            val wrongRejected = try {
                unwrap(dWrong.kek, blob)
                log("(6) FAIL — wrong-password KEK unwrapped the DEK")
                false
            } catch (e: Exception) {
                log("(6) wrong-password KEK rejected: ${e.javaClass.simpleName}")
                true
            }
            if (!wrongRejected) allPass = false

            val tamperedCt = blob.copyOf()
            val at = GCM_IV_LEN + (tamperedCt.size - GCM_IV_LEN) / 2
            tamperedCt[at] = (tamperedCt[at].toInt() xor 0x01).toByte()
            val ctTamperRejected = try {
                unwrap(d1.kek, tamperedCt)
                log("(7) FAIL — tampered ciphertext accepted")
                false
            } catch (e: Exception) {
                log("(7) tampered ciphertext rejected: ${e.javaClass.simpleName}")
                true
            }
            if (!ctTamperRejected) allPass = false

            val tamperedIv = blob.copyOf()
            tamperedIv[0] = (tamperedIv[0].toInt() xor 0x01).toByte()
            val ivTamperRejected = try {
                unwrap(d1.kek, tamperedIv)
                log("(8) FAIL — tampered IV accepted")
                false
            } catch (e: Exception) {
                log("(8) tampered IV rejected: ${e.javaClass.simpleName}")
                true
            }
            if (!ivTamperRejected) allPass = false

            dNew = derive(newPw, newSalt, params)
            dekFromOld = unwrap(d1.kek, blob)
            val rewrapped = wrap(dNew.kek, dekFromOld)
            newRecovered = unwrap(dNew.kek, rewrapped)
            val changeKeepsDek = newRecovered.contentEquals(dekCopy)
            log("(9) password change re-wraps same DEK (no re-encryption): $changeKeepsDek")
            if (!changeKeepsDek) allPass = false

            val oldKeyFailsNew = try {
                unwrap(d1.kek, rewrapped)
                log("(10) FAIL — old KEK still unwraps new blob")
                false
            } catch (e: Exception) {
                log("(10) old KEK no longer unwraps after change: ${e.javaClass.simpleName}")
                true
            }
            if (!oldKeyFailsNew) allPass = false

            val again = derive(pw, salt, params)
            val verifierMatch = MessageDigest.isEqual(again.verifier, d1.verifier)
            val verifierWrong = !MessageDigest.isEqual(dWrong.verifier, d1.verifier)
            log("(11) verifier matches right pw=$verifierMatch, rejects wrong pw=$verifierWrong")
            if (!(verifierMatch && verifierWrong)) allPass = false
            again.close()

            NativeCrypto.secureZero(pw)
            NativeCrypto.secureZero(wrongPw)
            NativeCrypto.secureZero(newPw)
        } catch (t: Throwable) {
            log("self-test threw: ${t.javaClass.simpleName}: ${t.message}")
            allPass = false
        } finally {
            d1?.close()
            d3?.close()
            dWrong?.close()
            dNew?.close()
            dek?.let { NativeCrypto.secureZero(it) }
            dekCopy?.let { NativeCrypto.secureZero(it) }
            recovered?.let { NativeCrypto.secureZero(it) }
            dekFromOld?.let { NativeCrypto.secureZero(it) }
            newRecovered?.let { NativeCrypto.secureZero(it) }
        }

        return allPass
    }
}
