package dev.encgallery.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import dev.encgallery.featuresettings.ThemedIcon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.encgallery.crypto.KeystoreAesGcm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.encgallery.logging.EncLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumMeta: AlbumMeta,
    entries: List<VaultEntry>,
    trashEnabled: Boolean,
    onBack: () -> Unit,
    onView: (VaultEntry) -> Unit,
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
    BackHandler { onBack() }

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

    val albumUuid = albumMeta.uuid
    val gridState: LazyGridState = remember(albumUuid) {
        val (idx, off) = GallerySession.albumDetailGridScroll[albumUuid] ?: (0 to 0)
        EncLog.d("AlbumDetail", "gridState init for album $albumUuid: idx=$idx offset=$off")
        LazyGridState(firstVisibleItemIndex = idx, firstVisibleItemScrollOffset = off)
    }
    DisposableEffect(albumUuid, gridState) {
        onDispose {
            GallerySession.albumDetailGridScroll[albumUuid] =
                gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
            EncLog.d(
                "AlbumDetail",
                "gridState disposing album $albumUuid: saved idx=${gridState.firstVisibleItemIndex} offset=${gridState.firstVisibleItemScrollOffset}"
            )
        }
    }

    val mosaicState = remember(albumUuid) {
        val (idx, off) = GallerySession.albumDetailMosaicScroll[albumUuid] ?: (0 to 0)
        androidx.compose.foundation.lazy.LazyListState(
            firstVisibleItemIndex = idx,
            firstVisibleItemScrollOffset = off
        )
    }
    DisposableEffect(albumUuid, mosaicState) {
        onDispose {
            GallerySession.albumDetailMosaicScroll[albumUuid] =
                mosaicState.firstVisibleItemIndex to mosaicState.firstVisibleItemScrollOffset
        }
    }

    val scrollToViewedUuid = GallerySession.pendingScrollToViewedUuid.value
    LaunchedEffect(scrollToViewedUuid) {
        val uuid = scrollToViewedUuid ?: return@LaunchedEffect
        if (enlarged) return@LaunchedEffect
        val idx = entries.indexOfFirst { it.uuid == uuid }
        if (idx >= 0) gridState.scrollToItem(idx)
        GallerySession.pendingScrollToViewedUuid.value = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {

            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),

                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = onBack) {
                        ThemedIcon(
                            vector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.album_detail_back)
                        )
                    }
                    Column(modifier = Modifier.padding(top = 12.dp, end = 12.dp)) {
                        Text(
                            text = albumMeta.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.album_photo_count,
                                entries.size,
                                entries.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (!isSearching) {
                ImportPanel(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    targetAlbumUuid = albumMeta.uuid,
                    onBatchComplete = onImportComplete
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.album_detail_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (isSearching && filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.search_no_results, trimmedQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.search_no_results_unnamed_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                GalleryGrid(
                    entries = filteredEntries,
                    trashEnabled = trashEnabled,
                    onView = onView,
                    onDeleteSelection = onDeleteSelection,
                    onMoveRequest = onMoveRequest,
                    onCopyRequest = onCopyRequest,
                    onShareSelection = onShareSelection,
                    onExportSelection = onExportSelection,
                    onRenameRequest = onRenameRequest,
                    onSetWallpaperRequest = onSetWallpaperRequest,
                    gridState = gridState,
                    enlarged = enlarged,
                    mosaicState = mosaicState,
                    modifier = Modifier.weight(1f),
                    gridHeightDp = null
                )
            }
        }
    }
}
