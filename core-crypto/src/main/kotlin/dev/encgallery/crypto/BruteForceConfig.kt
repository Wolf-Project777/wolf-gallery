package dev.encgallery.crypto

data class BruteForceConfig(
    val backoffEnabled: Boolean,
    val wipeEnabled: Boolean,
    val wipeAfterN: Int
) {
    companion object {
        val DEFAULT = BruteForceConfig(
            backoffEnabled = true,
            wipeEnabled = false,
            wipeAfterN = 10
        )
    }
}
