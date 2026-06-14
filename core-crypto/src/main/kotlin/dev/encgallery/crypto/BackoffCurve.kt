package dev.encgallery.crypto

object BackoffCurve {
    fun delayMillisFor(consecutiveFailures: Int): Long = when {
        consecutiveFailures <= 0 -> 0L
        consecutiveFailures == 1 -> 1_000L
        consecutiveFailures == 2 -> 4_000L
        consecutiveFailures == 3 -> 16_000L
        consecutiveFailures == 4 -> 64_000L
        consecutiveFailures == 5 -> 5 * 60_000L
        else -> 30 * 60_000L
    }
}
