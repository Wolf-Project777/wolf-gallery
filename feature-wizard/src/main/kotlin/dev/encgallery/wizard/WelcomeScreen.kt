package dev.encgallery.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.KeystoreAesGcm.HardwareTier

@Composable
fun WelcomeScreen(
    tierProbe: TierProbeResult,
    onCanAdvanceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(tierProbe) {

        onCanAdvanceChange(tierProbe !is TierProbeResult.Probing)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wizard_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wizard_welcome_tagline),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.wizard_welcome_threat_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        FactBullet(text = stringResource(R.string.wizard_welcome_fact_offline))
        Spacer(Modifier.height(6.dp))
        FactBullet(text = stringResource(R.string.wizard_welcome_fact_no_account))
        Spacer(Modifier.height(6.dp))
        FactBullet(text = stringResource(R.string.wizard_welcome_fact_hwbacked))

        Spacer(Modifier.height(24.dp))

        TierStatusCard(tierProbe = tierProbe)
    }
}

@Composable
private fun FactBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TierStatusCard(tierProbe: TierProbeResult) {
    val (statusText, detailText, accent) = when (tierProbe) {
        TierProbeResult.Probing -> Triple(
            stringResource(R.string.wizard_welcome_tier_probing),
            stringResource(R.string.wizard_welcome_tier_probing_detail),
            Color(0xFF1E88E5)
        )
        is TierProbeResult.Done -> when (tierProbe.tier) {
            HardwareTier.STRONGBOX -> Triple(
                stringResource(R.string.wizard_welcome_tier_strongbox),
                stringResource(R.string.wizard_welcome_tier_strongbox_detail),
                Color(0xFF2E7D32)
            )
            HardwareTier.TEE -> Triple(
                stringResource(R.string.wizard_welcome_tier_tee),
                stringResource(R.string.wizard_welcome_tier_tee_detail),
                Color(0xFF00838F)
            )
            HardwareTier.SOFTWARE -> Triple(
                stringResource(R.string.wizard_welcome_tier_software),
                stringResource(R.string.wizard_welcome_tier_software_detail),
                Color(0xFFC62828)
            )
        }
        is TierProbeResult.Error -> Triple(
            stringResource(R.string.wizard_welcome_tier_error),
            stringResource(R.string.wizard_welcome_tier_error_detail, tierProbe.reason),
            Color(0xFFEF6C00)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tierProbe is TierProbeResult.Probing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accent
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
