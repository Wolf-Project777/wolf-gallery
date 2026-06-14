package dev.encgallery.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.encgallery.featuresettings.ThemedIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.encgallery.featuresettings.AlbumTileShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntrySourcePickerContent(
    destAlbumName: String,
    selectedCount: Int,
    openedGroup: AlbumGroupSummary?,
    openedAlbum: AlbumSummary?,
    topLevelAlbums: List<AlbumSummary>,
    groupAlbums: List<AlbumSummary>,
    groups: List<AlbumGroupSummary>,
    allEntries: List<VaultEntry>,
    selectedUuids: Set<String>,
    columns: Int,
    tileShape: AlbumTileShape,
    enlarged: Boolean,
    onOpenGroup: (String) -> Unit,
    onCloseGroup: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onCloseAlbum: () -> Unit,
    onToggleEntry: (String) -> Unit,
    onSelectAction: () -> Unit,
    onCancel: () -> Unit
) {
    val isAlbumLevel = openedAlbum != null
    val isGroupLevel = !isAlbumLevel && openedGroup != null
    val isTopLevel = !isAlbumLevel && !isGroupLevel

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                isAlbumLevel -> openedAlbum!!.meta.name
                                isGroupLevel -> openedGroup!!.meta.name
                                else -> stringResource(R.string.src_picker_title)
                            },
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isAlbumLevel -> pluralStringResource(
                                    R.plurals.album_photo_count,
                                    openedAlbum!!.entryCount,
                                    openedAlbum.entryCount
                                )
                                isGroupLevel -> pluralStringResource(
                                    R.plurals.group_album_count,
                                    openedGroup!!.albums.size,
                                    openedGroup.albums.size
                                )
                                else -> stringResource(
                                    R.string.src_picker_subtitle_dest,
                                    destAlbumName
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (isTopLevel) {
                        IconButton(onClick = onCancel) {
                            ThemedIcon(
                                vector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.src_picker_close)
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            if (isAlbumLevel) onCloseAlbum() else onCloseGroup()
                        }) {
                            ThemedIcon(
                                vector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.src_picker_close)
                            )
                        }
                    }
                },
                actions = {
                    if (selectedCount > 0) {
                        TextButton(onClick = onSelectAction) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.src_picker_action_select_count,
                                    selectedCount,
                                    selectedCount
                                )
                            )
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
            when {
                isAlbumLevel -> EntrySourceEntryGrid(
                    albumUuid = openedAlbum!!.meta.uuid,
                    allEntries = allEntries,
                    selectedUuids = selectedUuids,
                    enlarged = enlarged,
                    onToggleEntry = onToggleEntry
                )
                isGroupLevel -> EntrySourceAlbumGrid(
                    albums = groupAlbums,
                    columns = columns,
                    tileShape = tileShape,
                    onOpenAlbum = onOpenAlbum,
                    showEmptyHint = groupAlbums.isEmpty()
                )
                else -> EntrySourceTopGrid(
                    topLevelAlbums = topLevelAlbums,
                    groups = groups,
                    columns = columns,
                    tileShape = tileShape,
                    onOpenGroup = onOpenGroup,
                    onOpenAlbum = onOpenAlbum
                )
            }
        }
    }
}

@Composable
internal fun SourcePickerCommitDialog(
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onCancel: () -> Unit
) {
    InWindowDialog(
        onDismiss = onCancel,
        title = { Text(stringResource(R.string.src_picker_commit_title)) },
        text = null,

        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CommitActionRow(
                    label = stringResource(R.string.src_picker_commit_copy),
                    emphasized = true,
                    onClick = onCopy
                )
                CommitActionRow(
                    label = stringResource(R.string.src_picker_commit_move),
                    emphasized = true,
                    onClick = onMove
                )
                CommitActionRow(
                    label = stringResource(R.string.src_picker_commit_cancel),
                    emphasized = false,
                    onClick = onCancel
                )
            }
        }
    )
}

@Composable
private fun CommitActionRow(
    label: String,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .height(52.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasized) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
