package dev.encgallery.gallery

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.encgallery.featuresettings.ThemedIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrashEntryViewer(
    entries: List<TrashEntry>,
    startIndex: Int,
    onClose: () -> Unit,
    onRestore: (TrashEntry) -> Unit,
    onPurge: (TrashEntry) -> Unit
) {
    if (entries.isEmpty()) {

        LaunchedEffect(Unit) { onClose() }
        return
    }
    val safeStart = startIndex.coerceIn(0, entries.size - 1)
    val pagerState = rememberPagerState(initialPage = safeStart) { entries.size }
    var showPurgeDialog by remember {
        mutableStateOf(GallerySession.lastTrashPurgeDialog)
    }
    LaunchedEffect(showPurgeDialog) {
        GallerySession.lastTrashPurgeDialog = showPurgeDialog
    }

    BackHandler { onClose() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { idx ->

                val entry = entries.getOrNull(idx)
                if (entry != null) {
                    TrashEntryPage(entry)
                }
            }

            val sysBars = WindowInsets.systemBars.asPaddingValues()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(top = sysBars.calculateTopPadding()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    ThemedIcon(
                        vector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.viewer_btn_close),
                        tint = Color.White
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 12.dp,
                            bottom = 12.dp + sysBars.calculateBottomPadding(),
                            start = 8.dp,
                            end = 8.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrashViewerAction(
                        icon = Icons.Default.Refresh,
                        label = stringResource(R.string.trash_action_restore),
                        tint = Color.White,
                        onClick = {
                            val current = entries.getOrNull(pagerState.currentPage) ?: return@TrashViewerAction
                            onRestore(current)
                        }
                    )
                    TrashViewerAction(
                        icon = Icons.Default.Delete,
                        label = stringResource(R.string.trash_action_delete_forever),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { showPurgeDialog = true }
                    )
                }
            }
        }
    }

    if (showPurgeDialog) {
        val current = entries.getOrNull(pagerState.currentPage)
        InWindowDialog(
            onDismiss = { showPurgeDialog = false },
            title = { Text(stringResource(R.string.trash_delete_forever_dialog_title)) },
            text = {

                Text(
                    text = androidx.compose.ui.res.pluralStringResource(
                        R.plurals.trash_delete_forever_dialog_body,
                        1, 1
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPurgeDialog = false
                        current?.let(onPurge)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.trash_delete_forever_btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeDialog = false }) {
                    Text(stringResource(R.string.trash_dialog_btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun TrashEntryPage(entry: TrashEntry) {
    val context = LocalContext.current
    var bitmap by remember(entry.uuid) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(entry.uuid) { mutableStateOf(false) }

    LaunchedEffect(entry.uuid) {
        val (decoded, ok) = withContext(Dispatchers.IO) {
            try {
                val bytes = EncryptedFileBlob(
                    KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                ).decryptToBytes(entry.blobFile)
                val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val (w, h) = info.size.width to info.size.height
                    val longest = maxOf(w, h)
                    if (longest > MAX_PREVIEW_LONG_EDGE_PX) {
                        val s = MAX_PREVIEW_LONG_EDGE_PX.toFloat() / longest
                        decoder.setTargetSize(
                            (w * s).toInt().coerceAtLeast(1),
                            (h * s).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                bmp.prepareToDraw()
                bmp to true
            } catch (t: Throwable) {
                EncLog.w(
                    "TrashViewer",
                    "decode failed for trashed ${entry.uuid}: ${t.javaClass.simpleName}: ${t.message}"
                )
                null to false
            }
        }
        bitmap = decoded
        failed = !ok
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        when {
            bmp != null -> Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ThemedIcon(
                    vector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.viewer_failed_title),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun TrashViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onClick) {
            ThemedIcon(
                vector = icon,
                contentDescription = null,
                tint = tint
            )
        }
        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private const val MAX_PREVIEW_LONG_EDGE_PX = 4096
