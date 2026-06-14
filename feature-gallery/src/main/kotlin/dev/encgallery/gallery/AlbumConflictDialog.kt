package dev.encgallery.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred

class AlbumConflictRequest(
    val albumName: String,
    val suggestedName: String,
    val intoGroup: Boolean,
    val nameTaken: (String) -> Boolean,
    val deferred: CompletableDeferred<FileConflictResolution>
)

@Composable
fun AlbumConflictFlow(request: AlbumConflictRequest) {
    var renamePending by remember(request) { mutableStateOf(false) }
    val dupTemplate = stringResource(
        if (request.intoGroup) R.string.album_dup_in_group else R.string.album_dup_in_section
    )
    if (renamePending) {
        AlbumNameDialog(
            title = stringResource(R.string.album_rename_dialog_title),
            nameFieldLabel = stringResource(R.string.album_create_name_label),
            initialName = request.suggestedName,
            confirmLabel = stringResource(R.string.album_rename_btn_save),
            duplicateChecker = { candidate ->
                if (request.nameTaken(candidate)) String.format(dupTemplate, candidate) else null
            },
            allowUnchanged = true,
            onDismiss = { renamePending = false },
            onConfirm = { newName ->
                request.deferred.complete(
                    FileConflictResolution(FileConflictChoice.RENAME, applyToAll = false, newName = newName)
                )
            }
        )
    } else {
        AlbumConflictDialog(
            albumName = request.albumName,
            intoGroup = request.intoGroup,
            onResolve = { res ->

                if (res.choice == FileConflictChoice.RENAME && !res.applyToAll) renamePending = true
                else request.deferred.complete(res)
            }
        )
    }
}

@Composable
fun AlbumConflictDialog(
    albumName: String,
    intoGroup: Boolean,
    onResolve: (FileConflictResolution) -> Unit
) {
    var applyToAll by remember { mutableStateOf(false) }
    val bodyRes = if (intoGroup) R.string.album_dup_in_group else R.string.album_dup_in_section

    InWindowDialog(
        onDismiss = { onResolve(FileConflictResolution(FileConflictChoice.CANCEL, false)) },
        title = { Text(stringResource(R.string.album_conflict_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.file_conflict_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(bodyRes, albumName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyToAll = !applyToAll },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = applyToAll, onCheckedChange = { applyToAll = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.album_conflict_apply_all),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onResolve(FileConflictResolution(FileConflictChoice.SKIP, applyToAll)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.file_conflict_skip)) }
                TextButton(
                    onClick = { onResolve(FileConflictResolution(FileConflictChoice.REPLACE, applyToAll)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.file_conflict_replace)) }
                TextButton(
                    onClick = { onResolve(FileConflictResolution(FileConflictChoice.RENAME, applyToAll)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.file_conflict_rename)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onResolve(FileConflictResolution(FileConflictChoice.CANCEL, false)) }
            ) { Text(stringResource(R.string.album_create_btn_cancel)) }
        }
    )
}
