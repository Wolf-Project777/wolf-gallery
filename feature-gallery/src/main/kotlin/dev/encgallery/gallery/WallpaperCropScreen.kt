package dev.encgallery.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperCropScreen(
    entry: VaultEntry,
    target: WallpaperTarget,
    onSave: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    val context = LocalContext.current

    var bitmap by remember(entry.uuid) {
        val cached = if (entry.uuid == GallerySession.lastWallpaperCropFrameUuid) {
            GallerySession.lastWallpaperCropBitmap
        } else null
        mutableStateOf<Bitmap?>(cached)
    }
    var loadFailed by remember(entry.uuid) { mutableStateOf(false) }

    val screenAspect: Float = remember {
        val (w, h) = readPhysicalScreenSize(context)
        val shorter = minOf(w, h).toFloat()
        val longer = maxOf(w, h).toFloat()
        if (longer <= 0f) 9f / 18f else shorter / longer
    }

    LaunchedEffect(entry.uuid) {

        if (bitmap != null) return@LaunchedEffect
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
                    "WallpaperCrop",
                    "decode failed for ${entry.uuid}: ${t.javaClass.simpleName}: ${t.message}"
                )
                null
            }
        }
        if (decoded == null) {
            loadFailed = true
        } else {
            bitmap = decoded

            GallerySession.lastWallpaperCropFrameUuid = entry.uuid
            GallerySession.lastWallpaperCropBitmap = decoded
        }
    }

    val subtitleId = when (target) {
        WallpaperTarget.HOME -> R.string.wallpaper_crop_subtitle_home
        WallpaperTarget.LOCK -> R.string.wallpaper_crop_subtitle_lock
        WallpaperTarget.BOTH -> R.string.wallpaper_crop_subtitle_both
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.wallpaper_crop_title),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(subtitleId),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        ThemedIcon(
                            vector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.wallpaper_crop_btn_cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            when {
                bmp != null -> WallpaperCropEditor(
                    entryUuid = entry.uuid,
                    bitmap = bmp,
                    aspect = screenAspect,
                    onSave = onSave,
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
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = stringResource(R.string.wallpaper_crop_loading),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperCropEditor(
    entryUuid: String,
    bitmap: Bitmap,
    aspect: Float,
    onSave: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val imgW = bitmap.width.toFloat()
    val imgH = bitmap.height.toFloat()

    val initialCropH: Float
    if (imgW / imgH > aspect) {
        initialCropH = imgH
    } else {
        initialCropH = imgW / aspect
    }
    val maxCropH: Float = initialCropH

    val seedFromSession = entryUuid == GallerySession.lastWallpaperCropFrameUuid &&
        GallerySession.lastWallpaperCropHPx > 0f
    var cropHPx by remember(bitmap, aspect) {
        mutableFloatStateOf(
            if (seedFromSession) GallerySession.lastWallpaperCropHPx
                .coerceAtMost(maxCropH)
            else initialCropH
        )
    }
    var cropCenterX by remember(bitmap) {
        mutableFloatStateOf(
            if (seedFromSession) GallerySession.lastWallpaperCropCenterX
            else imgW / 2f
        )
    }
    var cropCenterY by remember(bitmap) {
        mutableFloatStateOf(
            if (seedFromSession) GallerySession.lastWallpaperCropCenterY
            else imgH / 2f
        )
    }

    LaunchedEffect(entryUuid, cropHPx, cropCenterX, cropCenterY) {
        GallerySession.lastWallpaperCropFrameUuid = entryUuid
        GallerySession.lastWallpaperCropHPx = cropHPx
        GallerySession.lastWallpaperCropCenterX = cropCenterX
        GallerySession.lastWallpaperCropCenterY = cropCenterY
    }

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
                    .pointerInput(bitmap, aspect) {
                        detectTransformGestures { _, panScreen, zoom, _ ->
                            cropHPx = (cropHPx * zoom).coerceIn(
                                max(MIN_CROP_PX, MIN_CROP_PX / aspect),
                                maxCropH
                            )
                            cropCenterX += panScreen.x / displayScale
                            cropCenterY += panScreen.y / displayScale
                            val halfW = (cropHPx * aspect) / 2f
                            val halfH = cropHPx / 2f
                            cropCenterX = cropCenterX.coerceIn(halfW, imgW - halfW)
                            cropCenterY = cropCenterY.coerceIn(halfH, imgH - halfH)
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                WallpaperCropOverlay(
                    cropCenterX = cropCenterX,
                    cropCenterY = cropCenterY,
                    cropWPx = cropHPx * aspect,
                    cropHPx = cropHPx,
                    displayScale = displayScale,
                    displayedW = displayedW,
                    displayedH = displayedH
                )
            }
        }

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
                    text = stringResource(R.string.wallpaper_crop_btn_cancel),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.size(8.dp))
            TextButton(
                onClick = {
                    val cropW = cropHPx * aspect
                    val left = (cropCenterX - cropW / 2f).toInt().coerceAtLeast(0)
                    val top = (cropCenterY - cropHPx / 2f).toInt().coerceAtLeast(0)
                    val width = cropW.toInt()
                        .coerceAtLeast(1)
                        .coerceAtMost(bitmap.width - left)
                    val height = cropHPx.toInt()
                        .coerceAtLeast(1)
                        .coerceAtMost(bitmap.height - top)
                    val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
                    onSave(cropped)
                }
            ) {
                Text(
                    text = stringResource(R.string.wallpaper_crop_btn_save),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WallpaperCropOverlay(
    cropCenterX: Float,
    cropCenterY: Float,
    cropWPx: Float,
    cropHPx: Float,
    displayScale: Float,
    displayedW: Float,
    displayedH: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val cropDispW = cropWPx * displayScale
                val cropDispH = cropHPx * displayScale
                val cropDispCx = cropCenterX * displayScale
                val cropDispCy = cropCenterY * displayScale
                val cropLeft = cropDispCx - cropDispW / 2f
                val cropTop = cropDispCy - cropDispH / 2f
                val cropRect = Rect(
                    left = cropLeft,
                    top = cropTop,
                    right = cropLeft + cropDispW,
                    bottom = cropTop + cropDispH
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
                    size = Size(cropDispW, cropDispH),
                    style = Stroke(width = 4f)
                )
                val thirdW = cropDispW / 3f
                val thirdH = cropDispH / 3f
                val gridColor = Color.White.copy(alpha = 0.4f)
                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft + thirdW, cropTop),
                    end = Offset(cropLeft + thirdW, cropTop + cropDispH),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft + 2f * thirdW, cropTop),
                    end = Offset(cropLeft + 2f * thirdW, cropTop + cropDispH),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft, cropTop + thirdH),
                    end = Offset(cropLeft + cropDispW, cropTop + thirdH),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(cropLeft, cropTop + 2f * thirdH),
                    end = Offset(cropLeft + cropDispW, cropTop + 2f * thirdH),
                    strokeWidth = 1f
                )
            }
    )
}

private fun readPhysicalScreenSize(context: Context): Pair<Int, Int> {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = wm.maximumWindowMetrics.bounds
        bounds.width() to bounds.height()
    } else {
        @Suppress("DEPRECATION")
        Point().also { wm.defaultDisplay.getRealSize(it) }.let { it.x to it.y }
    }
}

private const val EDITOR_LONG_EDGE_PX = 2048
private const val MIN_CROP_PX = 64f
