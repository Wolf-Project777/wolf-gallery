package dev.encgallery.nativec

object NativeCrypto {
    init {

        System.loadLibrary("encnative")
    }

    external fun secureZero(buf: ByteArray)

    external fun argon2idHashRaw(
        password: ByteArray,
        salt: ByteArray,
        memoryKib: Int,
        iterations: Int,
        parallelism: Int,
        hashLen: Int
    ): ByteArray?
}
