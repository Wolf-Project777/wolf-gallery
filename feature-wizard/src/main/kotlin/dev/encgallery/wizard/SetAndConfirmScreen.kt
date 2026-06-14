package dev.encgallery.wizard

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.Argon2idParams
import dev.encgallery.crypto.BruteForceConfig
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.crypto.LockMethod
import dev.encgallery.crypto.SecureBytes
import dev.encgallery.crypto.VerifierStore
import dev.encgallery.logging.EncLog
import dev.encgallery.nativec.NativeCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom

@Composable
fun SetAndConfirmScreen(
    lockMethod: LockMethod,
    bruteForceConfig: BruteForceConfig,
    onBruteForceConfigChange: (BruteForceConfig) -> Unit,
    onSuccess: () -> Unit,
    onCanAdvanceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { onCanAdvanceChange(false) }

    var primary by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var primaryVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    val validationErrorRes = computeValidationError(primary, confirm, lockMethod)
    val canSubmit = !working && validationErrorRes == null && primary.isNotEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wizard_set_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (lockMethod) {
                    LockMethod.PIN -> R.string.wizard_set_intro_pin
                    LockMethod.PASSWORD -> R.string.wizard_set_intro_password
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        SecretField(
            label = stringResource(
                when (lockMethod) {
                    LockMethod.PIN -> R.string.wizard_set_field_pin
                    LockMethod.PASSWORD -> R.string.wizard_set_field_password
                }
            ),
            value = primary,
            onValueChange = { primary = filterInput(it, lockMethod) },
            visible = primaryVisible,
            onVisibleChange = { primaryVisible = it },
            lockMethod = lockMethod,
            enabled = !working
        )

        Spacer(Modifier.height(12.dp))

        SecretField(
            label = stringResource(R.string.wizard_set_field_confirm),
            value = confirm,
            onValueChange = { confirm = filterInput(it, lockMethod) },
            visible = confirmVisible,
            onVisibleChange = { confirmVisible = it },
            lockMethod = lockMethod,
            enabled = !working
        )

        Spacer(Modifier.height(8.dp))

        validationErrorRes?.let { resId ->
            Text(
                text = stringResource(resId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }

        serverError?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        WipeSection(
            wipeEnabled = bruteForceConfig.wipeEnabled,
            wipeAfterN = bruteForceConfig.wipeAfterN,
            enabled = !working,
            onWipeEnabledChange = {
                onBruteForceConfigChange(bruteForceConfig.copy(wipeEnabled = it))
            },
            onWipeAfterNChange = {
                onBruteForceConfigChange(bruteForceConfig.copy(wipeAfterN = it))
            }
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        BackoffInfoNote()

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                serverError = null
                working = true
                val typed = primary
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        createVault(
                            ctx = context.applicationContext,
                            password = typed,
                            lockMethod = lockMethod
                        )
                    }
                    working = false
                    when (result) {
                        is CreateVaultResult.Success -> {
                            primary = ""
                            confirm = ""
                            onSuccess()
                        }
                        is CreateVaultResult.Failure -> {
                            serverError = result.message
                        }
                    }
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (working) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.wizard_set_btn_creating))
                } else {
                    Text(stringResource(R.string.wizard_set_btn_create))
                }
            }
        }
    }
}

@Composable
private fun SecretField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    lockMethod: LockMethod,
    enabled: Boolean
) {
    val keyboardOptions = when (lockMethod) {
        LockMethod.PIN -> KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        LockMethod.PASSWORD -> KeyboardOptions(keyboardType = KeyboardType.Password)
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        visualTransformation =
            if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { onVisibleChange(!visible) }) {
                Text(
                    stringResource(
                        if (visible) R.string.wizard_set_hide
                        else R.string.wizard_set_show
                    )
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WipeSection(
    wipeEnabled: Boolean,
    wipeAfterN: Int,
    enabled: Boolean,
    onWipeEnabledChange: (Boolean) -> Unit,
    onWipeAfterNChange: (Int) -> Unit
) {
    var customDialogShown by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.wizard_brute_wipe_heading),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = stringResource(R.string.wizard_brute_wipe_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.wizard_brute_wipe_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = wipeEnabled,
            enabled = enabled,
            onCheckedChange = onWipeEnabledChange
        )
    }

    if (wipeEnabled) {
        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Text(
                text = stringResource(R.string.wizard_brute_wipe_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.wizard_brute_wipe_threshold_label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )
        Spacer(Modifier.height(6.dp))

        val presets = listOf(5, 10, 15, 20)
        val isCustom = wipeAfterN !in presets

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (preset in presets) {
                FilterChip(
                    selected = !isCustom && wipeAfterN == preset,
                    onClick = { onWipeAfterNChange(preset) },
                    label = {
                        Text(stringResource(R.string.wizard_brute_wipe_after_n, preset))
                    },
                    enabled = enabled
                )
            }
            FilterChip(
                selected = isCustom,
                onClick = { customDialogShown = true },
                label = {
                    Text(
                        if (isCustom)
                            stringResource(R.string.wizard_brute_wipe_after_n, wipeAfterN)
                        else
                            stringResource(R.string.wizard_brute_wipe_custom)
                    )
                },
                enabled = enabled
            )
        }

        if (customDialogShown) {
            CustomThresholdDialog(
                initialValue = if (isCustom) wipeAfterN else 7,
                onDismiss = { customDialogShown = false },
                onConfirm = { value ->
                    onWipeAfterNChange(value)
                    customDialogShown = false
                }
            )
        }
    }
}

@Composable
private fun CustomThresholdDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initialValue.toString()) }
    val parsed = text.toIntOrNull()
    val canConfirm = parsed != null && parsed in 1..999

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wizard_brute_wipe_custom_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.wizard_brute_wipe_custom_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() }.take(3) },
                    label = {
                        Text(stringResource(R.string.wizard_brute_wipe_custom_label))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = canConfirm) {
                Text(stringResource(R.string.wizard_brute_wipe_custom_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.wizard_brute_wipe_custom_cancel))
            }
        }
    )
}

@Composable
private fun BackoffInfoNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.wizard_set_backoff_info_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "1s → 4s → 16s → 64s → 5min → 30min",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.wizard_set_backoff_info_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun filterInput(s: String, method: LockMethod): String = when (method) {
    LockMethod.PIN -> s.filter { it.isDigit() }.take(12)
    LockMethod.PASSWORD -> s
}

private fun computeValidationError(
    primary: String,
    confirm: String,
    method: LockMethod
): Int? {
    if (primary.isEmpty()) return null
    return when (method) {
        LockMethod.PIN -> when {
            !primary.all { it.isDigit() } -> R.string.wizard_set_err_pin_digits
            primary.length < 6 -> R.string.wizard_set_err_pin_too_short
            primary.length > 12 -> R.string.wizard_set_err_pin_too_long
            confirm.isEmpty() -> R.string.wizard_set_err_confirm_empty
            primary != confirm -> R.string.wizard_set_err_confirm_mismatch
            else -> null
        }
        LockMethod.PASSWORD -> when {
            primary.length < 8 -> R.string.wizard_set_err_password_too_short
            confirm.isEmpty() -> R.string.wizard_set_err_confirm_empty
            primary != confirm -> R.string.wizard_set_err_confirm_mismatch
            else -> null
        }
    }
}

private sealed class CreateVaultResult {
    data object Success : CreateVaultResult()
    data class Failure(val message: String) : CreateVaultResult()
}

private fun createVault(
    ctx: Context,
    password: String,
    lockMethod: LockMethod
): CreateVaultResult {
    val tag = "WizardSetAndConfirm"
    var pwBytes: ByteArray? = null
    var verifier: ByteArray? = null
    var salt: ByteArray? = null
    var secure: SecureBytes? = null

    return try {
        pwBytes = password.toByteArray(Charsets.UTF_8)
        secure = SecureBytes.fromAndWipe(pwBytes)
        pwBytes = null

        salt = SecureRandom().generateSeed(VerifierStore.SALT_LEN)

        val params = Argon2idParams.forLockMethod(lockMethod)
        val tStart = System.currentTimeMillis()
        secure.read { plain ->
            verifier = NativeCrypto.argon2idHashRaw(
                plain,
                salt,
                params.memoryKib,
                params.iterations,
                params.parallelism,
                params.hashLen
            ) ?: error("argon2idHashRaw returned null")
        }
        val argonMs = System.currentTimeMillis() - tStart
        EncLog.i(
            tag,
            "verifier computed (Argon2id, mem=${params.memoryKib}KiB iter=${params.iterations}, took ${argonMs}ms)"
        )

        VerifierStore(ctx).write(salt!!, verifier!!)
        EncLog.i(
            tag,
            "verifier persisted to filesDir/auth/verifier.bin (${VerifierStore.EXPECTED_SIZE} bytes)"
        )

        val tier = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            .ensureExists(ctx, strongBoxPreferred = true)
        EncLog.i(tag, "master key created — tier: $tier")

        CreateVaultResult.Success
    } catch (t: Throwable) {
        EncLog.e(tag, "vault creation failed: ${t.javaClass.simpleName}: ${t.message}")
        CreateVaultResult.Failure("${t.javaClass.simpleName}: ${t.message ?: "unknown"}")
    } finally {
        verifier?.let { NativeCrypto.secureZero(it) }
        salt?.let { NativeCrypto.secureZero(it) }
        pwBytes?.let { NativeCrypto.secureZero(it) }
        try {
            secure?.close()
        } catch (_: Throwable) {

        }
    }
}
