package dev.encgallery.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.LockMethod

@Composable
fun LockMethodScreen(
    selected: LockMethod,
    onSelect: (LockMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wizard_lock_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wizard_lock_intro),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        WizardOptionCard(
            title = stringResource(R.string.wizard_lock_password_title),
            subtitle = stringResource(R.string.wizard_lock_password_subtitle),
            badge = stringResource(R.string.wizard_lock_password_badge),
            isSelected = selected == LockMethod.PASSWORD,
            isAvailable = true,
            onClick = { onSelect(LockMethod.PASSWORD) }
        )
        Spacer(Modifier.height(12.dp))
        WizardOptionCard(
            title = stringResource(R.string.wizard_lock_pin_title),
            subtitle = stringResource(R.string.wizard_lock_pin_subtitle),
            badge = stringResource(R.string.wizard_lock_pin_badge),
            isSelected = selected == LockMethod.PIN,
            isAvailable = true,
            onClick = { onSelect(LockMethod.PIN) }
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.wizard_lock_argon_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
