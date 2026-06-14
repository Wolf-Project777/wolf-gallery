package dev.encgallery.gallery

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import dev.encgallery.featuresettings.ThemedIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDestinationPickerScreen(
    op: EntryPickerOp,
    selectionCount: Int,
    albums: List<AlbumSummary>,
    groups: List<AlbumGroupSummary>,
    columns: Int,
    tileShape: AlbumTileShape,
    onPick: (String) -> Unit,
    onCreateAlbumInGroup: (groupUuid: String, name: String) -> Unit,
    onCreateAlbum: (name: String) -> Unit,
    onCancel: () -> Unit
) {

    var openedGroupUuid by remember { mutableStateOf<String?>(null) }
    val openedGroup: AlbumGroupSummary? = openedGroupUuid?.let { uuid ->
        groups.find { it.meta.uuid == uuid }
    }

    LaunchedEffect(groups, openedGroupUuid) {
        if (openedGroupUuid != null && openedGroup == null) {
            openedGroupUuid = null
        }
    }

    BackHandler {
        if (openedGroupUuid != null) {
            openedGroupUuid = null
        } else {
            onCancel()
        }
    }

    val groupedAlbumUuids: Set<String> = remember(groups) {
        groups.flatMap { it.meta.albumUuids }.toSet()
    }
    val topLevelAlbums: List<AlbumSummary> = remember(albums, groupedAlbumUuids) {
        albums.filter { it.meta.uuid !in groupedAlbumUuids }
    }
    val visibleAlbums: List<AlbumSummary> = if (openedGroup != null) {
        openedGroup.albums
    } else {
        topLevelAlbums
    }
    val showGroupTiles = openedGroup == null && groups.isNotEmpty()

    val tileCornerShape = RoundedCornerShape(
        when {
            tileShape != AlbumTileShape.ROUNDED -> 0.dp
            columns <= 2 -> 16.dp
            else -> 8.dp
        }
    )

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = {

                    Text(
                        text = if (openedGroup != null) openedGroup.meta.name else stringResource(
                            when (op) {
                                EntryPickerOp.MOVE -> R.string.dest_picker_title_move
                                EntryPickerOp.COPY -> R.string.dest_picker_title_copy
                                EntryPickerOp.NONE -> R.string.dest_picker_title_move
                            }
                        ),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {

                    TextButton(onClick = { showCreateDialog = true }) {
                        Text(stringResource(R.string.album_create_btn_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {

            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            when (op) {
                                EntryPickerOp.COPY -> R.string.dest_picker_op_copy
                                else -> R.string.dest_picker_op_move
                            }
                        ) + " · " + pluralStringResource(
                            R.plurals.dest_picker_subtitle_count,
                            selectionCount,
                            selectionCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancel) {
                        ThemedIcon(
                            vector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dest_picker_cancel)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (visibleAlbums.isEmpty() && !showGroupTiles) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.dest_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                columns = GridCells.Fixed(columns),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showGroupTiles) {
                    items(
                        items = groups,
                        key = { "g:${it.meta.uuid}" }
                    ) { groupSummary ->
                        DestinationGroupTile(
                            summary = groupSummary,
                            cornerShape = tileCornerShape,
                            onClick = { openedGroupUuid = groupSummary.meta.uuid }
                        )
                    }
                }
                items(
                    items = visibleAlbums,
                    key = { "a:${it.meta.uuid}" }
                ) { summary ->
                    DestinationAlbumTile(
                        summary = summary,
                        cornerShape = tileCornerShape,
                        onClick = { onPick(summary.meta.uuid) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {

        val dupInSection = stringResource(R.string.album_dup_in_section)
        val dupInGroup = stringResource(R.string.album_dup_in_group)
        AlbumNameDialog(
            title = stringResource(R.string.album_create_dialog_title),
            nameFieldLabel = stringResource(R.string.album_create_name_label),
            initialName = "",
            confirmLabel = stringResource(R.string.album_create_btn_save),
            duplicateChecker = { candidate ->
                val scope = if (openedGroup != null) openedGroup.albums else topLevelAlbums
                if (scope.any { it.meta.name.trim().equals(candidate, ignoreCase = true) })
                    String.format(if (openedGroup != null) dupInGroup else dupInSection, candidate)
                else null
            },
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                val group = openedGroup
                if (group != null) onCreateAlbumInGroup(group.meta.uuid, name)
                else onCreateAlbum(name)
            }
        )
    }
}

@Composable
private fun DestinationAlbumTile(
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

@Composable
private fun DestinationGroupTile(
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
                        DestinationMosaicQuadrant(
                            album = mosaicAlbums.getOrNull(0),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(Modifier.size(1.dp))
                        DestinationMosaicQuadrant(
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
                        DestinationMosaicQuadrant(
                            album = mosaicAlbums.getOrNull(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(Modifier.size(1.dp))
                        DestinationMosaicQuadrant(
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
private fun DestinationMosaicQuadrant(
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
