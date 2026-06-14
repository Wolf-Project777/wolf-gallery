package dev.encgallery.gallery

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.encgallery.featuresettings.ThemedIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    records: List<TrashRecord>,
    onClose: () -> Unit,
    onRestoreSelection: (Set<String>) -> Unit,
    onPermanentDeleteSelection: (Set<String>) -> Unit,
    onEmptyTrash: () -> Unit,
    onPreviewEntry: (TrashEntry) -> Unit,
    columns: Int,
    tileShape: AlbumTileShape,
    modifier: Modifier = Modifier
) {

    val tileCornerShape = RoundedCornerShape(
        when {
            tileShape != AlbumTileShape.ROUNDED -> 0.dp
            columns <= 2 -> 16.dp
            else -> 8.dp
        }
    )

    var selectedUuids by remember {
        mutableStateOf(GallerySession.lastTrashSelectedUuids)
    }
    LaunchedEffect(selectedUuids) {
        GallerySession.lastTrashSelectedUuids = selectedUuids
    }
    val inSelectionMode = selectedUuids.isNotEmpty()
    val clearSelection = { selectedUuids = emptySet() }

    var showDeleteForeverDialog by remember {
        mutableStateOf(GallerySession.lastTrashShowDeleteForeverDialog)
    }
    LaunchedEffect(showDeleteForeverDialog) {
        GallerySession.lastTrashShowDeleteForeverDialog = showDeleteForeverDialog
    }
    var showEmptyTrashDialog by remember {
        mutableStateOf(GallerySession.lastTrashShowEmptyTrashDialog)
    }
    LaunchedEffect(showEmptyTrashDialog) {
        GallerySession.lastTrashShowEmptyTrashDialog = showEmptyTrashDialog
    }

    val visibleUuidSet: Set<String> = remember(records) {
        records.map { it.uuid }.toSet()
    }

    val trashAlbumsByGroup: Map<String, List<TrashAlbum>> = remember(records) {
        records.filterIsInstance<TrashAlbum>()
            .filter { it.meta.originalGroupUuid != null }
            .groupBy { it.meta.originalGroupUuid!! }
    }
    LaunchedEffect(visibleUuidSet, selectedUuids) {
        val pruned = selectedUuids.filter { it in visibleUuidSet }.toSet()
        if (pruned != selectedUuids) selectedUuids = pruned
    }

    BackHandler {
        if (inSelectionMode) clearSelection() else onClose()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            if (inSelectionMode) {
                                Text(
                                    text = stringResource(
                                        R.string.trash_sel_count,
                                        selectedUuids.size
                                    ),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.trash_screen_title),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.trash_screen_count,
                                        records.size,
                                        records.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = if (inSelectionMode) clearSelection else onClose
                        ) {
                            ThemedIcon(
                                vector = if (inSelectionMode) {
                                    Icons.Default.Close
                                } else {
                                    Icons.Default.ArrowBack
                                },
                                contentDescription = stringResource(
                                    if (inSelectionMode) R.string.trash_sel_cancel
                                    else R.string.trash_screen_back
                                )
                            )
                        }
                    },
                    actions = {

                        if (!inSelectionMode && records.isNotEmpty()) {
                            TextButton(
                                onClick = { showEmptyTrashDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.trash_action_empty))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (records.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ThemedIcon(
                            vector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.trash_screen_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(columns),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 8.dp, end = 8.dp,
                            top = 8.dp,

                            bottom = if (inSelectionMode) 96.dp else 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = records,
                            key = { it.uuid }
                        ) { record ->
                            val toggle: () -> Unit = {
                                selectedUuids = if (record.uuid in selectedUuids) {
                                    selectedUuids - record.uuid
                                } else {
                                    selectedUuids + record.uuid
                                }
                            }
                            TrashTileDispatch(
                                record = record,
                                groupMemberAlbums = if (record is TrashGroup) {
                                    trashAlbumsByGroup[record.uuid].orEmpty()
                                } else emptyList(),
                                cornerShape = tileCornerShape,
                                selected = record.uuid in selectedUuids,
                                inSelectionMode = inSelectionMode,
                                onClick = {
                                    when {
                                        inSelectionMode -> toggle()
                                        record is TrashEntry -> onPreviewEntry(record)

                                    }
                                },
                                onLongClick = toggle
                            )
                        }
                    }
                }

                if (inSelectionMode) {
                    TrashSelectionBottomBar(
                        onRestore = {

                            val snapshot = selectedUuids
                            clearSelection()
                            onRestoreSelection(snapshot)
                        },
                        onPermanentDelete = { showDeleteForeverDialog = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        if (showDeleteForeverDialog) {
            val count = selectedUuids.size
            InWindowDialog(
                onDismiss = { showDeleteForeverDialog = false },
                title = { Text(stringResource(R.string.trash_delete_forever_dialog_title)) },
                text = {
                    Text(
                        pluralStringResource(
                            R.plurals.trash_delete_forever_dialog_body,
                            count, count
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val snapshot = selectedUuids
                            showDeleteForeverDialog = false
                            clearSelection()
                            onPermanentDeleteSelection(snapshot)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.trash_delete_forever_btn_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteForeverDialog = false }) {
                        Text(stringResource(R.string.trash_dialog_btn_cancel))
                    }
                }
            )
        }

        if (showEmptyTrashDialog) {
            val count = records.size
            InWindowDialog(
                onDismiss = { showEmptyTrashDialog = false },
                title = { Text(stringResource(R.string.trash_empty_dialog_title)) },
                text = {
                    Text(
                        pluralStringResource(
                            R.plurals.trash_empty_dialog_body,
                            count, count
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEmptyTrashDialog = false

                            clearSelection()
                            onEmptyTrash()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.trash_empty_btn_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashDialog = false }) {
                        Text(stringResource(R.string.trash_dialog_btn_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun TrashTileDispatch(
    record: TrashRecord,
    groupMemberAlbums: List<TrashAlbum>,
    cornerShape: RoundedCornerShape,
    selected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    when (record) {
        is TrashEntry -> TrashEntryTile(
            entry = record,
            cornerShape = cornerShape,
            selected = selected,
            inSelectionMode = inSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick
        )
        is TrashAlbum -> TrashAlbumTile(
            album = record,
            cornerShape = cornerShape,
            selected = selected,
            inSelectionMode = inSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick
        )
        is TrashGroup -> TrashGroupTile(
            group = record,
            memberAlbums = groupMemberAlbums,
            cornerShape = cornerShape,
            selected = selected,
            inSelectionMode = inSelectionMode,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashEntryTile(
    entry: TrashEntry,
    cornerShape: RoundedCornerShape,
    selected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(entry.uuid) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(entry.uuid) {
        bitmap = withContext(Dispatchers.IO) {
            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            ThumbnailLoader.loadTrashThumb(
                context = context.applicationContext,
                trashEntry = entry,
                keystore = keystore
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(cornerShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = cornerShape
                    )
                } else Modifier
            )
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        DaysRemainingBadge(
            daysLeft = entry.daysUntilPurge(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.30f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
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
        } else if (inSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(
                        width = 1.5.dp,
                        color = Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashAlbumTile(
    album: TrashAlbum,
    cornerShape: RoundedCornerShape,
    selected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val displayName = album.name.ifBlank { stringResource(R.string.trash_album_unnamed) }
    val subtitle = pluralStringResource(
        R.plurals.trash_album_entry_count,
        album.entryCount, album.entryCount
    )

    val isEmpty = album.entryCount == 0
    val emptyColors = emptyCoverColors()
    var cover by remember(album.uuid) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(album.uuid) {
        cover = withContext(Dispatchers.IO) {
            ThumbnailLoader.loadTrashAlbumCover(
                context = context.applicationContext,
                trashAlbum = album,
                keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            )
        }
    }
    TrashContainerTile(
        primary = displayName,
        secondary = subtitle,
        daysLeft = album.daysUntilPurge(),
        cornerShape = cornerShape,
        emptyTile = isEmpty,
        selected = selected,
        inSelectionMode = inSelectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
        coverContent = { mod ->
            val bmp = cover
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = mod
                )
            } else if (isEmpty) {
                Box(modifier = mod, contentAlignment = Alignment.Center) {
                    EmptyCoverWolf(
                        color = emptyColors.wolf,
                        modifier = Modifier.fillMaxSize(0.5f)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashGroupTile(
    group: TrashGroup,
    memberAlbums: List<TrashAlbum>,
    cornerShape: RoundedCornerShape,
    selected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val displayName = group.name.ifBlank { stringResource(R.string.trash_group_unnamed) }
    val subtitle = pluralStringResource(
        R.plurals.trash_group_album_count,
        group.albumCount, group.albumCount
    )

    val mosaic = remember(memberAlbums, group.meta.albumUuids) {
        val orderIndex = group.meta.albumUuids
            .withIndex().associate { (i, u) -> u to i }
        memberAlbums
            .sortedBy { orderIndex[it.uuid] ?: Int.MAX_VALUE }
            .take(4)
    }

    val hasAnyCover = mosaic.any { it.entryCount > 0 }
    val emptyColors = emptyCoverColors()
    TrashContainerTile(
        primary = displayName,
        secondary = subtitle,
        daysLeft = group.daysUntilPurge(),
        cornerShape = cornerShape,
        emptyTile = !hasAnyCover,
        selected = selected,
        inSelectionMode = inSelectionMode,
        onClick = onClick,
        onLongClick = onLongClick,
        coverContent = { mod ->
            if (!hasAnyCover) {
                Box(
                    modifier = mod,
                    contentAlignment = Alignment.Center
                ) {
                    EmptyCoverWolf(
                        color = emptyColors.wolf,
                        res = R.drawable.ic_empty_cover_group,
                        modifier = Modifier.fillMaxSize(0.96f)
                    )
                }
            } else {
                Column(modifier = mod) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        TrashMosaicQuadrant(
                            album = mosaic.getOrNull(0),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(Modifier.size(1.dp))
                        TrashMosaicQuadrant(
                            album = mosaic.getOrNull(1),
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
                        TrashMosaicQuadrant(
                            album = mosaic.getOrNull(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(Modifier.size(1.dp))
                        TrashMosaicQuadrant(
                            album = mosaic.getOrNull(3),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun TrashMosaicQuadrant(
    album: TrashAlbum?,
    modifier: Modifier = Modifier
) {
    if (album == null) {
        Box(
            modifier = modifier.background(emptyCoverColors().tile)
        )
        return
    }
    val context = LocalContext.current
    var bmp by remember(album.uuid) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(album.uuid) {
        bmp = withContext(Dispatchers.IO) {
            ThumbnailLoader.loadTrashAlbumCover(
                context = context.applicationContext,
                trashAlbum = album,
                keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            )
        }
    }
    Box(
        modifier = modifier.background(emptyCoverColors().tile)
    ) {
        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashContainerTile(
    primary: String,
    secondary: String,
    daysLeft: Int,
    cornerShape: RoundedCornerShape,
    emptyTile: Boolean,
    selected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    coverContent: @Composable (Modifier) -> Unit
) {
    val emptyColors = emptyCoverColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cornerShape)
                .background(if (emptyTile) emptyColors.tile else MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = cornerShape
                        )
                    } else Modifier
                )
        ) {
            coverContent(Modifier.fillMaxSize())

            DaysRemainingBadge(
                daysLeft = daysLeft,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )

            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
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
            } else if (inSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.85f),
                            shape = CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = primary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = secondary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DaysRemainingBadge(
    daysLeft: Int,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, label) = when {
        daysLeft <= 0 -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            stringResource(R.string.trash_expired)
        )
        daysLeft <= 7 -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            pluralStringResource(R.plurals.trash_days_remaining, daysLeft, daysLeft)
        )
        else -> Triple(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.onSurface,
            pluralStringResource(R.plurals.trash_days_remaining, daysLeft, daysLeft)
        )
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun TrashSelectionBottomBar(
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sysBars = WindowInsets.systemBars.asPaddingValues()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp, end = 8.dp,
                    top = 6.dp,
                    bottom = 6.dp + sysBars.calculateBottomPadding()
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrashBarItem(
                icon = Icons.Default.Refresh,
                label = stringResource(R.string.trash_action_restore),
                onClick = onRestore
            )
            TrashBarItem(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.trash_action_delete_forever),
                onClick = onPermanentDelete,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TrashBarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val effectiveTint = if (enabled) tint
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ThemedIcon(
            vector = icon,
            contentDescription = null,
            tint = effectiveTint
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = effectiveTint
        )
    }
}
