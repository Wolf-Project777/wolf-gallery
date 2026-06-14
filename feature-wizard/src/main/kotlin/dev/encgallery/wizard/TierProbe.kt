package dev.encgallery.wizard

import android.content.Context
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog

sealed class TierProbeResult {

    data object Probing : TierProbeResult()

    data class Done(val tier: KeystoreAesGcm.HardwareTier) : TierProbeResult()

    data class Error(val reason: String) : TierProbeResult()
}

private const val WELCOME_PROBE_ALIAS = "enc_gallery_welcome_probe_v1"

private const val TAG = "WizardWelcome"

fun probeHardwareTier(ctx: Context): TierProbeResult {
    val probe = KeystoreAesGcm(WELCOME_PROBE_ALIAS)
    return try {

        probe.delete()

        val tier = probe.ensureExists(ctx, strongBoxPreferred = true)
        EncLog.i(TAG, "tier probe: $tier")
        TierProbeResult.Done(tier)
    } catch (t: Throwable) {
        EncLog.e(TAG, "tier probe failed: ${t.javaClass.simpleName}: ${t.message}")
        TierProbeResult.Error(t.javaClass.simpleName)
    } finally {

        try {
            probe.delete()
        } catch (_: Throwable) {

        }
    }
}
