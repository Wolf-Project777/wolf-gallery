package dev.encgallery.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.encgallery.featuresettings.ThemedIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.PlayArrow
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun GalleryGrid(
    entries: List<VaultEntry>,
    trashEnabled: Boolean,
    onView: (VaultEntry) -> Unit,
    onDeleteSelection: (Set<String>) -> Unit,
    onMoveRequest: (Set<String>) -> Unit,
    onCopyRequest: (Set<String>) -> Unit,
    onShareSelection: (Set<String>) -> Unit,

    onExportSelection: (Set<String>) -> Unit = {},

    onRenameRequest: (uuid: String, newName: String) -> Unit = { _, _ -> },

    onSetWallpaperRequest: (uuid: String, target: WallpaperTarget) -> Unit = { _, _ -> },
    gridState: LazyGridState = rememberLazyGridState(),

    enlarged: Boolean = false,
    mosaicState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,

    gridHeightDp: Int? = GRID_HEIGHT_DP
) {
    val sizeMod: Modifier = if (gridHeightDp != null) {
        Modifier.height(gridHeightDp.dp)
    } else {
        Modifier.fillMaxSize()
    }

    if (entries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(sizeMod),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.gallery_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val aspectStoreContext = LocalContext.current
    LaunchedEffect(Unit) {
        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
        val appCtx = aspectStoreContext.applicationContext
        withContext(Dispatchers.IO) { MosaicAspectStore.loadInto(appCtx, keystore) }
        try {
            while (true) {
                delay(4000)
                withContext(Dispatchers.IO) { MosaicAspectStore.flush(appCtx, keystore) }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                MosaicAspectStore.flush(appCtx, keystore)
            }
        }
    }

    var selectedUuids by remember(GallerySession.selectionRevision) {
        mutableStateOf(GallerySession.lastSelectedEntryUuids)
    }

    var actionStage by remember(
        GallerySession.selectionRevision,
        GallerySession.lastGridActionStage
    ) {
        mutableStateOf(GallerySession.lastGridActionStage)
    }

    var entryOptionsAnchorBounds by remember {
        mutableStateOf(GallerySession.lastEntryOptionsAnchor)
    }

    var explicitSelectionMode by remember(GallerySession.selectionRevision) {
        mutableStateOf(GallerySession.inEntrySelectionMode)
    }
    LaunchedEffect(selectedUuids) {
        GallerySession.lastSelectedEntryUuids = selectedUuids
    }
    LaunchedEffect(actionStage) {
        GallerySession.lastGridActionStage = actionStage
    }
    LaunchedEffect(explicitSelectionMode) {
        GallerySession.inEntrySelectionMode = explicitSelectionMode
    }

    val visibleUuidSet: Set<String> = remember(entries) { entries.map { it.uuid }.toSet() }
    LaunchedEffect(visibleUuidSet, selectedUuids) {
        val pruned = selectedUuids.filter { it in visibleUuidSet }.toSet()
        if (pruned != selectedUuids) {
            selectedUuids = pruned

            if (pruned.isEmpty() && actionStage != GridActionStage.NONE) {
                actionStage = GridActionStage.NONE
            }
        }
    }

    val inSelectionMode = explicitSelectionMode || selectedUuids.isNotEmpty()
    val clearSelection = {
        selectedUuids = emptySet()
        actionStage = GridActionStage.NONE
        explicitSelectionMode = false
    }

    BackHandler(enabled = inSelectionMode) {
        clearSelection()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(sizeMod)
    ) {

        val toggle = { uuid: String ->
            selectedUuids = if (uuid in selectedUuids) {
                selectedUuids - uuid
            } else {
                selectedUuids + uuid
            }
        }
        if (enlarged) {

            MosaicJustified(
                entries = entries,
                state = mosaicState,
                selectedUuids = selectedUuids,
                inSelectionMode = inSelectionMode,
                onView = onView,
                onToggle = { toggle(it) },
                modifier = Modifier.fillMaxSize()
            )
            FastScroller(
                state = mosaicState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        } else {
            LazyVerticalGrid(
                state = gridState,
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
                    GalleryTile(
                        entry = entry,
                        selected = entry.uuid in selectedUuids,
                        inSelectionMode = inSelectionMode,
                        enlarged = false,
                        onClick = {
                            if (inSelectionMode) toggle(entry.uuid) else onView(entry)
                        },
                        onLongClick = { toggle(entry.uuid) }
                    )
                }
            }
            FastScroller(
                state = gridState,
                columns = 3,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }

        if (!inSelectionMode && gridHeightDp == null) {
            if (enlarged) {
                ScrollToTopButton(
                    state = mosaicState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                ScrollToTopButton(
                    state = gridState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        if (inSelectionMode) {
            if (actionStage == GridActionStage.NONE ||
                actionStage == GridActionStage.OPTIONS
            ) {

                val anySelected = selectedUuids.isNotEmpty()
                SelectionBottomBar(
                    onShare = {
                        if (anySelected) onShareSelection(selectedUuids)
                    },
                    onDelete = {
                        if (anySelected) actionStage = GridActionStage.DELETE_CONFIRM
                    },
                    onOptions = { actionStage = GridActionStage.OPTIONS },
                    shareEnabled = anySelected,
                    deleteEnabled = anySelected,
                    optionsAnchorModifier = Modifier.onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            val pos = coords.positionInWindow()
                            val r = androidx.compose.ui.geometry.Rect(
                                left = pos.x,
                                top = pos.y,
                                right = pos.x + coords.size.width,
                                bottom = pos.y + coords.size.height
                            )
                            entryOptionsAnchorBounds = r
                            GallerySession.lastEntryOptionsAnchor = r
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        val resetStage = { actionStage = GridActionStage.NONE }
        when (actionStage) {
            GridActionStage.NONE -> Unit
            GridActionStage.MENU -> Unit

            GridActionStage.OPTIONS -> SelectionOptionsDialog(
                onCopyToAlbum = {
                    onCopyRequest(selectedUuids)
                    resetStage()
                },
                onMoveToAlbum = {
                    onMoveRequest(selectedUuids)
                    resetStage()
                },
                onSelectAll = {
                    selectedUuids = visibleUuidSet
                    resetStage()
                },
                onRename = { actionStage = GridActionStage.RENAME },
                renameEnabled = selectedUuids.size == 1,
                onSetAsWallpaper = { actionStage = GridActionStage.WALLPAPER },
                wallpaperEnabled = selectedUuids.size == 1,
                onExport = {
                    onExportSelection(selectedUuids)
                    resetStage()
                },
                actionsEnabled = selectedUuids.isNotEmpty(),
                anchorBoundsInWindow = entryOptionsAnchorBounds,
                onDismiss = resetStage
            )

            GridActionStage.RENAME -> Unit

            GridActionStage.MOVE_PICK -> Unit
            GridActionStage.COPY_PICK -> Unit

            GridActionStage.WALLPAPER -> {
                val targetUuid = selectedUuids.singleOrNull()
                if (targetUuid == null) {
                    resetStage()
                } else {
                    WallpaperTargetDialog(
                        onPick = { wpTarget ->
                            onSetWallpaperRequest(targetUuid, wpTarget)
                            clearSelection()
                        },
                        onDismiss = resetStage
                    )
                }
            }

            GridActionStage.DELETE_CONFIRM -> SelectionDeleteConfirmDialog(
                count = selectedUuids.size,
                trashEnabled = trashEnabled,
                onDismiss = resetStage,
                onConfirmDelete = {
                    onDeleteSelection(selectedUuids)
                    selectedUuids = emptySet()
                    resetStage()
                }
            )
        }
    }
}

internal data class MosaicRow(
    val items: List<VaultEntry>,
    val heightPx: Int,

    val justified: Boolean,

    val key: String
)

internal fun computeMosaicRows(
    entries: List<VaultEntry>,
    availWidthPx: Int,
    spacingPx: Int,
    targetRowHeightPx: Int,
    maxPerRow: Int
): List<MosaicRow> {
    if (availWidthPx <= 0 || entries.isEmpty()) return emptyList()
    fun aspectOf(e: VaultEntry): Float =
        (GallerySession.mosaicAspectRatios[e.uuid] ?: 1f).coerceIn(0.2f, 5f)
    val rows = ArrayList<MosaicRow>()
    var cur = ArrayList<VaultEntry>()
    var aspectSum = 0f
    for (e in entries) {
        cur.add(e)
        aspectSum += aspectOf(e)

        if (cur.size >= maxPerRow) {
            val rowH = (availWidthPx - spacingPx * (cur.size - 1)) / aspectSum
            rows.add(MosaicRow(cur, rowH.roundToInt().coerceAtLeast(1), true, cur.first().uuid))
            cur = ArrayList()
            aspectSum = 0f
        }
    }
    if (cur.isNotEmpty()) {
        val natural = (availWidthPx - spacingPx * (cur.size - 1)) / aspectSum
        val rowH = minOf(natural, targetRowHeightPx.toFloat())
        rows.add(MosaicRow(cur, rowH.roundToInt().coerceAtLeast(1), false, cur.first().uuid))
    }
    return rows
}

@Composable
private fun MosaicJustified(
    entries: List<VaultEntry>,
    state: LazyListState,
    selectedUuids: Set<String>,
    inSelectionMode: Boolean,
    onView: (VaultEntry) -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val spacingPx = with(density) { 2.dp.roundToPx() }
    val targetRowHeightPx = with(density) { 200.dp.roundToPx() }

    val maxPerRow = 2
    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth
        val rows = remember(entries, widthPx) {
            computeMosaicRows(entries, widthPx, spacingPx, targetRowHeightPx, maxPerRow)
        }

        val pendingUuid = GallerySession.pendingScrollToViewedUuid.value
        LaunchedEffect(pendingUuid, rows) {
            val uuid = pendingUuid ?: return@LaunchedEffect
            val rowIdx = rows.indexOfFirst { row -> row.items.any { it.uuid == uuid } }
            if (rowIdx >= 0) {
                state.scrollToItem(rowIdx)
                GallerySession.pendingScrollToViewedUuid.value = null
            }
        }
        LazyColumn(
            state = state,
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
                        val aspect = (GallerySession.mosaicAspectRatios[entry.uuid] ?: 1f)
                            .coerceIn(0.2f, 5f)
                        val cellMod = if (row.justified) {
                            Modifier.weight(aspect).height(rowHeight)
                        } else {
                            Modifier
                                .width(with(density) { (row.heightPx * aspect).roundToInt().toDp() })
                                .height(rowHeight)
                        }
                        Box(cellMod) {
                            GalleryTile(
                                entry = entry,
                                selected = entry.uuid in selectedUuids,
                                inSelectionMode = inSelectionMode,
                                enlarged = true,
                                mosaicFill = true,
                                onClick = {
                                    if (inSelectionMode) onToggle(entry.uuid) else onView(entry)
                                },
                                onLongClick = { onToggle(entry.uuid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryTile(
    entry: VaultEntry,
    selected: Boolean,
    inSelectionMode: Boolean,
    enlarged: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,

    mosaicFill: Boolean = false
) {
    val context = LocalContext.current
    var bitmap by remember(entry.uuid) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(entry.uuid) { mutableStateOf(false) }

    var isVideo by remember(entry.uuid) {
        mutableStateOf(GallerySession.entryIsVideo[entry.uuid] ?: false)
    }
    LaunchedEffect(entry.uuid) {
        if (entry.uuid in GallerySession.entryIsVideo) {
            isVideo = GallerySession.entryIsVideo[entry.uuid] == true
            return@LaunchedEffect
        }

        delay(180)
        val v = withContext(Dispatchers.IO) {
            try {

                val name = ThumbnailLoader.gated {
                    EntriesRepository(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                        .getMeta(entry.blobFile)?.originalFilename
                }
                MimeSniffer.isLikelyVideo(name, entry.blobSizeBytes)
            } catch (t: Throwable) {
                false
            }
        }
        GallerySession.entryIsVideo[entry.uuid] = v
        isVideo = v
    }

    LaunchedEffect(entry.uuid) {

        var loaded: Bitmap? = null
        var attempt = 0
        while (loaded == null && attempt < 3) {
            if (attempt > 0) delay(120L * attempt)
            loaded = withContext(Dispatchers.IO) {
                ThumbnailLoader.loadOrGenerate(
                    context = context.applicationContext,
                    entry = entry,
                    keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS),
                    gridSized = true
                )
            }
            attempt++
        }
        if (loaded != null) {
            bitmap = loaded
            failed = false

            MosaicAspectStore.put(
                entry.uuid,
                loaded.width.toFloat() / loaded.height.coerceAtLeast(1)
            )
        } else {
            failed = true
        }
    }

    val sizeModifier: Modifier = if (mosaicFill) {
        Modifier.fillMaxSize()
    } else {
        val tileAspect: Float = if (enlarged) {
            GallerySession.mosaicAspectRatios[entry.uuid]
                ?: bitmap?.let { it.width.toFloat() / it.height.coerceAtLeast(1) }
                ?: 1f
        } else 1f
        Modifier.fillMaxWidth().aspectRatio(tileAspect)
    }

    Box(
        modifier = sizeModifier

            .background(MaterialTheme.colorScheme.surfaceVariant)

            .then(
                if (selected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else Modifier
            )

            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,

                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize()
                )
            }
            failed -> {

                Text(
                    text = "✕",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
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
        } else if (inSelectionMode) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
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

        if (isVideo) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                ThemedIcon(
                    vector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.selection_cancel)
                )
            }
            Text(
                text = stringResource(R.string.selection_count, count),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SelectionBottomBar(
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onOptions: () -> Unit,
    shareEnabled: Boolean,
    deleteEnabled: Boolean,
    optionsAnchorModifier: Modifier = Modifier,
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
                    start = 8.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 6.dp + sysBars.calculateBottomPadding()
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionBarItem(
                icon = Icons.Default.Share,
                label = stringResource(R.string.selection_btn_share),
                onClick = onShare,
                enabled = shareEnabled
            )
            ActionBarItem(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.selection_btn_delete),
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error,
                enabled = deleteEnabled
            )
            ActionBarItem(
                icon = Icons.Default.MoreVert,
                label = stringResource(R.string.selection_btn_options),
                onClick = onOptions,
                modifier = optionsAnchorModifier
            )
        }
    }
}

@Composable
private fun ActionBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val effectiveTint = if (enabled) tint
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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

@Composable
private fun SelectionOptionsDialog(
    onCopyToAlbum: () -> Unit,
    onMoveToAlbum: () -> Unit,
    onSelectAll: () -> Unit,
    onRename: () -> Unit,
    renameEnabled: Boolean,
    onSetAsWallpaper: () -> Unit,
    wallpaperEnabled: Boolean,
    onExport: () -> Unit,

    actionsEnabled: Boolean,
    anchorBoundsInWindow: androidx.compose.ui.geometry.Rect?,
    onDismiss: () -> Unit
) {
    InWindowDropdown(
        expanded = true,
        onDismissRequest = onDismiss,
        anchorBoundsInWindow = anchorBoundsInWindow
    ) {
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.selection_opt_copy)) },
            onClick = onCopyToAlbum,
            enabled = actionsEnabled
        )
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.selection_opt_move)) },
            onClick = onMoveToAlbum,
            enabled = actionsEnabled
        )
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.selection_opt_select_all)) },
            onClick = onSelectAll
        )
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.selection_opt_rename)) },
            onClick = onRename,
            enabled = renameEnabled
        )
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.selection_opt_wallpaper)) },
            onClick = onSetAsWallpaper,
            enabled = wallpaperEnabled
        )
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.selection_opt_export)) },
            onClick = onExport,
            enabled = actionsEnabled
        )
    }
}

@Composable
fun EntryRenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,

    allowUnchanged: Boolean = false,

    duplicateChecker: (String) -> String? = { null }
) {

    val originalExt: String = run {
        val dotIdx = initialName.lastIndexOf('.')
        if (dotIdx <= 0 || dotIdx == initialName.length - 1) ""
        else initialName.substring(dotIdx)
    }
    val originalBase: String = if (originalExt.isEmpty()) initialName
        else initialName.substring(0, initialName.length - originalExt.length)

    var fieldValue by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = originalBase,
                selection = androidx.compose.ui.text.TextRange(originalBase.length)
            )
        )
    }
    val trimmedBase = fieldValue.text.trim()
    val canConfirm = trimmedBase.isNotEmpty() && (allowUnchanged || trimmedBase != originalBase)
    var errorText by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.entry_rename_dialog_title)) },
        text = {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            OutlinedTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    errorText = null
                    val sanitizedText = newValue.text.replace("\n", "")
                    val limit = (200 - originalExt.length).coerceAtLeast(1)
                    val capped = sanitizedText.take(limit)
                    fieldValue = if (capped == newValue.text) newValue
                        else newValue.copy(text = capped)
                },
                label = { Text(stringResource(R.string.entry_rename_field_label)) },
                isError = errorText != null,
                supportingText = errorText?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions.Default,
                singleLine = false,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canConfirm) {
                        val full = trimmedBase + originalExt
                        val err = duplicateChecker(full)
                        if (err != null) errorText = err else onConfirm(full)
                    }
                },
                enabled = canConfirm
            ) {
                Text(stringResource(R.string.entry_rename_btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.album_action_cancel))
            }
        }
    )
}

@Composable
private fun SelectionDeleteConfirmDialog(
    count: Int,
    trashEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val bodyResId = if (trashEnabled) {
        R.string.selection_delete_dialog_body
    } else {
        R.string.selection_delete_dialog_body_permanent
    }
    InWindowDialog(
        onDismiss = onDismiss,
        title = {
            Text(stringResource(R.string.selection_delete_dialog_title, count))
        },
        text = {
            Text(
                text = stringResource(bodyResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.gallery_action_btn_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.gallery_action_btn_close))
            }
        }
    )
}

@Composable
private fun EntryActionMenuDialog(
    entry: VaultEntry,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sizeKib = entry.blobSizeBytes / 1024

    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.gallery_action_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.gallery_action_size_line, sizeKib),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.uuid,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onMove,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.gallery_action_btn_move),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(
                    onClick = onCopy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.gallery_action_btn_copy),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.gallery_action_btn_delete),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.gallery_action_btn_close))
            }
        }
    )
}

@Composable
private fun EntryDeleteConfirmDialog(
    entry: VaultEntry,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val sizeKib = entry.blobSizeBytes / 1024
    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.gallery_action_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.gallery_action_size_line, sizeKib),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.uuid,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.gallery_action_delete_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.gallery_action_btn_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.gallery_action_btn_close))
            }
        }
    )
}

@Composable
private fun AlbumPickerDialog(
    title: String,
    albums: List<AlbumSummary>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(title) },
        text = {
            if (albums.isEmpty()) {
                Text(
                    text = stringResource(R.string.gallery_action_picker_no_other_albums),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    listItems(
                        items = albums,
                        key = { it.meta.uuid }
                    ) { summary ->
                        AlbumPickerRow(
                            summary = summary,
                            onClick = { onPick(summary.meta.uuid) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.album_action_cancel))
            }
        }
    )
}

@Composable
private fun AlbumPickerRow(
    summary: AlbumSummary,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var coverBitmap by remember(summary.firstEntry?.uuid) { mutableStateOf<Bitmap?>(null) }
    val firstEntry = summary.firstEntry
    if (firstEntry != null) {
        LaunchedEffect(firstEntry.uuid) {
            coverBitmap = withContext(Dispatchers.IO) {
                ThumbnailLoader.loadOrGenerate(
                    context = context.applicationContext,
                    entry = firstEntry,
                    keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS),
                    gridSized = true
                )
            }
        }
    }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = summary.meta.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = androidx.compose.ui.res.pluralStringResource(
                    R.plurals.album_photo_count,
                    summary.entryCount,
                    summary.entryCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private const val GRID_HEIGHT_DP = 360

private const val GRID_EMPTY_HEIGHT_DP = GRID_HEIGHT_DP

fun deleteEntryOnDisk(entry: VaultEntry) {
    if (entry.blobFile.exists() && !entry.blobFile.delete()) {
        EncLog.w("GalleryGrid", "could not delete blob ${entry.blobFile.name}")
    }
    if (entry.thumbFile.exists() && !entry.thumbFile.delete()) {
        EncLog.w("GalleryGrid", "could not delete thumb ${entry.thumbFile.name}")
    }
    ThumbnailLoader.forget(entry.uuid)
    MosaicAspectStore.forget(entry.uuid)
    EncLog.i("GalleryGrid", "deleted vault entry ${entry.uuid}")
}
