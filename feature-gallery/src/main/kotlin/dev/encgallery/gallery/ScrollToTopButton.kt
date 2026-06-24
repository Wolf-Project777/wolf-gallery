package dev.encgallery.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ScrollToTopButton(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    appearAfterIndex: Int = 11
) {
    val visible by remember(state) {
        derivedStateOf { state.firstVisibleItemIndex > appearAfterIndex }
    }
    val scope = rememberCoroutineScope()
    ScrollToTopButtonCore(
        visible = visible,
        onClick = { scope.launch { state.animateScrollToItem(0) } },
        modifier = modifier
    )
}

@Composable
fun ScrollToTopButton(
    state: LazyListState,
    modifier: Modifier = Modifier,
    appearAfterIndex: Int = 3
) {
    val visible by remember(state) {
        derivedStateOf { state.firstVisibleItemIndex > appearAfterIndex }
    }
    val scope = rememberCoroutineScope()
    ScrollToTopButtonCore(
        visible = visible,
        onClick = { scope.launch { state.animateScrollToItem(0) } },
        modifier = modifier
    )
}

@Composable
private fun ScrollToTopButtonCore(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sysBars = WindowInsets.systemBars.asPaddingValues()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier.padding(bottom = 20.dp + sysBars.calculateBottomPadding())
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.40f),
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.scroll_to_top)
                )
            }
        }
    }
}
