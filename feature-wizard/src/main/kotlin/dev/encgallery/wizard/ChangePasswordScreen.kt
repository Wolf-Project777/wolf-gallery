package dev.encgallery.wizard

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.Argon2idParams
import dev.encgallery.crypto.KekWrap
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.crypto.LockMethod
import dev.encgallery.crypto.SecureBytes
import dev.encgallery.crypto.VaultDataKey
import dev.encgallery.crypto.VerifierStore
import dev.encgallery.logging.EncLog
import dev.encgallery.nativec.NativeCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

@Composable
fun ChangePasswordScreen(
    lockMethod: LockMethod,
    onLockMethodChanged: (LockMethod) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var oldSecret by remember { mutableStateOf("") }
    var newSecret by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var newMethod by remember { mutableStateOf(lockMethod) }
    var oldVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val validationErrorRes = computeChangeValidationError(newSecret, confirm, newMethod)
    val canSubmit = !working && oldSecret.isNotEmpty() &&
        newSecret.isNotEmpty() && validationErrorRes == null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wizard_change_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wizard_change_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(20.dp))

        ChangeSecretField(
            label = stringResource(
                when (lockMethod) {
                    LockMethod.PIN -> R.string.wizard_change_field_old_pin
                    LockMethod.PASSWORD -> R.string.wizard_change_field_old_password
                }
            ),
            value = oldSecret,
            onValueChange = { oldSecret = filterChangeInput(it, lockMethod); error = null },
            visible = oldVisible,
            onVisibleChange = { oldVisible = it },
            lockMethod = lockMethod,
            enabled = !working
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.wizard_change_new_type_label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = newMethod == LockMethod.PASSWORD,
                enabled = !working,
                onClick = {
                    if (newMethod != LockMethod.PASSWORD) {
                        newMethod = LockMethod.PASSWORD
                        newSecret = ""
                        confirm = ""
                    }
                },
                label = { Text(stringResource(R.string.wizard_lock_password_title)) }
            )
            FilterChip(
                selected = newMethod == LockMethod.PIN,
                enabled = !working,
                onClick = {
                    if (newMethod != LockMethod.PIN) {
                        newMethod = LockMethod.PIN
                        newSecret = ""
                        confirm = ""
                    }
                },
                label = { Text(stringResource(R.string.wizard_lock_pin_title)) }
            )
        }

        Spacer(Modifier.height(16.dp))

        ChangeSecretField(
            label = stringResource(
                when (newMethod) {
                    LockMethod.PIN -> R.string.wizard_change_field_new_pin
                    LockMethod.PASSWORD -> R.string.wizard_change_field_new_password
                }
            ),
            value = newSecret,
            onValueChange = { newSecret = filterChangeInput(it, newMethod) },
            visible = newVisible,
            onVisibleChange = { newVisible = it },
            lockMethod = newMethod,
            enabled = !working
        )

        Spacer(Modifier.height(12.dp))

        ChangeSecretField(
            label = stringResource(R.string.wizard_change_field_confirm),
            value = confirm,
            onValueChange = { confirm = filterChangeInput(it, newMethod) },
            visible = confirmVisible,
            onVisibleChange = { confirmVisible = it },
            lockMethod = newMethod,
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

        error?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                error = null
                working = true
                val oldTyped = oldSecret
                val newTyped = newSecret
                val targetMethod = newMethod
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        performChange(context.applicationContext, oldTyped, newTyped, lockMethod, targetMethod)
                    }
                    working = false
                    when (result) {
                        is ChangeResult.Success -> {
                            oldSecret = ""
                            newSecret = ""
                            confirm = ""
                            onLockMethodChanged(targetMethod)
                            Toast.makeText(
                                context,
                                when (targetMethod) {
                                    LockMethod.PIN -> R.string.wizard_change_toast_success_pin
                                    LockMethod.PASSWORD -> R.string.wizard_change_toast_success_password
                                },
                                Toast.LENGTH_LONG
                            ).show()
                            onClose()
                        }
                        is ChangeResult.WrongOld -> {
                            oldSecret = ""
                            error = context.getString(
                                when (lockMethod) {
                                    LockMethod.PIN -> R.string.wizard_change_err_wrong_pin
                                    LockMethod.PASSWORD -> R.string.wizard_change_err_wrong_password
                                }
                            )
                        }
                        is ChangeResult.Failure -> {
                            error = result.message
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
                    Text(stringResource(R.string.wizard_change_btn_working))
                } else {
                    Text(stringResource(R.string.wizard_change_btn_apply))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onClose,
            enabled = !working,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.wizard_change_btn_cancel))
        }
    }
}

@Composable
private fun ChangeSecretField(
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

private fun filterChangeInput(s: String, method: LockMethod): String = when (method) {
    LockMethod.PIN -> s.filter { it.isDigit() }.take(12)
    LockMethod.PASSWORD -> s
}

private fun computeChangeValidationError(
    newSecret: String,
    confirm: String,
    method: LockMethod
): Int? {
    if (newSecret.isEmpty()) return null
    return when (method) {
        LockMethod.PIN -> when {
            !newSecret.all { it.isDigit() } -> R.string.wizard_set_err_pin_digits
            newSecret.length < 6 -> R.string.wizard_set_err_pin_too_short
            newSecret.length > 12 -> R.string.wizard_set_err_pin_too_long
            confirm.isEmpty() -> R.string.wizard_set_err_confirm_empty
            newSecret != confirm -> R.string.wizard_set_err_confirm_mismatch
            else -> null
        }
        LockMethod.PASSWORD -> when {
            newSecret.length < 8 -> R.string.wizard_set_err_password_too_short
            confirm.isEmpty() -> R.string.wizard_set_err_confirm_empty
            newSecret != confirm -> R.string.wizard_set_err_confirm_mismatch
            else -> null
        }
    }
}

private sealed class ChangeResult {
    data object Success : ChangeResult()
    data object WrongOld : ChangeResult()
    data class Failure(val message: String) : ChangeResult()
}

private fun performChange(
    ctx: Context,
    oldSecret: String,
    newSecret: String,
    currentLockMethod: LockMethod,
    newLockMethod: LockMethod
): ChangeResult {
    val tag = "ChangePassword"
    var oldSecure: SecureBytes? = null
    var newSecure: SecureBytes? = null
    var oldDerived: KekWrap.Derived? = null
    var newDerived: KekWrap.Derived? = null
    var v1Check: ByteArray? = null
    var newSalt: ByteArray? = null
    var stored: VerifierStore.Stored? = null

    return try {
        stored = VerifierStore(ctx).read()
            ?: return ChangeResult.Failure("verifier missing — vault data incomplete")

        val currentMethod = if (stored.version == 3) (stored.lockMethod ?: currentLockMethod) else currentLockMethod
        val oldParams = Argon2idParams.forLockMethod(currentMethod)

        oldSecure = SecureBytes.fromAndWipe(oldSecret.toByteArray(Charsets.UTF_8))
        oldSecure.read { plain ->
            oldDerived = KekWrap.derive(plain, stored!!.salt, oldParams)
            if (stored!!.version == 1) {
                v1Check = NativeCrypto.argon2idHashRaw(
                    plain,
                    stored!!.salt,
                    oldParams.memoryKib,
                    oldParams.iterations,
                    oldParams.parallelism,
                    32
                ) ?: error("argon2idHashRaw returned null")
            }
        }

        val match = if (stored!!.version == 1) {
            MessageDigest.isEqual(v1Check, stored!!.verifier)
        } else {
            MessageDigest.isEqual(oldDerived!!.verifier, stored!!.verifier)
        }
        if (!match) {
            EncLog.i(tag, "current secret verification failed")
            return ChangeResult.WrongOld
        }

        val newParams = Argon2idParams.forLockMethod(newLockMethod)
        newSalt = SecureRandom().generateSeed(VerifierStore.SALT_LEN)
        newSecure = SecureBytes.fromAndWipe(newSecret.toByteArray(Charsets.UTF_8))
        newSecure.read { plain ->
            newDerived = KekWrap.derive(plain, newSalt!!, newParams)
        }

        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
        VaultDataKey.changeSecret(keystore, newDerived!!.kek, newSalt!!, newDerived!!.verifier, newLockMethod)
        EncLog.i(tag, "secret changed ($currentMethod→$newLockMethod) — DEK re-wrapped under new KEK, no files re-encrypted")

        ChangeResult.Success
    } catch (t: Throwable) {
        EncLog.e(tag, "change failed: ${t.javaClass.simpleName}: ${t.message}")
        ChangeResult.Failure("${t.javaClass.simpleName}: ${t.message ?: "unknown"}")
    } finally {
        oldDerived?.close()
        newDerived?.close()
        v1Check?.let { NativeCrypto.secureZero(it) }
        newSalt?.let { NativeCrypto.secureZero(it) }
        stored?.let {
            NativeCrypto.secureZero(it.salt)
            NativeCrypto.secureZero(it.verifier)
        }
        try {
            oldSecure?.close()
        } catch (_: Throwable) {
        }
        try {
            newSecure?.close()
        } catch (_: Throwable) {
        }
    }
}
