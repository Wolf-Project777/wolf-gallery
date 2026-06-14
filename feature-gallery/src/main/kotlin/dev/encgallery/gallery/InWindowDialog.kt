package dev.encgallery.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun InWindowDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null
) {

    val host = LocalDialogHost.current
    val forceInline = LocalDialogForceInline.current
    val currentBody by rememberUpdatedState<@Composable () -> Unit> {
        InWindowDialogBody(
            onDismiss = onDismiss,
            modifier = modifier,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton
        )
    }

    if (host == null || forceInline) {
        currentBody()
        return
    }

    DisposableEffect(host) {
        val entry = DialogHostState.Entry(id = Any()) { currentBody() }
        host.add(entry)
        onDispose { host.remove(entry) }
    }
}

@Composable
private fun InWindowDialogBody(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null
) {
    BackHandler { onDismiss() }

    val sysBars: PaddingValues = WindowInsets.systemBars.asPaddingValues()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            },
        contentAlignment = Alignment.Center
    ) {
        val parentMaxHeight = maxHeight

        val frameShape = RoundedCornerShape(0.dp)
        val outline = MaterialTheme.colorScheme.outline
        Surface(
            modifier = modifier
                .padding(sysBars)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .widthIn(min = 280.dp, max = 560.dp)
                .heightIn(max = (parentMaxHeight - 48.dp).coerceAtLeast(200.dp))
                .border(width = 3.dp, color = outline.copy(alpha = 0.35f), shape = frameShape)
                .border(width = 1.dp, color = outline, shape = frameShape)
                .pointerInput(Unit) {
                    detectTapGestures { }
                },
            shape = frameShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                title?.let {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) { it() }
                    Spacer(Modifier.height(16.dp))
                }
                text?.let {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { it() }
                    Spacer(Modifier.height(24.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}
