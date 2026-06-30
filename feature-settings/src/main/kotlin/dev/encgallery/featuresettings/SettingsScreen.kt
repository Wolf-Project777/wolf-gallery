package dev.encgallery.featuresettings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(

    autoLockTimeoutMinutes: Int,
    onAutoLockTimeoutChange: (Int) -> Unit,
    screenshotsAllowed: Boolean,
    onScreenshotsChange: (Boolean) -> Unit,

    themeVariant: ThemeVariant,
    onThemeVariantChange: (ThemeVariant) -> Unit,
    lightAccent: AccentColor,
    onLightAccentChange: (AccentColor) -> Unit,
    darkAccent: AccentColor,
    onDarkAccentChange: (AccentColor) -> Unit,
    tabBarAtBottom: Boolean,
    onTabBarAtBottomChange: (Boolean) -> Unit,

    albumColumns: Int,
    albumColumnChoices: List<Int>,
    onAlbumColumnsChange: (Int) -> Unit,
    albumTileShape: AlbumTileShape,
    onAlbumTileShapeChange: (AlbumTileShape) -> Unit,
    photoGridLayout: PhotoGridLayout,
    onPhotoGridLayoutChange: (PhotoGridLayout) -> Unit,

    appLanguage: String,
    onAppLanguageChange: (String) -> Unit,

    trashEnabled: Boolean,
    onTrashEnabledChange: (Boolean) -> Unit,

    loggingEnabled: Boolean,
    onLoggingChange: (Boolean) -> Unit,
    testsAndLogsContent: @Composable () -> Unit,

    appIconContent: @Composable () -> Unit,

    changePasswordContent: @Composable (onClose: () -> Unit) -> Unit,

    appVersionName: String,

    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    var current by remember { mutableStateOf(SettingsSession.lastOpenCategory) }
    LaunchedEffect(current) {
        SettingsSession.lastOpenCategory = current
    }

    BackHandler {
        if (current != SettingsCategory.HOME) {
            current = SettingsCategory.HOME
        } else {
            onClose()
        }
    }

    val title = when (current) {
        SettingsCategory.HOME -> stringResource(R.string.settings_title)
        SettingsCategory.SECURITY -> stringResource(R.string.settings_cat_security_title)
        SettingsCategory.APPEARANCE -> stringResource(R.string.settings_cat_appearance_title)
        SettingsCategory.LANGUAGE -> stringResource(R.string.settings_cat_language_title)
        SettingsCategory.APP_ICON -> stringResource(R.string.settings_cat_app_icon_title)
        SettingsCategory.TRASH -> stringResource(R.string.settings_cat_trash_title)
        SettingsCategory.SUPPORT -> stringResource(R.string.settings_cat_support_title)
        SettingsCategory.TESTS_AND_LOGS -> stringResource(R.string.settings_cat_tests_logs_title)
        SettingsCategory.ABOUT -> stringResource(R.string.settings_cat_about_title)
    }

    Surface(
        modifier = modifier.fillMaxSize(),

        color = MaterialTheme.colorScheme.surface
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (current != SettingsCategory.HOME) {
                                current = SettingsCategory.HOME
                            } else {
                                onClose()
                            }
                        }) {
                            ThemedIcon(
                                vector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.settings_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            when (current) {
                SettingsCategory.HOME -> SettingsHome(
                    onNavigate = { current = it },
                    modifier = Modifier.padding(innerPadding)
                )
                SettingsCategory.SECURITY -> SecurityCategory(
                    autoLockTimeoutMinutes = autoLockTimeoutMinutes,
                    onAutoLockTimeoutChange = onAutoLockTimeoutChange,
                    screenshotsAllowed = screenshotsAllowed,
                    onScreenshotsChange = onScreenshotsChange,
                    changePasswordContent = changePasswordContent,
                    modifier = Modifier.padding(innerPadding)
                )
                SettingsCategory.APPEARANCE -> AppearanceCategory(
                    themeVariant = themeVariant,
                    onThemeVariantChange = onThemeVariantChange,
                    lightAccent = lightAccent,
                    onLightAccentChange = onLightAccentChange,
                    darkAccent = darkAccent,
                    onDarkAccentChange = onDarkAccentChange,
                    tabBarAtBottom = tabBarAtBottom,
                    onTabBarAtBottomChange = onTabBarAtBottomChange,
                    albumColumns = albumColumns,
                    albumColumnChoices = albumColumnChoices,
                    onAlbumColumnsChange = onAlbumColumnsChange,
                    albumTileShape = albumTileShape,
                    onAlbumTileShapeChange = onAlbumTileShapeChange,
                    photoGridLayout = photoGridLayout,
                    onPhotoGridLayoutChange = onPhotoGridLayoutChange,
                    modifier = Modifier.padding(innerPadding)
                )
                SettingsCategory.LANGUAGE -> LanguageCategory(
                    appLanguage = appLanguage,
                    onAppLanguageChange = onAppLanguageChange,
                    modifier = Modifier.padding(innerPadding)
                )
                SettingsCategory.APP_ICON -> AppIconCategory(
                    content = appIconContent,
                    modifier = Modifier.padding(innerPadding)
                )
                SettingsCategory.TRASH -> TrashCategory(
                    trashEnabled = trashEnabled,
                    onTrashEnabledChange = onTrashEnabledChange,
                    modifier = Modifier.padding(innerPadding)
                )
                SettingsCategory.SUPPORT -> SupportCategory(
                    modifier = Modifier.padding(innerPadding)
                )
                SettingsCategory.TESTS_AND_LOGS -> TestsAndLogsCategory(
                    loggingEnabled = loggingEnabled,
                    onLoggingChange = onLoggingChange,
                    content = testsAndLogsContent,
                    modifier = Modifier.padding(innerPadding)
                )
                SettingsCategory.ABOUT -> AboutCategory(
                    appVersionName = appVersionName,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun VerticalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
    thumbWidth: Dp = 4.dp,
    thumbColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var dragging by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (state.isScrollInProgress || dragging) 0.5f else 0f,
        animationSpec = tween(
            durationMillis = if (state.isScrollInProgress || dragging) 120 else 500
        ),
        label = "scrollbarAlpha"
    )

    Box(
        modifier
            .fillMaxHeight()
            .width(32.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val max = state.maxValue
                    if (max <= 0) return@awaitEachGesture
                    val viewportH = size.height.toFloat()
                    val thumbH = (viewportH / (viewportH + max)) * viewportH
                    val thumbY = (state.value.toFloat() / max) * (viewportH - thumbH)

                    val slop = 22.dp.toPx()
                    if (down.position.y !in (thumbY - slop)..(thumbY + thumbH + slop)) {

                        return@awaitEachGesture
                    }
                    down.consume()
                    dragging = true

                    val travel = (viewportH - thumbH).coerceAtLeast(1f)
                    drag(down.id) { change ->
                        state.dispatchRawDelta(change.positionChange().y * (max / travel))
                        change.consume()
                    }
                    dragging = false
                }
            }
            .drawBehind {
                val max = state.maxValue
                if (max > 0 && alpha > 0f) {
                    val viewportH = size.height
                    val thumbH = (viewportH / (viewportH + max)) * viewportH
                    val thumbY = (state.value.toFloat() / max) * (viewportH - thumbH)
                    val wPx = thumbWidth.toPx()
                    drawRoundRect(
                        color = thumbColor,
                        topLeft = Offset(size.width - wPx, thumbY),
                        size = Size(wPx, thumbH),
                        cornerRadius = CornerRadius(wPx / 2f, wPx / 2f),
                        alpha = alpha
                    )
                }
            }
    )
}

@Composable
private fun SettingsScrollColumn(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    state: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state)
                .padding(contentPadding),
            content = content
        )
        VerticalScrollbar(state, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun SettingsHome(
    onNavigate: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberScrollState()
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(state)
            ) {
                CategoryRow(
                    title = stringResource(R.string.settings_cat_security_title),
                    subtitle = stringResource(R.string.settings_cat_security_subtitle),
                    onClick = { onNavigate(SettingsCategory.SECURITY) }
                )
                HorizontalDivider()
                CategoryRow(
                    title = stringResource(R.string.settings_cat_appearance_title),
                    subtitle = stringResource(R.string.settings_cat_appearance_subtitle),
                    onClick = { onNavigate(SettingsCategory.APPEARANCE) }
                )
                HorizontalDivider()
                CategoryRow(
                    title = stringResource(R.string.settings_cat_language_title),
                    subtitle = stringResource(R.string.settings_cat_language_subtitle),
                    onClick = { onNavigate(SettingsCategory.LANGUAGE) }
                )
                HorizontalDivider()
                CategoryRow(
                    title = stringResource(R.string.settings_cat_app_icon_title),
                    subtitle = stringResource(R.string.settings_cat_app_icon_subtitle),
                    onClick = { onNavigate(SettingsCategory.APP_ICON) }
                )
                HorizontalDivider()

                CategoryRow(
                    title = stringResource(R.string.settings_cat_trash_title),
                    subtitle = stringResource(R.string.settings_cat_trash_subtitle),
                    onClick = { onNavigate(SettingsCategory.TRASH) }
                )
                HorizontalDivider()
                CategoryRow(
                    title = stringResource(R.string.settings_cat_support_title),
                    subtitle = stringResource(R.string.settings_cat_support_subtitle),
                    onClick = { onNavigate(SettingsCategory.SUPPORT) }
                )
                HorizontalDivider()
                CategoryRow(
                    title = stringResource(R.string.settings_cat_tests_logs_title),
                    subtitle = stringResource(R.string.settings_cat_tests_logs_subtitle),
                    onClick = { onNavigate(SettingsCategory.TESTS_AND_LOGS) }
                )
                HorizontalDivider()
                CategoryRow(
                    title = stringResource(R.string.settings_cat_about_title),
                    subtitle = stringResource(R.string.settings_cat_about_subtitle),
                    onClick = { onNavigate(SettingsCategory.ABOUT) }
                )
                HorizontalDivider()
            }
            VerticalScrollbar(state, Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun SecurityCategory(
    autoLockTimeoutMinutes: Int,
    onAutoLockTimeoutChange: (Int) -> Unit,
    screenshotsAllowed: Boolean,
    onScreenshotsChange: (Boolean) -> Unit,
    changePasswordContent: @Composable (onClose: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showChange by remember { mutableStateOf(false) }

    if (showChange) {
        androidx.compose.foundation.layout.Box(modifier = modifier) {
            changePasswordContent { showChange = false }
        }
        return
    }

    SettingsScrollColumn(modifier = modifier) {
        AutoLockBlock(
            timeoutMinutes = autoLockTimeoutMinutes,
            onTimeoutChange = onAutoLockTimeoutChange
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        SwitchRow(
            title = stringResource(R.string.settings_security_screenshots_title),
            subtitle = stringResource(R.string.settings_security_screenshots_subtitle),
            checked = screenshotsAllowed,
            onCheckedChange = onScreenshotsChange
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showChange = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = stringResource(R.string.settings_security_changepw_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.settings_security_changepw_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ThemedIcon(
                vector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppearanceCategory(
    themeVariant: ThemeVariant,
    onThemeVariantChange: (ThemeVariant) -> Unit,
    lightAccent: AccentColor,
    onLightAccentChange: (AccentColor) -> Unit,
    darkAccent: AccentColor,
    onDarkAccentChange: (AccentColor) -> Unit,
    tabBarAtBottom: Boolean,
    onTabBarAtBottomChange: (Boolean) -> Unit,
    albumColumns: Int,
    albumColumnChoices: List<Int>,
    onAlbumColumnsChange: (Int) -> Unit,
    albumTileShape: AlbumTileShape,
    onAlbumTileShapeChange: (AlbumTileShape) -> Unit,
    photoGridLayout: PhotoGridLayout,
    onPhotoGridLayoutChange: (PhotoGridLayout) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScrollColumn(modifier = modifier) {
        SectionHeader(stringResource(R.string.settings_appearance_themes_header))
        Spacer(Modifier.height(8.dp))
        ThemeVariantCard(
            selected = themeVariant,
            onSelect = onThemeVariantChange
        )

        if (themeVariant.supportsAccent) {
            Spacer(Modifier.height(16.dp))
            AccentBlock(
                variant = themeVariant,
                lightAccent = lightAccent,
                onLightAccentChange = onLightAccentChange,
                darkAccent = darkAccent,
                onDarkAccentChange = onDarkAccentChange
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        AlbumGridBlock(
            albumColumns = albumColumns,
            albumColumnChoices = albumColumnChoices,
            onAlbumColumnsChange = onAlbumColumnsChange,
            albumTileShape = albumTileShape,
            onAlbumTileShapeChange = onAlbumTileShapeChange
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        PhotoGridBlock(
            photoGridLayout = photoGridLayout,
            onPhotoGridLayoutChange = onPhotoGridLayoutChange
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        TabPositionBlock(
            tabBarAtBottom = tabBarAtBottom,
            onTabBarAtBottomChange = onTabBarAtBottomChange
        )
    }
}

@Composable
private fun LanguageCategory(
    appLanguage: String,
    onAppLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScrollColumn(modifier = modifier) {
        SectionHeader(stringResource(R.string.settings_language_header))
        Spacer(Modifier.height(8.dp))
        LanguageRow(

            label = stringResource(R.string.settings_language_system),
            selected = appLanguage == "system",
            onSelect = { onAppLanguageChange("system") }
        )

        LanguageRow("English", appLanguage == "en") { onAppLanguageChange("en") }
        LanguageRow("Русский", appLanguage == "ru") { onAppLanguageChange("ru") }
        LanguageRow("Español", appLanguage == "es") { onAppLanguageChange("es") }
        LanguageRow("العربية", appLanguage == "ar") { onAppLanguageChange("ar") }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = selected, onClick = onSelect)
    }
}

@Composable
private fun TabPositionBlock(
    tabBarAtBottom: Boolean,
    onTabBarAtBottomChange: (Boolean) -> Unit
) {
    Text(
        text = stringResource(R.string.settings_appearance_tab_position_title),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = stringResource(R.string.settings_appearance_tab_position_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    RadioOptionRow(
        title = stringResource(R.string.settings_appearance_tab_pos_top),
        subtitle = stringResource(R.string.settings_appearance_tab_pos_top_desc),
        selected = !tabBarAtBottom,
        onSelect = { onTabBarAtBottomChange(false) }
    )
    RadioOptionRow(
        title = stringResource(R.string.settings_appearance_tab_pos_bottom),
        subtitle = stringResource(R.string.settings_appearance_tab_pos_bottom_desc),
        selected = tabBarAtBottom,
        onSelect = { onTabBarAtBottomChange(true) }
    )
}

@Composable
private fun AlbumGridBlock(
    albumColumns: Int,
    albumColumnChoices: List<Int>,
    onAlbumColumnsChange: (Int) -> Unit,
    albumTileShape: AlbumTileShape,
    onAlbumTileShapeChange: (AlbumTileShape) -> Unit
) {
    SectionHeader(stringResource(R.string.settings_appearance_grid_header))
    Spacer(Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.settings_appearance_grid_columns_title),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = stringResource(R.string.settings_appearance_grid_columns_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (count in albumColumnChoices) {
            FilterChip(
                selected = albumColumns == count,
                onClick = { onAlbumColumnsChange(count) },
                label = { Text(count.toString()) }
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.settings_appearance_tile_shape_title),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = stringResource(R.string.settings_appearance_tile_shape_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (shape in AlbumTileShape.entries) {
            FilterChip(
                selected = albumTileShape == shape,
                onClick = { onAlbumTileShapeChange(shape) },
                leadingIcon = { TileShapeSwatch(shape) },
                label = { Text(stringResource(shape.labelRes)) }
            )
        }
    }
}

@Composable
private fun PhotoGridBlock(
    photoGridLayout: PhotoGridLayout,
    onPhotoGridLayoutChange: (PhotoGridLayout) -> Unit
) {
    SectionHeader(stringResource(R.string.settings_appearance_photo_grid_header))
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.settings_appearance_photo_layout_title),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = stringResource(R.string.settings_appearance_photo_layout_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (layout in PhotoGridLayout.entries) {
            FilterChip(
                selected = photoGridLayout == layout,
                onClick = { onPhotoGridLayoutChange(layout) },
                label = { Text(stringResource(layout.labelRes)) }
            )
        }
    }
}

@Composable
private fun TileShapeSwatch(shape: AlbumTileShape) {
    val cornerShape = RoundedCornerShape(
        if (shape == AlbumTileShape.ROUNDED) 5.dp else 0.dp
    )
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(cornerShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

@Composable
private fun ThemeVariantCard(
    selected: ThemeVariant,
    onSelect: (ThemeVariant) -> Unit
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        for (variant in ThemeVariant.entries) {

            if (variant == ThemeVariant.MEDIEVAL) continue
            ThemeVariantRow(
                variant = variant,
                selected = variant == selected,
                onSelect = { onSelect(variant) }
            )
        }
    }
}

@Composable
private fun ThemeVariantRow(
    variant: ThemeVariant,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val nameColor = when (variant) {
            ThemeVariant.LIGHT -> Color(0xFF9E9E9E)
            ThemeVariant.DARK_DEFAULT -> Color(0xFF5C5C5C)
            else -> variant.previewAccent
        }
        Text(
            text = stringResource(variant.nameRes),
            style = MaterialTheme.typography.titleMedium,
            color = nameColor,
            fontFamily = fontFamilyForVariant(variant),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        RadioButton(selected = selected, onClick = onSelect)
    }
}

private fun fontFamilyForVariant(variant: ThemeVariant): FontFamily = when (variant) {
    ThemeVariant.MEDIEVAL -> FontFamily.Serif
    ThemeVariant.HACKER -> FontFamily.Monospace
    else -> FontFamily.Default
}

@Composable
private fun AccentBlock(
    variant: ThemeVariant,
    lightAccent: AccentColor,
    onLightAccentChange: (AccentColor) -> Unit,
    darkAccent: AccentColor,
    onDarkAccentChange: (AccentColor) -> Unit
) {
    SectionHeader(stringResource(R.string.settings_appearance_accent_header))
    Spacer(Modifier.height(8.dp))

    AccentPickerRow(
        isDarkBlock = variant.isDark,
        selected = if (variant.isDark) darkAccent else lightAccent,
        onSelect = {
            if (variant.isDark) onDarkAccentChange(it)
            else onLightAccentChange(it)
        }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPickerRow(
    isDarkBlock: Boolean,
    selected: AccentColor,
    onSelect: (AccentColor) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (color in AccentColor.PICKER_ORDER) {
            AccentSwatch(
                color = color,
                isDarkBlock = isDarkBlock,
                selected = color == selected,
                onClick = { onSelect(color) }
            )
        }
    }
}

@Composable
private fun AccentSwatch(
    color: AccentColor,
    isDarkBlock: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val swatch = color.primaryFor(isDarkBlock)
    val ringColor = MaterialTheme.colorScheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(swatch)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(width = 2.dp, color = ringColor, shape = CircleShape)
            )
        }
    }
}

@Composable
private fun AppIconCategory(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScrollColumn(modifier = modifier) {
        content()
    }
}

@Composable
private fun TrashCategory(
    trashEnabled: Boolean,
    onTrashEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScrollColumn(modifier = modifier) {

        RadioOptionRow(
            title = stringResource(R.string.settings_trash_enabled_title),
            subtitle = stringResource(R.string.settings_trash_on_desc),
            selected = trashEnabled,
            onSelect = { onTrashEnabledChange(true) }
        )
        RadioOptionRow(
            title = stringResource(R.string.settings_trash_off_title),
            subtitle = stringResource(R.string.settings_trash_off_desc),
            selected = !trashEnabled,
            onSelect = { onTrashEnabledChange(false) }
        )
    }
}

private const val MONERO_ADDRESS =
    "42nfQ6zMT9jbLM2XGwNLLX137s8RkYRrhhJSqrFFZvCwcWegttcMQJZ85gTG6niredYWfRP6FYoEtPKYDDWQ63CM8hT5xzK"
private const val ZCASH_ADDRESS =
    "u1hwe88n4r8zz8958ssr02swq3svxmpqxq5uzhn3sp3zkjna39ymphtkc4egc2w0s4pxvhemmw2cw89gz7qzkzxvhnhrywwhm0aaq706h0a8p7jc447m59nululu77y42x2u6n056jwyvclphau2tu9jayt4c4szzkrjh5vxtrd587rx65"

@Composable
private fun SupportCategory(modifier: Modifier = Modifier) {
    SettingsScrollColumn(modifier = modifier) {
        Text(
            text = stringResource(R.string.support_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        CoinDonationRow(
            iconRes = R.drawable.ic_coin_monero,
            name = "Monero (XMR)",
            subtitle = stringResource(R.string.support_xmr_subtitle),
            address = MONERO_ADDRESS,

            qrContent = "monero:$MONERO_ADDRESS"
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        CoinDonationRow(
            iconRes = R.drawable.ic_coin_zcash,
            name = "Zcash (ZEC)",
            subtitle = stringResource(R.string.support_zec_subtitle),
            address = ZCASH_ADDRESS,

            qrContent = "zcash:$ZCASH_ADDRESS"
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.support_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CoinDonationRow(
    iconRes: Int,
    name: String,
    subtitle: String,
    address: String,

    qrContent: String
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedMsg = stringResource(R.string.support_address_copied)

    val qr = remember(qrContent) {
        runCatching { generateQrBitmap(qrContent, 600) }.getOrNull()
    }
    val copy = {
        clipboard.setText(AnnotatedString(address))
        Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,

                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = copy)
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = copy) {
                Text(stringResource(R.string.support_copy))
            }
        }
        if (qr != null) {
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = stringResource(R.string.support_qr_cd, name),
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.size(180.dp)
                    )
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
    val hints = hashMapOf<EncodeHintType, Any>(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val w = matrix.width
    val h = matrix.height
    val black = AndroidColor.BLACK
    val white = AndroidColor.WHITE
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val rowOffset = y * w
        for (x in 0 until w) {
            pixels[rowOffset + x] = if (matrix[x, y]) black else white
        }
    }
    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, w, 0, 0, w, h)
    }
}

@Composable
private fun TestsAndLogsCategory(
    loggingEnabled: Boolean,
    onLoggingChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = remember {
        ScrollState(initial = SettingsSession.lastTestsAndLogsScrollPx)
    }
    DisposableEffect(scrollState) {
        onDispose {
            SettingsSession.lastTestsAndLogsScrollPx = scrollState.value
        }
    }

    SettingsScrollColumn(modifier = modifier, state = scrollState) {
        SwitchRow(
            title = stringResource(R.string.settings_logs_logging_title),
            subtitle = stringResource(R.string.settings_logs_logging_subtitle),
            checked = loggingEnabled,
            onCheckedChange = onLoggingChange
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        content()
    }
}

private const val FEEDBACK_EMAIL = "wolf-project777@protonmail.com"
private const val PROJECT_URL = "https://github.com/Wolf-Project777/wolf-gallery"

@Composable
private fun AboutCategory(
    appVersionName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedMsg = stringResource(R.string.about_email_copied)
    val copy = {
        clipboard.setText(AnnotatedString(FEEDBACK_EMAIL))
        Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
    }
    val copiedLinkMsg = stringResource(R.string.about_link_copied)
    val copyLink = {
        clipboard.setText(AnnotatedString(PROJECT_URL))
        Toast.makeText(context, copiedLinkMsg, Toast.LENGTH_SHORT).show()
    }

    SettingsScrollColumn(modifier = modifier) {
        Text(
            text = "Wolf Gallery",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.about_version, appVersionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.about_license),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.about_developed_by),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.about_feedback_header),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.about_feedback_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = copy)
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = FEEDBACK_EMAIL,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = copy) {
                Text(stringResource(R.string.support_copy))
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.about_source_header),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = copyLink)
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = PROJECT_URL,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = copyLink) {
                Text(stringResource(R.string.support_copy))
            }
        }
    }
}

@Composable
private fun CategoryRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ThemedIcon(
            vector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RadioOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = selected, onClick = onSelect)
    }
}

@Composable
private fun AutoLockBlock(
    timeoutMinutes: Int,
    onTimeoutChange: (Int) -> Unit
) {
    var customDialogShown by remember { mutableStateOf(false) }

    Text(
        text = stringResource(R.string.settings_security_autolock_title),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.settings_security_autolock_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))

    val presets = listOf(0, 5, 10, 15, 30)
    val isCustom = timeoutMinutes != 0 && timeoutMinutes !in presets

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (preset in presets) {
            FilterChip(
                selected = !isCustom && timeoutMinutes == preset,
                onClick = { onTimeoutChange(preset) },
                label = {
                    Text(
                        if (preset == 0)
                            stringResource(R.string.settings_security_autolock_off)
                        else
                            stringResource(R.string.settings_security_autolock_minutes, preset)
                    )
                }
            )
        }
        FilterChip(
            selected = isCustom,
            onClick = { customDialogShown = true },
            label = {
                Text(
                    if (isCustom)
                        stringResource(R.string.settings_security_autolock_minutes, timeoutMinutes)
                    else
                        stringResource(R.string.settings_security_autolock_custom)
                )
            }
        )
    }

    if (customDialogShown) {
        CustomTimeoutDialog(
            initialValue = if (isCustom) timeoutMinutes else 7,
            onDismiss = { customDialogShown = false },
            onConfirm = { mins ->
                onTimeoutChange(mins)
                customDialogShown = false
            }
        )
    }
}

@Composable
private fun CustomTimeoutDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initialValue.toString()) }
    val parsed = text.toIntOrNull()
    val canConfirm = parsed != null && parsed in 1..1440

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_security_autolock_custom_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_security_autolock_custom_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.settings_security_autolock_custom_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = canConfirm) {
                Text(stringResource(R.string.settings_security_autolock_custom_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_security_autolock_custom_cancel))
            }
        }
    )
}
