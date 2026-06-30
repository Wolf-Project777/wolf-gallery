package dev.encgallery.crypto

import android.content.Context
import dev.encgallery.nativec.NativeCrypto
import java.io.File
import java.security.SecureRandom

object PasswordChange {

    private const val DEK_FILE = "vault.dek"
    private const val DEK_NEW = "vault.dek.new"
    private const val AUTH_DIR = "auth"
    private const val VER_FILE = "verifier.bin"
    private const val VER_NEW = "verifier.bin.new"

    fun apply(
        filesDir: File,
        keystore: KeystoreAesGcm,
        dekPlain: ByteArray,
        newKek: ByteArray,
        newSalt: ByteArray,
        newVerifier: ByteArray,
        newLockMethod: LockMethod
    ) {
        require(newSalt.size == VerifierStore.SALT_LEN)
        require(newVerifier.size == VerifierStore.VERIFIER_LEN)

        val dekFile = File(filesDir, DEK_FILE)
        val dekNew = File(filesDir, DEK_NEW)
        val authDir = File(filesDir, AUTH_DIR)
        val verFile = File(authDir, VER_FILE)
        val verNew = File(authDir, VER_NEW)
        authDir.mkdirs()

        val dekBytes = DekVault.wrapV2(keystore, newKek, dekPlain)
        val check = DekVault.loadV2(keystore, newKek, dekBytes)
        val ok = check.contentEquals(dekPlain)
        NativeCrypto.secureZero(check)
        if (!ok) error("new DEK wrap self-verify failed — change aborted")

        val verBytes = verBytesV3(newLockMethod, newSalt, newVerifier)

        writeSync(dekNew, dekBytes)
        writeSync(verNew, verBytes)

        commit(dekNew, dekFile)
        commit(verNew, verFile)
    }

    fun recoverIfPending(filesDir: File): String? {
        val dekNew = File(filesDir, DEK_NEW)
        val authDir = File(filesDir, AUTH_DIR)
        val verFile = File(authDir, VER_FILE)
        val verNew = File(authDir, VER_NEW)

        return when {
            verNew.exists() && dekNew.exists() -> {
                dekNew.delete()
                verNew.delete()
                "rolled back interrupted password change (pre-commit)"
            }
            verNew.exists() -> {
                commit(verNew, verFile)
                "rolled forward interrupted password change (verifier commit)"
            }
            dekNew.exists() -> {
                dekNew.delete()
                "discarded stray staged DEK from interrupted password change"
            }
            else -> null
        }
    }

    private fun writeSync(f: File, bytes: ByteArray) {
        f.outputStream().use { out ->
            out.write(bytes)
            out.fd.sync()
        }
    }

    private fun commit(src: File, dst: File) {
        if (!src.renameTo(dst)) {
            dst.delete()
            if (!src.renameTo(dst)) {
                src.delete()
                error("commit rename failed: ${src.absolutePath} → ${dst.absolutePath}")
            }
        }
    }

    fun runSelfTest(context: Context, log: (String) -> Unit): Boolean {
        val testAlias = "enc_gallery_test_pwchange_v1"
        val keystore = KeystoreAesGcm(testAlias)
        keystore.delete()
        var allPass = true
        val params = Argon2idParams(memoryKib = 8 * 1024, iterations = 2, parallelism = 1)
        val root = File(context.cacheDir, "pwchange_selftest")

        var oldD: KekWrap.Derived? = null
        var newD: KekWrap.Derived? = null
        var dek: ByteArray? = null
        var dekCopy: ByteArray? = null

        try {
            keystore.ensureExists(context, strongBoxPreferred = true)

            val oldSalt = ByteArray(VerifierStore.SALT_LEN).also { SecureRandom().nextBytes(it) }
            val newSalt = ByteArray(VerifierStore.SALT_LEN).also { SecureRandom().nextBytes(it) }
            oldD = KekWrap.derive("old-pass".toByteArray(Charsets.UTF_8), oldSalt, params)
            newD = KekWrap.derive("new-pass".toByteArray(Charsets.UTF_8), newSalt, params)

            dek = ByteArray(32).also { SecureRandom().nextBytes(it) }
            dekCopy = dek.copyOf()

            val oldDekBytes = DekVault.wrapV2(keystore, oldD.kek, dek)
            val oldVerBytes = verBytesV3(LockMethod.PASSWORD, oldSalt, oldD.verifier)
            val newDekBytes = DekVault.wrapV2(keystore, newD.kek, dek)
            val newVerBytes = verBytesV3(LockMethod.PIN, newSalt, newD.verifier)

            val r1 = freshDir(root)
            seedOld(r1, oldDekBytes, oldVerBytes)
            stage(r1, newDekBytes, newVerBytes)
            recoverIfPending(r1)
            val p1 = loadsWith(keystore, r1, oldD.kek, dekCopy) &&
                !File(r1, DEK_NEW).exists() &&
                verifierVersion(r1) == 3 && verifierSalt(r1).contentEquals(oldSalt) &&
                verifierMethod(r1) == LockMethod.PASSWORD
            log("(1) crash pre-commit → rolled back to OLD: $p1")
            if (!p1) allPass = false

            val r2 = freshDir(root)
            seedOld(r2, oldDekBytes, oldVerBytes)
            File(r2, DEK_FILE).writeBytes(newDekBytes)
            File(File(r2, AUTH_DIR), VER_NEW).writeBytes(newVerBytes)
            recoverIfPending(r2)
            val p2 = loadsWith(keystore, r2, newD.kek, dekCopy) &&
                !File(File(r2, AUTH_DIR), VER_NEW).exists() &&
                verifierVersion(r2) == 3 && verifierMethod(r2) == LockMethod.PIN
            log("(2) crash post-DEK-commit → rolled forward to NEW: $p2")
            if (!p2) allPass = false

            val r3 = freshDir(root)
            seedOld(r3, oldDekBytes, oldVerBytes)
            File(r3, DEK_NEW).writeBytes(newDekBytes)
            recoverIfPending(r3)
            val p3 = loadsWith(keystore, r3, oldD.kek, dekCopy) && !File(r3, DEK_NEW).exists()
            log("(3) crash mid-stage (stray DEK) → discarded, OLD intact: $p3")
            if (!p3) allPass = false

            val r4 = freshDir(root)
            seedOld(r4, oldDekBytes, oldVerBytes)
            apply(r4, keystore, dekCopy, newD.kek, newSalt, newD.verifier, LockMethod.PIN)
            val newLoads = loadsWith(keystore, r4, newD.kek, dekCopy)
            val oldFails = !loadsWith(keystore, r4, oldD.kek, dekCopy)
            val verNew = verifierVersion(r4) == 3 && verifierSalt(r4).contentEquals(newSalt) &&
                verifierMethod(r4) == LockMethod.PIN
            val noStaging = !File(r4, DEK_NEW).exists() && !File(File(r4, AUTH_DIR), VER_NEW).exists()
            log("(4) full apply (PASSWORD→PIN type switch): NEW loads=$newLoads, OLD fails=$oldFails, verifier=NEW+PIN=$verNew, no staging=$noStaging")
            if (!(newLoads && oldFails && verNew && noStaging)) allPass = false

            root.deleteRecursively()
        } catch (t: Throwable) {
            log("self-test threw: ${t.javaClass.simpleName}: ${t.message}")
            allPass = false
        } finally {
            oldD?.close()
            newD?.close()
            dek?.let { NativeCrypto.secureZero(it) }
            dekCopy?.let { NativeCrypto.secureZero(it) }
            keystore.delete()
            root.deleteRecursively()
            log("(z) test alias + temp dir removed")
        }
        return allPass
    }

    private fun verBytesV3(method: LockMethod, salt: ByteArray, verifier: ByteArray): ByteArray {
        val b = ByteArray(VerifierStore.V3_SIZE)
        b[0] = VerifierStore.VERSION_3
        b[1] = VerifierStore.methodToByte(method)
        System.arraycopy(salt, 0, b, 2, VerifierStore.SALT_LEN)
        System.arraycopy(verifier, 0, b, 2 + VerifierStore.SALT_LEN, VerifierStore.VERIFIER_LEN)
        return b
    }

    private fun freshDir(root: File): File {
        val d = File(root, "case_${java.util.UUID.randomUUID()}")
        File(d, AUTH_DIR).mkdirs()
        return d
    }

    private fun seedOld(dir: File, dekBytes: ByteArray, verBytes: ByteArray) {
        File(dir, DEK_FILE).writeBytes(dekBytes)
        File(File(dir, AUTH_DIR), VER_FILE).writeBytes(verBytes)
    }

    private fun stage(dir: File, dekBytes: ByteArray, verBytes: ByteArray) {
        File(dir, DEK_NEW).writeBytes(dekBytes)
        File(File(dir, AUTH_DIR), VER_NEW).writeBytes(verBytes)
    }

    private fun loadsWith(keystore: KeystoreAesGcm, dir: File, kek: ByteArray, expected: ByteArray): Boolean {
        return try {
            val bytes = File(dir, DEK_FILE).readBytes()
            val dek = DekVault.loadV2(keystore, kek, bytes)
            val ok = dek.contentEquals(expected)
            NativeCrypto.secureZero(dek)
            ok
        } catch (_: Exception) {
            false
        }
    }

    private fun verifierVersion(dir: File): Int {
        val f = File(File(dir, AUTH_DIR), VER_FILE)
        return when (f.length().toInt()) {
            VerifierStore.V1_SIZE -> 1
            VerifierStore.V2_SIZE -> 2
            VerifierStore.V3_SIZE -> 3
            else -> -1
        }
    }

    private fun verifierSalt(dir: File): ByteArray {
        val all = File(File(dir, AUTH_DIR), VER_FILE).readBytes()
        return all.copyOfRange(2, 2 + VerifierStore.SALT_LEN)
    }

    private fun verifierMethod(dir: File): LockMethod {
        val all = File(File(dir, AUTH_DIR), VER_FILE).readBytes()
        return VerifierStore.methodFromByte(all[1])
    }
}
