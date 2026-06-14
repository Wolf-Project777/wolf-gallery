package dev.encgallery.crypto

data class Argon2idParams(
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
    val hashLen: Int = 32
) {
    companion object {
        val FOR_PASSWORD = Argon2idParams(
            memoryKib = 64 * 1024,
            iterations = 3,
            parallelism = 4,
        )

        val FOR_PIN = Argon2idParams(
            memoryKib = 128 * 1024,
            iterations = 5,
            parallelism = 4,
        )

        fun forLockMethod(method: LockMethod): Argon2idParams = when (method) {
            LockMethod.PIN -> FOR_PIN
            LockMethod.PASSWORD -> FOR_PASSWORD
        }
    }
}
