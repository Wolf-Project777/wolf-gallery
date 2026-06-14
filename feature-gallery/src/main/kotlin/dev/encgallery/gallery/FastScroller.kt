package dev.encgallery.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FastScroller(state: LazyGridState, columns: Int, modifier: Modifier = Modifier) {
    val cols = columns.coerceAtLeast(1)

    var dragTotalRows by remember { mutableIntStateOf(1) }
    var dragMaxRow by remember { mutableIntStateOf(0) }
    FastScrollerCore(

        scrollable = state.canScrollForward || state.canScrollBackward,

        passiveFraction = {
            val info = state.layoutInfo
            val total = info.totalItemsCount
            val vis = info.visibleItemsInfo
            when {
                vis.isEmpty() -> 0f
                !state.canScrollForward -> 1f
                !state.canScrollBackward -> 0f
                else -> {
                    val totalRows = (total + cols - 1) / cols

                    val rowYs = vis.map { it.offset.y }.distinct().sorted()
                    val pitch = if (rowYs.size >= 2) {
                        (rowYs.last() - rowYs.first()).toFloat() / (rowYs.size - 1)
                    } else vis.first().size.height.toFloat().coerceAtLeast(1f)
                    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                    val scrolled = (state.firstVisibleItemIndex / cols) * pitch +
                        state.firstVisibleItemScrollOffset
                    val maxScroll = (pitch * totalRows - viewport).coerceAtLeast(1f)
                    (scrolled / maxScroll).coerceIn(0f, 1f)
                }
            }
        },

        onDragStart = {
            val info = state.layoutInfo
            val total = info.totalItemsCount
            dragTotalRows = ((total + cols - 1) / cols).coerceAtLeast(1)
            val visibleRows = info.visibleItemsInfo.map { it.row }.distinct().size
            dragMaxRow = (dragTotalRows - visibleRows).coerceAtLeast(0)
        },
        scrollToFraction = { f ->
            val total = state.layoutInfo.totalItemsCount
            val targetRow =
                if (f >= 0.999f) dragTotalRows - 1 else (f * dragMaxRow).roundToInt()
            state.scrollToItem((targetRow * cols).coerceIn(0, (total - 1).coerceAtLeast(0)))
        },
        modifier = modifier
    )
}

@Composable
fun FastScroller(state: LazyListState, modifier: Modifier = Modifier) {

    var dragTotal by remember { mutableIntStateOf(1) }
    var dragMaxIndex by remember { mutableIntStateOf(0) }

    FastScrollerCore(
        scrollable = state.canScrollForward || state.canScrollBackward,
        passiveFraction = {
            val info = state.layoutInfo
            val total = info.totalItemsCount
            val vis = info.visibleItemsInfo
            when {
                vis.isEmpty() -> 0f
                !state.canScrollForward -> 1f
                !state.canScrollBackward -> 0f
                else -> {

                    val pitch = if (vis.size >= 2) {
                        (vis.last().offset - vis.first().offset).toFloat() / (vis.size - 1)
                    } else vis.first().size.toFloat().coerceAtLeast(1f)
                    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
                    val scrolled =
                        state.firstVisibleItemIndex * pitch + state.firstVisibleItemScrollOffset
                    val maxScroll = (pitch * total - viewport).coerceAtLeast(1f)
                    (scrolled / maxScroll).coerceIn(0f, 1f)
                }
            }
        },
        onDragStart = {
            val info = state.layoutInfo
            dragTotal = info.totalItemsCount.coerceAtLeast(1)
            val visible = info.visibleItemsInfo.size
            dragMaxIndex = (dragTotal - visible).coerceAtLeast(0)
        },
        scrollToFraction = { f ->
            val total = state.layoutInfo.totalItemsCount
            val target = if (f >= 0.999f) total - 1 else (f * dragMaxIndex).roundToInt()
            state.scrollToItem(target.coerceIn(0, (total - 1).coerceAtLeast(0)))
        },
        modifier = modifier
    )
}

@Composable
private fun FastScrollerCore(
    scrollable: Boolean,
    passiveFraction: () -> Float,
    scrollToFraction: suspend (Float) -> Unit,
    onDragStart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!scrollable) return

    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    var pendingFraction by remember { mutableFloatStateOf(-1f) }
    val scrollToFractionState = rememberUpdatedState(scrollToFraction)
    val passiveFractionState = rememberUpdatedState(passiveFraction)
    val onDragStartState = rememberUpdatedState(onDragStart)
    LaunchedEffect(Unit) {
        snapshotFlow { pendingFraction }
            .collect { f -> if (f >= 0f) scrollToFractionState.value(f) }
    }

    BoxWithConstraints(modifier) {
        val trackPx = constraints.maxHeight.toFloat()

        val thumbPx = with(density) { 48.dp.toPx() }
        val travel = (trackPx - thumbPx).coerceAtLeast(1f)

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)

                .offset {
                    val f = if (dragging) dragFraction else passiveFractionState.value()
                    IntOffset(0, (f * travel).roundToInt())
                }
                .height(with(density) { thumbPx.toDp() })
                .width(44.dp)
                .systemGestureExclusion()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {

                            onDragStartState.value()
                            dragFraction = passiveFractionState.value()
                            dragging = true
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        val newTop = (dragFraction * travel + dragAmount).coerceIn(0f, travel)
                        dragFraction = newTop / travel

                        pendingFraction = dragFraction
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {

            Box(
                Modifier
                    .padding(end = 3.dp)
                    .width(12.dp)
                    .height(44.dp)
                    .graphicsLayer { alpha = 0.5f }
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
