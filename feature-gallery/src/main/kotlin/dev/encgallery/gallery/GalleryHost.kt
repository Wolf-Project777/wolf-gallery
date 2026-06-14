package dev.encgallery.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalleryHost(
    entries: List<VaultEntry>,
    loaded: Boolean,
    trashEnabled: Boolean,
    onView: (Int) -> Unit,
    onDeleteSelection: (Set<String>) -> Unit,
    onMoveRequest: (Set<String>) -> Unit,
    onCopyRequest: (Set<String>) -> Unit,
    onShareSelection: (Set<String>) -> Unit,
    onExportSelection: (Set<String>) -> Unit = {},
    onRenameRequest: (uuid: String, newName: String) -> Unit = { _, _ -> },
    onSetWallpaperRequest: (uuid: String, target: WallpaperTarget) -> Unit = { _, _ -> },
    onImportComplete: () -> Unit,

    searchQuery: String = "",

    enlarged: Boolean = false,
    modifier: Modifier = Modifier
) {

    val gridState: LazyGridState = remember {
        EncLog.d(
            "GalleryHost",
            "gridState init from GallerySession: idx=${GallerySession.lastGridFirstVisibleItem} offset=${GallerySession.lastGridFirstVisibleOffset}"
        )
        LazyGridState(
            firstVisibleItemIndex = GallerySession.lastGridFirstVisibleItem,
            firstVisibleItemScrollOffset = GallerySession.lastGridFirstVisibleOffset
        )
    }
    DisposableEffect(gridState) {
        onDispose {

            val idx = gridState.firstVisibleItemIndex
            val offset = gridState.firstVisibleItemScrollOffset
            GallerySession.lastGridFirstVisibleItem = idx
            GallerySession.lastGridFirstVisibleOffset = offset
            EncLog.d(
                "GalleryHost",
                "gridState disposing: saved idx=$idx offset=$offset"
            )
        }
    }

    val mosaicState = remember {
        androidx.compose.foundation.lazy.LazyListState(
            firstVisibleItemIndex = GallerySession.lastMosaicFirstVisibleItem,
            firstVisibleItemScrollOffset = GallerySession.lastMosaicFirstVisibleOffset
        )
    }
    DisposableEffect(mosaicState) {
        onDispose {
            GallerySession.lastMosaicFirstVisibleItem = mosaicState.firstVisibleItemIndex
            GallerySession.lastMosaicFirstVisibleOffset = mosaicState.firstVisibleItemScrollOffset
        }
    }

    val trimmedQuery = searchQuery.trim()
    val isSearching = trimmedQuery.isNotEmpty()
    LaunchedEffect(isSearching, entries) {
        if (!isSearching) return@LaunchedEffect
        val cache = GallerySession.entryFilenameCache
        val missing = entries.filter { it.uuid !in cache }
        if (missing.isEmpty()) return@LaunchedEffect
        val resolved = withContext(Dispatchers.IO) {
            EntriesRepository(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                .batchResolve(missing)
        }

        cache.putAll(resolved)
        GallerySession.entryFilenameCacheRevision++
    }

    val filenameCacheRevision = GallerySession.entryFilenameCacheRevision
    val filteredEntries = remember(entries, trimmedQuery, filenameCacheRevision) {
        if (!isSearching) entries else {
            val cache = GallerySession.entryFilenameCache
            entries.filter { entry ->
                val name = cache[entry.uuid]
                name != null && name.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }

    val scrollToViewedUuid = GallerySession.pendingScrollToViewedUuid.value
    LaunchedEffect(scrollToViewedUuid) {
        val uuid = scrollToViewedUuid ?: return@LaunchedEffect
        if (enlarged) return@LaunchedEffect
        val idx = filteredEntries.indexOfFirst { it.uuid == uuid }
        if (idx >= 0) gridState.scrollToItem(idx)
        GallerySession.pendingScrollToViewedUuid.value = null
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (!isSearching) {

            ImportPanel(
                modifier = Modifier.padding(horizontal = 16.dp),
                onBatchComplete = onImportComplete
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.gallery_grid_header,
                    if (loaded) entries.size else 0
                ),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }

        if (isSearching && filteredEntries.isEmpty()) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.search_no_results, trimmedQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.search_no_results_unnamed_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            GalleryGrid(
                entries = filteredEntries,
                trashEnabled = trashEnabled,
                gridState = gridState,
                onView = { entry: VaultEntry ->

                    val idx = entries.indexOfFirst { it.uuid == entry.uuid }
                    if (idx >= 0) onView(idx)
                },
                onDeleteSelection = onDeleteSelection,
                onMoveRequest = onMoveRequest,
                onCopyRequest = onCopyRequest,
                onShareSelection = onShareSelection,
                onExportSelection = onExportSelection,
                onRenameRequest = onRenameRequest,
                onSetWallpaperRequest = onSetWallpaperRequest,
                enlarged = enlarged,
                mosaicState = mosaicState,
                modifier = Modifier.weight(1f),
                gridHeightDp = null
            )
        }
    }
}
