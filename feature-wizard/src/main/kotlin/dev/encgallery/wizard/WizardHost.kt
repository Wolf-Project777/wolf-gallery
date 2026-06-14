package dev.encgallery.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.BruteForceConfig
import dev.encgallery.crypto.CryptoMode
import dev.encgallery.crypto.LockMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WizardHost(
    onComplete: () -> Unit,
    cryptoMode: CryptoMode,
    onCryptoModeChange: (CryptoMode) -> Unit,
    lockMethod: LockMethod,
    onLockMethodChange: (LockMethod) -> Unit,
    bruteForceConfig: BruteForceConfig,
    onBruteForceConfigChange: (BruteForceConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var step by rememberSaveable(stateSaver = WizardStepSaver) {
        mutableStateOf(WizardStep.WELCOME)
    }

    var canAdvance by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()
    LaunchedEffect(step) {
        canAdvance = true
        scrollState.scrollTo(0)
    }

    var stagedCryptoMode by rememberSaveable(stateSaver = CryptoModeSaver) {
        mutableStateOf(cryptoMode)
    }

    var stagedLockMethod by rememberSaveable(stateSaver = LockMethodSaver) {
        mutableStateOf(lockMethod)
    }

    var stagedBruteForceConfig by rememberSaveable(stateSaver = BruteForceConfigSaver) {
        mutableStateOf(bruteForceConfig)
    }

    var tierProbe by remember { mutableStateOf<TierProbeResult>(TierProbeResult.Probing) }
    LaunchedEffect(Unit) {

        val appCtx = context.applicationContext
        tierProbe = withContext(Dispatchers.IO) { probeHardwareTier(appCtx) }
    }

    val canGoBack = step.prev() != null && step != WizardStep.DONE
    BackHandler(enabled = canGoBack) {
        step.prev()?.let { step = it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.wizard_step_progress, step.number, step.total),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { step.number.toFloat() / step.total },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    WizardStep.WELCOME -> WelcomeScreen(
                        tierProbe = tierProbe,
                        onCanAdvanceChange = { canAdvance = it }
                    )
                    WizardStep.CRYPTO_CHOICE -> CryptoChoiceScreen(
                        selected = stagedCryptoMode,
                        onSelect = { stagedCryptoMode = it }
                    )
                    WizardStep.LOCK_METHOD -> LockMethodScreen(
                        selected = stagedLockMethod,
                        onSelect = { stagedLockMethod = it }
                    )
                    WizardStep.SET_AND_CONFIRM -> SetAndConfirmScreen(
                        lockMethod = lockMethod,
                        bruteForceConfig = stagedBruteForceConfig,
                        onBruteForceConfigChange = { stagedBruteForceConfig = it },
                        onSuccess = {

                            onBruteForceConfigChange(stagedBruteForceConfig)
                            step.next()?.let { step = it }
                        },
                        onCanAdvanceChange = { canAdvance = it }
                    )

                    WizardStep.DONE -> DoneScreen(
                        cryptoMode = cryptoMode,
                        lockMethod = lockMethod,
                        bruteForceConfig = bruteForceConfig
                    )
                }

                Spacer(Modifier.height(28.dp))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                        )
                    )
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedButton(
                onClick = { step.prev()?.let { step = it } },
                enabled = canGoBack
            ) {
                Text(stringResource(R.string.wizard_btn_back))
            }

            val isLast = step == WizardStep.DONE
            Button(
                enabled = canAdvance,
                onClick = {

                    when (step) {
                        WizardStep.CRYPTO_CHOICE -> onCryptoModeChange(stagedCryptoMode)
                        WizardStep.LOCK_METHOD -> onLockMethodChange(stagedLockMethod)
                        else -> {}
                    }
                    if (isLast) onComplete()
                    else step.next()?.let { step = it }
                }
            ) {
                Text(
                    stringResource(
                        if (isLast) R.string.wizard_btn_done
                        else R.string.wizard_btn_next
                    )
                )
            }
        }
    }
}

private val WizardStepSaver: Saver<WizardStep, String> = Saver(
    save = { it.name },
    restore = { WizardStep.valueOf(it) }
)

private val CryptoModeSaver: Saver<CryptoMode, String> = Saver(
    save = { it.name },
    restore = { CryptoMode.valueOf(it) }
)

private val LockMethodSaver: Saver<LockMethod, String> = Saver(
    save = { it.name },
    restore = { LockMethod.valueOf(it) }
)

private val BruteForceConfigSaver: Saver<BruteForceConfig, ArrayList<Any>> = Saver(
    save = {
        arrayListOf<Any>(it.backoffEnabled, it.wipeEnabled, it.wipeAfterN)
    },
    restore = {
        BruteForceConfig(
            backoffEnabled = it[0] as Boolean,
            wipeEnabled = it[1] as Boolean,
            wipeAfterN = it[2] as Int
        )
    }
)

@Preview(showBackground = true, name = "Wizard — Welcome")
@Composable
private fun WizardHostPreview() {
    MaterialTheme {
        WizardHost(
            onComplete = {},
            cryptoMode = CryptoMode.AES_GCM,
            onCryptoModeChange = {},
            lockMethod = LockMethod.PASSWORD,
            onLockMethodChange = {},
            bruteForceConfig = BruteForceConfig.DEFAULT,
            onBruteForceConfigChange = {}
        )
    }
}
