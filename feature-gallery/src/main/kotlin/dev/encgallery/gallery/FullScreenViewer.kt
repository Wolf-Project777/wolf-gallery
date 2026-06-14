package dev.encgallery.gallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.SurfaceTexture
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import dev.encgallery.featuresettings.AlbumTileShape
import dev.encgallery.featuresettings.ThemedIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.logging.EncLog
import dev.encgallery.storage.EncryptedFileBlob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

@Composable
fun FullScreenViewer(
    entries: List<VaultEntry>,
    startIndex: Int,
    trashEnabled: Boolean,
    tileShape: AlbumTileShape,
    onClose: () -> Unit,
    onDelete: (VaultEntry) -> Unit,
    onCopySingle: (VaultEntry) -> Unit,
    onMoveSingle: (VaultEntry) -> Unit,
    onRenameSingle: (VaultEntry, String) -> Unit,
    onSetWallpaper: (VaultEntry, WallpaperTarget) -> Unit,
    onExportSingle: (VaultEntry) -> Unit = {}
) {
    if (entries.isEmpty() || startIndex !in entries.indices) {

        BackHandler { onClose() }
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex) { entries.size }
    val filmstripListState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        sweepPlaybackTemps(context.applicationContext)
        onDispose { sweepPlaybackTemps(context.applicationContext) }
    }

    var currentScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pagerState.currentPage) {
        GallerySession.lastViewingIndex = pagerState.currentPage
    }

    var pendingDeleteUuid by remember { mutableStateOf(GallerySession.lastViewerDeleteUuid) }
    LaunchedEffect(pendingDeleteUuid) {
        GallerySession.lastViewerDeleteUuid = pendingDeleteUuid
    }

    var pendingRenameUuid by remember {
        mutableStateOf(GallerySession.lastViewerRenameUuid)
    }
    var pendingWallpaperUuid by remember {
        mutableStateOf(GallerySession.lastViewerWallpaperUuid)
    }
    LaunchedEffect(pendingRenameUuid) {
        GallerySession.lastViewerRenameUuid = pendingRenameUuid
    }
    LaunchedEffect(pendingWallpaperUuid) {
        GallerySession.lastViewerWallpaperUuid = pendingWallpaperUuid
    }
    val pendingDelete: VaultEntry? = pendingDeleteUuid?.let { uuid ->
        entries.firstOrNull { it.uuid == uuid }
    }

    LaunchedEffect(pendingDeleteUuid, pendingDelete) {
        if (pendingDeleteUuid != null && pendingDelete == null) {
            pendingDeleteUuid = null
        }
    }

    var pendingInfoUuid by remember { mutableStateOf(GallerySession.lastViewerInfoUuid) }
    LaunchedEffect(pendingInfoUuid) {
        GallerySession.lastViewerInfoUuid = pendingInfoUuid
    }
    val pendingInfo: VaultEntry? = pendingInfoUuid?.let { uuid ->
        entries.firstOrNull { it.uuid == uuid }
    }
    LaunchedEffect(pendingInfoUuid, pendingInfo) {
        if (pendingInfoUuid != null && pendingInfo == null) {
            pendingInfoUuid = null
        }
    }

    val pendingRename: VaultEntry? = pendingRenameUuid?.let { uuid ->
        entries.firstOrNull { it.uuid == uuid }
    }
    LaunchedEffect(pendingRenameUuid, pendingRename) {
        if (pendingRenameUuid != null && pendingRename == null) {
            pendingRenameUuid = null
        }
    }
    val pendingWallpaper: VaultEntry? = pendingWallpaperUuid?.let { uuid ->
        entries.firstOrNull { it.uuid == uuid }
    }
    LaunchedEffect(pendingWallpaperUuid, pendingWallpaper) {
        if (pendingWallpaperUuid != null && pendingWallpaper == null) {
            pendingWallpaperUuid = null
        }
    }

    var uiVisible by remember { mutableStateOf(GallerySession.lastUiVisible) }
    LaunchedEffect(uiVisible) {
        GallerySession.lastUiVisible = uiVisible
    }

    var menuExpanded by remember {
        mutableStateOf(GallerySession.lastViewerMenuExpanded)
    }
    LaunchedEffect(menuExpanded) {
        GallerySession.lastViewerMenuExpanded = menuExpanded
    }

    var menuAnchorBounds by remember {
        mutableStateOf(GallerySession.lastViewerMenuAnchor)
    }

    var menuRootOffset by remember {
        mutableStateOf(GallerySession.lastViewerMenuRootOffset)
    }

    BackHandler { onClose() }

    LaunchedEffect(pagerState.currentPage) {
        val info = filmstripListState.layoutInfo
        val viewportPx = info.viewportEndOffset - info.viewportStartOffset

        val itemPx = info.visibleItemsInfo.firstOrNull()?.size
        if (itemPx == null || viewportPx <= itemPx) {
            filmstripListState.animateScrollToItem(pagerState.currentPage)
        } else {

            val centerOffset = -(viewportPx - itemPx) / 2
            filmstripListState.animateScrollToItem(
                index = pagerState.currentPage,
                scrollOffset = centerOffset
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),

            beyondViewportPageCount = 2,

            userScrollEnabled = currentScale == 1f,

            key = { idx -> entries[idx].uuid }
        ) { pageIndex ->
            PagerPage(
                entry = entries[pageIndex],
                isCurrent = pageIndex == pagerState.currentPage,
                chromeVisible = uiVisible,
                onScaleChange = { newScale ->
                    if (pageIndex == pagerState.currentPage) {
                        currentScale = newScale
                    }
                },
                onTap = { uiVisible = !uiVisible }
            )
        }

        AnimatedVisibility(
            visible = uiVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(
                animationSpec = tween(durationMillis = CHROME_ANIM_MS, easing = LinearOutSlowInEasing),
                initialOffsetY = { fullHeight -> -fullHeight }
            ),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = CHROME_ANIM_MS, easing = FastOutLinearInEasing),
                targetOffsetY = { fullHeight -> -fullHeight }
            )
        ) {
            ViewerTopBar(
                position = pagerState.currentPage + 1,
                total = entries.size,
                onClose = onClose,
                onShare = {

                    val target = entries[pagerState.currentPage]
                    scope.launch {
                        try {
                            EntryShare.share(
                                context = context,
                                entry = target,
                                keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                            )
                        } catch (t: Throwable) {
                            EncLog.w(
                                "Viewer",
                                "share '${target.uuid}' failed: ${t.javaClass.simpleName}: ${t.message}"
                            )
                        }
                    }
                },
                onInfo = {
                    pendingInfoUuid = entries[pagerState.currentPage].uuid
                },
                onDelete = {
                    pendingDeleteUuid = entries[pagerState.currentPage].uuid
                },
                onMenuOpenRequest = { menuExpanded = true },
                onMenuAnchorBoundsChange = { menuAnchorBounds = it }
            )
        }

        AnimatedVisibility(
            visible = uiVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = tween(durationMillis = CHROME_ANIM_MS, easing = LinearOutSlowInEasing),
                initialOffsetY = { fullHeight -> fullHeight }
            ),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = CHROME_ANIM_MS, easing = FastOutLinearInEasing),
                targetOffsetY = { fullHeight -> fullHeight }
            )
        ) {
            Filmstrip(
                entries = entries,
                currentIndex = pagerState.currentPage,
                listState = filmstripListState,
                tileShape = tileShape,
                onClickThumb = { index ->

                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )
        }

        InWindowDropdown(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            anchorBoundsInWindow = menuAnchorBounds,
            seedRootOffset = menuRootOffset,
            onRootOffset = {
                menuRootOffset = it
                GallerySession.lastViewerMenuRootOffset = it
            }
        ) {
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.viewer_menu_copy)) },
                onClick = {
                    menuExpanded = false
                    onCopySingle(entries[pagerState.currentPage])
                }
            )
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.viewer_menu_move)) },
                onClick = {
                    menuExpanded = false
                    onMoveSingle(entries[pagerState.currentPage])
                }
            )
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.viewer_menu_rename)) },
                onClick = {
                    menuExpanded = false
                    pendingRenameUuid = entries[pagerState.currentPage].uuid
                }
            )
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.viewer_menu_wallpaper)) },
                onClick = {
                    menuExpanded = false
                    pendingWallpaperUuid = entries[pagerState.currentPage].uuid
                }
            )
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.viewer_menu_export)) },
                onClick = {
                    menuExpanded = false
                    onExportSingle(entries[pagerState.currentPage])
                }
            )
        }
    }

    CompositionLocalProvider(LocalDialogForceInline provides true) {
    pendingDelete?.let { target ->
        DeleteConfirmDialog(
            entry = target,
            trashEnabled = trashEnabled,
            onDismiss = { pendingDeleteUuid = null },
            onConfirm = {
                pendingDeleteUuid = null

                onDelete(target)
            }
        )
    }

    pendingInfo?.let { target ->
        EntryInfoDialog(
            entry = target,
            onDismiss = { pendingInfoUuid = null }
        )
    }

    pendingRename?.let { target ->
        ViewerRenameDialog(
            entry = target,
            siblings = entries,
            onDismiss = { pendingRenameUuid = null },
            onConfirm = { newName ->
                pendingRenameUuid = null
                onRenameSingle(target, newName)
            }
        )
    }

    pendingWallpaper?.let { target ->
        WallpaperTargetDialog(
            onPick = { wpTarget ->
                pendingWallpaperUuid = null
                onSetWallpaper(target, wpTarget)
            },
            onDismiss = { pendingWallpaperUuid = null }
        )
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagerPage(
    entry: VaultEntry,
    isCurrent: Boolean,
    chromeVisible: Boolean,
    onScaleChange: (Float) -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val viewerTargetPx = remember(configuration, density) {
        val longestDp = maxOf(configuration.screenWidthDp, configuration.screenHeightDp)
        (longestDp * density.density).toInt().coerceIn(1440, MAX_VIEWER_LONG_EDGE_PX)
    }

    var bitmap by remember(entry.uuid) {
        val cached = if (entry.uuid == GallerySession.lastViewedEntryUuid) {
            GallerySession.lastViewerBitmap
        } else null
        mutableStateOf<Bitmap?>(cached)
    }

    var placeholder by remember(entry.uuid) { mutableStateOf<Bitmap?>(null) }
    var unsupported by remember(entry.uuid) { mutableStateOf(false) }
    var failed by remember(entry.uuid) { mutableStateOf(false) }

    var mediaKind by remember(entry.uuid) { mutableStateOf(MediaKind.UNRESOLVED) }
    var animatedDrawable by remember(entry.uuid) { mutableStateOf<AnimatedImageDrawable?>(null) }

    var videoSource by remember(entry.uuid) { mutableStateOf<VideoSource?>(null) }

    var videoAspectRatio by remember(entry.uuid) { mutableFloatStateOf(0f) }

    LaunchedEffect(entry.uuid, isCurrent) {
        if (isCurrent) {
            kotlinx.coroutines.delay(700)
            BlobMigrator.migrateOnOpen(context, entry)
        }
    }

    var scale by remember(entry.uuid) {
        val saved = if (entry.uuid == GallerySession.lastViewedEntryUuid) {
            GallerySession.lastViewerScale
        } else {
            1f
        }
        mutableFloatStateOf(saved)
    }
    var offset by remember(entry.uuid) {
        val saved = if (entry.uuid == GallerySession.lastViewedEntryUuid) {
            Offset(GallerySession.lastViewerOffsetX, GallerySession.lastViewerOffsetY)
        } else {
            Offset.Zero
        }
        mutableStateOf(saved)
    }

    LaunchedEffect(scale, offset, isCurrent, bitmap) {
        if (isCurrent) {
            onScaleChange(scale)
            GallerySession.lastViewedEntryUuid = entry.uuid
            GallerySession.lastViewerScale = scale
            GallerySession.lastViewerOffsetX = offset.x
            GallerySession.lastViewerOffsetY = offset.y
            GallerySession.lastViewerBitmap = bitmap
        }
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var pinchCentroid by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        if (newScale > 1f) {

            val effZoom = newScale / scale
            val cx = containerSize.width / 2f
            val cy = containerSize.height / 2f
            val cRelX = pinchCentroid.x - cx
            val cRelY = pinchCentroid.y - cy
            val focal = Offset(
                cRelX * (1f - effZoom) + offset.x * effZoom + panChange.x,
                cRelY * (1f - effZoom) + offset.y * effZoom + panChange.y
            )
            scale = newScale

            offset = clampViewerOffset(
                focal, newScale, containerSize,
                if (mediaKind == MediaKind.VIDEO) videoAspectRatio else (bitmap?.width?.toFloat() ?: 0f),
                if (mediaKind == MediaKind.VIDEO) 1f else (bitmap?.height?.toFloat() ?: 0f)
            )
        } else {
            scale = newScale
            offset = Offset.Zero
        }
    }

    LaunchedEffect(entry.uuid) {
        if (bitmap != null) return@LaunchedEffect
        placeholder = withContext(Dispatchers.IO) {

            ThumbnailLoader.loadOrGenerate(
                context = context.applicationContext,
                entry = entry,
                keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            )?.also { it.prepareToDraw() }
        }
    }

    LaunchedEffect(entry.uuid) {
        if (bitmap != null) {
            mediaKind = MediaKind.STATIC_IMAGE
            return@LaunchedEffect
        }
        val resolved = withContext(Dispatchers.IO) {
            try {

                val name = EntriesRepository(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                    .getMeta(entry.blobFile)?.originalFilename
                if (MimeSniffer.isLikelyVideo(name, entry.blobSizeBytes)) {
                    return@withContext PageMedia(MediaKind.VIDEO)
                }

                val bytes = EncryptedFileBlob(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                    .decryptToBytes(entry.blobFile)
                val mime = MimeSniffer.sniff(bytes)
                if (mime != null && mime.startsWith("video/")) {
                    return@withContext PageMedia(MediaKind.VIDEO)
                }
                decodeImageBytes(bytes, mime, viewerTargetPx)
            } catch (t: Throwable) {
                EncLog.w(
                    "Viewer",
                    "decrypt ${entry.uuid} failed: ${t.javaClass.simpleName}: ${t.message}"
                )
                PageMedia(MediaKind.UNRESOLVED, failed = true)
            }
        }
        mediaKind = resolved.kind
        bitmap = resolved.bitmap
        animatedDrawable = resolved.animated
        unsupported = resolved.unsupported
        failed = resolved.failed
    }

    LaunchedEffect(entry.uuid, mediaKind, isCurrent) {
        if (mediaKind == MediaKind.VIDEO && isCurrent && videoSource == null) {
            val src = withContext(Dispatchers.IO) {
                try {
                    decryptVideoForPlayback(
                        context.applicationContext,
                        entry.blobFile,
                        KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS),
                    )
                } catch (t: Throwable) {
                    EncLog.w("Viewer", "video decrypt ${entry.uuid} failed: ${t.javaClass.simpleName}")
                    null
                }
            }
            if (src != null) videoSource = src else failed = true
        }
    }

    DisposableEffect(entry.uuid) {
        onDispose { videoSource?.close() }
    }

    val gestureModifier = Modifier

            .pointerInput(entry.uuid) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            var sx = 0f
                            var sy = 0f
                            for (c in pressed) { sx += c.position.x; sy += c.position.y }
                            pinchCentroid = Offset(sx / pressed.size, sy / pressed.size)
                        }
                    }
                }
            }

            .pointerInput(entry.uuid) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tapPos ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {

                            val k = DOUBLE_TAP_ZOOM
                            val cx = containerSize.width / 2f
                            val cy = containerSize.height / 2f
                            val anchored = Offset(
                                (tapPos.x - cx) * (1f - k),
                                (tapPos.y - cy) * (1f - k)
                            )
                            scale = k
                            offset = clampViewerOffset(
                                anchored, k, containerSize,
                                if (mediaKind == MediaKind.VIDEO) videoAspectRatio else (bitmap?.width?.toFloat() ?: 0f),
                                if (mediaKind == MediaKind.VIDEO) 1f else (bitmap?.height?.toFloat() ?: 0f)
                            )
                        }
                    }
                )
            }

            .transformable(
                state = transformableState,
                canPan = { scale > 1f }
            )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .then(gestureModifier),
        contentAlignment = Alignment.Center
    ) {

        when {
            mediaKind == MediaKind.VIDEO -> {
                val vs = videoSource
                if (vs != null) {
                    VideoPlayer(
                        source = vs,
                        entryUuid = entry.uuid,
                        isCurrent = isCurrent,
                        controlsVisible = chromeVisible,
                        scale = scale,
                        offset = offset,
                        onAspect = { videoAspectRatio = it },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {

                    VideoPoster(placeholder = placeholder)
                }
            }
            animatedDrawable != null -> AnimatedImage(
                drawable = animatedDrawable!!,
                scale = scale,
                offset = offset,
                modifier = Modifier.fillMaxSize()
            )
            bitmap != null -> Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,

                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()

                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
            unsupported || failed -> ViewerPlaceholder(
                title = stringResource(R.string.viewer_failed_title),
                body = stringResource(R.string.viewer_failed_body)
            )
            placeholder != null -> Image(
                bitmap = placeholder!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,

                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
            else -> CircularProgressIndicator(color = Color.White)
        }
    }
}

private enum class MediaKind { UNRESOLVED, STATIC_IMAGE, ANIMATED_IMAGE, VIDEO }

private class PageMedia(
    val kind: MediaKind,
    val bitmap: Bitmap? = null,
    val animated: AnimatedImageDrawable? = null,
    val unsupported: Boolean = false,
    val failed: Boolean = false
)

private val POTENTIALLY_ANIMATED_MIMES =
    setOf("image/gif", "image/webp", "image/png", "image/avif")

private fun decodeImageBytes(bytes: ByteArray, mime: String?, targetPx: Int): PageMedia {

    if (mime == null || mime in POTENTIALLY_ANIMATED_MIMES) {
        try {
            val drawable = ImageDecoder.decodeDrawable(
                ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            ) { decoder, info, _ -> applyViewerSampleSize(decoder, info, targetPx) }
            when (drawable) {
                is AnimatedImageDrawable -> {
                    drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    return PageMedia(MediaKind.ANIMATED_IMAGE, animated = drawable)
                }
                is BitmapDrawable -> {
                    drawable.bitmap?.prepareToDraw()
                    return PageMedia(MediaKind.STATIC_IMAGE, bitmap = drawable.bitmap)
                }
            }
        } catch (e: Throwable) {
            EncLog.i("Viewer", "decodeDrawable failed, trying static: ${e.javaClass.simpleName}")
        }
    }
    return try {
        decodeStaticBitmap(bytes, targetPx)
    } catch (e: Throwable) {
        EncLog.i("Viewer", "image decode rejected: ${e.javaClass.simpleName}")
        PageMedia(MediaKind.UNRESOLVED, unsupported = true)
    }
}

private fun decodeStaticBitmap(bytes: ByteArray, targetPx: Int): PageMedia {
    val bmp = ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(ByteBuffer.wrap(bytes))
    ) { decoder, info, _ ->
        applyViewerTargetSize(decoder, info, targetPx)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    bmp.prepareToDraw()
    return PageMedia(MediaKind.STATIC_IMAGE, bitmap = bmp)
}

private fun applyViewerTargetSize(decoder: ImageDecoder, info: ImageDecoder.ImageInfo, targetPx: Int) {
    val longest = maxOf(info.size.width, info.size.height)
    if (longest <= targetPx) return
    var s = 1
    while (longest / (s * 2) >= targetPx) s *= 2
    decoder.setTargetSampleSize(s)
}

private fun clampViewerOffset(
    offset: Offset,
    scale: Float,
    container: IntSize,
    contentW: Float,
    contentH: Float
): Offset {
    if (scale <= 1f) return Offset.Zero
    if (contentW <= 0f || contentH <= 0f || container.width == 0 || container.height == 0) return offset
    val cw = container.width.toFloat()
    val ch = container.height.toFloat()

    val fit = minOf(cw / contentW, ch / contentH)
    val maxX = ((contentW * fit * scale - cw) / 2f).coerceAtLeast(0f)
    val maxY = ((contentH * fit * scale - ch) / 2f).coerceAtLeast(0f)
    return Offset(
        offset.x.coerceIn(-maxX, maxX),
        offset.y.coerceIn(-maxY, maxY)
    )
}

private fun applyViewerSampleSize(decoder: ImageDecoder, info: ImageDecoder.ImageInfo, targetPx: Int) {
    val longest = maxOf(info.size.width, info.size.height)
    if (longest > targetPx) {
        val sample = (longest + targetPx - 1) / targetPx
        if (sample > 1) decoder.setTargetSampleSize(sample)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayer(
    source: VideoSource,
    entryUuid: String,
    isCurrent: Boolean,
    controlsVisible: Boolean,
    scale: Float,
    offset: Offset,
    onAspect: (Float) -> Unit,
    modifier: Modifier = Modifier
) {

    val latestIsCurrent by rememberUpdatedState(isCurrent)
    val player = remember(source) { MediaPlayer() }
    var prepared by remember(source) { mutableStateOf(false) }
    var playing by remember(source) { mutableStateOf(false) }
    var durationMs by remember(source) { mutableIntStateOf(0) }

    var positionMs by remember(source) { mutableFloatStateOf(0f) }
    var scrubbing by remember(source) { mutableStateOf(false) }
    var videoAspect by remember(source) { mutableFloatStateOf(0f) }
    var surface by remember(source) { mutableStateOf<Surface?>(null) }

    var intendedPlaying by remember(source) { mutableStateOf(true) }

    var foreground by remember { mutableStateOf(true) }

    val playerContext = LocalContext.current
    DisposableEffect(playerContext) {
        val owner = playerContext.findLifecycleOwner()
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> foreground = true
                Lifecycle.Event.ON_STOP -> foreground = false
                else -> {}
            }
        }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }

    DisposableEffect(source) {
        player.setOnPreparedListener { mp ->
            mp.isLooping = false
            durationMs = mp.duration.coerceAtLeast(0)
            if (mp.videoWidth > 0 && mp.videoHeight > 0) {
                videoAspect = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
            }

            if (entryUuid == GallerySession.lastViewerVideoUuid) {
                intendedPlaying = GallerySession.lastViewerVideoWasPlaying
                val savedPos = GallerySession.lastViewerVideoPositionMs
                if (savedPos in 1 until durationMs) {

                    scrubbing = true
                    try { mp.seekTo(savedPos) } catch (_: Throwable) { scrubbing = false }
                    positionMs = savedPos.toFloat()
                }
            }
            prepared = true
        }
        player.setOnVideoSizeChangedListener { _, w, h ->
            if (w > 0 && h > 0) videoAspect = w.toFloat() / h.toFloat()
        }
        player.setOnCompletionListener {

            intendedPlaying = false
            playing = false
            positionMs = durationMs.toFloat()
        }
        player.setOnSeekCompleteListener {

            scrubbing = false
        }
        player.setOnErrorListener { _, what, extra ->
            EncLog.w("Viewer", "MediaPlayer error what=$what extra=$extra")
            true
        }
        try {
            when (source) {
                is VideoSource.InMemory -> player.setDataSource(source.dataSource)
                is VideoSource.TempFile -> player.setDataSource(source.file.absolutePath)
            }
            player.prepareAsync()
        } catch (t: Throwable) {
            EncLog.w("Viewer", "video setDataSource failed: ${t.javaClass.simpleName}")
        }
        onDispose {

            if (latestIsCurrent) {
                GallerySession.lastViewerVideoUuid = entryUuid
                GallerySession.lastViewerVideoPositionMs = positionMs.toInt()

                GallerySession.lastViewerVideoWasPlaying = intendedPlaying
            }
            try { player.release() } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(surface, prepared, isCurrent, intendedPlaying, foreground) {
        val s = surface
        if (s != null) {
            try { player.setSurface(s) } catch (_: Throwable) {}
        }
        if (!prepared) return@LaunchedEffect

        val shouldPlay = isCurrent && s != null && intendedPlaying && foreground
        try {
            if (shouldPlay) {
                if (!player.isPlaying) player.start()
                playing = true
            } else {
                if (player.isPlaying) player.pause()
                playing = false
            }
        } catch (_: Throwable) {}
    }

    LaunchedEffect(prepared, playing, scrubbing) {
        if (prepared && playing && !scrubbing) {

            val startPos = try { player.currentPosition } catch (_: Throwable) { positionMs.toInt() }
            positionMs = startPos.toFloat()
            val settleStart = withFrameMillis { it }
            var basePos = startPos
            while (true) {
                val now = withFrameMillis { it }
                val cur = try { player.currentPosition } catch (_: Throwable) { startPos }
                if (cur > startPos || now - settleStart >= 300) { basePos = cur; break }
            }
            var baseFrame = withFrameMillis { it }

            var shown = basePos.toFloat()
            positionMs = shown
            while (true) {
                val frame = withFrameMillis { it }
                val elapsed = frame - baseFrame
                val interpolated = (basePos + elapsed).toFloat()
                    .coerceIn(0f, durationMs.toFloat().coerceAtLeast(0f))
                if (interpolated > shown) {
                    shown = interpolated
                    positionMs = shown
                }
                if (elapsed >= 700) {
                    basePos = try { player.currentPosition } catch (_: Throwable) { basePos + elapsed.toInt() }
                    baseFrame = frame
                }
            }
        }
    }

    LaunchedEffect(videoAspect) { onAspect(videoAspect) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            surface = Surface(st)
                        }
                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            try { player.setSurface(null) } catch (_: Throwable) {}
                            surface = null
                            return true
                        }
                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },

            modifier = (if (videoAspect > 0f) {
                Modifier.aspectRatio(videoAspect)
            } else {
                Modifier.fillMaxSize()
            }).graphicsLayer {

                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
        )

        if (controlsVisible) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable {

                        intendedPlaying = !intendedPlaying
                    },
                contentAlignment = Alignment.Center
            ) {
                if (playing) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) {
                            Box(
                                Modifier
                                    .size(width = 7.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.viewer_video_play),
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        if (controlsVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = FILMSTRIP_HEIGHT_DP.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatVideoTime(positionMs.toInt()),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )

                val seekColors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.30f),
                    disabledThumbColor = Color.White.copy(alpha = 0.5f),
                    disabledActiveTrackColor = Color.White.copy(alpha = 0.3f),
                    disabledInactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
                Slider(
                    value = positionMs.coerceIn(0f, durationMs.toFloat().coerceAtLeast(0f)),
                    onValueChange = {
                        scrubbing = true
                        positionMs = it
                    },
                    onValueChangeFinished = {

                        try {
                            player.seekTo(positionMs.toLong(), MediaPlayer.SEEK_CLOSEST)
                        } catch (_: Throwable) {
                            scrubbing = false
                        }
                    },
                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                    enabled = prepared && durationMs > 0,
                    colors = seekColors,
                    thumb = { _: SliderState ->

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    },
                    track = { state: SliderState ->
                        SliderDefaults.Track(
                            sliderState = state,
                            colors = seekColors,

                            drawStopIndicator = null,
                            thumbTrackGapSize = 0.dp,
                            modifier = Modifier.height(3.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = formatVideoTime(durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatVideoTime(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is LifecycleOwner) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun VideoPoster(placeholder: Bitmap?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (placeholder != null) {
            Image(
                bitmap = placeholder.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.viewer_video_play),
            tint = Color.White,
            modifier = Modifier.size(72.dp)
        )
    }
}

@Composable
private fun AnimatedImage(
    drawable: AnimatedImageDrawable,
    scale: Float,
    offset: Offset,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(drawable)
                if (!drawable.isRunning) drawable.start()
            }
        },
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        update = { iv ->
            (iv.drawable as? AnimatedImageDrawable)?.let { if (!it.isRunning) it.start() }
        }
    )
}

@Composable
private fun ViewerTopBar(
    position: Int,
    total: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
    onMenuOpenRequest: () -> Unit,
    onMenuAnchorBoundsChange: (Rect) -> Unit
) {
    val chromeBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val chromeFg = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(chromeBg)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            ThemedIcon(
                vector = Icons.Default.Close,
                contentDescription = stringResource(R.string.viewer_btn_close),
                tint = chromeFg
            )
        }

        Text(
            text = "$position / $total",
            color = chromeFg,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onInfo) {
            ThemedIcon(
                vector = Icons.Default.Info,
                contentDescription = stringResource(R.string.viewer_btn_info),
                tint = chromeFg
            )
        }
        IconButton(onClick = onShare) {
            ThemedIcon(
                vector = Icons.Default.Share,
                contentDescription = stringResource(R.string.viewer_btn_share),
                tint = chromeFg
            )
        }
        IconButton(onClick = onDelete) {

            ThemedIcon(
                vector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.viewer_btn_delete),
                tint = MaterialTheme.colorScheme.error
            )
        }
        IconButton(
            onClick = { onMenuOpenRequest() },
            modifier = Modifier.onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    val pos = coords.positionInWindow()
                    val r = Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + coords.size.width,
                        bottom = pos.y + coords.size.height
                    )
                    onMenuAnchorBoundsChange(r)
                    GallerySession.lastViewerMenuAnchor = r
                }
            }
        ) {
            ThemedIcon(
                vector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.viewer_btn_more),
                tint = chromeFg
            )
        }
    }
}

@Composable
private fun EntryInfoDialog(
    entry: VaultEntry,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val cached = GallerySession.infoProbeCache[entry.uuid]
    var mime by remember(entry.uuid) { mutableStateOf(cached?.mime) }
    var dimensions by remember(entry.uuid) { mutableStateOf(cached?.dimensions) }
    var originalFilename by remember(entry.uuid) {
        mutableStateOf(cached?.originalFilename)
    }
    var probing by remember(entry.uuid) { mutableStateOf(cached == null) }

    LaunchedEffect(entry.uuid) {
        if (cached != null) return@LaunchedEffect
        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
        val probe = withContext(Dispatchers.IO) {

            val resolvedName = try {
                EntriesRepository(keystore).getMeta(entry.blobFile)?.originalFilename
            } catch (t: Throwable) {
                EncLog.w(
                    "Viewer",
                    "info-dialog filename read failed for ${entry.uuid}: ${t.javaClass.simpleName}"
                )
                null
            }
            try {
                val bytes = EncryptedFileBlob(keystore).decryptToBytes(entry.blobFile)
                val mimeOut = MimeSniffer.sniff(bytes)
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                val dimsOut = if (opts.outWidth > 0 && opts.outHeight > 0) {
                    opts.outWidth to opts.outHeight
                } else null
                EntryInfoProbe(
                    mime = mimeOut,
                    dimensions = dimsOut,
                    originalFilename = resolvedName
                )
            } catch (t: Throwable) {
                EncLog.w(
                    "Viewer",
                    "info-dialog probe failed for ${entry.uuid}: ${t.javaClass.simpleName}"
                )
                EntryInfoProbe(
                    mime = null,
                    dimensions = null,
                    originalFilename = resolvedName
                )
            }
        }
        mime = probe.mime
        dimensions = probe.dimensions
        originalFilename = probe.originalFilename
        probing = false

        GallerySession.infoProbeCache[entry.uuid] = probe
    }

    val sizeKib = entry.blobSizeBytes / 1024
    val dateAdded = remember(entry.mtimeMillis) {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(entry.mtimeMillis))
    }

    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.viewer_info_dialog_title)) },
        text = {
            Column {

                InfoRow(
                    label = stringResource(R.string.viewer_info_label_filename),
                    value = when {
                        probing -> stringResource(R.string.viewer_info_value_probing)
                        originalFilename != null -> originalFilename!!
                        else -> stringResource(R.string.viewer_info_value_no_filename)
                    }
                )
                InfoRow(
                    label = stringResource(R.string.viewer_info_label_added),
                    value = dateAdded
                )
                InfoRow(
                    label = stringResource(R.string.viewer_info_label_enc_size),
                    value = "$sizeKib KiB"
                )
                InfoRow(
                    label = stringResource(R.string.viewer_info_label_format),
                    value = when {
                        probing -> stringResource(R.string.viewer_info_value_probing)
                        mime != null -> mime!!
                        else -> stringResource(R.string.viewer_info_value_unknown)
                    }
                )
                InfoRow(
                    label = stringResource(R.string.viewer_info_label_dimensions),
                    value = when {
                        probing -> stringResource(R.string.viewer_info_value_probing)
                        dimensions != null -> "${dimensions!!.first} × ${dimensions!!.second}"
                        else -> stringResource(R.string.viewer_info_value_unknown)
                    }
                )
                InfoRow(
                    label = stringResource(R.string.viewer_info_label_uuid),
                    value = entry.uuid.take(8) + "…",
                    monospace = true
                )
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
private fun InfoRow(
    label: String,
    value: String,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    entry: VaultEntry,
    trashEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sizeKib = entry.blobSizeBytes / 1024
    val warningResId = if (trashEnabled) {
        R.string.gallery_action_delete_warning
    } else {
        R.string.gallery_action_delete_warning_permanent
    }
    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.viewer_delete_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.gallery_action_size_line, sizeKib),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = entry.uuid,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(warningResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
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
private fun Filmstrip(
    entries: List<VaultEntry>,
    currentIndex: Int,
    listState: LazyListState,
    tileShape: AlbumTileShape,
    onClickThumb: (Int) -> Unit
) {
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(FILMSTRIP_HEIGHT_DP.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
    ) {
        itemsIndexed(
            items = entries,
            key = { _, entry -> entry.uuid }
        ) { index, entry ->
            FilmstripThumb(
                entry = entry,
                isCurrent = index == currentIndex,
                tileShape = tileShape,
                onClick = { onClickThumb(index) }
            )
        }
    }
}

@Composable
private fun FilmstripThumb(
    entry: VaultEntry,
    isCurrent: Boolean,
    tileShape: AlbumTileShape,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(entry.uuid) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(entry.uuid) {
        bitmap = withContext(Dispatchers.IO) {
            ThumbnailLoader.loadOrGenerate(
                context = context.applicationContext,
                entry = entry,
                keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS),
                gridSized = true
            )
        }
    }

    val cornerShape = RoundedCornerShape(
        if (tileShape == AlbumTileShape.ROUNDED) 8.dp else 0.dp
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(cornerShape)

            .then(
                if (isCurrent) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = cornerShape
                    )
                } else Modifier
            )
            .background(Color.DarkGray)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 1.5.dp,
            color = Color.White
        )
    }
}

@Composable
private fun ViewerPlaceholder(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.viewer_tap_to_close),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private const val MAX_VIEWER_LONG_EDGE_PX = 4096

private const val FILMSTRIP_HEIGHT_DP = 72

private const val DOUBLE_TAP_ZOOM = 2f

private const val MAX_ZOOM = 5f

private const val CHROME_ANIM_MS = 180

@Composable
private fun ViewerRenameDialog(
    entry: VaultEntry,
    siblings: List<VaultEntry>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val cache = GallerySession.entryFilenameCache
    var hasResolution by remember(entry.uuid) {
        mutableStateOf(entry.uuid in cache)
    }
    LaunchedEffect(entry.uuid) {
        if (entry.uuid !in cache) {
            val name = withContext(Dispatchers.IO) {
                EntriesRepository(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                    .getMeta(entry.blobFile)?.originalFilename
            }
            cache[entry.uuid] = name
            GallerySession.entryFilenameCacheRevision++
            hasResolution = true
        }
    }

    val dupMsg = stringResource(R.string.entry_dup_in_album)
    var siblingNames by remember(entry.uuid) { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(entry.uuid) {
        siblingNames = withContext(Dispatchers.IO) {
            val repo = EntriesRepository(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
            siblings.asSequence()
                .filter { it.uuid != entry.uuid && it.albumUuid == entry.albumUuid }
                .mapNotNull { repo.getMeta(it.blobFile)?.originalFilename?.trim()?.lowercase() }
                .toSet()
        }
    }
    if (hasResolution) {
        val initialName = cache[entry.uuid] ?: ""
        EntryRenameDialog(
            initialName = initialName,
            duplicateChecker = { candidate ->
                if (siblingNames?.contains(candidate.trim().lowercase()) == true) dupMsg else null
            },
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
internal fun WallpaperTargetDialog(
    onPick: (WallpaperTarget) -> Unit,
    onDismiss: () -> Unit
) {

    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.viewer_wallpaper_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { onPick(WallpaperTarget.HOME) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.viewer_wallpaper_target_home),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(
                    onClick = { onPick(WallpaperTarget.LOCK) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.viewer_wallpaper_target_lock),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(
                    onClick = { onPick(WallpaperTarget.BOTH) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.viewer_wallpaper_target_both),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.viewer_wallpaper_cancel))
            }
        }
    )
}
