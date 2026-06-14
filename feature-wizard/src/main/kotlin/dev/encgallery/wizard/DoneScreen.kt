package dev.encgallery.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.BruteForceConfig
import dev.encgallery.crypto.CryptoMode
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.crypto.KeystoreAesGcm.HardwareTier
import dev.encgallery.crypto.LockMethod
import dev.encgallery.logging.EncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DoneScreen(
    cryptoMode: CryptoMode,
    lockMethod: LockMethod,
    bruteForceConfig: BruteForceConfig,
    modifier: Modifier = Modifier
) {

    var tier by remember { mutableStateOf<HardwareTier?>(null) }
    var tierResolved by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val resolved = withContext(Dispatchers.IO) {
            try {
                KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS).inspectTier()
            } catch (t: Throwable) {
                EncLog.e(
                    "WizardDone",
                    "tier inspect failed: ${t.javaClass.simpleName}: ${t.message}"
                )
                null
            }
        }
        tier = resolved
        tierResolved = true
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wizard_done_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wizard_done_intro),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        SummaryCard(
            cryptoMode = cryptoMode,
            lockMethod = lockMethod,
            bruteForceConfig = bruteForceConfig,
            tier = tier,
            tierResolved = tierResolved
        )

        if (tierResolved && tier == HardwareTier.SOFTWARE) {
            Spacer(Modifier.height(12.dp))
            WarningCard(
                text = stringResource(R.string.wizard_done_warning_software),
                accent = Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    cryptoMode: CryptoMode,
    lockMethod: LockMethod,
    bruteForceConfig: BruteForceConfig,
    tier: HardwareTier?,
    tierResolved: Boolean
) {
    val cryptoValue = when (cryptoMode) {
        CryptoMode.AES_GCM -> stringResource(R.string.wizard_done_value_crypto_aes)
    }
    val lockValue = when (lockMethod) {
        LockMethod.PIN -> stringResource(R.string.wizard_done_value_lock_pin)
        LockMethod.PASSWORD -> stringResource(R.string.wizard_done_value_lock_password)
    }
    val bruteValue = if (bruteForceConfig.wipeEnabled) {
        stringResource(R.string.wizard_done_value_brute_with_wipe, bruteForceConfig.wipeAfterN)
    } else {
        stringResource(R.string.wizard_done_value_brute_backoff_only)
    }
    val tierValue = when {
        !tierResolved -> stringResource(R.string.wizard_done_value_tier_probing)
        tier == HardwareTier.STRONGBOX -> stringResource(R.string.wizard_done_value_tier_strongbox)
        tier == HardwareTier.TEE -> stringResource(R.string.wizard_done_value_tier_tee)
        tier == HardwareTier.SOFTWARE -> stringResource(R.string.wizard_done_value_tier_software)
        else -> stringResource(R.string.wizard_done_value_tier_unknown)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow(
                label = stringResource(R.string.wizard_done_label_crypto),
                value = cryptoValue
            )
            SummaryDivider()
            SummaryRow(
                label = stringResource(R.string.wizard_done_label_lock),
                value = lockValue
            )
            SummaryDivider()
            SummaryRow(
                label = stringResource(R.string.wizard_done_label_brute),
                value = bruteValue
            )
            SummaryDivider()
            SummaryRow(
                label = stringResource(R.string.wizard_done_label_tier),
                value = tierValue,
                showSpinner = !tierResolved
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    showSpinner: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showSpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 1.5.dp
            )
        }
        Text(
            text = value,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SummaryDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun WarningCard(text: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Text(
            modifier = Modifier.padding(12.dp),
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = accent
        )
    }
}
