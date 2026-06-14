package dev.encgallery.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.featuresettings.AlbumTileShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun srcTileCornerShape(columns: Int, tileShape: AlbumTileShape) =
    RoundedCornerShape(
        when {
            tileShape != AlbumTileShape.ROUNDED -> 0.dp
            columns <= 2 -> 16.dp
            else -> 8.dp
        }
    )

@Composable
private fun SrcGroupTile(
    summary: AlbumGroupSummary,
    cornerShape: RoundedCornerShape,
    onClick: () -> Unit
) {

    val mosaicAlbums = remember(summary.albums) {
        summary.albums.take(4)
    }
    val hasAnyCover = mosaicAlbums.any {
        it.meta.coverEntryUuid != null || it.firstEntry != null
    }
    val emptyColors = emptyCoverColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cornerShape)
                .background(emptyColors.tile),
            contentAlignment = Alignment.Center
        ) {
            if (!hasAnyCover) {

                EmptyCoverWolf(
                    color = emptyColors.wolf,
                    res = R.drawable.ic_empty_cover_group,
                    modifier = Modifier.fillMaxSize(0.96f)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        SrcMosaicQuadrant(
                            album = mosaicAlbums.getOrNull(0),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(Modifier.size(1.dp))
                        SrcMosaicQuadrant(
                            album = mosaicAlbums.getOrNull(1),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    Spacer(Modifier.size(1.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        SrcMosaicQuadrant(
                            album = mosaicAlbums.getOrNull(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(Modifier.size(1.dp))
                        SrcMosaicQuadrant(
                            album = mosaicAlbums.getOrNull(3),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = summary.meta.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = pluralStringResource(
                R.plurals.group_album_count,
                summary.albums.size,
                summary.albums.size
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SrcMosaicQuadrant(
    album: AlbumSummary?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customCoverUuid = album?.meta?.coverEntryUuid
    val cacheKey = customCoverUuid ?: album?.firstEntry?.uuid
    var bitmap by remember(cacheKey, album?.meta?.modifiedAt) {
        mutableStateOf<Bitmap?>(null)
    }
    if (album != null) {
        LaunchedEffect(cacheKey, album.meta.modifiedAt) {
            if (customCoverUuid == null && album.firstEntry == null) return@LaunchedEffect
            bitmap = withContext(Dispatchers.IO) {
                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                if (customCoverUuid != null) {
                    val custom = ThumbnailLoader.loadAlbumCover(
                        context = context.applicationContext,
                        albumUuid = album.meta.uuid,
                        keystore = keystore
                    )
                    if (custom != null) return@withContext custom
                }
                album.firstEntry?.let { entry ->
                    ThumbnailLoader.loadOrGenerate(
                        context = context.applicationContext,
                        entry = entry,
                        keystore = keystore
                    )
                }
            }
        }
    }
    Box(
        modifier = modifier.background(emptyCoverColors().tile),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SrcEntryTile(
    entry: VaultEntry,
    selected: Boolean,
    enlarged: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(entry.uuid) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(entry.uuid) {
        bitmap = withContext(Dispatchers.IO) {
            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            ThumbnailLoader.loadOrGenerate(
                context = context.applicationContext,
                entry = entry,
                keystore = keystore,
                gridSized = true
            )
        }?.also {
            GallerySession.mosaicAspectRatios[entry.uuid] =
                it.width.toFloat() / it.height.coerceAtLeast(1)
        }
    }

    val tileAspect: Float = if (enlarged) {
        bitmap?.let { it.width.toFloat() / it.height.coerceAtLeast(1) }
            ?: GallerySession.mosaicAspectRatios[entry.uuid]
            ?: 1f
    } else 1f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(tileAspect)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SrcAlbumTile(
    summary: AlbumSummary,
    cornerShape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val customCoverUuid = summary.meta.coverEntryUuid
    val cacheKey = customCoverUuid ?: summary.firstEntry?.uuid
    var coverBitmap by remember(cacheKey, summary.meta.modifiedAt) {
        mutableStateOf<Bitmap?>(null)
    }
    val firstEntry = summary.firstEntry
    val isEmpty = customCoverUuid == null && firstEntry == null
    val emptyColors = emptyCoverColors()
    LaunchedEffect(cacheKey, summary.meta.modifiedAt) {
        if (customCoverUuid == null && firstEntry == null) return@LaunchedEffect
        coverBitmap = withContext(Dispatchers.IO) {
            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            if (customCoverUuid != null) {
                val custom = ThumbnailLoader.loadAlbumCover(
                    context = context.applicationContext,
                    albumUuid = summary.meta.uuid,
                    keystore = keystore
                )
                if (custom != null) return@withContext custom
            }
            firstEntry?.let { entry ->
                ThumbnailLoader.loadOrGenerate(
                    context = context.applicationContext,
                    entry = entry,
                    keystore = keystore
                )
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cornerShape)
                .background(if (isEmpty) emptyColors.tile else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val bmp = coverBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isEmpty) {

                EmptyCoverWolf(color = emptyColors.wolf, modifier = Modifier.fillMaxSize(0.5f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = summary.meta.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = pluralStringResource(
                R.plurals.album_photo_count,
                summary.entryCount,
                summary.entryCount
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EntrySourceEntryGrid(
    albumUuid: String,
    allEntries: List<VaultEntry>,
    selectedUuids: Set<String>,
    enlarged: Boolean,
    onToggleEntry: (String) -> Unit
) {
    val entries = remember(albumUuid, allEntries) {
        allEntries.filter { it.albumUuid == albumUuid }
    }
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.src_picker_empty_album),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (enlarged) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize(),
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
            verticalItemSpacing = 2.dp,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            staggeredItems(items = entries, key = { "e:${it.uuid}" }) { entry ->
                SrcEntryTile(
                    entry = entry,
                    selected = entry.uuid in selectedUuids,
                    enlarged = true,
                    onClick = { onToggleEntry(entry.uuid) }
                )
            }
        }
    } else {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(3),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(items = entries, key = { "e:${it.uuid}" }) { entry ->
                SrcEntryTile(
                    entry = entry,
                    selected = entry.uuid in selectedUuids,
                    enlarged = false,
                    onClick = { onToggleEntry(entry.uuid) }
                )
            }
        }
    }
}

@Composable
internal fun EntrySourceTopGrid(
    topLevelAlbums: List<AlbumSummary>,
    groups: List<AlbumGroupSummary>,
    columns: Int,
    tileShape: AlbumTileShape,
    onOpenGroup: (String) -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    if (topLevelAlbums.isEmpty() && groups.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.src_picker_empty_albums),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val cornerShape = srcTileCornerShape(columns, tileShape)
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(columns),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = groups, key = { "g:${it.meta.uuid}" }) { groupSummary ->
            SrcGroupTile(
                summary = groupSummary,
                cornerShape = cornerShape,
                onClick = { onOpenGroup(groupSummary.meta.uuid) }
            )
        }
        items(items = topLevelAlbums, key = { "a:${it.meta.uuid}" }) { summary ->
            SrcAlbumTile(
                summary = summary,
                cornerShape = cornerShape,
                onClick = { onOpenAlbum(summary.meta.uuid) }
            )
        }
    }
}

@Composable
internal fun EntrySourceAlbumGrid(
    albums: List<AlbumSummary>,
    columns: Int,
    tileShape: AlbumTileShape,
    onOpenAlbum: (String) -> Unit,
    showEmptyHint: Boolean
) {
    if (showEmptyHint) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.src_picker_empty_albums),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val cornerShape = srcTileCornerShape(columns, tileShape)
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(columns),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = albums, key = { "a:${it.meta.uuid}" }) { summary ->
            SrcAlbumTile(
                summary = summary,
                cornerShape = cornerShape,
                onClick = { onOpenAlbum(summary.meta.uuid) }
            )
        }
    }
}
