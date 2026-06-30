package dev.encgallery.crypto

import android.content.Context
import dev.encgallery.nativec.NativeCrypto
import java.security.SecureRandom

object DekVault {

    private val MAGIC = byteArrayOf(
        'E'.code.toByte(), 'G'.code.toByte(), 'D'.code.toByte(), 'K'.code.toByte()
    )
    private const val VERSION_2: Byte = 2
    private const val DEK_SIZE = 32
    private const val HEADER_LEN = 5

    class Result(val fileBytes: ByteArray, val dek: ByteArray)

    fun isV2(fileBytes: ByteArray): Boolean =
        fileBytes.size > HEADER_LEN &&
            fileBytes[0] == MAGIC[0] &&
            fileBytes[1] == MAGIC[1] &&
            fileBytes[2] == MAGIC[2] &&
            fileBytes[3] == MAGIC[3] &&
            fileBytes[4] == VERSION_2

    fun wrapV2(keystore: KeystoreAesGcm, kek: ByteArray, dek: ByteArray): ByteArray {
        val inner = KekWrap.wrap(kek, dek)
        try {
            val outer = keystore.encrypt(inner)
            return MAGIC + byteArrayOf(VERSION_2) + outer
        } finally {
            NativeCrypto.secureZero(inner)
        }
    }

    fun createV2(keystore: KeystoreAesGcm, kek: ByteArray): Result {
        val dek = ByteArray(DEK_SIZE)
        SecureRandom().nextBytes(dek)
        return Result(wrapV2(keystore, kek, dek), dek)
    }

    fun loadV2(keystore: KeystoreAesGcm, kek: ByteArray, fileBytes: ByteArray): ByteArray {
        require(isV2(fileBytes)) { "not a v2 DEK blob" }
        val outer = fileBytes.copyOfRange(HEADER_LEN, fileBytes.size)
        val inner = keystore.decrypt(outer)
        try {
            return KekWrap.unwrap(kek, inner)
        } finally {
            NativeCrypto.secureZero(inner)
        }
    }

    fun loadV1(keystore: KeystoreAesGcm, fileBytes: ByteArray): ByteArray =
        keystore.decrypt(fileBytes)

    fun migrateV1toV2(keystore: KeystoreAesGcm, kek: ByteArray, v1Bytes: ByteArray): Result {
        val dek = loadV1(keystore, v1Bytes)
        return Result(wrapV2(keystore, kek, dek), dek)
    }

    fun rewrap(
        keystore: KeystoreAesGcm,
        oldKek: ByteArray,
        newKek: ByteArray,
        v2Bytes: ByteArray
    ): Result {
        val dek = loadV2(keystore, oldKek, v2Bytes)
        return Result(wrapV2(keystore, newKek, dek), dek)
    }

    fun runSelfTest(context: Context, log: (String) -> Unit): Boolean {
        val testAlias = "enc_gallery_test_dek_v1"
        val keystore = KeystoreAesGcm(testAlias)
        keystore.delete()
        var allPass = true
        val params = Argon2idParams(memoryKib = 8 * 1024, iterations = 2, parallelism = 1)

        var d: KekWrap.Derived? = null
        var wrongD: KekWrap.Derived? = null
        var newD: KekWrap.Derived? = null
        var dek0: ByteArray? = null
        var dek0copy: ByteArray? = null

        try {
            val tier = keystore.ensureExists(context, strongBoxPreferred = true)
            log("(0) test key generated — hardware tier: $tier")

            val pw = "correct horse battery staple".toByteArray(Charsets.UTF_8)
            val wrongPw = "Tr0ub4dor&3".toByteArray(Charsets.UTF_8)
            val newPw = "a brand new passphrase".toByteArray(Charsets.UTF_8)
            val salt = ByteArray(KekWrap.SALT_LEN).also { SecureRandom().nextBytes(it) }
            val newSalt = ByteArray(KekWrap.SALT_LEN).also { SecureRandom().nextBytes(it) }

            d = KekWrap.derive(pw, salt, params)

            val created = createV2(keystore, d.kek)
            val v2flag = isV2(created.fileBytes)
            val loaded = loadV2(keystore, d.kek, created.fileBytes)
            val roundTrip = loaded.contentEquals(created.dek)
            log("(1) create→load v2 round-trip=$roundTrip, magic/version ok=$v2flag")
            if (!roundTrip || !v2flag) allPass = false
            NativeCrypto.secureZero(loaded)

            wrongD = KekWrap.derive(wrongPw, salt, params)
            val wrongRejected = try {
                loadV2(keystore, wrongD.kek, created.fileBytes)
                log("(2) FAIL — wrong-password KEK loaded the v2 DEK")
                false
            } catch (e: Exception) {
                log("(2) wrong-password KEK rejected at unwrap: ${e.javaClass.simpleName}")
                true
            }
            if (!wrongRejected) allPass = false

            dek0 = ByteArray(DEK_SIZE).also { SecureRandom().nextBytes(it) }
            dek0copy = dek0.copyOf()
            val v1Bytes = keystore.encrypt(dek0)
            val detectedV1 = !isV2(v1Bytes)
            log("(3) legacy v1 blob (raw Keystore) detected as non-v2: $detectedV1")
            if (!detectedV1) allPass = false

            val migrated = migrateV1toV2(keystore, d.kek, v1Bytes)
            val dekPreserved = migrated.dek.contentEquals(dek0copy)
            val migIsV2 = isV2(migrated.fileBytes)
            val reload = loadV2(keystore, d.kek, migrated.fileBytes)
            val reloadOk = reload.contentEquals(dek0copy)
            log("(4) v1→v2 migration keeps same DEK=$dekPreserved, output v2=$migIsV2, reload ok=$reloadOk")
            if (!(dekPreserved && migIsV2 && reloadOk)) allPass = false
            NativeCrypto.secureZero(reload)
            NativeCrypto.secureZero(migrated.dek)

            newD = KekWrap.derive(newPw, newSalt, params)
            val changed = rewrap(keystore, d.kek, newD.kek, migrated.fileBytes)
            val stillSame = changed.dek.contentEquals(dek0copy)
            val newKekLoads = loadV2(keystore, newD.kek, changed.fileBytes).let {
                val ok = it.contentEquals(dek0copy)
                NativeCrypto.secureZero(it)
                ok
            }
            val oldKekFails = try {
                loadV2(keystore, d.kek, changed.fileBytes)
                false
            } catch (e: Exception) {
                true
            }
            log("(5) change-password keeps DEK=$stillSame, new KEK loads=$newKekLoads, old KEK fails=$oldKekFails")
            if (!(stillSame && newKekLoads && oldKekFails)) allPass = false

            NativeCrypto.secureZero(created.dek)
            NativeCrypto.secureZero(changed.dek)
            NativeCrypto.secureZero(pw)
            NativeCrypto.secureZero(wrongPw)
            NativeCrypto.secureZero(newPw)
        } catch (t: Throwable) {
            log("self-test threw: ${t.javaClass.simpleName}: ${t.message}")
            allPass = false
        } finally {
            d?.close()
            wrongD?.close()
            newD?.close()
            dek0?.let { NativeCrypto.secureZero(it) }
            dek0copy?.let { NativeCrypto.secureZero(it) }
            keystore.delete()
            log("(z) test key alias deleted")
        }

        return allPass
    }
}
