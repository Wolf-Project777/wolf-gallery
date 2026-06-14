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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import dev.encgallery.featuresettings.AlbumTileShape
import dev.encgallery.featuresettings.LocalThemeVariant
import dev.encgallery.featuresettings.MedievalIcons
import dev.encgallery.featuresettings.ThemeVariant
import dev.encgallery.featuresettings.ThemedIcon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.activity.compose.BackHandler
import dev.encgallery.crypto.KeystoreAesGcm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumsListScreen(
    albums: List<AlbumSummary>,
    groups: List<AlbumGroupSummary>,
    openedGroup: AlbumGroupSummary?,
    loaded: Boolean,
    trashEnabled: Boolean,

    addRequestSignal: Int,

    fabAnchorBounds: Rect?,

    searchQuery: String,
    onCreate: (String) -> Unit,
    onRename: (AlbumMeta, String) -> Unit,
    onChangeCover: (AlbumMeta) -> Unit,
    onDelete: (AlbumMeta) -> Unit,
    onOpen: (AlbumMeta) -> Unit,
    onOpenGroup: (String) -> Unit,
    onCloseGroup: () -> Unit,
    onCreateGroup: (String, Set<String>) -> Unit,

    onImportFolder: () -> Unit,
    onMoveAlbumsRequest: (Set<String>) -> Unit,
    onDeleteAlbumsSelection: (Set<String>) -> Unit,

    onExportAlbums: (Set<String>) -> Unit,
    onRenameGroup: (AlbumGroupMeta, String) -> Unit,
    onDeleteGroup: (AlbumGroupMeta) -> Unit,

    onSetAlbumsPinned: (Set<String>, Boolean) -> Unit,
    onSetGroupPinned: (String, Boolean) -> Unit,

    columns: Int,
    tileShape: AlbumTileShape,
    modifier: Modifier = Modifier
) {

    var dialogType by remember { mutableStateOf(GallerySession.lastAlbumDialogType) }
    var dialogUuid by remember { mutableStateOf(GallerySession.lastAlbumDialogUuid) }
    LaunchedEffect(dialogType, dialogUuid) {
        GallerySession.lastAlbumDialogType = dialogType
        GallerySession.lastAlbumDialogUuid = dialogUuid
    }

    var selectedAlbumUuids by remember(GallerySession.albumSelectionRevision) {
        mutableStateOf(GallerySession.lastSelectedAlbumUuids)
    }
    var selectionStage by remember(GallerySession.albumSelectionRevision) {
        mutableStateOf(GallerySession.lastAlbumSelectionStage)
    }

    var albumOptionsAnchorBounds by remember {
        mutableStateOf(GallerySession.lastAlbumOptionsAnchor)
    }

    var explicitSelectionMode by remember(GallerySession.albumSelectionRevision) {
        mutableStateOf(GallerySession.inAlbumSelectionMode)
    }
    LaunchedEffect(selectedAlbumUuids) {
        GallerySession.lastSelectedAlbumUuids = selectedAlbumUuids
    }
    LaunchedEffect(selectionStage) {
        GallerySession.lastAlbumSelectionStage = selectionStage
    }
    LaunchedEffect(explicitSelectionMode) {
        GallerySession.inAlbumSelectionMode = explicitSelectionMode
    }
    val inSelectionMode = explicitSelectionMode || selectedAlbumUuids.isNotEmpty()
    val resetSelectionStage = { selectionStage = AlbumSelectionStage.NONE }
    val clearSelection = {
        selectedAlbumUuids = emptySet()
        selectionStage = AlbumSelectionStage.NONE
        explicitSelectionMode = false
    }

    BackHandler(enabled = inSelectionMode) { clearSelection() }
    BackHandler(enabled = !inSelectionMode && openedGroup != null) { onCloseGroup() }

    val albumsScrollKey: String? = openedGroup?.meta?.uuid
    val gridState = remember(albumsScrollKey) {
        val (idx, off) = if (albumsScrollKey == null) {
            GallerySession.lastAlbumsGridFirstVisibleItem to
                GallerySession.lastAlbumsGridFirstVisibleOffset
        } else {
            GallerySession.groupAlbumsGridScroll[albumsScrollKey] ?: (0 to 0)
        }
        LazyGridState(firstVisibleItemIndex = idx, firstVisibleItemScrollOffset = off)
    }
    DisposableEffect(albumsScrollKey, gridState) {
        onDispose {
            val idx = gridState.firstVisibleItemIndex
            val off = gridState.firstVisibleItemScrollOffset
            if (albumsScrollKey == null) {
                GallerySession.lastAlbumsGridFirstVisibleItem = idx
                GallerySession.lastAlbumsGridFirstVisibleOffset = off
            } else {
                GallerySession.groupAlbumsGridScroll[albumsScrollKey] = idx to off
            }
        }
    }

    val visibleAlbums: List<AlbumSummary> = if (openedGroup != null) {
        openedGroup.albums
    } else {
        albums
    }

    val trimmedQuery = searchQuery.trim()
    val filteredAlbums = if (trimmedQuery.isEmpty()) visibleAlbums else {
        visibleAlbums.filter { it.meta.name.contains(trimmedQuery, ignoreCase = true) }
    }
    val filteredGroups = if (trimmedQuery.isEmpty() || openedGroup != null) groups else {
        groups.filter { it.meta.name.contains(trimmedQuery, ignoreCase = true) }
    }
    val isSearching = trimmedQuery.isNotEmpty()

    val visibleAlbumUuidSet: Set<String> = remember(visibleAlbums) {
        visibleAlbums.map { it.meta.uuid }.toSet()
    }
    LaunchedEffect(visibleAlbumUuidSet, selectedAlbumUuids) {
        val pruned = selectedAlbumUuids.filter { it in visibleAlbumUuidSet }.toSet()
        if (pruned != selectedAlbumUuids) {
            selectedAlbumUuids = pruned
            if (pruned.isEmpty() && selectionStage != AlbumSelectionStage.NONE) {
                selectionStage = AlbumSelectionStage.NONE
            }
        }
    }

    val dialogTarget: AlbumSummary? = dialogUuid?.let { uuid ->

        albums.find { it.meta.uuid == uuid }
            ?: groups.firstNotNullOfOrNull { g ->
                g.albums.find { it.meta.uuid == uuid }
            }
    }
    LaunchedEffect(loaded, dialogType, dialogTarget) {
        if (loaded && dialogType in setOf(
                AlbumDialogType.ACTION_MENU,
                AlbumDialogType.RENAME,
                AlbumDialogType.DELETE
            ) && dialogTarget == null
        ) {

            dialogType = AlbumDialogType.NONE
            dialogUuid = null
        }
    }

    var groupActionUuid by remember {
        mutableStateOf(GallerySession.lastGroupActionUuid)
    }
    var groupActionStage by remember {
        mutableStateOf(GallerySession.lastGroupActionStage)
    }
    LaunchedEffect(groupActionUuid, groupActionStage) {
        GallerySession.lastGroupActionUuid = groupActionUuid
        GallerySession.lastGroupActionStage = groupActionStage
    }
    val groupActionTarget: AlbumGroupSummary? = groupActionUuid?.let { uuid ->
        groups.find { it.meta.uuid == uuid }
    }

    var fabDialog by remember { mutableStateOf(GallerySession.lastFabDialog) }
    LaunchedEffect(fabDialog) { GallerySession.lastFabDialog = fabDialog }

    val mountedAtSignal = remember { addRequestSignal }
    LaunchedEffect(addRequestSignal) {
        if (addRequestSignal > mountedAtSignal && !inSelectionMode) {

            if (openedGroup != null) {
                dialogType = AlbumDialogType.CREATE
                dialogUuid = null
            } else {
                fabDialog = FabDialogState.CHOICE
            }
        }
    }

    val tileCornerShape = RoundedCornerShape(
        when {
            tileShape != AlbumTileShape.ROUNDED -> 0.dp
            columns <= 2 -> 16.dp
            else -> 8.dp
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            if (openedGroup != null) {
                GroupDrillDownHeader(
                    group = openedGroup,
                    onBack = onCloseGroup
                )
            }

            val hasAnyTiles = if (openedGroup != null) {
                filteredAlbums.isNotEmpty()
            } else {
                filteredGroups.isNotEmpty() || filteredAlbums.isNotEmpty()
            }
            if (!hasAnyTiles) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            isSearching -> stringResource(
                                R.string.search_no_results,
                                trimmedQuery
                            )
                            openedGroup != null -> stringResource(R.string.album_detail_empty)
                            else -> stringResource(R.string.albums_empty_state)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Fixed(columns),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 8.dp, end = 8.dp,
                        top = 8.dp, bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (openedGroup == null) {
                        items(
                            items = filteredGroups,
                            key = { "g:${it.meta.uuid}" }
                        ) { groupSummary ->
                            GroupTile(
                                summary = groupSummary,
                                cornerShape = tileCornerShape,
                                onClick = { onOpenGroup(groupSummary.meta.uuid) },
                                onLongClick = {
                                    groupActionUuid = groupSummary.meta.uuid
                                    groupActionStage = GroupActionStage.ACTION_MENU
                                }
                            )
                        }
                    }
                    items(
                        items = filteredAlbums,
                        key = { "a:${it.meta.uuid}" }
                    ) { summary ->
                        AlbumTile(
                            summary = summary,
                            selected = summary.meta.uuid in selectedAlbumUuids,
                            inSelectionMode = inSelectionMode,
                            cornerShape = tileCornerShape,
                            onClick = {
                                if (inSelectionMode) {
                                    selectedAlbumUuids = if (
                                        summary.meta.uuid in selectedAlbumUuids
                                    ) {
                                        selectedAlbumUuids - summary.meta.uuid
                                    } else {
                                        selectedAlbumUuids + summary.meta.uuid
                                    }
                                } else {
                                    onOpen(summary.meta)
                                }
                            },
                            onLongClick = {
                                selectedAlbumUuids = if (
                                    summary.meta.uuid in selectedAlbumUuids
                                ) {
                                    selectedAlbumUuids - summary.meta.uuid
                                } else {
                                    selectedAlbumUuids + summary.meta.uuid
                                }
                            }
                        )
                    }
                }
                FastScroller(
                    state = gridState,
                    columns = columns,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
                }
            }
        }

        if (inSelectionMode) {
            if (selectionStage == AlbumSelectionStage.NONE ||
                selectionStage == AlbumSelectionStage.OPTIONS
            ) {

                val anySelected = selectedAlbumUuids.isNotEmpty()

                val movableSelected = selectedAlbumUuids - VaultPaths.IMPORTED_ALBUM_UUID
                val deleteEnabled = movableSelected.isNotEmpty() &&
                    selectedAlbumUuids.none { it == VaultPaths.IMPORTED_ALBUM_UUID }
                AlbumSelectionBottomBar(
                    onGroup = {
                        if (movableSelected.isNotEmpty()) selectionStage = AlbumSelectionStage.GROUP_NAME
                    },
                    onMove = {
                        if (movableSelected.isNotEmpty()) onMoveAlbumsRequest(movableSelected)
                    },
                    onShareDisabled = {

                    },
                    onDelete = {
                        if (deleteEnabled) {
                            selectionStage = AlbumSelectionStage.DELETE_CONFIRM
                        }
                    },
                    groupEnabled = movableSelected.isNotEmpty(),
                    moveEnabled = movableSelected.isNotEmpty(),
                    deleteEnabled = deleteEnabled,
                    onOptions = { selectionStage = AlbumSelectionStage.OPTIONS },
                    optionsAnchorModifier = Modifier.onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            val pos = coords.positionInWindow()
                            val r = Rect(
                                left = pos.x,
                                top = pos.y,
                                right = pos.x + coords.size.width,
                                bottom = pos.y + coords.size.height
                            )
                            albumOptionsAnchorBounds = r
                            GallerySession.lastAlbumOptionsAnchor = r
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    val clearDialog = {
        dialogType = AlbumDialogType.NONE
        dialogUuid = null
    }

    val dupAlbumInSection = stringResource(R.string.album_dup_in_section)
    val dupAlbumInGroup = stringResource(R.string.album_dup_in_group)
    val dupGroupInSection = stringResource(R.string.group_dup_in_section)
    fun albumNameChecker(excludeUuid: String?): (String) -> String? = { candidate ->
        val scope = if (openedGroup != null) openedGroup.albums else albums
        val clash = scope.any {
            it.meta.uuid != excludeUuid &&
                it.meta.name.trim().equals(candidate, ignoreCase = true)
        }
        if (clash) String.format(
            if (openedGroup != null) dupAlbumInGroup else dupAlbumInSection, candidate
        ) else null
    }
    fun groupNameChecker(excludeUuid: String?): (String) -> String? = { candidate ->
        val clash = groups.any {
            it.meta.uuid != excludeUuid &&
                it.meta.name.trim().equals(candidate, ignoreCase = true)
        }
        if (clash) String.format(dupGroupInSection, candidate) else null
    }

    when (dialogType) {
        AlbumDialogType.NONE -> Unit

        AlbumDialogType.CREATE -> AlbumNameDialog(
            title = stringResource(R.string.album_create_dialog_title),
            nameFieldLabel = stringResource(R.string.album_create_name_label),
            initialName = "",
            confirmLabel = stringResource(R.string.album_create_btn_save),
            duplicateChecker = albumNameChecker(excludeUuid = null),
            onDismiss = clearDialog,
            onConfirm = { name ->
                onCreate(name)
                clearDialog()
            }
        )

        AlbumDialogType.ACTION_MENU -> dialogTarget?.let { summary ->
            AlbumActionDialog(
                albumName = summary.meta.name,
                isSystemAlbum = summary.meta.uuid == VaultPaths.IMPORTED_ALBUM_UUID,
                onRename = {
                    dialogType = AlbumDialogType.RENAME

                },
                onChangeCover = {

                    onChangeCover(summary.meta)
                    clearDialog()
                },
                onDelete = {
                    dialogType = AlbumDialogType.DELETE
                },
                onDismiss = clearDialog
            )
        }

        AlbumDialogType.RENAME -> dialogTarget?.let { summary ->

            if (summary.meta.uuid == VaultPaths.IMPORTED_ALBUM_UUID) {
                clearDialog()
            } else {
                AlbumNameDialog(
                    title = stringResource(R.string.album_rename_dialog_title),
                    nameFieldLabel = stringResource(R.string.album_create_name_label),
                    initialName = summary.meta.name,
                    confirmLabel = stringResource(R.string.album_rename_btn_save),
                    duplicateChecker = albumNameChecker(excludeUuid = summary.meta.uuid),
                    onDismiss = clearDialog,
                    onConfirm = { newName ->
                        onRename(summary.meta, newName)
                        clearDialog()

                        clearSelection()
                    }
                )
            }
        }

        AlbumDialogType.DELETE -> dialogTarget?.let { summary ->

            if (summary.meta.uuid == VaultPaths.IMPORTED_ALBUM_UUID) {
                clearDialog()
            } else {
                AlbumDeleteDialog(
                    summary = summary,
                    trashEnabled = trashEnabled,
                    onConfirm = {
                        onDelete(summary.meta)
                        clearDialog()
                    },
                    onDismiss = clearDialog
                )
            }
        }
    }

    val singleSelectedAlbum: AlbumSummary? = if (selectedAlbumUuids.size == 1) {
        val uuid = selectedAlbumUuids.first()
        visibleAlbums.find { it.meta.uuid == uuid }
    } else null
    val isOnlySystemAlbumSelected = singleSelectedAlbum?.meta?.uuid ==
        VaultPaths.IMPORTED_ALBUM_UUID

    when (selectionStage) {
        AlbumSelectionStage.NONE -> Unit

        AlbumSelectionStage.OPTIONS -> {

            val selectedSummaries = visibleAlbums.filter {
                it.meta.uuid in selectedAlbumUuids &&
                    it.meta.uuid != VaultPaths.IMPORTED_ALBUM_UUID
            }
            val selectedUnpinned = selectedSummaries
                .filter { !it.meta.pinned }.map { it.meta.uuid }.toSet()
            val selectedPinned = selectedSummaries
                .filter { it.meta.pinned }.map { it.meta.uuid }.toSet()
            AlbumSelectionOptionsDialog(
            selectionCount = selectedAlbumUuids.size,
            isSystemAlbumOnly = isOnlySystemAlbumSelected,
            pinEnabled = selectedUnpinned.isNotEmpty(),
            unpinEnabled = selectedPinned.isNotEmpty(),
            anchorBoundsInWindow = albumOptionsAnchorBounds,
            onPin = {
                onSetAlbumsPinned(selectedUnpinned, true)
                resetSelectionStage()
            },
            onUnpin = {
                onSetAlbumsPinned(selectedPinned, false)
                resetSelectionStage()
            },
            onRename = {

                singleSelectedAlbum?.let { summary ->
                    if (summary.meta.uuid != VaultPaths.IMPORTED_ALBUM_UUID) {
                        dialogUuid = summary.meta.uuid
                        dialogType = AlbumDialogType.RENAME

                    }
                }
                resetSelectionStage()
            },
            onChangeCover = {
                singleSelectedAlbum?.let { summary ->
                    onChangeCover(summary.meta)
                }
                resetSelectionStage()
            },
            onExport = {
                onExportAlbums(selectedAlbumUuids - VaultPaths.IMPORTED_ALBUM_UUID)
                resetSelectionStage()
            },
            exportEnabled = (selectedAlbumUuids - VaultPaths.IMPORTED_ALBUM_UUID).isNotEmpty(),
            onSelectAll = {
                selectedAlbumUuids = visibleAlbumUuidSet
                resetSelectionStage()
            },
            onDismiss = resetSelectionStage
            )
        }

        AlbumSelectionStage.GROUP_NAME -> AlbumNameDialog(
            title = stringResource(R.string.group_create_dialog_title),
            nameFieldLabel = stringResource(R.string.group_create_name_label),
            initialName = "",
            confirmLabel = stringResource(R.string.group_create_btn_save),
            duplicateChecker = groupNameChecker(excludeUuid = null),
            onDismiss = resetSelectionStage,
            onConfirm = { name ->

                onCreateGroup(name, selectedAlbumUuids - VaultPaths.IMPORTED_ALBUM_UUID)
                resetSelectionStage()

            }
        )

        AlbumSelectionStage.DELETE_CONFIRM -> AlbumMultiDeleteConfirmDialog(
            count = selectedAlbumUuids.size,
            trashEnabled = trashEnabled,
            onDismiss = resetSelectionStage,
            onConfirm = {
                onDeleteAlbumsSelection(selectedAlbumUuids)
                resetSelectionStage()
            }
        )

        AlbumSelectionStage.RENAME_NAME -> Unit
    }

    when (fabDialog) {
        FabDialogState.NONE -> Unit
        FabDialogState.CHOICE -> InWindowDialog(
            onDismiss = { fabDialog = FabDialogState.NONE },
            title = {
                Text(
                    text = stringResource(R.string.fab_choose_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CreateChoiceItem(
                        iconRes = R.drawable.ic_album_outline,
                        medievalRes = MedievalIcons.Album,
                        title = stringResource(R.string.fab_choose_album),
                        description = stringResource(R.string.fab_choose_album_desc),
                        onClick = {
                            fabDialog = FabDialogState.NONE
                            dialogType = AlbumDialogType.CREATE
                            dialogUuid = null
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CreateChoiceItem(
                        iconRes = R.drawable.ic_group_outline,
                        medievalRes = MedievalIcons.Folder,
                        title = stringResource(R.string.fab_choose_group),
                        description = stringResource(R.string.fab_choose_group_desc),
                        onClick = { fabDialog = FabDialogState.GROUP_NAME }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CreateChoiceItem(
                        iconRes = R.drawable.ic_import_outline,
                        medievalRes = MedievalIcons.ArrowDown,
                        title = stringResource(R.string.fab_choose_import),
                        description = stringResource(R.string.fab_choose_import_desc),
                        onClick = {
                            fabDialog = FabDialogState.NONE
                            onImportFolder()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { fabDialog = FabDialogState.NONE }) {
                    Text(stringResource(R.string.album_create_btn_cancel))
                }
            }
        )
        FabDialogState.GROUP_NAME -> AlbumNameDialog(
            title = stringResource(R.string.group_create_dialog_title),
            nameFieldLabel = stringResource(R.string.group_create_name_label),
            initialName = "",
            confirmLabel = stringResource(R.string.group_create_btn_save),
            duplicateChecker = groupNameChecker(excludeUuid = null),
            onDismiss = { fabDialog = FabDialogState.NONE },
            onConfirm = { name ->
                onCreateGroup(name, emptySet())
                fabDialog = FabDialogState.NONE
            }
        )
    }

    val clearGroupAction = {
        groupActionUuid = null
        groupActionStage = GroupActionStage.NONE
    }
    when (groupActionStage) {
        GroupActionStage.NONE -> Unit
        GroupActionStage.ACTION_MENU -> groupActionTarget?.let { target ->
            GroupActionDialog(
                groupName = target.meta.name,
                isPinned = target.meta.pinned,
                onTogglePin = {
                    onSetGroupPinned(target.meta.uuid, !target.meta.pinned)
                    clearGroupAction()
                },
                onRename = { groupActionStage = GroupActionStage.RENAME },
                onDelete = { groupActionStage = GroupActionStage.DELETE_CONFIRM },
                onDismiss = clearGroupAction
            )
        }
        GroupActionStage.RENAME -> groupActionTarget?.let { target ->
            AlbumNameDialog(
                title = stringResource(R.string.group_rename_dialog_title),
                nameFieldLabel = stringResource(R.string.group_create_name_label),
                initialName = target.meta.name,
                confirmLabel = stringResource(R.string.album_rename_btn_save),
                duplicateChecker = groupNameChecker(excludeUuid = target.meta.uuid),
                onDismiss = clearGroupAction,
                onConfirm = { newName ->
                    onRenameGroup(target.meta, newName)
                    clearGroupAction()
                }
            )
        }
        GroupActionStage.DELETE_CONFIRM -> groupActionTarget?.let { target ->
            GroupDeleteConfirmDialog(
                summary = target,
                trashEnabled = trashEnabled,
                onConfirm = {
                    onDeleteGroup(target.meta)
                    clearGroupAction()
                },
                onDismiss = clearGroupAction
            )
        }
    }
}

enum class GroupActionStage { NONE, ACTION_MENU, RENAME, DELETE_CONFIRM }

@Composable
private fun CreateChoiceItem(
    iconRes: Int,
    medievalRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {

    val medieval = LocalThemeVariant.current == ThemeVariant.MEDIEVAL
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(if (medieval) medievalRes else iconRes),
            contentDescription = null,
            tint = if (medieval) Color.Unspecified else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal data class EmptyCoverColors(val tile: Color, val wolf: Color)

@Composable
internal fun emptyCoverColors(): EmptyCoverColors {
    val cs = MaterialTheme.colorScheme
    return when {
        cs.surface.luminance() > 0.5f ->
            EmptyCoverColors(tile = cs.surfaceVariant, wolf = cs.onSurfaceVariant)

        cs.surfaceVariant.luminance() < 0.02f -> {
            val tile = lerp(cs.surface, cs.onSurfaceVariant, 0.42f)
            EmptyCoverColors(tile = tile, wolf = lerp(tile, Color.Black, 0.70f))
        }
        else -> {
            val tile = lerp(cs.surface, cs.onSurfaceVariant, 0.30f)
            EmptyCoverColors(tile = tile, wolf = lerp(tile, Color.Black, 0.62f))
        }
    }
}

@Composable
internal fun EmptyCoverWolf(
    color: Color,
    modifier: Modifier = Modifier,
    res: Int = R.drawable.ic_empty_cover_wolf
) {
    Icon(
        painter = painterResource(res),
        contentDescription = null,
        tint = color,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumTile(
    summary: AlbumSummary,
    selected: Boolean,
    inSelectionMode: Boolean,
    cornerShape: RoundedCornerShape,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
                .background(if (isEmpty) emptyColors.tile else MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = cornerShape
                        )
                    } else Modifier
                ),
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

            if (summary.meta.pinned && !inSelectionMode) {
                PinBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        Text(
            text = summary.meta.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
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
private fun PinBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)

            .alpha(0.5f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pin),
            contentDescription = stringResource(R.string.album_pin_badge_desc),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
internal fun AlbumNameDialog(
    title: String,
    nameFieldLabel: String,
    initialName: String,
    confirmLabel: String,

    duplicateChecker: (String) -> String? = { null },

    allowUnchanged: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {

    var fieldValue by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = initialName,
                selection = androidx.compose.ui.text.TextRange(initialName.length)
            )
        )
    }
    val trimmed = fieldValue.text.trim()
    val canConfirm = trimmed.isNotEmpty() && (allowUnchanged || trimmed != initialName)

    var errorText by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(title) },
        text = {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    errorText = null
                    val sanitizedText = newValue.text.replace("\n", "")
                    val capped = sanitizedText.take(80)
                    fieldValue = if (capped == newValue.text) newValue
                        else newValue.copy(text = capped)
                },
                label = { Text(nameFieldLabel) },
                isError = errorText != null,
                supportingText = errorText?.let { msg -> { Text(msg) } },
                keyboardOptions = KeyboardOptions.Default,
                singleLine = false,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canConfirm) {
                        val err = duplicateChecker(trimmed)
                        if (err != null) errorText = err else onConfirm(trimmed)
                    }
                },
                enabled = canConfirm
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.album_create_btn_cancel))
            }
        }
    )
}

@Composable
private fun AlbumActionDialog(
    albumName: String,
    isSystemAlbum: Boolean,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {

    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(albumName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isSystemAlbum) {
                    TextButton(
                        onClick = onRename,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.album_action_rename),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                TextButton(
                    onClick = onChangeCover,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.album_action_change_cover),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!isSystemAlbum) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.album_action_delete),
                            modifier = Modifier.fillMaxWidth()
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
private fun AlbumDeleteDialog(
    summary: AlbumSummary,
    trashEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    val body = if (summary.entryCount == 0) {
        if (trashEnabled) {
            stringResource(R.string.album_delete_dialog_body_empty)
        } else {
            stringResource(R.string.album_delete_dialog_body_empty_permanent)
        }
    } else {
        if (trashEnabled) {
            stringResource(R.string.album_delete_dialog_body_with_entries, summary.entryCount)
        } else {
            stringResource(R.string.album_delete_dialog_body_with_entries_permanent, summary.entryCount)
        }
    }
    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.album_delete_dialog_title)) },
        text = {
            Column {
                Text(text = summary.meta.name, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.album_delete_btn_confirm))
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
private fun AlbumSelectionTopBar(
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
                    contentDescription = stringResource(R.string.album_sel_cancel)
                )
            }
            Text(
                text = stringResource(R.string.album_sel_count, count),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AlbumSelectionBottomBar(
    onGroup: () -> Unit,
    onMove: () -> Unit,
    onShareDisabled: () -> Unit,
    onDelete: () -> Unit,
    groupEnabled: Boolean,
    moveEnabled: Boolean,
    deleteEnabled: Boolean,
    onOptions: () -> Unit,
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
                    start = 8.dp, end = 8.dp,
                    top = 6.dp,
                    bottom = 6.dp + sysBars.calculateBottomPadding()
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumBarItem(
                icon = Icons.Default.Add,
                label = stringResource(R.string.album_sel_btn_group),
                onClick = onGroup,
                enabled = groupEnabled
            )
            AlbumBarItem(
                icon = Icons.Default.Send,
                label = stringResource(R.string.album_sel_btn_move),
                onClick = onMove,
                enabled = moveEnabled
            )
            AlbumBarItem(
                icon = Icons.Default.Share,
                label = stringResource(R.string.album_sel_btn_share),
                onClick = onShareDisabled,
                enabled = false
            )
            AlbumBarItem(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.album_sel_btn_delete),
                onClick = onDelete,
                enabled = deleteEnabled,
                tint = MaterialTheme.colorScheme.error
            )
            AlbumBarItem(
                icon = Icons.Default.MoreVert,
                label = stringResource(R.string.album_sel_btn_options),
                onClick = onOptions,
                modifier = optionsAnchorModifier
            )
        }
    }
}

@Composable
private fun AlbumBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val effectiveTint = if (enabled) tint
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
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
private fun AlbumMultiDeleteConfirmDialog(
    count: Int,
    trashEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val bodyResId = if (trashEnabled) {
        R.string.album_multi_delete_dialog_body
    } else {
        R.string.album_multi_delete_dialog_body_permanent
    }
    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.album_multi_delete_dialog_title, count)) },
        text = {
            Text(
                text = stringResource(bodyResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.album_delete_btn_confirm))
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
private fun AlbumSelectionOptionsDialog(
    selectionCount: Int,
    isSystemAlbumOnly: Boolean,
    pinEnabled: Boolean,
    unpinEnabled: Boolean,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
    onSelectAll: () -> Unit,
    anchorBoundsInWindow: Rect?,
    onDismiss: () -> Unit
) {
    val singleSelect = selectionCount == 1
    val renameEnabled = singleSelect && !isSystemAlbumOnly

    InWindowDropdown(
        expanded = true,
        onDismissRequest = onDismiss,
        anchorBoundsInWindow = anchorBoundsInWindow
    ) {
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.album_sel_opt_rename)) },
            onClick = onRename,
            enabled = renameEnabled
        )
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.album_sel_opt_change_cover)) },
            onClick = onChangeCover,
            enabled = singleSelect
        )

        InWindowDropdownItem(
            text = { Text(stringResource(R.string.album_sel_opt_pin)) },
            onClick = onPin,
            enabled = pinEnabled
        )
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.album_sel_opt_unpin)) },
            onClick = onUnpin,
            enabled = unpinEnabled
        )

        InWindowDropdownItem(
            text = { Text(stringResource(R.string.selection_opt_export)) },
            onClick = onExport,
            enabled = exportEnabled
        )
        InWindowDropdownItem(
            text = { Text(stringResource(R.string.album_sel_opt_select_all)) },
            onClick = onSelectAll
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupTile(
    summary: AlbumGroupSummary,
    cornerShape: RoundedCornerShape,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
                        MosaicQuadrant(
                            album = mosaicAlbums.getOrNull(0),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(Modifier.size(1.dp))
                        MosaicQuadrant(
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
                        MosaicQuadrant(
                            album = mosaicAlbums.getOrNull(2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Spacer(Modifier.size(1.dp))
                        MosaicQuadrant(
                            album = mosaicAlbums.getOrNull(3),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            if (summary.meta.pinned) {
                PinBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = summary.meta.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
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
private fun MosaicQuadrant(
    album: AlbumSummary?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customCoverUuid = album?.meta?.coverEntryUuid
    val cacheKey = customCoverUuid ?: album?.firstEntry?.uuid

    var bitmap by remember(cacheKey, album?.meta?.modifiedAt, album?.entryCount) {
        mutableStateOf<Bitmap?>(null)
    }
    if (album != null) {
        LaunchedEffect(cacheKey, album.meta.modifiedAt, album.entryCount) {
            if (customCoverUuid == null && album.firstEntry == null) return@LaunchedEffect
            bitmap = withContext(Dispatchers.IO) {
                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                if (customCoverUuid != null) {
                    val custom = ThumbnailLoader.loadAlbumCover(
                        context = context.applicationContext,
                        albumUuid = album.meta.uuid,
                        keystore = keystore,
                        gridSized = true
                    )
                    if (custom != null) return@withContext custom
                }
                album.firstEntry?.let { entry ->
                    ThumbnailLoader.loadOrGenerate(
                        context = context.applicationContext,
                        entry = entry,
                        keystore = keystore,
                        gridSized = true
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
private fun GroupActionDialog(
    groupName: String,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(groupName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                TextButton(
                    onClick = onTogglePin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            if (isPinned) R.string.group_action_unpin
                            else R.string.group_action_pin
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(
                    onClick = onRename,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.group_action_rename),
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
                        text = stringResource(R.string.group_action_delete),
                        modifier = Modifier.fillMaxWidth()
                    )
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
private fun GroupDeleteConfirmDialog(
    summary: AlbumGroupSummary,
    trashEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val body = if (summary.albums.isEmpty()) {
        if (trashEnabled) {
            stringResource(R.string.group_delete_dialog_body_empty)
        } else {
            stringResource(R.string.group_delete_dialog_body_empty_permanent)
        }
    } else {
        if (trashEnabled) {
            stringResource(R.string.group_delete_dialog_body_with_albums, summary.albums.size)
        } else {
            stringResource(R.string.group_delete_dialog_body_with_albums_permanent, summary.albums.size)
        }
    }
    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.group_delete_dialog_title)) },
        text = {
            Column {
                Text(text = summary.meta.name, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = body,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.group_delete_btn_confirm))
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
private fun GroupDrillDownHeader(
    group: AlbumGroupSummary,
    onBack: () -> Unit
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
                contentDescription = stringResource(R.string.group_drilldown_back)
            )
        }
        Column(modifier = Modifier.padding(top = 12.dp)) {

            Text(
                text = group.meta.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(
                    R.plurals.group_drilldown_subtitle,
                    group.albums.size,
                    group.albums.size
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    androidx.compose.material3.HorizontalDivider()
}
