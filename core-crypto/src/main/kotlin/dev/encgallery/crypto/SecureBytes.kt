package dev.encgallery.crypto

import dev.encgallery.nativec.NativeCrypto
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

class SecureBytes private constructor(
    private val masked: ByteArray,
    private var mask: ByteArray
) : AutoCloseable {

    private val closed = AtomicBoolean(false)

    val size: Int get() = masked.size

    val isClosed: Boolean get() = closed.get()

    fun <R> read(block: (ByteArray) -> R): R {
        check(!closed.get()) { "SecureBytes already closed — cannot read after wipe" }

        val n = masked.size
        val plaintext = ByteArray(n)

        for (i in 0 until n) {
            plaintext[i] = (masked[i].toInt() xor mask[i].toInt()).toByte()
        }

        try {

            val newMask = ByteArray(n)
            secureRandom.nextBytes(newMask)
            for (i in 0 until n) {
                masked[i] = (plaintext[i].toInt() xor newMask[i].toInt()).toByte()
            }

            NativeCrypto.secureZero(mask)
            mask = newMask

            return block(plaintext)
        } finally {

            NativeCrypto.secureZero(plaintext)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            NativeCrypto.secureZero(masked)
            NativeCrypto.secureZero(mask)
        }
    }

    companion object {

        private val secureRandom: SecureRandom = SecureRandom()

        fun fromAndWipe(plaintext: ByteArray): SecureBytes {
            val n = plaintext.size
            val mask = ByteArray(n)
            secureRandom.nextBytes(mask)
            val masked = ByteArray(n)
            try {
                for (i in 0 until n) {
                    masked[i] = (plaintext[i].toInt() xor mask[i].toInt()).toByte()
                }
                return SecureBytes(masked, mask)
            } finally {
                NativeCrypto.secureZero(plaintext)
            }
        }

        fun runSelfTest(log: (String) -> Unit): Boolean {
            return try {
                var allPass = true
                val n = 32
                val original = ByteArray(n) { (it + 1).toByte() }
                val originalCopy = original.copyOf()

                val sb = fromAndWipe(original)

                val callerWiped = original.all { it == 0.toByte() }
                log("(a) caller buffer wiped after fromAndWipe: $callerWiped")
                if (!callerWiped) allPass = false

                val storageMatchesPlaintext = sb.masked.contentEquals(originalCopy)
                log("(b) masked storage equals plaintext: $storageMatchesPlaintext (must be false)")
                if (storageMatchesPlaintext) allPass = false

                var readOk = false
                sb.read { plain ->
                    readOk = plain.contentEquals(originalCopy)
                }
                log("(c) round-trip read returned original: $readOk")
                if (!readOk) allPass = false

                val maskedBefore = sb.masked.copyOf()
                sb.read {   }
                val maskedAfter = sb.masked.copyOf()
                val maskRotated = !maskedBefore.contentEquals(maskedAfter)
                log("(d) masked storage changed across reads: $maskRotated")
                if (!maskRotated) allPass = false

                NativeCrypto.secureZero(maskedBefore)
                NativeCrypto.secureZero(maskedAfter)

                sb.close()
                val maskedAllZero = sb.masked.all { it == 0.toByte() }
                val maskAllZero = sb.mask.all { it == 0.toByte() }
                log("(e) after close: masked all-zero=$maskedAllZero, mask all-zero=$maskAllZero")
                if (!(maskedAllZero && maskAllZero)) allPass = false

                val throwsAfterClose = try {
                    sb.read {   }
                    false
                } catch (e: IllegalStateException) {
                    true
                }
                log("(f) read after close throws IllegalStateException: $throwsAfterClose")
                if (!throwsAfterClose) allPass = false

                val idempotent = try {
                    sb.close()
                    true
                } catch (t: Throwable) {
                    log("(g) second close threw ${t.javaClass.simpleName}: ${t.message}")
                    false
                }
                log("(g) close is idempotent: $idempotent")
                if (!idempotent) allPass = false

                NativeCrypto.secureZero(originalCopy)

                allPass
            } catch (t: Throwable) {
                log("self-test threw: ${t.javaClass.simpleName}: ${t.message}")
                false
            }
        }
    }
}
