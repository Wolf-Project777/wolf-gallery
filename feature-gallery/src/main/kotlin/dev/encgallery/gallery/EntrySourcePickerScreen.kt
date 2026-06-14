package dev.encgallery.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.encgallery.featuresettings.AlbumTileShape

@Composable
fun EntrySourcePickerScreen(
    destAlbumUuid: String,
    destAlbumName: String,
    albums: List<AlbumSummary>,
    groups: List<AlbumGroupSummary>,
    allEntries: List<VaultEntry>,
    columns: Int,
    tileShape: AlbumTileShape,
    enlarged: Boolean,
    onCommit: (Set<String>, EntryPickerOp) -> Unit,
    onCancel: () -> Unit
) {

    var selectedUuids by remember {
        mutableStateOf(GallerySession.lastEntryPickerInnerSelection)
    }
    var openedGroupUuid by remember {
        mutableStateOf(GallerySession.lastEntryPickerOpenedGroupUuid)
    }
    var openedAlbumUuid by remember {
        mutableStateOf(GallerySession.lastEntryPickerOpenedAlbumUuid)
    }
    var showCommitDialog by remember {
        mutableStateOf(GallerySession.lastEntryPickerShowCommitDialog)
    }
    LaunchedEffect(selectedUuids) { GallerySession.lastEntryPickerInnerSelection = selectedUuids }
    LaunchedEffect(openedGroupUuid) { GallerySession.lastEntryPickerOpenedGroupUuid = openedGroupUuid }
    LaunchedEffect(openedAlbumUuid) { GallerySession.lastEntryPickerOpenedAlbumUuid = openedAlbumUuid }
    LaunchedEffect(showCommitDialog) { GallerySession.lastEntryPickerShowCommitDialog = showCommitDialog }

    val resetPickerSession: () -> Unit = {
        selectedUuids = emptySet()
        showCommitDialog = false
        openedGroupUuid = null
        openedAlbumUuid = null
        GallerySession.lastEntryPickerInnerSelection = emptySet()
        GallerySession.lastEntryPickerShowCommitDialog = false
        GallerySession.lastEntryPickerOpenedGroupUuid = null
        GallerySession.lastEntryPickerOpenedAlbumUuid = null
    }

    val openedGroup: AlbumGroupSummary? = openedGroupUuid?.let { uuid ->
        groups.find { it.meta.uuid == uuid }
    }
    val openedAlbum: AlbumSummary? = openedAlbumUuid?.let { uuid ->
        albums.find { it.meta.uuid == uuid }
    }

    LaunchedEffect(groups, openedGroupUuid) {
        if (openedGroupUuid != null && openedGroup == null) openedGroupUuid = null
    }
    LaunchedEffect(albums, openedAlbumUuid) {
        if (openedAlbumUuid != null && openedAlbum == null) openedAlbumUuid = null
    }

    BackHandler {
        when {
            showCommitDialog -> showCommitDialog = false
            openedAlbumUuid != null -> openedAlbumUuid = null
            openedGroupUuid != null -> openedGroupUuid = null
            else -> { resetPickerSession(); onCancel() }
        }
    }

    val groupedAlbumUuids: Set<String> = remember(groups) {
        groups.flatMap { it.meta.albumUuids }.toSet()
    }
    val topLevelAlbums: List<AlbumSummary> = remember(albums, groupedAlbumUuids, destAlbumUuid) {
        albums.filter { it.meta.uuid !in groupedAlbumUuids && it.meta.uuid != destAlbumUuid }
    }
    val groupAlbums: List<AlbumSummary> = openedGroup
        ?.albums
        ?.filter { it.meta.uuid != destAlbumUuid }
        ?: emptyList()

    EntrySourcePickerContent(
        destAlbumName = destAlbumName,
        selectedCount = selectedUuids.size,
        openedGroup = openedGroup,
        openedAlbum = openedAlbum,
        topLevelAlbums = topLevelAlbums,
        groupAlbums = groupAlbums,
        groups = groups,
        allEntries = allEntries,
        selectedUuids = selectedUuids,
        columns = columns,
        tileShape = tileShape,
        enlarged = enlarged,
        onOpenGroup = { uuid -> openedGroupUuid = uuid },
        onCloseGroup = { openedGroupUuid = null },
        onOpenAlbum = { uuid -> openedAlbumUuid = uuid },
        onCloseAlbum = { openedAlbumUuid = null },
        onToggleEntry = { uuid ->
            selectedUuids = if (uuid in selectedUuids) {
                selectedUuids - uuid
            } else {
                selectedUuids + uuid
            }
        },
        onSelectAction = { showCommitDialog = true },
        onCancel = { resetPickerSession(); onCancel() }
    )

    if (showCommitDialog) {
        SourcePickerCommitDialog(
            onCopy = {
                val sel = selectedUuids
                resetPickerSession()
                onCommit(sel, EntryPickerOp.COPY)
            },
            onMove = {
                val sel = selectedUuids
                resetPickerSession()
                onCommit(sel, EntryPickerOp.MOVE)
            },
            onCancel = { showCommitDialog = false }
        )
    }
}
