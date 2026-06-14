package dev.encgallery.gallery

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumCoverCropScreen(
    albumMeta: AlbumMeta,
    entry: VaultEntry,
    onSave: (NormalizedRect) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    val context = LocalContext.current
    var bitmap by remember(entry.uuid) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(entry.uuid) { mutableStateOf(false) }

    LaunchedEffect(entry.uuid) {
        val decoded = withContext(Dispatchers.IO) {
            try {
                val plain = EncryptedFileBlob(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                    .decryptToBytes(entry.blobFile)
                val source = ImageDecoder.createSource(ByteBuffer.wrap(plain))
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val w = info.size.width
                    val h = info.size.height
                    val longest = maxOf(w, h)
                    if (longest > EDITOR_LONG_EDGE_PX) {
                        val s = EDITOR_LONG_EDGE_PX.toFloat() / longest
                        decoder.setTargetSize(
                            (w * s).toInt().coerceAtLeast(1),
                            (h * s).toInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (t: Throwable) {
                EncLog.w(
                    "CoverCrop",
                    "decode failed for ${entry.uuid}: ${t.javaClass.simpleName}: ${t.message}"
                )
                null
            }
        }
        if (decoded == null) {
            loadFailed = true
        } else {
            bitmap = decoded
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.cover_crop_title),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = albumMeta.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        ThemedIcon(
                            vector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cover_crop_btn_cancel)
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                when {
                    bmp != null -> CropEditor(
                        bitmap = bmp,
                        onSaveRect = onSave,
                        showSaveBar = true,
                        onCancel = onCancel
                    )
                    loadFailed -> Text(
                        text = stringResource(R.string.viewer_failed_title),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    else -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.cover_crop_loading),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CropEditor(
    bitmap: Bitmap,
    onSaveRect: (NormalizedRect) -> Unit,
    showSaveBar: Boolean,
    onCancel: () -> Unit
) {
    val imgW = bitmap.width.toFloat()
    val imgH = bitmap.height.toFloat()
    val minImgEdge = min(imgW, imgH)

    var cropSidePx by remember(bitmap) { mutableFloatStateOf(minImgEdge) }
    var cropCenterX by remember(bitmap) { mutableFloatStateOf(imgW / 2f) }
    var cropCenterY by remember(bitmap) { mutableFloatStateOf(imgH / 2f) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current

            val maxWPx = with(density) { maxWidth.toPx() }
            val maxHPx = with(density) { maxHeight.toPx() }
            val imgAspect = imgW / imgH
            val containerAspect = maxWPx / maxHPx
            val (displayedW, displayedH) = if (containerAspect > imgAspect) {

                maxHPx * imgAspect to maxHPx
            } else {

                maxWPx to maxWPx / imgAspect
            }
            val displayScale = displayedW / imgW

            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { displayedW.toDp() },
                        height = with(density) { displayedH.toDp() }
                    )
                    .pointerInput(bitmap) {
                        detectTransformGestures { _, panScreen, zoom, _ ->

                            cropSidePx = (cropSidePx * zoom).coerceIn(
                                MIN_CROP_PX,
                                minImgEdge
                            )

                            cropCenterX += panScreen.x / displayScale
                            cropCenterY += panScreen.y / displayScale

                            val half = cropSidePx / 2f
                            cropCenterX = cropCenterX.coerceIn(half, imgW - half)
                            cropCenterY = cropCenterY.coerceIn(half, imgH - half)
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                CropOverlay(
                    cropCenterX = cropCenterX,
                    cropCenterY = cropCenterY,
                    cropSidePx = cropSidePx,
                    displayScale = displayScale,
                    displayedW = displayedW,
                    displayedH = displayedH
                )
            }
        }

        if (showSaveBar) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(
                        text = stringResource(R.string.cover_crop_btn_cancel),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.size(8.dp))
                TextButton(
                    onClick = {
                        val cropX = (cropCenterX - cropSidePx / 2f) / imgW
                        val cropY = (cropCenterY - cropSidePx / 2f) / imgH
                        val cropW = cropSidePx / imgW
                        val cropH = cropSidePx / imgH
                        onSaveRect(NormalizedRect(cropX, cropY, cropW, cropH))
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cover_crop_btn_save),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CropOverlay(
    cropCenterX: Float,
    cropCenterY: Float,
    cropSidePx: Float,
    displayScale: Float,
    displayedW: Float,
    displayedH: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()

                val cropDispSide = cropSidePx * displayScale
                val cropDispCx = cropCenterX * displayScale
                val cropDispCy = cropCenterY * displayScale
                val cropLeft = cropDispCx - cropDispSide / 2f
                val cropTop = cropDispCy - cropDispSide / 2f
                val cropRect = Rect(
                    left = cropLeft,
                    top = cropTop,
                    right = cropLeft + cropDispSide,
                    bottom = cropTop + cropDispSide
                )

                val scrim = Color.Black.copy(alpha = 0.55f)

                drawRect(
                    color = scrim,
                    topLeft = Offset(0f, 0f),
                    size = Size(displayedW, max(0f, cropTop))
                )

                drawRect(
                    color = scrim,
                    topLeft = Offset(0f, cropRect.bottom),
                    size = Size(displayedW, max(0f, displayedH - cropRect.bottom))
                )

                drawRect(
                    color = scrim,
                    topLeft = Offset(0f, cropTop),
                    size = Size(max(0f, cropLeft), cropRect.height)
                )

                drawRect(
                    color = scrim,
                    topLeft = Offset(cropRect.right, cropTop),
                    size = Size(max(0f, displayedW - cropRect.right), cropRect.height)
                )

                drawRect(
                    color = Color.White,
                    topLeft = Offset(cropLeft, cropTop),
                    size = Size(cropDispSide, cropDispSide),
                    style = Stroke(width = 4f)
                )

                val third = cropDispSide / 3f
                val gridColor = Color.White.copy(alpha = 0.4f)

                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft + third, cropTop),
                    end = Offset(cropLeft + third, cropTop + cropDispSide),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft + 2f * third, cropTop),
                    end = Offset(cropLeft + 2f * third, cropTop + cropDispSide),
                    strokeWidth = 1f
                )

                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft, cropTop + third),
                    end = Offset(cropLeft + cropDispSide, cropTop + third),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft, cropTop + 2f * third),
                    end = Offset(cropLeft + cropDispSide, cropTop + 2f * third),
                    strokeWidth = 1f
                )
            }
    )
}

private const val EDITOR_LONG_EDGE_PX = 2048

private const val MIN_CROP_PX = 64f
