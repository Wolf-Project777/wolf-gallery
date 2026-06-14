package dev.encgallery.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun InWindowDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchorBoundsInWindow: Rect?,

    surfaceMinWidth: Dp = 140.dp,
    surfaceMaxWidth: Dp = 320.dp,

    endMargin: Dp = 4.dp,

    verticalGap: Dp = 8.dp,

    seedRootOffset: Offset? = null,
    onRootOffset: (Offset) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return
    if (anchorBoundsInWindow == null) return

    InWindowDropdownBody(
        onDismissRequest = onDismissRequest,
        anchor = anchorBoundsInWindow,
        surfaceMinWidth = surfaceMinWidth,
        surfaceMaxWidth = surfaceMaxWidth,
        endMargin = endMargin,
        verticalGap = verticalGap,
        seedRootOffset = seedRootOffset,
        onRootOffset = onRootOffset,
        content = content
    )
}

@Composable
private fun InWindowDropdownBody(
    onDismissRequest: () -> Unit,
    anchor: Rect,
    surfaceMinWidth: Dp,
    surfaceMaxWidth: Dp,
    endMargin: Dp,
    verticalGap: Dp,
    seedRootOffset: Offset?,
    onRootOffset: (Offset) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler { onDismissRequest() }

    val density = LocalDensity.current
    val systemBarsBottomDp = WindowInsets.systemBars.asPaddingValues()
        .calculateBottomPadding()
    val systemBarsTopDp = WindowInsets.systemBars.asPaddingValues()
        .calculateTopPadding()
    val systemBarsBottomPx = with(density) { systemBarsBottomDp.toPx() }
    val systemBarsTopPx = with(density) { systemBarsTopDp.toPx() }
    val surfaceMinWidthPx = with(density) { surfaceMinWidth.toPx() }
    val surfaceMaxWidthPx = with(density) { surfaceMaxWidth.toPx() }
    val endMarginPx = with(density) { endMargin.toPx() }
    val verticalGapPx = with(density) { verticalGap.toPx() }

    var rootOffsetInWindow by remember {
        mutableStateOf<Offset?>(seedRootOffset)
    }

    Layout(
        content = {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures { onDismissRequest() }
                    }
            )

            val frameShape = RoundedCornerShape(0.dp)
            val outline = MaterialTheme.colorScheme.outline
            Surface(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .border(width = 3.dp, color = outline.copy(alpha = 0.35f), shape = frameShape)
                    .border(width = 1.dp, color = outline, shape = frameShape)
                    .pointerInput(Unit) {
                        detectTapGestures {   }
                    },
                shape = frameShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    content()
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    val pos = coords.positionInWindow()
                    val current = rootOffsetInWindow

                    if (current == null ||
                        current.x != pos.x ||
                        current.y != pos.y
                    ) {
                        rootOffsetInWindow = pos

                        onRootOffset(pos)
                    }
                }
            }
    ) { measurables, constraints ->
        val parentW = constraints.maxWidth
        val parentH = constraints.maxHeight

        val scrim = measurables[0].measure(
            Constraints.fixed(parentW, parentH)
        )

        val offset = rootOffsetInWindow
        if (offset == null) {

            val surface = measurables[1].measure(
                Constraints(
                    minWidth = surfaceMinWidthPx.toInt().coerceAtMost(parentW),
                    maxWidth = surfaceMaxWidthPx.toInt().coerceAtMost(parentW),
                    minHeight = 0,
                    maxHeight = parentH
                )
            )
            layout(parentW, parentH) {
                scrim.place(0, 0)
                surface.place(-surface.width, -surface.height)
            }
        } else {

            val anchorRightLocal = anchor.right - offset.x
            val anchorTopLocal = anchor.top - offset.y
            val anchorBottomLocal = anchor.bottom - offset.y

            val safeBottomLocal = parentH - systemBarsBottomPx
            val safeTopLocal = (systemBarsTopPx - offset.y).coerceAtLeast(0f)

            val availableAbove = (anchorTopLocal - verticalGapPx - safeTopLocal)
                .coerceAtLeast(0f)
            val availableBelow = (safeBottomLocal - anchorBottomLocal - verticalGapPx)
                .coerceAtLeast(0f)
            val maxAvailable = max(availableAbove, availableBelow)

            val surface = measurables[1].measure(
                Constraints(
                    minWidth = surfaceMinWidthPx.toInt().coerceAtMost(parentW),
                    maxWidth = surfaceMaxWidthPx.toInt().coerceAtMost(parentW),
                    minHeight = 0,
                    maxHeight = maxAvailable.toInt().coerceAtLeast(0)
                )
            )

            val rawLeft = anchorRightLocal - endMarginPx - surface.width
            val left = rawLeft.coerceIn(
                0f,
                max(0f, parentW - surface.width.toFloat())
            )

            val placeAbove = surface.height.toFloat() > availableBelow
            val top: Float = if (placeAbove) {

                (anchorTopLocal - verticalGapPx - surface.height)
                    .coerceAtLeast(safeTopLocal)
            } else {
                anchorBottomLocal + verticalGapPx
            }

            layout(parentW, parentH) {
                scrim.place(0, 0)
                surface.place(left.roundToInt(), top.roundToInt())
            }
        }
    }
}

@Composable
fun InWindowDropdownItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,

    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start
) {
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        if (leadingIcon != null) {
            Box(modifier = Modifier.padding(end = 12.dp)) { leadingIcon() }
        }
        ProvideTextStyle(
            MaterialTheme.typography.bodyLarge.copy(color = textColor)
        ) { text() }
    }
}
