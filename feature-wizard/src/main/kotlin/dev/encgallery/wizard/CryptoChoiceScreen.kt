package dev.encgallery.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.CryptoMode

@Composable
fun CryptoChoiceScreen(
    selected: CryptoMode,
    onSelect: (CryptoMode) -> Unit,
    modifier: Modifier = Modifier
) {

    LaunchedEffect(Unit) { onSelect(CryptoMode.AES_GCM) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wizard_crypto_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wizard_crypto_intro),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        WizardOptionCard(
            title = stringResource(R.string.wizard_crypto_aes_title),
            subtitle = stringResource(R.string.wizard_crypto_aes_subtitle),
            badge = null,
            isSelected = selected == CryptoMode.AES_GCM,
            isAvailable = true,
            onClick = null
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.wizard_crypto_quantum_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
