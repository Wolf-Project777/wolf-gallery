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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.Argon2idParams
import dev.encgallery.crypto.BackoffCurve
import dev.encgallery.crypto.BruteForceConfig
import dev.encgallery.crypto.KekWrap
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.crypto.LockMethod
import dev.encgallery.crypto.SecureBytes
import dev.encgallery.crypto.VaultDataKey
import dev.encgallery.crypto.VerifierStore
import dev.encgallery.logging.EncLog
import dev.encgallery.nativec.NativeCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

@Composable
fun UnlockScreen(
    lockMethod: LockMethod,
    failureCount: Int,
    lastFailureAt: Long,
    bruteForceConfig: BruteForceConfig,
    onWrongAttempt: () -> Unit,
    onSuccessfulUnlock: () -> Unit,
    onWipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var effectiveMethod by remember { mutableStateOf(lockMethod) }
    LaunchedEffect(Unit) {
        val embedded = withContext(Dispatchers.IO) {
            VerifierStore(context.applicationContext).read()?.lockMethod
        }
        if (embedded != null) effectiveMethod = embedded
    }

    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val backoffActive = bruteForceConfig.backoffEnabled && failureCount > 0
    val backoffMs = if (backoffActive) BackoffCurve.delayMillisFor(failureCount) else 0L
    val nextAllowedAt = lastFailureAt + backoffMs
    val remainingMs = (nextAllowedAt - nowTick).coerceAtLeast(0L)
    val isInBackoff = remainingMs > 0

    LaunchedEffect(failureCount, lastFailureAt, bruteForceConfig.backoffEnabled) {
        while (true) {
            val now = System.currentTimeMillis()
            nowTick = now
            val live = bruteForceConfig.backoffEnabled && failureCount > 0
            val target = lastFailureAt + (if (live) BackoffCurve.delayMillisFor(failureCount) else 0L)
            if (now >= target) break
            delay(500)
        }
    }

    val minLen = when (effectiveMethod) {
        LockMethod.PIN -> 6
        LockMethod.PASSWORD -> 8
    }
    val canSubmit = !working && !isInBackoff && input.length >= minLen

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wizard_unlock_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(
                when (effectiveMethod) {
                    LockMethod.PIN -> R.string.wizard_unlock_label_pin
                    LockMethod.PASSWORD -> R.string.wizard_unlock_label_password
                }
            ),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = filterUnlockInput(it, effectiveMethod) },
            singleLine = true,
            enabled = !working && !isInBackoff,
            keyboardOptions = when (effectiveMethod) {
                LockMethod.PIN -> KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                LockMethod.PASSWORD -> KeyboardOptions(keyboardType = KeyboardType.Password)
            },
            visualTransformation =
                if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { visible = !visible }) {
                    Text(
                        stringResource(
                            if (visible) R.string.wizard_unlock_hide
                            else R.string.wizard_unlock_show
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (isInBackoff) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.wizard_unlock_backoff_countdown,
                    formatRemaining(remainingMs)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (bruteForceConfig.wipeEnabled && failureCount > 0) {
            val remaining = (bruteForceConfig.wipeAfterN - failureCount).coerceAtLeast(0)
            if (remaining in 1..3) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.wizard_unlock_wipe_warning,
                        remaining
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                error = null
                working = true
                val typed = input
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        tryUnlock(context.applicationContext, typed, effectiveMethod)
                    }
                    working = false
                    when (result) {
                        is UnlockResult.Success -> {
                            input = ""
                            onSuccessfulUnlock()
                        }
                        is UnlockResult.WrongSecret -> {
                            input = ""

                            val wouldHitWipe = bruteForceConfig.wipeEnabled &&
                                (failureCount + 1) >= bruteForceConfig.wipeAfterN
                            if (wouldHitWipe) {
                                EncLog.w(
                                    "Unlock",
                                    "WIPE TRIGGERED — failureCount=${failureCount + 1} >= wipeAfterN=${bruteForceConfig.wipeAfterN}"
                                )
                                Toast.makeText(
                                    context,
                                    R.string.wizard_unlock_wipe_toast,
                                    Toast.LENGTH_LONG
                                ).show()
                                onWipe()
                            } else {
                                onWrongAttempt()
                                error = context.getString(
                                    when (effectiveMethod) {
                                        LockMethod.PIN -> R.string.wizard_unlock_err_wrong_pin
                                        LockMethod.PASSWORD -> R.string.wizard_unlock_err_wrong_password
                                    }
                                )
                            }
                        }
                        is UnlockResult.Error -> {
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
                    Text(stringResource(R.string.wizard_unlock_btn_unlocking))
                } else if (isInBackoff) {
                    Text(
                        stringResource(
                            R.string.wizard_unlock_btn_locked,
                            formatRemaining(remainingMs)
                        )
                    )
                } else {
                    Text(stringResource(R.string.wizard_unlock_btn_unlock))
                }
            }
        }
    }
}

private sealed class UnlockResult {
    data object Success : UnlockResult()
    data object WrongSecret : UnlockResult()
    data class Error(val message: String) : UnlockResult()
}

private suspend fun tryUnlock(
    ctx: Context,
    input: String,
    lockMethod: LockMethod
): UnlockResult = withContext(Dispatchers.IO) {
    val tag = "Unlock"
    var inputBytes: ByteArray? = null
    var stored: VerifierStore.Stored? = null
    var secure: SecureBytes? = null
    var derived: KekWrap.Derived? = null
    var v1Check: ByteArray? = null

    try {
        stored = VerifierStore(ctx).read()
            ?: run {
                EncLog.e(tag, "verifier missing or wrong size — vault data is incomplete")
                return@withContext UnlockResult.Error(
                    "verifier missing — vault data incomplete"
                )
            }

        inputBytes = input.toByteArray(Charsets.UTF_8)
        secure = SecureBytes.fromAndWipe(inputBytes)
        inputBytes = null

        val effectiveMethod =
            if (stored!!.version == 3) (stored!!.lockMethod ?: lockMethod) else lockMethod
        val params = Argon2idParams.forLockMethod(effectiveMethod)
        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)

        val tStart = System.currentTimeMillis()
        secure.read { plain ->
            derived = KekWrap.derive(plain, stored!!.salt, params)
            if (stored!!.version == 1) {
                v1Check = NativeCrypto.argon2idHashRaw(
                    plain,
                    stored!!.salt,
                    params.memoryKib,
                    params.iterations,
                    params.parallelism,
                    32
                ) ?: error("argon2idHashRaw returned null")
            }
        }
        val tookMs = System.currentTimeMillis() - tStart
        EncLog.i(tag, "verifying (Argon2id, verifier v${stored!!.version}, method=$effectiveMethod, took ${tookMs}ms)")

        val match = if (stored!!.version == 1) {
            MessageDigest.isEqual(v1Check, stored!!.verifier)
        } else {
            MessageDigest.isEqual(derived!!.verifier, stored!!.verifier)
        }

        if (!match) {
            EncLog.i(tag, "verification failed (wrong secret)")
            UnlockResult.WrongSecret
        } else {
            val migrated = VaultDataKey.unlockAndPrime(keystore, derived!!.kek)
            if (stored!!.version != 3) {
                VerifierStore(ctx).writeV3(effectiveMethod, stored!!.salt, derived!!.verifier)
                EncLog.i(tag, "verifier upgraded → v3 (method=$effectiveMethod)")
            }
            EncLog.i(tag, "verification success (DEK primed, dek migrated=$migrated)")
            UnlockResult.Success
        }
    } catch (t: Throwable) {
        EncLog.e(tag, "unlock attempt failed: ${t.javaClass.simpleName}: ${t.message}")
        UnlockResult.Error("${t.javaClass.simpleName}: ${t.message ?: "unknown"}")
    } finally {
        derived?.close()
        v1Check?.let { NativeCrypto.secureZero(it) }
        stored?.let {
            NativeCrypto.secureZero(it.salt)
            NativeCrypto.secureZero(it.verifier)
        }
        inputBytes?.let { NativeCrypto.secureZero(it) }
        try {
            secure?.close()
        } catch (_: Throwable) {

        }
    }
}

private fun filterUnlockInput(s: String, method: LockMethod): String = when (method) {
    LockMethod.PIN -> s.filter { it.isDigit() }.take(12)
    LockMethod.PASSWORD -> s
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = ((ms + 999) / 1000).toInt()
    return if (totalSeconds < 60) {
        "${totalSeconds}s"
    } else {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        if (seconds == 0) "${minutes}m" else "${minutes}m ${seconds}s"
    }
}
