package dev.encgallery.gallery

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.KeystoreAesGcm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumCoverPickerScreen(
    albumMeta: AlbumMeta,
    entries: List<VaultEntry>,
    enlarged: Boolean,
    onPick: (VaultEntry) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    val coverGridState = remember(albumMeta.uuid) {
        LazyGridState(
            firstVisibleItemIndex = GallerySession.lastCoverPickerFirstVisibleItem,
            firstVisibleItemScrollOffset = GallerySession.lastCoverPickerFirstVisibleOffset
        )
    }
    val coverMosaicState = remember(albumMeta.uuid) {
        LazyListState(
            firstVisibleItemIndex = GallerySession.lastCoverPickerFirstVisibleItem,
            firstVisibleItemScrollOffset = GallerySession.lastCoverPickerFirstVisibleOffset
        )
    }
    DisposableEffect(albumMeta.uuid, enlarged) {
        onDispose {
            if (enlarged) {
                GallerySession.lastCoverPickerFirstVisibleItem = coverMosaicState.firstVisibleItemIndex
                GallerySession.lastCoverPickerFirstVisibleOffset = coverMosaicState.firstVisibleItemScrollOffset
            } else {
                GallerySession.lastCoverPickerFirstVisibleItem = coverGridState.firstVisibleItemIndex
                GallerySession.lastCoverPickerFirstVisibleOffset = coverGridState.firstVisibleItemScrollOffset
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.cover_picker_title),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = albumMeta.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        ThemedIcon(
                            vector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cover_picker_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.cover_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {

            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (enlarged) {

                    val density = LocalDensity.current
                    val spacingPx = with(density) { 2.dp.roundToPx() }
                    val targetRowHeightPx = with(density) { 200.dp.roundToPx() }
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val widthPx = constraints.maxWidth
                        val rows = remember(entries, widthPx) {
                            computeMosaicRows(entries, widthPx, spacingPx, targetRowHeightPx, 2)
                        }
                        LazyColumn(
                            state = coverMosaicState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listItems(items = rows, key = { it.key }) { row ->
                                val rowHeight = with(density) { row.heightPx.toDp() }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    row.items.forEach { entry ->
                                        val aspect =
                                            (GallerySession.mosaicAspectRatios[entry.uuid] ?: 1f)
                                                .coerceIn(0.2f, 5f)
                                        val cellMod = if (row.justified) {
                                            Modifier.weight(aspect).height(rowHeight)
                                        } else {
                                            Modifier
                                                .width(
                                                    with(density) {
                                                        (row.heightPx * aspect).roundToInt().toDp()
                                                    }
                                                )
                                                .height(rowHeight)
                                        }
                                        Box(cellMod) {
                                            CoverPickerTile(
                                                entry = entry,
                                                enlarged = true,
                                                mosaicFill = true,
                                                onClick = { onPick(entry) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    FastScroller(
                        state = coverMosaicState,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                } else {
                    LazyVerticalGrid(
                        state = coverGridState,
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(3),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            items = entries,
                            key = { it.uuid }
                        ) { entry ->
                            CoverPickerTile(
                                entry = entry,
                                enlarged = false,
                                onClick = { onPick(entry) }
                            )
                        }
                    }
                    FastScroller(
                        state = coverGridState,
                        columns = 3,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverPickerTile(
    entry: VaultEntry,
    enlarged: Boolean,
    onClick: () -> Unit,

    mosaicFill: Boolean = false
) {
    val context = LocalContext.current
    var bitmap by remember(entry.uuid) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(entry.uuid) {

        val loaded = withContext(Dispatchers.IO) {
            ThumbnailLoader.loadOrGenerate(
                context = context.applicationContext,
                entry = entry,
                keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS),
                gridSized = true
            )
        }
        bitmap = loaded

        if (loaded != null) {
            GallerySession.mosaicAspectRatios[entry.uuid] =
                loaded.width.toFloat() / loaded.height.coerceAtLeast(1)
        }
    }

    val tileAspect: Float = if (enlarged) {
        bitmap?.let { it.width.toFloat() / it.height.coerceAtLeast(1) }
            ?: GallerySession.mosaicAspectRatios[entry.uuid]
            ?: 1f
    } else 1f

    val sizeMod = if (mosaicFill) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxWidth().aspectRatio(tileAspect)
    }
    Box(
        modifier = sizeMod
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,

                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
