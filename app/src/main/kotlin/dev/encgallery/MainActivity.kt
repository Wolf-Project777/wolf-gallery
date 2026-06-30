package dev.encgallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.encgallery.crypto.DekVault
import dev.encgallery.crypto.KekWrap
import dev.encgallery.crypto.KeystoreAesGcm
import dev.encgallery.crypto.PasswordChange
import dev.encgallery.crypto.SecureBytes
import dev.encgallery.featuresettings.SettingsScreen
import dev.encgallery.launcher.AppIconPicker
import dev.encgallery.gallery.AlbumDetailScreen
import dev.encgallery.gallery.AlbumSummary
import dev.encgallery.gallery.AlbumsListScreen
import dev.encgallery.gallery.AlbumCoverCropScreen
import dev.encgallery.gallery.AlbumCoverPickerScreen
import dev.encgallery.gallery.AlbumGroupSummary
import dev.encgallery.gallery.AlbumGroupsRepository
import dev.encgallery.gallery.AlbumSortOrder
import dev.encgallery.gallery.deleteEntryOnDisk
import dev.encgallery.gallery.AlbumPickerOp
import dev.encgallery.gallery.AlbumSelectionStage
import dev.encgallery.gallery.AlbumsRepository
import dev.encgallery.gallery.DialogHostContainer
import dev.encgallery.gallery.EntriesRepository
import dev.encgallery.gallery.EntryRenameDialog
import dev.encgallery.gallery.EntrySortOrder
import dev.encgallery.gallery.sortAlbumGroupSummaries
import dev.encgallery.gallery.sortAlbumSummaries
import dev.encgallery.gallery.sortEntries
import dev.encgallery.gallery.GroupDestinationPickerScreen
import dev.encgallery.gallery.EntryDestinationPickerScreen
import dev.encgallery.gallery.EntrySourcePickerScreen
import dev.encgallery.gallery.WallpaperCropScreen
import dev.encgallery.gallery.WallpaperOps
import dev.encgallery.gallery.WallpaperTarget
import dev.encgallery.gallery.AlbumConflictFlow
import dev.encgallery.gallery.AlbumConflictRequest
import dev.encgallery.gallery.nextFreeConflictName
import dev.encgallery.gallery.EntryOps
import dev.encgallery.gallery.EntryBatchOp
import dev.encgallery.gallery.EntryBatchOutcome
import dev.encgallery.gallery.EntryConflictResolver
import dev.encgallery.gallery.FileConflictChoice
import dev.encgallery.gallery.FileConflictDialog
import dev.encgallery.gallery.FileConflictRequest
import dev.encgallery.gallery.FileConflictResolution
import dev.encgallery.gallery.EntryPickerOp
import dev.encgallery.gallery.AlbumExportBundle
import dev.encgallery.gallery.EntryExport
import dev.encgallery.gallery.EntryShare
import dev.encgallery.gallery.FullScreenViewer
import dev.encgallery.gallery.OperationKind
import dev.encgallery.gallery.OperationProgress
import dev.encgallery.gallery.OperationProgressOverlay
import dev.encgallery.gallery.DeviceBucket
import dev.encgallery.gallery.DeviceImportPickerScreen
import dev.encgallery.gallery.DeviceMediaBuckets
import dev.encgallery.gallery.PhotoImporter
import dev.encgallery.gallery.ThumbnailLoader
import dev.encgallery.gallery.MosaicAspectStore
import dev.encgallery.gallery.ThumbnailMigration
import dev.encgallery.gallery.TrashRepository
import dev.encgallery.gallery.VaultPaths
import dev.encgallery.gallery.GalleryHost
import dev.encgallery.gallery.GallerySession
import dev.encgallery.gallery.InWindowDialog
import dev.encgallery.gallery.InWindowDropdown
import dev.encgallery.gallery.InWindowDropdownItem
import dev.encgallery.gallery.GalleryTab
import dev.encgallery.gallery.GridActionStage
import dev.encgallery.gallery.NormalizedRect
import dev.encgallery.gallery.VaultEntry
import dev.encgallery.gallery.VaultIndex
import dev.encgallery.logging.EncLog
import dev.encgallery.nativec.NativeCrypto
import dev.encgallery.session.SessionState
import dev.encgallery.session.wipeVault
import dev.encgallery.settings.AppSettings
import dev.encgallery.settings.LocaleHelper
import dev.encgallery.storage.EncryptedFileBlob
import dev.encgallery.ui.theme.EncGalleryTheme
import dev.encgallery.featuresettings.LocalThemeVariant
import dev.encgallery.featuresettings.MedievalIcons
import dev.encgallery.featuresettings.ThemedIcon
import dev.encgallery.featuresettings.ThemeVariant
import dev.encgallery.wizard.ChangePasswordScreen
import dev.encgallery.wizard.UnlockScreen
import dev.encgallery.wizard.WizardHost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private var autoPurgeRanThisProcess: Boolean = false

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleHelper.wrap(newBase, AppSettings.persistedLanguage(newBase))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyScreenshotPolicy(AppSettings.screenshotsAllowed.value)

        EncLog.i("MainActivity", "onCreate — Activity initialized")

        setContent {

            val themeVariantNow by AppSettings.themeVariant
            val lightAccentNow by AppSettings.lightAccent
            val darkAccentNow by AppSettings.darkAccent
            EncGalleryTheme(
                variant = themeVariantNow,
                lightAccent = lightAccentNow,
                darkAccent = darkAccentNow
            ) {

                DialogHostContainer {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val wizardDone by AppSettings.wizardComplete
                    val sessionUnlocked by SessionState.unlocked
                    val cryptoMode by AppSettings.cryptoMode
                    val lockMethod by AppSettings.lockMethod
                    val bruteForceConfig by AppSettings.bruteForceConfig
                    val failureCount by AppSettings.failureCount
                    val lastFailureAt by AppSettings.lastFailureAt
                    val activityCtx = LocalContext.current

                    var settingsOpen by remember {
                        mutableStateOf(GallerySession.lastSettingsOpen)
                    }
                    LaunchedEffect(settingsOpen) {
                        GallerySession.lastSettingsOpen = settingsOpen
                    }

                    var trashOpen by remember {
                        mutableStateOf(GallerySession.lastTrashOpen)
                    }
                    LaunchedEffect(trashOpen) {
                        GallerySession.lastTrashOpen = trashOpen
                    }
                    var trashRecords by remember {
                        mutableStateOf<List<dev.encgallery.gallery.TrashRecord>>(emptyList())
                    }

                    var trashRefreshTick by remember { mutableIntStateOf(0) }
                    LaunchedEffect(trashOpen, trashRefreshTick) {
                        if (trashOpen) {
                            trashRecords = withContext(Dispatchers.IO) {
                                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                TrashRepository(keystore).listAllRecords(activityCtx.applicationContext)
                            }
                        }
                    }

                    LaunchedEffect(wizardDone, sessionUnlocked) {
                        if (wizardDone && sessionUnlocked && !autoPurgeRanThisProcess) {
                            autoPurgeRanThisProcess = true
                            withContext(Dispatchers.IO) {
                                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                TrashRepository(keystore).purgeOlderThan(activityCtx.applicationContext)
                            }
                        }
                    }
                    when {
                        !wizardDone -> WizardHost(
                            modifier = Modifier.padding(innerPadding),
                            cryptoMode = cryptoMode,
                            onCryptoModeChange = { AppSettings.setCryptoMode(it) },
                            lockMethod = lockMethod,
                            onLockMethodChange = { AppSettings.setLockMethod(it) },
                            bruteForceConfig = bruteForceConfig,
                            onBruteForceConfigChange = { AppSettings.setBruteForceConfig(it) },
                            onComplete = {
                                AppSettings.setWizardComplete(true)

                                SessionState.unlocked.value = true
                                EncLog.i("Session", "unlocked = true (wizard handoff)")
                            }
                        )
                        !sessionUnlocked -> UnlockScreen(
                            modifier = Modifier.padding(innerPadding),
                            lockMethod = lockMethod,
                            failureCount = failureCount,
                            lastFailureAt = lastFailureAt,
                            bruteForceConfig = bruteForceConfig,
                            onWrongAttempt = { AppSettings.incrementFailure() },
                            onSuccessfulUnlock = {
                                AppSettings.resetFailureState()
                                SessionState.unlocked.value = true
                                EncLog.i("Session", "unlocked = true (unlock success)")
                            },
                            onWipe = {
                                wipeVault(activityCtx.applicationContext)

                            }
                        )
                        trashOpen -> {
                            val trashScope = rememberCoroutineScope()

                            var trashViewingUuid by remember {
                                mutableStateOf(GallerySession.lastTrashViewingUuid)
                            }
                            LaunchedEffect(trashViewingUuid) {
                                GallerySession.lastTrashViewingUuid = trashViewingUuid
                            }
                            val trashViewerEntries: List<dev.encgallery.gallery.TrashEntry> =
                                trashRecords.filterIsInstance<dev.encgallery.gallery.TrashEntry>()

                            fun nextViewerUuidAfter(removed: String): String? {
                                val curIdx = trashViewerEntries.indexOfFirst { it.uuid == removed }
                                if (curIdx < 0) return null
                                return trashViewerEntries.getOrNull(curIdx + 1)?.uuid
                                    ?: trashViewerEntries.getOrNull(curIdx - 1)?.uuid
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                dev.encgallery.gallery.TrashScreen(
                                    records = trashRecords,
                                    onClose = {
                                        trashOpen = false
                                        trashRecords = emptyList()
                                        trashViewingUuid = null

                                    },
                                    onRestoreSelection = { uuids ->
                                        if (uuids.isNotEmpty()) {

                                            val toRestore = trashRecords.filter { it.uuid in uuids }
                                            trashRecords = trashRecords.filter { it.uuid !in uuids }
                                            trashScope.launch {

                                                var groupsOk = 0
                                                var albumsOk = 0
                                                var entriesOk = 0
                                                val groupsReq = toRestore.count { it is dev.encgallery.gallery.TrashGroup }
                                                val albumsReq = toRestore.count { it is dev.encgallery.gallery.TrashAlbum }
                                                val entriesReq = toRestore.count { it is dev.encgallery.gallery.TrashEntry }
                                                val tBatch = System.currentTimeMillis()
                                                withContext(Dispatchers.IO) {
                                                    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                                    val repo = TrashRepository(keystore)

                                                    toRestore.filterIsInstance<dev.encgallery.gallery.TrashGroup>()
                                                        .forEach {
                                                            if (repo.restoreGroup(activityCtx.applicationContext, it) != null) groupsOk++
                                                        }
                                                    toRestore.filterIsInstance<dev.encgallery.gallery.TrashAlbum>()
                                                        .forEach {
                                                            if (repo.restoreAlbum(activityCtx.applicationContext, it) != null) albumsOk++
                                                        }
                                                    toRestore.filterIsInstance<dev.encgallery.gallery.TrashEntry>()
                                                        .forEach {
                                                            if (repo.restore(activityCtx.applicationContext, it) != null) entriesOk++
                                                        }
                                                }
                                                val took = System.currentTimeMillis() - tBatch
                                                EncLog.i(
                                                    "MainActivity",
                                                    "restore batch: requested=${toRestore.size} " +
                                                        "groups=$groupsOk/$groupsReq albums=$albumsOk/$albumsReq " +
                                                        "entries=$entriesOk/$entriesReq took=${took}ms"
                                                )
                                                trashRefreshTick++
                                            }
                                        }
                                    },
                                    onPermanentDeleteSelection = { uuids ->
                                        if (uuids.isNotEmpty()) {
                                            val toPurge = trashRecords.filter { it.uuid in uuids }
                                            trashRecords = trashRecords.filter { it.uuid !in uuids }
                                            trashScope.launch {
                                                var purgedOk = 0
                                                val tBatch = System.currentTimeMillis()
                                                withContext(Dispatchers.IO) {
                                                    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                                    val repo = TrashRepository(keystore)
                                                    toPurge.forEach { rec ->
                                                        if (repo.purgePermanentlyRecord(
                                                                activityCtx.applicationContext, rec
                                                            )
                                                        ) purgedOk++
                                                    }
                                                }
                                                val took = System.currentTimeMillis() - tBatch
                                                EncLog.i(
                                                    "MainActivity",
                                                    "purge batch: requested=${toPurge.size} " +
                                                        "purged=$purgedOk took=${took}ms"
                                                )
                                                trashRefreshTick++
                                            }
                                        }
                                    },
                                    onEmptyTrash = {

                                        trashRecords = emptyList()
                                        trashViewingUuid = null
                                        trashScope.launch {
                                            withContext(Dispatchers.IO) {
                                                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                                TrashRepository(keystore).emptyTrash(activityCtx.applicationContext)
                                            }
                                            trashRefreshTick++
                                        }
                                    },
                                    onPreviewEntry = { entry ->
                                        trashViewingUuid = entry.uuid
                                    },
                                    columns = AppSettings.albumColumns.value,
                                    tileShape = AppSettings.albumTileShape.value,
                                    modifier = Modifier.padding(innerPadding)
                                )

                                val viewingUuid = trashViewingUuid
                                if (viewingUuid != null && trashViewerEntries.isNotEmpty()) {
                                    val startIndex = trashViewerEntries
                                        .indexOfFirst { it.uuid == viewingUuid }
                                        .coerceAtLeast(0)
                                    androidx.compose.runtime.key(viewingUuid) {
                                        dev.encgallery.gallery.TrashEntryViewer(
                                            entries = trashViewerEntries,
                                            startIndex = startIndex,
                                            onClose = { trashViewingUuid = null },
                                            onRestore = { entry ->
                                                val nextUuid = nextViewerUuidAfter(entry.uuid)
                                                trashRecords = trashRecords.filter {
                                                    it.uuid != entry.uuid
                                                }
                                                trashViewingUuid = nextUuid
                                                trashScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                                        TrashRepository(keystore).restore(
                                                            activityCtx.applicationContext, entry
                                                        )
                                                    }
                                                    trashRefreshTick++
                                                }
                                            },
                                            onPurge = { entry ->
                                                val nextUuid = nextViewerUuidAfter(entry.uuid)
                                                trashRecords = trashRecords.filter {
                                                    it.uuid != entry.uuid
                                                }
                                                trashViewingUuid = nextUuid
                                                trashScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                                        TrashRepository(keystore).purgePermanently(
                                                            activityCtx.applicationContext, entry
                                                        )
                                                    }
                                                    trashRefreshTick++
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        settingsOpen -> {
                            val loggingEnabled by AppSettings.loggingEnabled
                            val allowScreenshots by AppSettings.screenshotsAllowed
                            val autoLockMinutes by AppSettings.lockTimeoutMinutes
                            val trashEnabledNow by AppSettings.trashEnabled

                            val appVersionName = remember {
                                runCatching {
                                    packageManager.getPackageInfo(packageName, 0).versionName
                                }.getOrNull().orEmpty()
                            }
                            SettingsScreen(
                                modifier = Modifier.padding(innerPadding),
                                autoLockTimeoutMinutes = autoLockMinutes,
                                onAutoLockTimeoutChange = { AppSettings.setLockTimeoutMinutes(it) },
                                screenshotsAllowed = allowScreenshots,
                                onScreenshotsChange = { value ->
                                    AppSettings.setScreenshotsAllowed(value)
                                    window.decorView.postDelayed({
                                        applyScreenshotPolicy(AppSettings.screenshotsAllowed.value)
                                    }, 180L)
                                },
                                themeVariant = themeVariantNow,
                                onThemeVariantChange = { AppSettings.setThemeVariant(it) },
                                lightAccent = lightAccentNow,
                                onLightAccentChange = { AppSettings.setLightAccent(it) },
                                darkAccent = darkAccentNow,
                                onDarkAccentChange = { AppSettings.setDarkAccent(it) },
                                tabBarAtBottom = AppSettings.tabBarAtBottom.value,
                                onTabBarAtBottomChange = { AppSettings.setTabBarAtBottom(it) },
                                albumColumns = AppSettings.albumColumns.value,
                                albumColumnChoices = AppSettings.ALBUM_COLUMN_CHOICES,
                                onAlbumColumnsChange = { AppSettings.setAlbumColumns(it) },
                                albumTileShape = AppSettings.albumTileShape.value,
                                onAlbumTileShapeChange = { AppSettings.setAlbumTileShape(it) },
                                photoGridLayout = AppSettings.photoGridLayout.value,
                                onPhotoGridLayoutChange = { AppSettings.setPhotoGridLayout(it) },
                                appLanguage = AppSettings.appLanguage.value,
                                onAppLanguageChange = { lang ->
                                    if (lang != AppSettings.appLanguage.value) {
                                        AppSettings.setAppLanguage(lang)

                                        this@MainActivity.recreate()
                                    }
                                },
                                trashEnabled = trashEnabledNow,
                                onTrashEnabledChange = { AppSettings.setTrashEnabled(it) },
                                loggingEnabled = loggingEnabled,
                                onLoggingChange = { AppSettings.setLoggingEnabled(it) },
                                testsAndLogsContent = { TestsAndLogsContent() },
                                appIconContent = { AppIconPicker() },
                                changePasswordContent = { onCloseChange ->
                                    ChangePasswordScreen(
                                        lockMethod = lockMethod,
                                        onLockMethodChanged = { AppSettings.setLockMethod(it) },
                                        onClose = onCloseChange
                                    )
                                },
                                appVersionName = appVersionName,
                                onClose = { settingsOpen = false }
                            )
                        }
                        else -> SkeletonContent(
                            modifier = Modifier.padding(innerPadding),
                            onOpenSettings = { settingsOpen = true },
                            onOpenTrash = { trashOpen = true }
                        )
                    }
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        EncLog.d("MainActivity", "onResume")
    }

    override fun onPause() {
        super.onPause()
        EncLog.d("MainActivity", "onPause")

    }

    override fun onStart() {
        super.onStart()
        val backgrounded = SessionState.backgroundedAt
        val timeoutMin = AppSettings.lockTimeoutMinutes.value
        if (backgrounded != null && timeoutMin > 0 && SessionState.unlocked.value) {
            val elapsedMs = System.currentTimeMillis() - backgrounded
            val timeoutMs = timeoutMin * 60_000L
            if (elapsedMs >= timeoutMs) {
                EncLog.i(
                    "Session",
                    "auto-lock triggered: idle ${elapsedMs / 1000}s, threshold ${timeoutMin}min"
                )
                lockVaultNow()
            }
        }
        SessionState.backgroundedAt = null
    }

    override fun onStop() {
        super.onStop()
        if (SessionState.unlocked.value) {
            SessionState.backgroundedAt = System.currentTimeMillis()
        }
    }

    private fun applyScreenshotPolicy(allowScreenshots: Boolean) {
        if (allowScreenshots) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SkeletonContent(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenTrash: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var entries by remember { mutableStateOf(GallerySession.lastEntries) }
    var albumSummaries by remember { mutableStateOf(GallerySession.lastAlbumSummaries) }

    val trashEnabled by AppSettings.trashEnabled

    var albumGroups by remember { mutableStateOf(GallerySession.lastAlbumGroups) }
    var loaded by remember {
        mutableStateOf(
            GallerySession.lastEntries.isNotEmpty() ||
                GallerySession.lastAlbumSummaries.isNotEmpty()
        )
    }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var viewingIndex by remember { mutableStateOf(GallerySession.lastViewingIndex) }
    LaunchedEffect(viewingIndex) {
        GallerySession.lastViewingIndex = viewingIndex
    }

    val initialTab = remember {

        if (GallerySession.lastSelectedTab == GalleryTab.ALL) {
            try {
                GalleryTab.valueOf(AppSettings.lastTabName.value)
            } catch (_: IllegalArgumentException) {
                GalleryTab.ALL
            }
        } else GallerySession.lastSelectedTab
    }
    var selectedTab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(selectedTab) {
        GallerySession.lastSelectedTab = selectedTab
        AppSettings.setLastTabName(selectedTab.name)
    }

    var openedAlbumUuid by remember {
        mutableStateOf(GallerySession.lastOpenedAlbumUuid)
    }
    LaunchedEffect(openedAlbumUuid) {
        GallerySession.lastOpenedAlbumUuid = openedAlbumUuid
    }

    var openedGroupUuid by remember {
        mutableStateOf(GallerySession.lastOpenedGroupUuid)
    }
    LaunchedEffect(openedGroupUuid) {
        GallerySession.lastOpenedGroupUuid = openedGroupUuid
    }

    var albumPickerOp by remember {
        mutableStateOf(GallerySession.lastAlbumPickerOp)
    }
    var albumPickerSelection by remember {
        mutableStateOf(GallerySession.lastAlbumPickerSelection)
    }
    LaunchedEffect(albumPickerOp) {
        GallerySession.lastAlbumPickerOp = albumPickerOp
    }
    LaunchedEffect(albumPickerSelection) {
        GallerySession.lastAlbumPickerSelection = albumPickerSelection
    }

    var coverPickerAlbumUuid by remember {
        mutableStateOf(GallerySession.lastCoverPickerAlbumUuid)
    }
    var coverCropEntryUuid by remember {
        mutableStateOf(GallerySession.lastCoverCropEntryUuid)
    }
    LaunchedEffect(coverPickerAlbumUuid) {
        GallerySession.lastCoverPickerAlbumUuid = coverPickerAlbumUuid
    }
    LaunchedEffect(coverCropEntryUuid) {
        GallerySession.lastCoverCropEntryUuid = coverCropEntryUuid
    }

    var entryPickerOp by remember {
        mutableStateOf(GallerySession.lastEntryPickerOp)
    }
    var entryPickerSelection by remember {
        mutableStateOf(GallerySession.lastEntryPickerSelection)
    }
    LaunchedEffect(entryPickerOp) {
        GallerySession.lastEntryPickerOp = entryPickerOp
    }
    LaunchedEffect(entryPickerSelection) {
        GallerySession.lastEntryPickerSelection = entryPickerSelection
    }

    var entrySourcePickerDestAlbumUuid by remember {
        mutableStateOf<String?>(null)
    }

    var operationProgress by remember { mutableStateOf<OperationProgress?>(null) }

    var fileConflict by remember { mutableStateOf<FileConflictRequest?>(null) }

    var fileRenamePending by remember { mutableStateOf(false) }

    var albumConflict by remember { mutableStateOf<AlbumConflictRequest?>(null) }

    val importedAlbumName = stringResource(R.string.album_default_imported_name)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            AlbumsRepository(keystore).ensureImportedAlbum(
                context.applicationContext,
                defaultName = importedAlbumName
            )
            VaultIndex.migrateFlatToAlbums(context.applicationContext)
        }

        if (AppSettings.thumbnailGen() < ThumbnailMigration.CURRENT_GEN) {
            withContext(Dispatchers.IO) {
                ThumbnailMigration.regenerateAll(
                    context.applicationContext,
                    KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                )
                AppSettings.setThumbnailGen(ThumbnailMigration.CURRENT_GEN)
            }
            ThumbnailLoader.clearCache()
            refreshTrigger++
        }
    }

    LaunchedEffect(refreshTrigger, importedAlbumName) {
        val (freshEntries, freshAlbums, freshGroups) = withContext(Dispatchers.IO) {
            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            val albumsRepo = AlbumsRepository(keystore)
            val groupsRepo = AlbumGroupsRepository(keystore)

            MosaicAspectStore.loadInto(context.applicationContext, keystore)
            val allEntries = VaultIndex.listAllEntries(context.applicationContext)
            val byAlbum = allEntries.groupBy { it.albumUuid }
            val metas = albumsRepo.listAlbums(context.applicationContext)
            val summaries = metas.map { meta ->
                val albumEntries = byAlbum[meta.uuid] ?: emptyList()

                val displayMeta = if (meta.uuid == VaultPaths.IMPORTED_ALBUM_UUID) {
                    meta.copy(name = importedAlbumName)
                } else meta
                AlbumSummary(
                    meta = displayMeta,
                    entryCount = albumEntries.size,
                    firstEntry = albumEntries.firstOrNull()
                )
            }

            val albumByUuid = summaries.associateBy { it.meta.uuid }
            val groupSummaries = groupsRepo.listGroups(context.applicationContext)
                .map { groupMeta ->
                    AlbumGroupSummary(
                        meta = groupMeta,
                        albums = groupMeta.albumUuids.mapNotNull { albumByUuid[it] }
                    )
                }
            Triple(allEntries, summaries, groupSummaries)
        }
        entries = freshEntries
        albumSummaries = freshAlbums
        albumGroups = freshGroups
        GallerySession.lastEntries = freshEntries
        GallerySession.lastAlbumSummaries = freshAlbums
        GallerySession.lastAlbumGroups = freshGroups
        loaded = true

        withContext(Dispatchers.IO) {
            delay(1500)
            MosaicAspectStore.backfill(
                context.applicationContext,
                KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS),
                freshEntries
            )
        }
    }

    val openedAlbum: AlbumSummary? = openedAlbumUuid?.let { uuid ->
        albumSummaries.find { it.meta.uuid == uuid }
    }

    val groupedAlbumUuids: Set<String> = remember(albumGroups) {

        albumGroups.flatMap { it.meta.albumUuids }.toSet() - VaultPaths.IMPORTED_ALBUM_UUID
    }

    val albumSortOrderState by AppSettings.albumSortOrder
    val entrySortOrderState by AppSettings.entrySortOrder
    val entrySortRevision = AppSettings.entrySortRevision.intValue
    val albumSortRevision = AppSettings.albumSortRevision.intValue
    val filenameCacheRevision = GallerySession.entryFilenameCacheRevision

    val entrySectionKey: String = when {
        selectedTab == GalleryTab.ALL -> "ALL"
        openedAlbumUuid != null -> "album:$openedAlbumUuid"
        else -> "ALL"
    }
    val albumSectionKey: String = openedGroupUuid?.let { "group:$it" } ?: "ROOT"

    val topLevelAlbums: List<AlbumSummary> =
        remember(albumSummaries, groupedAlbumUuids, albumSortRevision, albumSortOrderState, importedAlbumName) {
            val sorted = sortAlbumSummaries(
                albumSummaries.filter { it.meta.uuid !in groupedAlbumUuids },
                AppSettings.albumSortOrderFor("ROOT")
            )

            val system = sorted.find { it.meta.uuid == VaultPaths.IMPORTED_ALBUM_UUID }
                ?.let { it.copy(meta = it.meta.copy(name = importedAlbumName)) }
            if (system == null) sorted
            else listOf(system) + sorted.filterNot { it.meta.uuid == VaultPaths.IMPORTED_ALBUM_UUID }
        }

    val sortedAlbumGroups: List<AlbumGroupSummary> =
        remember(albumGroups, albumSortRevision, albumSortOrderState) {
            sortAlbumGroupSummaries(albumGroups, AppSettings.albumSortOrderFor("ROOT"))
                .map { g ->
                    g.copy(

                        albums = sortAlbumSummaries(
                            g.albums.filterNot { it.meta.uuid == VaultPaths.IMPORTED_ALBUM_UUID },
                            AppSettings.albumSortOrderFor("group:${g.meta.uuid}")
                        )
                    )
                }
        }

    val openedGroup: AlbumGroupSummary? = openedGroupUuid?.let { uuid ->
        sortedAlbumGroups.find { it.meta.uuid == uuid }
    }
    LaunchedEffect(loaded, openedGroupUuid, openedGroup) {
        if (loaded && openedGroupUuid != null && openedGroup == null) {
            openedGroupUuid = null
        }
    }

    val currentEntrySort = AppSettings.entrySortOrderFor(entrySectionKey)
    val needsEntryNameResolve = currentEntrySort == EntrySortOrder.NAME_AZ ||
        currentEntrySort == EntrySortOrder.NAME_ZA
    LaunchedEffect(needsEntryNameResolve, entries) {
        if (!needsEntryNameResolve) return@LaunchedEffect
        val cache = GallerySession.entryFilenameCache
        val missing = entries.filter { it.uuid !in cache }
        if (missing.isEmpty()) return@LaunchedEffect
        val resolved = withContext(Dispatchers.IO) {
            EntriesRepository(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                .batchResolve(missing)
        }
        cache.putAll(resolved)
        GallerySession.entryFilenameCacheRevision++
    }

    val sortedEntries: List<VaultEntry> =
        remember(entries, entrySortRevision, entrySortOrderState, filenameCacheRevision) {
            sortEntries(entries, AppSettings.entrySortOrderFor("ALL"), GallerySession.entryFilenameCache)
        }

    val albumEntries: List<VaultEntry> =
        remember(entries, openedAlbumUuid, entrySortRevision, entrySortOrderState, filenameCacheRevision) {
            val albumUuid = openedAlbumUuid ?: return@remember emptyList()
            sortEntries(
                entries.filter { it.albumUuid == albumUuid },
                AppSettings.entrySortOrderFor("album:$albumUuid"),
                GallerySession.entryFilenameCache
            )
        }

    val viewerEntries: List<VaultEntry> = when (selectedTab) {
        GalleryTab.ALL -> sortedEntries
        GalleryTab.ALBUMS -> if (openedAlbum != null) albumEntries else emptyList()
    }

    val permanentDelete: (VaultEntry) -> Unit = { entry ->
        entries = entries.filter { it.uuid != entry.uuid }
        GallerySession.lastEntries = entries
        val albumNameHint = albumSummaries
            .firstOrNull { it.meta.uuid == entry.albumUuid }
            ?.meta?.name
        val routeToTrash = trashEnabled
        scope.launch {
            withContext(Dispatchers.IO) {
                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                if (routeToTrash) {
                    TrashRepository(keystore).moveToTrash(
                        context.applicationContext,
                        entry,
                        originalAlbumNameHint = albumNameHint
                    )
                } else {

                    deleteEntryOnDisk(entry)
                }
            }
            refreshTrigger++
        }
    }

    val moveTitleTemplate = stringResource(R.string.op_progress_title_move)
    val copyTitleTemplate = stringResource(R.string.op_progress_title_copy)
    val trashTitleTemplate = stringResource(R.string.op_progress_title_trash)
    val importTitleTemplate = stringResource(R.string.import_folder_progress_title)

    val clearGridSelection = {
        GallerySession.lastSelectedEntryUuids = emptySet()

        GallerySession.lastGridActionStage = GridActionStage.NONE

        GallerySession.inEntrySelectionMode = false
        GallerySession.selectionRevision = GallerySession.selectionRevision + 1
    }

    val handleDeleteSelection: (Set<String>) -> Unit = { uuids ->
        val toTrash = entries.filter { it.uuid in uuids }
        if (toTrash.isNotEmpty()) {

            val nameHintByUuid: Map<String, String?> = toTrash.associate { entry ->
                entry.uuid to albumSummaries
                    .firstOrNull { it.meta.uuid == entry.albumUuid }
                    ?.meta?.name
            }
            operationProgress = OperationProgress(
                kind = OperationKind.TRASH,
                destAlbumName = "",
                current = 0,
                total = toTrash.size,
                titleText = trashTitleTemplate
            )
            entries = entries.filter { it.uuid !in uuids }
            GallerySession.lastEntries = entries
            clearGridSelection()
            val routeToTrash = trashEnabled
            scope.launch {

                try {
                    withContext(Dispatchers.IO) {
                        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                        val repo = TrashRepository(keystore)
                        toTrash.forEachIndexed { idx, entry ->
                            if (routeToTrash) {
                                repo.moveToTrash(
                                    context.applicationContext,
                                    entry,
                                    originalAlbumNameHint = nameHintByUuid[entry.uuid]
                                )
                            } else {

                                deleteEntryOnDisk(entry)
                            }
                            operationProgress = OperationProgress(
                                kind = OperationKind.TRASH,
                                destAlbumName = "",
                                current = idx + 1,
                                total = toTrash.size,
                                titleText = trashTitleTemplate
                            )
                        }
                    }
                } finally {
                    operationProgress = null
                    refreshTrigger++
                }
            }
        }
    }

    val askFileConflict: suspend (String, String, (String) -> Boolean) -> FileConflictResolution =
        { name, suggested, nameTaken ->
            val def = CompletableDeferred<FileConflictResolution>()
            withContext(Dispatchers.Main) {
                fileRenamePending = false
                fileConflict = FileConflictRequest(name, suggested, nameTaken, def)
            }
            val decision = def.await()
            withContext(Dispatchers.Main) {
                fileConflict = null
                fileRenamePending = false
            }
            decision
        }

    val applyNameCacheChanges: (EntryBatchOutcome) -> Unit = { outcome ->
        outcome.renamed.forEach { (uuid, nm) ->
            GallerySession.entryFilenameCache[uuid] = nm
            GallerySession.infoProbeCache.remove(uuid)
        }
        outcome.removed.forEach { uuid ->
            GallerySession.entryFilenameCache.remove(uuid)
            GallerySession.infoProbeCache.remove(uuid)
        }
        if (outcome.renamed.isNotEmpty() || outcome.removed.isNotEmpty()) {
            GallerySession.entryFilenameCacheRevision++
        }
    }

    val handleMoveSelection: (Set<String>, String) -> Unit = { uuids, destAlbumUuid ->
        val toMove = entries.filter { it.uuid in uuids }
        if (toMove.isNotEmpty()) {
            val destName = albumSummaries
                .find { it.meta.uuid == destAlbumUuid }
                ?.meta?.name
                ?: ""
            val initialTitle = String.format(moveTitleTemplate, destName)
            operationProgress = OperationProgress(
                kind = OperationKind.MOVE,
                destAlbumName = destName,
                current = 0,
                total = toMove.size,
                titleText = initialTitle
            )

            entries = entries.filter { it.uuid !in uuids }
            GallerySession.lastEntries = entries
            clearGridSelection()
            scope.launch {

                try {
                    val outcome = withContext(Dispatchers.IO) {
                        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                        val moveResult = EntryConflictResolver.run(
                            context = context.applicationContext,
                            incoming = toMove,
                            destAlbumUuid = destAlbumUuid,
                            op = EntryBatchOp.MOVE,
                            keystore = keystore,
                            ask = askFileConflict,
                            onProgress = { done, total ->
                                operationProgress = OperationProgress(
                                    kind = OperationKind.MOVE,
                                    destAlbumName = destName,
                                    current = done,
                                    total = total,
                                    titleText = initialTitle
                                )
                            }
                        )

                        val repo = AlbumsRepository(keystore)
                        repo.touch(context.applicationContext, destAlbumUuid)
                        toMove.map { it.albumUuid }.toSet().forEach {
                            repo.touch(context.applicationContext, it)
                        }
                        moveResult
                    }
                    applyNameCacheChanges(outcome)
                } finally {
                    operationProgress = null
                    refreshTrigger++
                }
            }
        }
    }

    val handleCopySelection: (Set<String>, String) -> Unit = { uuids, destAlbumUuid ->
        val toCopy = entries.filter { it.uuid in uuids }
        if (toCopy.isNotEmpty()) {
            val destName = albumSummaries
                .find { it.meta.uuid == destAlbumUuid }
                ?.meta?.name
                ?: ""
            val initialTitle = String.format(copyTitleTemplate, destName)
            operationProgress = OperationProgress(
                kind = OperationKind.COPY,
                destAlbumName = destName,
                current = 0,
                total = toCopy.size,
                titleText = initialTitle
            )
            clearGridSelection()
            scope.launch {

                try {
                    val outcome = withContext(Dispatchers.IO) {
                        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                        val copyResult = EntryConflictResolver.run(
                            context = context.applicationContext,
                            incoming = toCopy,
                            destAlbumUuid = destAlbumUuid,
                            op = EntryBatchOp.COPY,
                            keystore = keystore,
                            ask = askFileConflict,
                            onProgress = { done, total ->
                                operationProgress = OperationProgress(
                                    kind = OperationKind.COPY,
                                    destAlbumName = destName,
                                    current = done,
                                    total = total,
                                    titleText = initialTitle
                                )
                            }
                        )

                        AlbumsRepository(keystore).touch(context.applicationContext, destAlbumUuid)
                        copyResult
                    }
                    applyNameCacheChanges(outcome)
                } finally {
                    operationProgress = null
                    refreshTrigger++
                }
            }
        }
    }

    val handleMoveRequest: (Set<String>) -> Unit = { uuids ->
        if (uuids.isNotEmpty()) {
            entryPickerSelection = uuids
            entryPickerOp = EntryPickerOp.MOVE
        }
    }
    val handleCopyRequest: (Set<String>) -> Unit = { uuids ->
        if (uuids.isNotEmpty()) {
            entryPickerSelection = uuids
            entryPickerOp = EntryPickerOp.COPY
        }
    }

    val clearAlbumSelection = {
        GallerySession.lastSelectedAlbumUuids = emptySet()

        GallerySession.lastAlbumSelectionStage = AlbumSelectionStage.NONE

        GallerySession.inAlbumSelectionMode = false
        GallerySession.albumSelectionRevision =
            GallerySession.albumSelectionRevision + 1
    }

    val handleCreateGroup: (String, Set<String>) -> Unit = { groupName, albumUuids ->
        scope.launch {
            withContext(Dispatchers.IO) {
                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                val groupsRepo = AlbumGroupsRepository(keystore)

                groupsRepo.ungroupAlbums(context.applicationContext, albumUuids.toList())
                groupsRepo.create(
                    context.applicationContext,
                    name = groupName,
                    albumUuids = albumUuids.toList()
                )
            }
            clearAlbumSelection()
            refreshTrigger++
        }
    }

    val handleAlbumMoveRequest: (Set<String>) -> Unit = { uuids ->

        val movable = uuids - VaultPaths.IMPORTED_ALBUM_UUID
        if (movable.isNotEmpty()) {
            albumPickerSelection = movable
            albumPickerOp = AlbumPickerOp.MOVE
        }
    }

    val askAlbumConflict: suspend (String, String, Boolean, (String) -> Boolean) -> FileConflictResolution =
        { albumName, suggested, intoGroup, nameTaken ->
            val def = CompletableDeferred<FileConflictResolution>()
            withContext(Dispatchers.Main) {
                albumConflict = AlbumConflictRequest(albumName, suggested, intoGroup, nameTaken, def)
            }
            val decision = def.await()
            withContext(Dispatchers.Main) { albumConflict = null }
            decision
        }

    val handleAlbumDestinationPicked: (String?) -> Unit = { destGroupUuid ->
        val uuids = albumPickerSelection
        albumPickerOp = AlbumPickerOp.NONE
        albumPickerSelection = emptySet()
        if (uuids.isNotEmpty()) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val appCtx = context.applicationContext
                    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                    val groupsRepo = AlbumGroupsRepository(keystore)
                    val albumsRepo = AlbumsRepository(keystore)
                    if (destGroupUuid == null) {

                        groupsRepo.ungroupAlbums(appCtx, uuids.toList())
                    } else {

                        val destGroup = groupsRepo.listGroups(appCtx)
                            .find { it.uuid == destGroupUuid }
                        val nameToUuid = HashMap<String, String>()
                        destGroup?.albumUuids?.forEach { aid ->
                            albumsRepo.getAlbum(appCtx, aid)?.name?.let { nm ->
                                nameToUuid[nm.trim().lowercase()] = aid
                            }
                        }
                        val taken = nameToUuid.keys.toMutableSet()
                        val nameTaken: (String) -> Boolean =
                            { c -> taken.contains(c.trim().lowercase()) }
                        val toAdd = ArrayList<String>()
                        var blanket: FileConflictChoice? = null

                        for (uuid in uuids) {
                            val meta = albumsRepo.getAlbum(appCtx, uuid) ?: continue
                            val nm = meta.name
                            val key = nm.trim().lowercase()
                            if (!taken.contains(key)) {
                                taken.add(key)
                                toAdd.add(uuid)
                                continue
                            }

                            val choice: FileConflictChoice
                            var typedName: String? = null
                            val pinned = blanket
                            if (pinned != null) {
                                choice = pinned
                            } else {
                                val suggested = nextFreeConflictName(nm, taken)
                                val res = askAlbumConflict(nm, suggested, true, nameTaken)

                                if (res.applyToAll &&
                                    res.choice != FileConflictChoice.CANCEL
                                ) {
                                    blanket = res.choice
                                }
                                choice = res.choice
                                typedName = res.newName
                            }
                            when (choice) {
                                FileConflictChoice.CANCEL -> return@withContext
                                FileConflictChoice.SKIP -> {   }
                                FileConflictChoice.REPLACE -> {
                                    nameToUuid[key]?.let { existing ->
                                        groupsRepo.ungroupAlbums(appCtx, listOf(existing))
                                        albumsRepo.deletePermanently(appCtx, existing)
                                    }
                                    toAdd.add(uuid)
                                }
                                FileConflictChoice.RENAME -> {
                                    val finalName = typedName?.takeIf { it.isNotBlank() }
                                        ?: nextFreeConflictName(nm, taken)
                                    albumsRepo.rename(appCtx, uuid, finalName)
                                    taken.add(finalName.trim().lowercase())
                                    toAdd.add(uuid)
                                }
                            }
                        }
                        if (toAdd.isNotEmpty()) {
                            groupsRepo.moveAlbumsToGroup(appCtx, destGroupUuid, toAdd)
                        }
                    }
                }
                clearAlbumSelection()
                refreshTrigger++
            }
        }
    }

    val handleDeleteAlbumsSelection: (Set<String>) -> Unit = { uuids ->
        if (uuids.isNotEmpty()) {
            val routeToTrash = trashEnabled
            scope.launch {
                withContext(Dispatchers.IO) {
                    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                    val albumsRepo = AlbumsRepository(keystore)
                    val groupsRepo = AlbumGroupsRepository(keystore)
                    val trashRepo = TrashRepository(keystore)
                    if (routeToTrash) {

                        val groups = groupsRepo.listGroups(context.applicationContext)
                        val groupOfAlbum: Map<String, String> = buildMap {
                            groups.forEach { g ->
                                g.albumUuids.forEach { aid -> put(aid, g.uuid) }
                            }
                        }
                        uuids.forEach { uuid ->
                            val albumMeta = albumsRepo.getAlbum(context.applicationContext, uuid)
                                ?: return@forEach
                            trashRepo.moveAlbumToTrash(
                                context.applicationContext,
                                albumMeta,
                                originalGroupUuid = groupOfAlbum[uuid]
                            )
                        }
                    } else {

                        groupsRepo.ungroupAlbums(context.applicationContext, uuids.toList())
                        uuids.forEach { uuid ->
                            albumsRepo.deletePermanently(context.applicationContext, uuid)
                        }
                    }
                }
                clearAlbumSelection()
                refreshTrigger++
            }
        }
    }

    val handleRenameGroup: (dev.encgallery.gallery.AlbumGroupMeta, String) -> Unit =
        { groupMeta, newName ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                    AlbumGroupsRepository(keystore).rename(
                        context.applicationContext,
                        groupMeta.uuid,
                        newName
                    )
                }
                refreshTrigger++
            }
        }
    val handleDeleteGroup: (dev.encgallery.gallery.AlbumGroupMeta) -> Unit = { groupMeta ->
        val routeToTrash = trashEnabled

        val displayOrderedMeta = sortedAlbumGroups
            .find { it.meta.uuid == groupMeta.uuid }
            ?.albums?.map { it.meta.uuid }
            ?.takeIf { it.isNotEmpty() }
            ?.let { groupMeta.copy(albumUuids = it) }
            ?: groupMeta
        scope.launch {
            withContext(Dispatchers.IO) {
                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                if (routeToTrash) {

                    TrashRepository(keystore).moveGroupToTrash(
                        context.applicationContext,
                        displayOrderedMeta
                    )
                } else {

                    AlbumGroupsRepository(keystore).deleteGroup(
                        context.applicationContext,
                        groupMeta.uuid
                    )
                }
            }

            if (openedGroupUuid == groupMeta.uuid) {
                openedGroupUuid = null
            }
            refreshTrigger++
        }
    }

    val handleSetAlbumsPinned: (Set<String>, Boolean) -> Unit = { uuids, pinned ->
        if (uuids.isNotEmpty()) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                    val albumsRepo = AlbumsRepository(keystore)
                    uuids.forEach { uuid ->
                        albumsRepo.setPinned(context.applicationContext, uuid, pinned)
                    }
                }
                refreshTrigger++
            }
        }
    }
    val handleSetGroupPinned: (String, Boolean) -> Unit = { groupUuid, pinned ->
        scope.launch {
            withContext(Dispatchers.IO) {
                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                AlbumGroupsRepository(keystore)
                    .setPinned(context.applicationContext, groupUuid, pinned)
            }
            refreshTrigger++
        }
    }

    val wallpaperDoneText = stringResource(R.string.viewer_wallpaper_done)
    val wallpaperFailedText = stringResource(R.string.viewer_wallpaper_failed)

    var wallpaperCropEntryUuid by remember {
        mutableStateOf(GallerySession.lastWallpaperCropEntryUuid)
    }
    var wallpaperCropTarget by remember {
        mutableStateOf(GallerySession.lastWallpaperCropTarget)
    }
    LaunchedEffect(wallpaperCropEntryUuid) {
        GallerySession.lastWallpaperCropEntryUuid = wallpaperCropEntryUuid
    }
    LaunchedEffect(wallpaperCropTarget) {
        GallerySession.lastWallpaperCropTarget = wallpaperCropTarget
    }
    val wallpaperCropEntry: VaultEntry? = wallpaperCropEntryUuid?.let { uuid ->
        entries.firstOrNull { it.uuid == uuid }
    }
    LaunchedEffect(loaded, wallpaperCropEntryUuid, wallpaperCropEntry) {
        if (loaded && wallpaperCropEntryUuid != null && wallpaperCropEntry == null) {
            wallpaperCropEntryUuid = null
            wallpaperCropTarget = null
        }
    }

    LaunchedEffect(wallpaperCropEntryUuid) {
        if (wallpaperCropEntryUuid == null) {
            GallerySession.lastWallpaperCropFrameUuid = null
            GallerySession.lastWallpaperCropHPx = 0f
            GallerySession.lastWallpaperCropCenterX = 0f
            GallerySession.lastWallpaperCropCenterY = 0f
            GallerySession.lastWallpaperCropBitmap = null
        }
    }
    val handleSetWallpaperRequest: (String, WallpaperTarget) -> Unit = { uuid, target ->
        val entry = entries.firstOrNull { it.uuid == uuid }
        if (entry != null) {
            wallpaperCropEntryUuid = entry.uuid
            wallpaperCropTarget = target
        }
    }

    val handleRenameRequest: (String, String) -> Unit = { uuid, newName ->
        val entry = entries.firstOrNull { it.uuid == uuid }
        if (entry != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                    EntriesRepository(keystore).rename(entry.blobFile, newName)
                }
                if (ok) {
                    GallerySession.entryFilenameCache[uuid] = newName
                    GallerySession.entryFilenameCacheRevision++
                    GallerySession.infoProbeCache.remove(uuid)
                }
            }
        }
    }

    val handleShareSelection: (Set<String>) -> Unit = { uuids ->
        val toShare = entries.filter { it.uuid in uuids }
        if (toShare.isNotEmpty()) {
            scope.launch {
                try {
                    EntryShare.shareMultiple(
                        context = context,
                        entries = toShare,
                        keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                    )
                } catch (t: Throwable) {
                    EncLog.w(
                        "MainActivity",
                        "shareMultiple failed (${toShare.size} files): ${t.javaClass.simpleName}: ${t.message}"
                    )
                }
            }
        }
    }

    var pendingExportEntry by remember { mutableStateOf<VaultEntry?>(null) }
    var pendingExportEntries by remember { mutableStateOf<List<VaultEntry>>(emptyList()) }

    var pendingExportAlbums by remember { mutableStateOf<List<AlbumExportBundle>>(emptyList()) }

    val exportSingleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val entry = pendingExportEntry
        pendingExportEntry = null
        if (uri != null && entry != null) {
            scope.launch {
                val ok = EntryExport.exportToUri(
                    context, entry, uri, KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                )
                Toast.makeText(
                    context,
                    if (ok) R.string.export_done_single else R.string.export_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val exportTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        val items = pendingExportEntries
        pendingExportEntries = emptyList()
        if (treeUri != null && items.isNotEmpty()) {
            scope.launch {
                val (ok, total) = EntryExport.exportToTree(
                    context, items, treeUri, KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                )
                val msg = if (ok == 0) {
                    context.getString(R.string.export_failed)
                } else {
                    context.getString(R.string.export_done_multi, ok, total)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportAlbumsTreeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        val bundles = pendingExportAlbums
        pendingExportAlbums = emptyList()
        if (treeUri != null && bundles.isNotEmpty()) {
            scope.launch {
                val (ok, total) = EntryExport.exportAlbumsToTree(
                    context, bundles, treeUri, KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                )
                val msg = if (ok == 0) {
                    context.getString(R.string.export_failed)
                } else {
                    context.getString(R.string.export_done_multi, ok, total)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var importPickerVisible by remember { mutableStateOf(false) }
    var importBuckets by remember { mutableStateOf<List<DeviceBucket>?>(null) }

    val mediaReadPerms = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val openImportPicker: () -> Unit = {
        importBuckets = null
        importPickerVisible = true
        scope.launch {
            importBuckets = DeviceMediaBuckets.listBuckets(context.applicationContext)
        }
    }

    val requestMediaPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->

        if (result.values.any { it }) {
            openImportPicker()
        } else {
            Toast.makeText(context, R.string.import_perm_denied, Toast.LENGTH_LONG).show()
        }
    }

    val handleImportFromDevice: () -> Unit = {
        requestMediaPermLauncher.launch(mediaReadPerms)
    }

    val handleImportBuckets: (List<DeviceBucket>) -> Unit = { chosen ->
        if (chosen.isNotEmpty()) {
            scope.launch {
                val appCtx = context.applicationContext
                val grandTotal = chosen.sumOf { it.count }
                var processed = 0
                var okCount = 0
                val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                try {
                    for (bucket in chosen) {
                        val title = String.format(importTitleTemplate, bucket.name)
                        operationProgress = OperationProgress(
                            OperationKind.IMPORT, bucket.name, processed, grandTotal, title
                        )
                        val uris = DeviceMediaBuckets.bucketItemUris(appCtx, bucket.id)
                        if (uris.isEmpty()) continue
                        val albumUuid = withContext(Dispatchers.IO) {
                            AlbumsRepository(keystore).create(appCtx, bucket.name).uuid
                        }
                        withContext(Dispatchers.IO) {
                            PhotoImporter(keystore).importAll(
                                context = appCtx,
                                uris = uris,
                                targetAlbumUuid = albumUuid
                            ) { prog ->
                                when (prog) {
                                    is PhotoImporter.Progress.FileSucceeded -> {
                                        okCount++; processed++
                                    }
                                    is PhotoImporter.Progress.FileFailed -> processed++
                                    else -> {}
                                }
                                if (prog is PhotoImporter.Progress.FileSucceeded ||
                                    prog is PhotoImporter.Progress.FileFailed
                                ) {
                                    operationProgress = OperationProgress(
                                        OperationKind.IMPORT, bucket.name, processed, grandTotal, title
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    operationProgress = null
                    refreshTrigger++
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.import_folder_done, okCount, grandTotal),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val handleExportSingle: (VaultEntry) -> Unit = { entry ->

        scope.launch {
            val name = EntryExport.suggestedName(
                context, entry, KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
            )
            pendingExportEntry = entry
            exportSingleLauncher.launch(name)
        }
    }
    val handleExportSelection: (Set<String>) -> Unit = { uuids ->
        val items = entries.filter { it.uuid in uuids }
        if (items.isNotEmpty()) {
            pendingExportEntries = items

            exportTreeLauncher.launch(null)
        }
    }

    val handleExportAlbums: (Set<String>) -> Unit = { albumUuids ->
        if (albumUuids.isNotEmpty()) {
            scope.launch {
                val bundles = withContext(Dispatchers.IO) {
                    val appCtx = context.applicationContext
                    albumUuids.mapNotNull { uuid ->
                        val name = albumSummaries
                            .find { it.meta.uuid == uuid }?.meta?.name
                            ?: return@mapNotNull null
                        val items = VaultIndex.listEntriesInAlbum(appCtx, uuid)
                        if (items.isEmpty()) null else AlbumExportBundle(name, items)
                    }
                }
                if (bundles.isNotEmpty()) {
                    pendingExportAlbums = bundles
                    exportAlbumsTreeLauncher.launch(null)
                }
            }
        }
    }

    val coverPickerAlbum: AlbumSummary? = coverPickerAlbumUuid?.let { uuid ->
        albumSummaries.find { it.meta.uuid == uuid }
    }
    val coverPickerEntries: List<VaultEntry> =
        remember(entries, coverPickerAlbumUuid, entrySortRevision, entrySortOrderState, filenameCacheRevision) {
            val uuid = coverPickerAlbumUuid ?: return@remember emptyList()
            sortEntries(
                entries.filter { it.albumUuid == uuid },
                AppSettings.entrySortOrderFor("album:$uuid"),
                GallerySession.entryFilenameCache
            )
        }
    LaunchedEffect(coverPickerAlbumUuid, entries, entrySortOrderState) {
        val uuid = coverPickerAlbumUuid ?: return@LaunchedEffect
        val order = AppSettings.entrySortOrderFor("album:$uuid")
        if (order != EntrySortOrder.NAME_AZ && order != EntrySortOrder.NAME_ZA) return@LaunchedEffect
        val cache = GallerySession.entryFilenameCache
        val missing = entries.filter { it.albumUuid == uuid && it.uuid !in cache }
        if (missing.isEmpty()) return@LaunchedEffect
        val resolved = withContext(Dispatchers.IO) {
            EntriesRepository(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                .batchResolve(missing)
        }
        cache.putAll(resolved)
        GallerySession.entryFilenameCacheRevision++
    }
    val coverCropEntry: VaultEntry? = coverCropEntryUuid?.let { uuid ->
        coverPickerEntries.firstOrNull { it.uuid == uuid }
    }
    LaunchedEffect(loaded, coverPickerAlbumUuid, coverPickerAlbum) {
        if (loaded && coverPickerAlbumUuid != null && coverPickerAlbum == null) {
            coverPickerAlbumUuid = null
            coverCropEntryUuid = null
        }
    }
    LaunchedEffect(loaded, coverCropEntryUuid, coverCropEntry) {
        if (loaded && coverCropEntryUuid != null && coverCropEntry == null) {

            coverCropEntryUuid = null
        }
    }

    val handleSaveCover: (NormalizedRect) -> Unit = { rect ->
        val album = coverPickerAlbum
        val entry = coverCropEntry
        if (album != null && entry != null) {
            val targetAlbumUuid = album.meta.uuid
            scope.launch {
                withContext(Dispatchers.IO) {
                    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                    val updated = AlbumsRepository(keystore).setCustomCover(
                        context.applicationContext,
                        targetAlbumUuid,
                        entry,
                        rect
                    )
                    if (updated == null) {
                        EncLog.w("MainActivity", "setCustomCover returned null for $targetAlbumUuid")
                    }
                    ThumbnailLoader.forgetAlbumCover(targetAlbumUuid)
                }
                refreshTrigger++
            }

            coverPickerAlbumUuid = null
            coverCropEntryUuid = null
        }
    }

    var addRequestSignal by remember { mutableIntStateOf(0) }

    var fabAnchorBounds by remember {
        mutableStateOf(GallerySession.lastFabAnchor)
    }
    val tabBarBottom by AppSettings.tabBarAtBottom

    val photoGridLayoutNow by AppSettings.photoGridLayout
    val photoGridEnlarged = photoGridLayoutNow == dev.encgallery.featuresettings.PhotoGridLayout.ENLARGED

    var searchActive by remember { mutableStateOf(GallerySession.lastSearchActive) }
    var searchQuery by remember { mutableStateOf(GallerySession.lastSearchQuery) }
    LaunchedEffect(searchActive) { GallerySession.lastSearchActive = searchActive }
    LaunchedEffect(searchQuery) { GallerySession.lastSearchQuery = searchQuery }

    var sortDialogIsAlbums by remember {
        mutableStateOf(GallerySession.lastSortDialogIsAlbums)
    }
    LaunchedEffect(sortDialogIsAlbums) {
        GallerySession.lastSortDialogIsAlbums = sortDialogIsAlbums
    }

    var topMenuExpanded by remember {
        mutableStateOf(GallerySession.lastTopMenuExpanded)
    }
    LaunchedEffect(topMenuExpanded) {
        GallerySession.lastTopMenuExpanded = topMenuExpanded
    }

    var topMenuAnchorBounds by remember {
        mutableStateOf(GallerySession.lastTopMenuAnchor)
    }

    var topMenuRootOffset by remember {
        mutableStateOf(GallerySession.lastTopMenuRootOffset)
    }
    var burgerExpanded by remember {
        mutableStateOf(GallerySession.lastBurgerMenuExpanded)
    }
    LaunchedEffect(burgerExpanded) {
        GallerySession.lastBurgerMenuExpanded = burgerExpanded
    }
    var burgerAnchorBounds by remember {
        mutableStateOf(GallerySession.lastBurgerAnchor)
    }
    var burgerRootOffset by remember {
        mutableStateOf(GallerySession.lastBurgerRootOffset)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            if (searchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        searchActive = false
                        searchQuery = ""
                    }) {
                        ThemedIcon(
                            fallback = Icons.Default.ArrowBack,
                            medievalDrawable = MedievalIcons.ArrowBack,
                            contentDescription = stringResource(R.string.search_close)
                        )
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text(stringResource(R.string.search_field_hint))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    ThemedIcon(
                                        fallback = Icons.Default.Clear,
                                        medievalDrawable = MedievalIcons.Close,
                                        contentDescription = stringResource(R.string.search_clear)
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                BackHandler(enabled = true) {
                    searchActive = false
                    searchQuery = ""
                }
            } else Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val chromeActionTint = if (LocalThemeVariant.current == ThemeVariant.BLOODY) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                if (selectedTab == GalleryTab.ALBUMS) {
                    IconButton(
                        onClick = {
                            val curAlbumUuid = openedAlbumUuid
                            if (curAlbumUuid != null) {
                                entrySourcePickerDestAlbumUuid = curAlbumUuid
                            } else {
                                addRequestSignal += 1
                            }
                        },
                        modifier = Modifier.onGloballyPositioned { coords ->

                            if (coords.isAttached) {
                                val pos = coords.positionInWindow()
                                val r = Rect(
                                    left = pos.x,
                                    top = pos.y,
                                    right = pos.x + coords.size.width,
                                    bottom = pos.y + coords.size.height
                                )
                                fabAnchorBounds = r
                                GallerySession.lastFabAnchor = r
                            }
                        }
                    ) {
                        ThemedIcon(
                            fallback = Icons.Default.Add,
                            medievalDrawable = MedievalIcons.Add,
                            contentDescription = stringResource(R.string.top_bar_add),
                            tint = chromeActionTint
                        )
                    }
                }

                IconButton(onClick = {

                    clearAlbumSelection()
                    clearGridSelection()
                    searchActive = true
                }) {
                    ThemedIcon(
                        fallback = Icons.Default.Search,
                        medievalDrawable = MedievalIcons.Search,
                        contentDescription = stringResource(R.string.top_bar_search),
                        tint = chromeActionTint
                    )
                }
                IconButton(
                    onClick = { topMenuExpanded = true },
                    modifier = Modifier.onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            val pos = coords.positionInWindow()
                            val r = Rect(
                                left = pos.x,
                                top = pos.y,
                                right = pos.x + coords.size.width,
                                bottom = pos.y + coords.size.height
                            )
                            topMenuAnchorBounds = r
                            GallerySession.lastTopMenuAnchor = r
                        }
                    }
                ) {
                    ThemedIcon(
                        fallback = Icons.Default.MoreVert,
                        medievalDrawable = MedievalIcons.MoreVert,
                        contentDescription = stringResource(R.string.menu_open),
                        tint = chromeActionTint
                    )
                }
            }

            val tabRow: @Composable () -> Unit = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,

                        divider = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Tab(
                            selected = selectedTab == GalleryTab.ALL,
                            onClick = {

                                if (selectedTab != GalleryTab.ALL) {
                                    GallerySession.albumsTabReturnAlbumUuid = openedAlbumUuid
                                    openedAlbumUuid = null
                                }
                                selectedTab = GalleryTab.ALL
                            },
                            text = { Text(stringResource(R.string.tab_all)) }
                        )
                        Tab(
                            selected = selectedTab == GalleryTab.ALBUMS,
                            onClick = {

                                if (selectedTab != GalleryTab.ALBUMS) {
                                    openedAlbumUuid =
                                        GallerySession.albumsTabReturnAlbumUuid
                                }
                                selectedTab = GalleryTab.ALBUMS
                            },
                            text = { Text(stringResource(R.string.tab_albums)) }
                        )
                    }
                    IconButton(
                        onClick = { burgerExpanded = true },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                val pos = coords.positionInWindow()
                                val r = Rect(
                                    left = pos.x,
                                    top = pos.y,
                                    right = pos.x + coords.size.width,
                                    bottom = pos.y + coords.size.height
                                )
                                burgerAnchorBounds = r
                                GallerySession.lastBurgerAnchor = r
                            }
                        }
                    ) {
                        ThemedIcon(
                            fallback = Icons.Default.Menu,
                            medievalDrawable = MedievalIcons.Menu,
                            contentDescription = stringResource(R.string.menu_open)
                        )
                    }
                }
            }

            val isEntryContextNow = selectedTab == GalleryTab.ALL ||
                openedAlbumUuid != null
            val anySelectionMode = if (isEntryContextNow) {
                GallerySession.inEntrySelectionMode ||
                    GallerySession.lastSelectedEntryUuids.isNotEmpty()
            } else {
                GallerySession.inAlbumSelectionMode ||
                    GallerySession.lastSelectedAlbumUuids.isNotEmpty()
            }
            val activeSelectionCount = if (isEntryContextNow) {
                GallerySession.lastSelectedEntryUuids.size
            } else {
                GallerySession.lastSelectedAlbumUuids.size
            }
            val cancelActiveSelection = {
                if (isEntryContextNow) clearGridSelection() else clearAlbumSelection()
            }
            if (!tabBarBottom) {
                if (anySelectionMode) {
                    HostSelectionTopBar(
                        count = activeSelectionCount,
                        onCancel = cancelActiveSelection
                    )
                } else {
                    tabRow()
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    GalleryTab.ALL -> GalleryHost(
                        entries = sortedEntries,
                        loaded = loaded,
                        trashEnabled = trashEnabled,
                        onView = { idx -> viewingIndex = idx },
                        onDeleteSelection = handleDeleteSelection,
                        onMoveRequest = handleMoveRequest,
                        onCopyRequest = handleCopyRequest,
                        onShareSelection = handleShareSelection,
                        onExportSelection = handleExportSelection,
                        onRenameRequest = handleRenameRequest,
                        onSetWallpaperRequest = handleSetWallpaperRequest,
                        onImportComplete = { refreshTrigger++ },
                        searchQuery = if (searchActive) searchQuery else "",
                        enlarged = photoGridEnlarged,

                        modifier = Modifier.fillMaxSize()
                    )
                    GalleryTab.ALBUMS -> {
                        if (openedAlbum != null) {
                            AlbumDetailScreen(
                                albumMeta = openedAlbum.meta,
                                entries = viewerEntries,
                                trashEnabled = trashEnabled,
                                onBack = { openedAlbumUuid = null },
                                onView = { entry ->
                                    val idx = viewerEntries.indexOfFirst { it.uuid == entry.uuid }
                                    if (idx >= 0) viewingIndex = idx
                                },
                                onDeleteSelection = handleDeleteSelection,
                                onMoveRequest = handleMoveRequest,
                                onCopyRequest = handleCopyRequest,
                                onShareSelection = handleShareSelection,
                                onExportSelection = handleExportSelection,
                                onRenameRequest = handleRenameRequest,
                                onSetWallpaperRequest = handleSetWallpaperRequest,
                                onImportComplete = { refreshTrigger++ },
                                searchQuery = if (searchActive) searchQuery else "",
                                enlarged = photoGridEnlarged
                            )
                        } else {
                            AlbumsListScreen(
                                albums = topLevelAlbums,
                                groups = sortedAlbumGroups,
                                openedGroup = openedGroup,
                                loaded = loaded,
                                trashEnabled = trashEnabled,
                                addRequestSignal = addRequestSignal,
                                fabAnchorBounds = fabAnchorBounds,
                                searchQuery = if (searchActive) searchQuery else "",
                                onCreate = { name ->

                                    val targetGroupUuid = openedGroupUuid
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                            val newMeta = AlbumsRepository(keystore).create(
                                                context.applicationContext,
                                                name
                                            )
                                            if (targetGroupUuid != null) {
                                                AlbumGroupsRepository(keystore).addAlbumsToGroup(
                                                    context.applicationContext,
                                                    targetGroupUuid,
                                                    listOf(newMeta.uuid)
                                                )
                                            }
                                        }
                                        refreshTrigger++
                                    }
                                },
                                onRename = { meta, newName ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                            AlbumsRepository(keystore).rename(
                                                context.applicationContext,
                                                meta.uuid,
                                                newName
                                            )
                                        }
                                        refreshTrigger++
                                    }
                                },
                                onChangeCover = { meta ->

                                    clearAlbumSelection()

                                    GallerySession.lastCoverPickerFirstVisibleItem = 0
                                    GallerySession.lastCoverPickerFirstVisibleOffset = 0
                                    coverPickerAlbumUuid = meta.uuid
                                    coverCropEntryUuid = null
                                },
                                onDelete = { meta ->
                                    val routeToTrash = trashEnabled
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                                            val groupsRepo = AlbumGroupsRepository(keystore)
                                            if (routeToTrash) {
                                                val trashRepo = TrashRepository(keystore)
                                                val originalGroupUuid =
                                                    groupsRepo.listGroups(context.applicationContext)
                                                        .firstOrNull { meta.uuid in it.albumUuids }?.uuid
                                                trashRepo.moveAlbumToTrash(
                                                    context.applicationContext,
                                                    meta,
                                                    originalGroupUuid = originalGroupUuid
                                                )
                                            } else {

                                                groupsRepo.ungroupAlbums(
                                                    context.applicationContext,
                                                    listOf(meta.uuid)
                                                )
                                                AlbumsRepository(keystore).deletePermanently(
                                                    context.applicationContext,
                                                    meta.uuid
                                                )
                                            }
                                        }
                                        refreshTrigger++
                                    }
                                },
                                onOpen = { meta -> openedAlbumUuid = meta.uuid },
                                onOpenGroup = { groupUuid ->
                                    openedGroupUuid = groupUuid
                                },
                                onCloseGroup = { openedGroupUuid = null },
                                onCreateGroup = handleCreateGroup,
                                onImportFolder = handleImportFromDevice,
                                onMoveAlbumsRequest = handleAlbumMoveRequest,
                                onDeleteAlbumsSelection = handleDeleteAlbumsSelection,
                                onExportAlbums = handleExportAlbums,
                                onRenameGroup = handleRenameGroup,
                                onDeleteGroup = handleDeleteGroup,
                                onSetAlbumsPinned = handleSetAlbumsPinned,
                                onSetGroupPinned = handleSetGroupPinned,
                                columns = AppSettings.albumColumns.value,
                                tileShape = AppSettings.albumTileShape.value
                            )
                        }
                    }
                }
            }
            if (tabBarBottom) {
                if (anySelectionMode) {
                    HostSelectionTopBar(
                        count = activeSelectionCount,
                        onCancel = cancelActiveSelection
                    )
                } else {
                    tabRow()
                }
            }
        }

        if (loaded) {
            viewingIndex?.let { idx ->
                if (idx in viewerEntries.indices) {
                    FullScreenViewer(
                        entries = viewerEntries,
                        startIndex = idx,
                        trashEnabled = trashEnabled,
                        tileShape = AppSettings.albumTileShape.value,
                        onClose = {

                            GallerySession.pendingScrollToViewedUuid.value =
                                GallerySession.lastViewedEntryUuid
                            viewingIndex = null
                        },
                        onDelete = { entry: VaultEntry ->

                            val remaining = viewerEntries.filter { it.uuid != entry.uuid }
                            if (remaining.isEmpty()) viewingIndex = null
                            permanentDelete(entry)
                        },
                        onCopySingle = { entry ->

                            handleCopyRequest(setOf(entry.uuid))
                        },
                        onMoveSingle = { entry ->

                            handleMoveRequest(setOf(entry.uuid))
                        },
                        onRenameSingle = { entry, newName ->
                            handleRenameRequest(entry.uuid, newName)
                        },
                        onSetWallpaper = { entry, target ->
                            handleSetWallpaperRequest(entry.uuid, target)
                        },
                        onExportSingle = handleExportSingle
                    )
                } else {

                    viewingIndex = null
                }
            }
        }

        if (importPickerVisible) {
            DeviceImportPickerScreen(
                buckets = importBuckets,
                columns = AppSettings.albumColumns.value,
                tileShape = AppSettings.albumTileShape.value,
                onImport = { chosen ->
                    importPickerVisible = false
                    importBuckets = null
                    handleImportBuckets(chosen)
                },
                onCancel = {
                    importPickerVisible = false
                    importBuckets = null
                }
            )
        }

        if (loaded && coverPickerAlbum != null) {
            if (coverCropEntry != null) {
                AlbumCoverCropScreen(
                    albumMeta = coverPickerAlbum.meta,
                    entry = coverCropEntry,
                    onSave = handleSaveCover,
                    onCancel = { coverCropEntryUuid = null }
                )
            } else {
                AlbumCoverPickerScreen(
                    albumMeta = coverPickerAlbum.meta,
                    entries = coverPickerEntries,
                    enlarged = photoGridEnlarged,
                    onPick = { entry -> coverCropEntryUuid = entry.uuid },
                    onCancel = {
                        coverPickerAlbumUuid = null
                        coverCropEntryUuid = null
                    }
                )
            }
        }

        if (loaded && entryPickerOp != EntryPickerOp.NONE) {
            EntryDestinationPickerScreen(
                op = entryPickerOp,
                selectionCount = entryPickerSelection.size,
                albums = albumSummaries,
                groups = sortedAlbumGroups,
                columns = AppSettings.albumColumns.value,
                tileShape = AppSettings.albumTileShape.value,
                onPick = { destAlbumUuid ->
                    val op = entryPickerOp
                    val uuids = entryPickerSelection

                    entryPickerOp = EntryPickerOp.NONE
                    entryPickerSelection = emptySet()
                    when (op) {
                        EntryPickerOp.MOVE -> handleMoveSelection(uuids, destAlbumUuid)
                        EntryPickerOp.COPY -> handleCopySelection(uuids, destAlbumUuid)
                        EntryPickerOp.NONE -> Unit
                    }
                },
                onCreateAlbumInGroup = { groupUuid, name ->

                    val op = entryPickerOp
                    val uuids = entryPickerSelection
                    entryPickerOp = EntryPickerOp.NONE
                    entryPickerSelection = emptySet()
                    scope.launch {
                        val newUuid = withContext(Dispatchers.IO) {
                            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                            val newMeta = AlbumsRepository(keystore).create(
                                context.applicationContext,
                                name
                            )
                            AlbumGroupsRepository(keystore).addAlbumsToGroup(
                                context.applicationContext,
                                groupUuid,
                                listOf(newMeta.uuid)
                            )
                            newMeta.uuid
                        }
                        refreshTrigger++
                        when (op) {
                            EntryPickerOp.MOVE -> handleMoveSelection(uuids, newUuid)
                            EntryPickerOp.COPY -> handleCopySelection(uuids, newUuid)
                            EntryPickerOp.NONE -> Unit
                        }
                    }
                },
                onCreateAlbum = { name ->

                    val op = entryPickerOp
                    val uuids = entryPickerSelection
                    entryPickerOp = EntryPickerOp.NONE
                    entryPickerSelection = emptySet()
                    scope.launch {
                        val newUuid = withContext(Dispatchers.IO) {
                            val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                            AlbumsRepository(keystore)
                                .create(context.applicationContext, name)
                                .uuid
                        }
                        refreshTrigger++
                        when (op) {
                            EntryPickerOp.MOVE -> handleMoveSelection(uuids, newUuid)
                            EntryPickerOp.COPY -> handleCopySelection(uuids, newUuid)
                            EntryPickerOp.NONE -> Unit
                        }
                    }
                },
                onCancel = {

                    entryPickerOp = EntryPickerOp.NONE
                    entryPickerSelection = emptySet()
                    clearGridSelection()
                }
            )
        }

        if (loaded && albumPickerOp != AlbumPickerOp.NONE) {
            GroupDestinationPickerScreen(
                selectionCount = albumPickerSelection.size,
                albums = albumSummaries,
                groups = sortedAlbumGroups,
                columns = AppSettings.albumColumns.value,
                tileShape = AppSettings.albumTileShape.value,
                onPick = { destGroupUuid ->
                    handleAlbumDestinationPicked(destGroupUuid)
                },
                onCancel = {

                    albumPickerOp = AlbumPickerOp.NONE
                    albumPickerSelection = emptySet()
                    clearAlbumSelection()
                }
            )
        }

        entrySourcePickerDestAlbumUuid?.let { destUuid ->
            if (loaded) {
                val destName = albumSummaries
                    .find { it.meta.uuid == destUuid }
                    ?.meta?.name ?: ""
                EntrySourcePickerScreen(
                    destAlbumUuid = destUuid,
                    destAlbumName = destName,
                    albums = albumSummaries,
                    groups = sortedAlbumGroups,
                    allEntries = entries,
                    columns = AppSettings.albumColumns.value,
                    tileShape = AppSettings.albumTileShape.value,
                    enlarged = AppSettings.photoGridLayout.value ==
                        dev.encgallery.featuresettings.PhotoGridLayout.ENLARGED,
                    onCommit = { uuids, op ->
                        entrySourcePickerDestAlbumUuid = null
                        if (uuids.isNotEmpty()) {
                            when (op) {
                                EntryPickerOp.COPY -> handleCopySelection(uuids, destUuid)
                                EntryPickerOp.MOVE -> handleMoveSelection(uuids, destUuid)
                                EntryPickerOp.NONE -> Unit
                            }
                        }
                    },
                    onCancel = { entrySourcePickerDestAlbumUuid = null }
                )
            }
        }

        wallpaperCropEntry?.let { wpEntry ->
            wallpaperCropTarget?.let { wpTarget ->
                WallpaperCropScreen(
                    entry = wpEntry,
                    target = wpTarget,
                    onSave = { croppedBitmap ->
                        wallpaperCropEntryUuid = null
                        wallpaperCropTarget = null
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                WallpaperOps.setBitmap(
                                    context = context.applicationContext,
                                    bitmap = croppedBitmap,
                                    target = wpTarget
                                )
                            }
                            Toast.makeText(
                                context.applicationContext,
                                if (ok) wallpaperDoneText else wallpaperFailedText,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onCancel = {
                        wallpaperCropEntryUuid = null
                        wallpaperCropTarget = null
                    }
                )
            }
        }

        operationProgress?.let { progress ->
            OperationProgressOverlay(progress = progress)
        }

        fileConflict?.let { req ->
            if (fileRenamePending) {

                val takenMsg = stringResource(R.string.file_rename_taken)
                EntryRenameDialog(
                    initialName = req.suggestedName,
                    allowUnchanged = true,
                    duplicateChecker = { candidate ->
                        if (req.nameTaken(candidate)) takenMsg else null
                    },
                    onConfirm = { finalName ->
                        fileRenamePending = false
                        req.deferred.complete(
                            FileConflictResolution(
                                FileConflictChoice.RENAME,
                                applyToAll = false,
                                newName = finalName
                            )
                        )
                    },

                    onDismiss = { fileRenamePending = false }
                )
            } else {
                FileConflictDialog(
                    fileName = req.fileName,
                    onResolve = { res ->

                        if (res.choice == FileConflictChoice.RENAME && !res.applyToAll) {
                            fileRenamePending = true
                        } else {
                            req.deferred.complete(res)
                        }
                    }
                )
            }
        }

        albumConflict?.let { req ->
            AlbumConflictFlow(request = req)
        }

        val gridActionStage = GallerySession.lastGridActionStage
        val selectedEntryUuids = GallerySession.lastSelectedEntryUuids
        if (gridActionStage == GridActionStage.RENAME) {
            val targetUuid = selectedEntryUuids.singleOrNull()
            val targetEntry: VaultEntry? = targetUuid?.let { uuid ->
                entries.firstOrNull { it.uuid == uuid }
            }
            if (targetEntry == null) {

                LaunchedEffect(targetUuid) {
                    GallerySession.lastGridActionStage = GridActionStage.NONE
                }
            } else {
                val cache = GallerySession.entryFilenameCache
                var hasResolution by remember(targetEntry.uuid) {
                    mutableStateOf(targetEntry.uuid in cache)
                }
                LaunchedEffect(targetEntry.uuid) {
                    if (targetEntry.uuid !in cache) {
                        val name = withContext(Dispatchers.IO) {
                            EntriesRepository(
                                KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
                            ).getMeta(targetEntry.blobFile)?.originalFilename
                        }
                        cache[targetEntry.uuid] = name
                        GallerySession.entryFilenameCacheRevision++
                        hasResolution = true
                    }
                }

                val dupMsg = stringResource(R.string.entry_dup_in_album)
                var siblingNames by remember(targetEntry.uuid) {
                    mutableStateOf<Set<String>?>(null)
                }
                LaunchedEffect(targetEntry.uuid) {
                    siblingNames = withContext(Dispatchers.IO) {
                        val repo = EntriesRepository(KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS))
                        entries.asSequence()
                            .filter { it.uuid != targetEntry.uuid && it.albumUuid == targetEntry.albumUuid }
                            .mapNotNull { repo.getMeta(it.blobFile)?.originalFilename?.trim()?.lowercase() }
                            .toSet()
                    }
                }
                if (hasResolution) {
                    val initialName = cache[targetEntry.uuid] ?: ""
                    EntryRenameDialog(
                        initialName = initialName,
                        duplicateChecker = { candidate ->
                            if (siblingNames?.contains(candidate.trim().lowercase()) == true) dupMsg else null
                        },
                        onDismiss = {
                            GallerySession.lastGridActionStage = GridActionStage.NONE
                        },
                        onConfirm = { newName ->
                            handleRenameRequest(targetEntry.uuid, newName)
                            clearGridSelection()
                        }
                    )
                }
            }
        }

        sortDialogIsAlbums?.let { isAlbums ->
            SortOptionsDialog(
                isAlbums = isAlbums,
                inGroup = albumSectionKey != "ROOT",

                currentAlbumOrder = AppSettings.albumSortOrderFor(albumSectionKey),
                currentEntryOrder = AppSettings.entrySortOrderFor(entrySectionKey),
                onAlbumOrderSelected = { newOrder ->
                    AppSettings.setAlbumSortOrderFor(albumSectionKey, newOrder)
                    sortDialogIsAlbums = null
                },
                onEntryOrderSelected = { newOrder ->
                    AppSettings.setEntrySortOrderFor(entrySectionKey, newOrder)
                    sortDialogIsAlbums = null
                },
                onDismiss = { sortDialogIsAlbums = null }
            )
        }

        InWindowDropdown(
            expanded = topMenuExpanded,
            onDismissRequest = { topMenuExpanded = false },
            anchorBoundsInWindow = topMenuAnchorBounds,
            seedRootOffset = topMenuRootOffset,
            onRootOffset = {
                topMenuRootOffset = it
                GallerySession.lastTopMenuRootOffset = it
            }
        ) {
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.menu_item_edit)) },
                leadingIcon = {
                    ThemedIcon(
                        fallback = Icons.Default.Edit,
                        medievalDrawable = MedievalIcons.Edit,
                        contentDescription = null
                    )
                },
                onClick = {
                    topMenuExpanded = false

                    val isEntryContext = selectedTab == GalleryTab.ALL ||
                        openedAlbumUuid != null
                    if (isEntryContext) {
                        GallerySession.inEntrySelectionMode = true
                        GallerySession.selectionRevision++
                    } else {
                        GallerySession.inAlbumSelectionMode = true
                        GallerySession.albumSelectionRevision++
                    }
                }
            )
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.menu_item_select_all)) },
                leadingIcon = {
                    ThemedIcon(
                        fallback = Icons.Default.Done,
                        medievalDrawable = MedievalIcons.SelectAll,
                        contentDescription = null
                    )
                },
                onClick = {
                    topMenuExpanded = false
                    val q = if (searchActive) searchQuery.trim() else ""
                    if (selectedTab == GalleryTab.ALL || openedAlbumUuid != null) {
                        val baseEntries = if (openedAlbumUuid != null) {
                            viewerEntries
                        } else sortedEntries
                        val cache = GallerySession.entryFilenameCache
                        val filtered = if (q.isBlank()) baseEntries else {
                            baseEntries.filter {
                                cache[it.uuid]?.contains(q, ignoreCase = true) == true
                            }
                        }
                        GallerySession.lastSelectedEntryUuids =
                            filtered.map { it.uuid }.toSet()
                        GallerySession.inEntrySelectionMode = true
                        GallerySession.selectionRevision++
                    } else {
                        val baseAlbums = openedGroup?.albums ?: topLevelAlbums
                        val filtered = if (q.isBlank()) baseAlbums else {
                            baseAlbums.filter {
                                it.meta.name.contains(q, ignoreCase = true)
                            }
                        }
                        GallerySession.lastSelectedAlbumUuids =
                            filtered.map { it.meta.uuid }.toSet()
                        GallerySession.inAlbumSelectionMode = true
                        GallerySession.albumSelectionRevision++
                    }
                }
            )
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.menu_item_sort)) },
                leadingIcon = {
                    ThemedIcon(
                        fallback = Icons.Default.KeyboardArrowDown,
                        medievalDrawable = MedievalIcons.Sort,
                        contentDescription = null
                    )
                },
                onClick = {
                    topMenuExpanded = false
                    val isEntryContext = selectedTab == GalleryTab.ALL ||
                        openedAlbumUuid != null
                    sortDialogIsAlbums = !isEntryContext
                }
            )
        }

        InWindowDropdown(
            expanded = burgerExpanded,
            onDismissRequest = { burgerExpanded = false },
            anchorBoundsInWindow = burgerAnchorBounds,
            seedRootOffset = burgerRootOffset,
            onRootOffset = {
                burgerRootOffset = it
                GallerySession.lastBurgerRootOffset = it
            }
        ) {

            InWindowDropdownItem(
                text = { Text(stringResource(R.string.menu_item_lock)) },
                leadingIcon = {
                    ThemedIcon(
                        vector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                onClick = {
                    burgerExpanded = false
                    GallerySession.lastBurgerMenuExpanded = false
                    lockVaultNow()
                }
            )

            if (trashEnabled) {
                InWindowDropdownItem(
                    text = { Text(stringResource(R.string.menu_item_trash)) },
                    leadingIcon = {
                        ThemedIcon(
                            fallback = Icons.Default.Delete,
                            medievalDrawable = MedievalIcons.Trash,
                            contentDescription = null
                        )
                    },
                    onClick = {

                        burgerExpanded = false
                        GallerySession.lastBurgerMenuExpanded = false
                        onOpenTrash()
                    }
                )
            }
            InWindowDropdownItem(
                text = { Text(stringResource(R.string.menu_item_settings)) },
                leadingIcon = {
                    ThemedIcon(
                        fallback = Icons.Default.Settings,
                        medievalDrawable = MedievalIcons.Settings,
                        contentDescription = null
                    )
                },
                onClick = {
                    burgerExpanded = false
                    GallerySession.lastBurgerMenuExpanded = false
                    onOpenSettings()
                }
            )
        }
    }
}

private fun lockVaultNow() {
    SessionState.unlocked.value = false
    dev.encgallery.crypto.VaultDataKey.wipe()
    dev.encgallery.gallery.ShareMemoryStore.clear()
}

@Composable
private fun TestsAndLogsContent() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var logRefreshTick by remember { mutableIntStateOf(0) }
    val logSize = remember(logRefreshTick) { EncLog.currentSize() }
    val tail = remember(logRefreshTick) { EncLog.tail(n = 15) }

    Text(
        text = stringResource(R.string.native_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.native_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val passed = runSecureZeroSelfTest()
            val msgRes = if (passed) R.string.toast_native_pass else R.string.toast_native_fail
            Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
        }
    ) {
        Text(stringResource(R.string.btn_run_native_test))
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.aesgcm_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.aesgcm_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))

    var aesgcmRunning by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !aesgcmRunning,
        onClick = {
            aesgcmRunning = true
            scope.launch {
                val passed = withContext(Dispatchers.IO) {
                    runAesGcmSelfTest(context.applicationContext)
                }
                aesgcmRunning = false
                val msgRes = if (passed) R.string.toast_aesgcm_pass else R.string.toast_aesgcm_fail
                Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
            }
        }
    ) {
        Text(
            if (aesgcmRunning) stringResource(R.string.aesgcm_running)
            else stringResource(R.string.btn_run_aesgcm_test)
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.securebytes_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.securebytes_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val passed = runSecureBytesSelfTest()
            val msgRes = if (passed) R.string.toast_securebytes_pass else R.string.toast_securebytes_fail
            Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
        }
    ) {
        Text(stringResource(R.string.btn_run_securebytes_test))
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.argon2_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.argon2_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))

    var argon2Running by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !argon2Running,
        onClick = {
            argon2Running = true
            scope.launch {
                val passed = withContext(Dispatchers.IO) { runArgon2idSelfTest() }
                argon2Running = false
                val msgRes = if (passed) R.string.toast_argon2_pass else R.string.toast_argon2_fail
                Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
            }
        }
    ) {
        Text(
            if (argon2Running) stringResource(R.string.argon2_running)
            else stringResource(R.string.btn_run_argon2_test)
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.kekwrap_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.kekwrap_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))

    var kekwrapRunning by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !kekwrapRunning,
        onClick = {
            kekwrapRunning = true
            scope.launch {
                val passed = withContext(Dispatchers.IO) { runKekWrapSelfTest() }
                kekwrapRunning = false
                val msgRes = if (passed) R.string.toast_kekwrap_pass else R.string.toast_kekwrap_fail
                Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
            }
        }
    ) {
        Text(
            if (kekwrapRunning) stringResource(R.string.kekwrap_running)
            else stringResource(R.string.btn_run_kekwrap_test)
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.dekvault_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.dekvault_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))

    var dekvaultRunning by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !dekvaultRunning,
        onClick = {
            dekvaultRunning = true
            scope.launch {
                val passed = withContext(Dispatchers.IO) {
                    runDekVaultSelfTest(context.applicationContext)
                }
                dekvaultRunning = false
                val msgRes = if (passed) R.string.toast_dekvault_pass else R.string.toast_dekvault_fail
                Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
            }
        }
    ) {
        Text(
            if (dekvaultRunning) stringResource(R.string.dekvault_running)
            else stringResource(R.string.btn_run_dekvault_test)
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.pwchange_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.pwchange_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))

    var pwchangeRunning by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !pwchangeRunning,
        onClick = {
            pwchangeRunning = true
            scope.launch {
                val passed = withContext(Dispatchers.IO) {
                    runPasswordChangeSelfTest(context.applicationContext)
                }
                pwchangeRunning = false
                val msgRes = if (passed) R.string.toast_pwchange_pass else R.string.toast_pwchange_fail
                Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
            }
        }
    ) {
        Text(
            if (pwchangeRunning) stringResource(R.string.pwchange_running)
            else stringResource(R.string.btn_run_pwchange_test)
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.blob_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.blob_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))

    var blobRunning by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !blobRunning,
        onClick = {
            blobRunning = true
            scope.launch {
                val passed = withContext(Dispatchers.IO) {
                    runBlobSelfTest(context.applicationContext)
                }
                blobRunning = false
                val msgRes = if (passed) R.string.toast_blob_pass else R.string.toast_blob_fail
                Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
            }
        }
    ) {
        Text(
            if (blobRunning) stringResource(R.string.blob_running)
            else stringResource(R.string.btn_run_blob_test)
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.albums_test_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.albums_test_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))

    var albumsRunning by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !albumsRunning,
        onClick = {
            albumsRunning = true
            scope.launch {
                val passed = withContext(Dispatchers.IO) {
                    runAlbumsSelfTest(context.applicationContext)
                }
                albumsRunning = false
                val msgRes = if (passed) R.string.toast_albums_pass else R.string.toast_albums_fail
                Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
            }
        }
    ) {
        Text(
            if (albumsRunning) stringResource(R.string.albums_running)
            else stringResource(R.string.btn_run_albums_test)
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.trash_test_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.trash_test_subtitle),
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(Modifier.height(8.dp))

    var trashRunning by remember { mutableStateOf(false) }
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !trashRunning,
        onClick = {
            trashRunning = true
            scope.launch {
                val passed = withContext(Dispatchers.IO) {
                    runTrashSelfTest(context.applicationContext)
                }
                trashRunning = false
                val msgRes = if (passed) R.string.toast_trash_pass else R.string.toast_trash_fail
                Toast.makeText(context, msgRes, Toast.LENGTH_LONG).show()
            }
        }
    ) {
        Text(
            if (trashRunning) stringResource(R.string.trash_running)
            else stringResource(R.string.btn_run_trash_test)
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.log_status_header),
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.log_size_bytes, logSize),
        style = MaterialTheme.typography.bodySmall
    )

    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = {
                val full = EncLog.readAll()
                if (full.isEmpty()) {
                    Toast.makeText(context, R.string.toast_log_empty, Toast.LENGTH_SHORT).show()
                } else {
                    clipboard.setText(AnnotatedString(full))
                    Toast.makeText(context, R.string.toast_log_copied, Toast.LENGTH_SHORT).show()
                    EncLog.d("MainActivity", "log copied to clipboard (${full.length} chars)")
                    logRefreshTick++
                }
            }
        ) {
            Text(stringResource(R.string.btn_copy_log))
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = {
                val file = EncLog.activeLogFile()
                if (file == null || file.length() == 0L) {
                    Toast.makeText(context, R.string.toast_log_empty, Toast.LENGTH_SHORT).show()
                } else {
                    val authority = "${context.packageName}.fileprovider"
                    val uri: Uri = FileProvider.getUriForFile(context, authority, file)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "EncGallery log")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(send, context.getString(R.string.share_log_chooser))
                    )
                    EncLog.d("MainActivity", "share file intent launched (${file.length()} bytes)")
                    logRefreshTick++
                }
            }
        ) {
            Text(stringResource(R.string.btn_share_log))
        }
    }

    Spacer(Modifier.height(8.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            EncLog.clear()
            logRefreshTick++
            Toast.makeText(context, R.string.toast_log_cleared, Toast.LENGTH_SHORT).show()
        }
    ) {
        Text(stringResource(R.string.btn_clear_log))
    }

    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.log_tail_header, tail.size),
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(4.dp))

    SelectionContainer {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (tail.isEmpty()) {
                Text(
                    text = stringResource(R.string.log_empty),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                tail.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

private fun runSecureZeroSelfTest(): Boolean {
    val tag = "NativeSelfTest"
    return try {
        val buf = ByteArray(64) { 0xAA.toByte() }
        EncLog.i(tag, "before secureZero: ${buf.toHexPreview()}")

        NativeCrypto.secureZero(buf)

        EncLog.i(tag, "after  secureZero: ${buf.toHexPreview()}")

        val firstBad = buf.indexOfFirst { it != 0.toByte() }
        if (firstBad < 0) {
            EncLog.i(tag, "PASS — all 64 bytes are 0x00")
            true
        } else {
            EncLog.e(
                tag,
                "FAIL — byte $firstBad is 0x${"%02X".format(buf[firstBad].toInt() and 0xFF)}, expected 0x00"
            )
            false
        }
    } catch (t: Throwable) {

        EncLog.e("NativeSelfTest", "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runAesGcmSelfTest(context: Context): Boolean {
    val tag = "AesGcmTest"
    return try {
        KeystoreAesGcm.runSelfTest(context) { line -> EncLog.i(tag, line) }
            .also { passed ->
                if (passed) EncLog.i(tag, "PASS — all 7 properties verified")
                else EncLog.e(tag, "FAIL — see lines above for which property")
            }
    } catch (t: Throwable) {
        EncLog.e(tag, "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runSecureBytesSelfTest(): Boolean {
    val tag = "SecureBytesTest"
    return try {
        SecureBytes.runSelfTest { line -> EncLog.i(tag, line) }
            .also { passed ->
                if (passed) EncLog.i(tag, "PASS — all 7 properties verified")
                else EncLog.e(tag, "FAIL — see lines above for which property")
            }
    } catch (t: Throwable) {
        EncLog.e(tag, "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runArgon2idSelfTest(): Boolean {
    val tag = "Argon2SelfTest"
    return try {
        val password = "password".toByteArray(Charsets.UTF_8)
        val salt1 = "somesalt12345678".toByteArray(Charsets.UTF_8)
        val salt2 = "differentSalt!!!".toByteArray(Charsets.UTF_8)

        val memoryKib = 4 * 1024
        val iterations = 2
        val parallelism = 1
        val hashLen = 32

        EncLog.i(
            tag,
            "params: memoryKib=$memoryKib iter=$iterations parallel=$parallelism hashLen=$hashLen"
        )

        val t0 = System.currentTimeMillis()
        val hash1 = NativeCrypto.argon2idHashRaw(
            password, salt1, memoryKib, iterations, parallelism, hashLen
        )
        val t1 = System.currentTimeMillis()
        if (hash1 == null) {
            EncLog.e(tag, "FAIL — first call returned null")
            return false
        }
        EncLog.i(tag, "hash#1 (salt1) [${t1 - t0} ms]: ${hash1.toHexPreview()}")

        val hash2 = NativeCrypto.argon2idHashRaw(
            password, salt1, memoryKib, iterations, parallelism, hashLen
        )
        val t2 = System.currentTimeMillis()
        if (hash2 == null) {
            EncLog.e(tag, "FAIL — second call returned null")
            return false
        }
        EncLog.i(tag, "hash#2 (salt1) [${t2 - t1} ms]: ${hash2.toHexPreview()}")

        val hash3 = NativeCrypto.argon2idHashRaw(
            password, salt2, memoryKib, iterations, parallelism, hashLen
        )
        val t3 = System.currentTimeMillis()
        if (hash3 == null) {
            EncLog.e(tag, "FAIL — third call returned null")
            return false
        }
        EncLog.i(tag, "hash#3 (salt2) [${t3 - t2} ms]: ${hash3.toHexPreview()}")

        val deterministic = hash1.contentEquals(hash2)
        val saltSensitive = !hash1.contentEquals(hash3)

        if (!deterministic) {
            EncLog.e(tag, "FAIL — determinism: hash#1 != hash#2")
        }
        if (!saltSensitive) {
            EncLog.e(tag, "FAIL — salt sensitivity: hash#1 == hash#3 (salt was ignored?)")
        }

        NativeCrypto.secureZero(hash1)
        NativeCrypto.secureZero(hash2)
        NativeCrypto.secureZero(hash3)
        NativeCrypto.secureZero(password)

        if (deterministic && saltSensitive) {
            EncLog.i(tag, "PASS — determinism + salt sensitivity confirmed")
            true
        } else {
            false
        }
    } catch (t: Throwable) {
        EncLog.e("Argon2SelfTest", "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runKekWrapSelfTest(): Boolean {
    val tag = "KekWrapTest"
    return try {
        KekWrap.runSelfTest { line -> EncLog.i(tag, line) }
            .also { passed ->
                if (passed) EncLog.i(tag, "PASS — all 11 properties verified")
                else EncLog.e(tag, "FAIL — see lines above for which property")
            }
    } catch (t: Throwable) {
        EncLog.e(tag, "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runDekVaultSelfTest(context: Context): Boolean {
    val tag = "DekVaultTest"
    return try {
        DekVault.runSelfTest(context) { line -> EncLog.i(tag, line) }
            .also { passed ->
                if (passed) EncLog.i(tag, "PASS — all 5 properties verified (create/load, wrong-pw, v1→v2 migration, change-password)")
                else EncLog.e(tag, "FAIL — see lines above for which property")
            }
    } catch (t: Throwable) {
        EncLog.e(tag, "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runPasswordChangeSelfTest(context: Context): Boolean {
    val tag = "PwChangeTest"
    return try {
        PasswordChange.runSelfTest(context) { line -> EncLog.i(tag, line) }
            .also { passed ->
                if (passed) EncLog.i(tag, "PASS — all 4 properties verified (rollback, roll-forward, stray-discard, full apply)")
                else EncLog.e(tag, "FAIL — see lines above for which property")
            }
    } catch (t: Throwable) {
        EncLog.e(tag, "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runBlobSelfTest(context: Context): Boolean {
    val tag = "BlobSelfTest"
    return try {
        EncryptedFileBlob.runSelfTest(context, context.cacheDir) { line -> EncLog.i(tag, line) }
            .also { passed ->
                if (passed) EncLog.i(tag, "PASS — all 6 properties verified")
                else EncLog.e(tag, "FAIL — see lines above for which property")
            }
    } catch (t: Throwable) {
        EncLog.e(tag, "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runAlbumsSelfTest(context: Context): Boolean {
    val tag = "AlbumsSelfTest"
    return try {
        val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
        val repo = AlbumsRepository(keystore)
        val ctx = context.applicationContext

        val testName = "Тестовый альбом 🐺"
        val createdMeta = repo.create(ctx, testName)
        EncLog.i(
            tag,
            "(1) created uuid=${createdMeta.uuid} name-len=${createdMeta.name.length}"
        )

        val foundAfterCreate = repo.listAlbums(ctx).any { it.uuid == createdMeta.uuid }
        EncLog.i(tag, "(2) found in listAlbums after create: $foundAfterCreate")
        if (!foundAfterCreate) {
            repo.deletePermanently(ctx, createdMeta.uuid)
            EncLog.e(tag, "FAIL — created album not present in listAlbums")
            return false
        }

        val newName = "Альбом 🦊 переименован"
        val renamed = repo.rename(ctx, createdMeta.uuid, newName)
        val renameOk = renamed != null && renamed.name == newName &&
                renamed.modifiedAt >= createdMeta.modifiedAt
        EncLog.i(
            tag,
            "(3) rename round-trip: $renameOk (new-name-len=${renamed?.name?.length})"
        )
        if (!renameOk) {
            repo.deletePermanently(ctx, createdMeta.uuid)
            EncLog.e(tag, "FAIL — rename did not persist")
            return false
        }

        val fakeEntryUuid = "11111111-2222-3333-4444-555555555555"
        val fakeCrop = NormalizedRect(0.1f, 0.2f, 0.5f, 0.5f)
        val withCover = repo.setCover(ctx, createdMeta.uuid, fakeEntryUuid, fakeCrop)
        val coverOk = withCover != null &&
                withCover.coverEntryUuid == fakeEntryUuid &&
                withCover.coverCropRect == fakeCrop
        EncLog.i(tag, "(4) cover round-trip: $coverOk")
        if (!coverOk) {
            repo.deletePermanently(ctx, createdMeta.uuid)
            EncLog.e(tag, "FAIL — cover did not persist")
            return false
        }

        val rehydrated = repo.getAlbum(ctx, createdMeta.uuid)
        val rehydrateOk = rehydrated != null &&
                rehydrated.name == newName &&
                rehydrated.coverEntryUuid == fakeEntryUuid &&
                rehydrated.coverCropRect == fakeCrop
        EncLog.i(tag, "(5) rehydrate-from-disk round-trip: $rehydrateOk")
        if (!rehydrateOk) {
            repo.deletePermanently(ctx, createdMeta.uuid)
            EncLog.e(tag, "FAIL — meta did not survive disk round-trip")
            return false
        }

        val deleteOk = repo.deletePermanently(ctx, createdMeta.uuid)
        val goneAfterDelete = repo.listAlbums(ctx).none { it.uuid == createdMeta.uuid }
        EncLog.i(tag, "(6) delete + gone-from-list: $deleteOk && $goneAfterDelete")
        if (!deleteOk || !goneAfterDelete) {
            EncLog.e(tag, "FAIL — delete did not remove the album")
            return false
        }

        EncLog.i(tag, "PASS — all 6 properties verified")
        true
    } catch (t: Throwable) {
        EncLog.e(tag, "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    }
}

private fun runTrashSelfTest(context: Context): Boolean {
    val tag = "TrashSelfTest"
    val keystore = KeystoreAesGcm(KeystoreAesGcm.PRODUCTION_ALIAS)
    val albumsRepo = AlbumsRepository(keystore)
    val trashRepo = TrashRepository(keystore)
    val ctx = context.applicationContext

    var albumUuid: String? = null
    val entryUuid = java.util.UUID.randomUUID().toString()
    return try {

        val albumName = "Trash test 🗑️"
        val createdAlbum = albumsRepo.create(ctx, albumName)
        albumUuid = createdAlbum.uuid

        val albumDir = VaultPaths.albumDir(ctx, createdAlbum.uuid)
        val blobFile = java.io.File(albumDir, "$entryUuid${VaultPaths.BLOB_SUFFIX}")
        val thumbFile = java.io.File(albumDir, "$entryUuid${VaultPaths.THUMB_SUFFIX}")

        blobFile.writeBytes("synthetic blob payload".toByteArray(Charsets.UTF_8))
        thumbFile.writeBytes("synthetic thumb payload".toByteArray(Charsets.UTF_8))
        val originalMtime = blobFile.lastModified()

        val syntheticEntry = dev.encgallery.gallery.VaultEntry(
            uuid = entryUuid,
            albumUuid = createdAlbum.uuid,
            blobFile = blobFile,
            thumbFile = thumbFile,
            blobSizeBytes = blobFile.length(),
            mtimeMillis = originalMtime
        )
        EncLog.i(tag, "(1) created album=${createdAlbum.uuid} + synthetic entry=$entryUuid")

        val moveOk = trashRepo.moveToTrash(ctx, syntheticEntry, originalAlbumNameHint = albumName)
        val blobInTrash = VaultPaths.trashBlobFile(ctx, entryUuid).exists()
        val thumbInTrash = VaultPaths.trashThumbFile(ctx, entryUuid).exists()
        val metaInTrash = VaultPaths.trashMetaFile(ctx, entryUuid).exists()
        val originalsGone = !blobFile.exists() && !thumbFile.exists()
        val moveCorrect = moveOk && blobInTrash && thumbInTrash && metaInTrash && originalsGone
        EncLog.i(
            tag,
            "(2) moveToTrash: ok=$moveOk blob=$blobInTrash thumb=$thumbInTrash " +
                "meta=$metaInTrash originalsGone=$originalsGone"
        )
        if (!moveCorrect) {
            EncLog.e(tag, "FAIL — moveToTrash did not produce expected file layout")
            return false
        }

        val listed = trashRepo.listTrash(ctx)
        val found = listed.firstOrNull { it.uuid == entryUuid }
        val listOk = found != null &&
            found.meta.originalAlbumUuid == createdAlbum.uuid &&
            found.meta.originalAlbumNameHint == albumName &&
            found.meta.originalMtime == originalMtime
        EncLog.i(
            tag,
            "(3) listTrash: found=${found != null} " +
                "albumUuid=${found?.meta?.originalAlbumUuid == createdAlbum.uuid} " +
                "nameHint=${found?.meta?.originalAlbumNameHint == albumName} " +
                "mtime=${found?.meta?.originalMtime == originalMtime}"
        )
        if (!listOk || found == null) {
            EncLog.e(tag, "FAIL — listTrash did not surface the moved entry correctly")
            return false
        }

        val restoreLanded = trashRepo.restore(ctx, found)
        val backInAlbum = blobFile.exists() && thumbFile.exists()
        val metaCleanedUp = !VaultPaths.trashMetaFile(ctx, entryUuid).exists()
        val restoreOk = restoreLanded == createdAlbum.uuid && backInAlbum && metaCleanedUp
        EncLog.i(
            tag,
            "(4) restore: landedAt=$restoreLanded back=$backInAlbum metaCleaned=$metaCleanedUp"
        )
        if (!restoreOk) {
            EncLog.e(tag, "FAIL — restore did not put files back / clean meta")
            return false
        }

        val refreshedEntry = syntheticEntry.copy(mtimeMillis = blobFile.lastModified())
        if (!trashRepo.moveToTrash(ctx, refreshedEntry, originalAlbumNameHint = albumName)) {
            EncLog.e(tag, "FAIL — second moveToTrash failed (setup for purge test)")
            return false
        }
        val secondListed = trashRepo.listTrash(ctx).first { it.uuid == entryUuid }
        val purgeOk = trashRepo.purgePermanently(ctx, secondListed)
        val purgeGone = !VaultPaths.trashBlobFile(ctx, entryUuid).exists() &&
            !VaultPaths.trashThumbFile(ctx, entryUuid).exists() &&
            !VaultPaths.trashMetaFile(ctx, entryUuid).exists()
        EncLog.i(tag, "(5) purgePermanently: ok=$purgeOk allGone=$purgeGone")
        if (!purgeOk || !purgeGone) {
            EncLog.e(tag, "FAIL — purgePermanently left files behind")
            return false
        }

        blobFile.writeBytes("third-pass payload".toByteArray(Charsets.UTF_8))
        thumbFile.writeBytes("third-pass thumb".toByteArray(Charsets.UTF_8))
        val thirdEntry = syntheticEntry.copy(mtimeMillis = blobFile.lastModified())
        if (!trashRepo.moveToTrash(ctx, thirdEntry, originalAlbumNameHint = albumName)) {
            EncLog.e(tag, "FAIL — third moveToTrash failed (setup for auto-purge test)")
            return false
        }
        val swept = trashRepo.purgeOlderThan(ctx, retentionMillis = 0L)
        val sweepGone = !VaultPaths.trashBlobFile(ctx, entryUuid).exists()
        EncLog.i(tag, "(6) purgeOlderThan(0): swept=$swept gone=$sweepGone")
        if (swept < 1 || !sweepGone) {
            EncLog.e(tag, "FAIL — purgeOlderThan(0) did not sweep the trashed entry")
            return false
        }

        EncLog.i(tag, "PASS — all 6 properties verified")
        true
    } catch (t: Throwable) {
        EncLog.e(tag, "exception: ${t.javaClass.simpleName}: ${t.message}")
        false
    } finally {

        albumUuid?.let { uuid ->
            try {
                albumsRepo.deletePermanently(ctx, uuid)
            } catch (_: Throwable) {   }
        }

        VaultPaths.trashBlobFile(ctx, entryUuid).delete()
        VaultPaths.trashThumbFile(ctx, entryUuid).delete()
        VaultPaths.trashMetaFile(ctx, entryUuid).delete()
    }
}

private fun ByteArray.toHexPreview(maxBytes: Int = 16): String {
    val n = minOf(size, maxBytes)
    val sb = StringBuilder(n * 3 + 8)
    for (i in 0 until n) {
        if (i > 0) sb.append(' ')
        sb.append("%02X".format(this[i].toInt() and 0xFF))
    }
    if (size > maxBytes) sb.append(" … (").append(size).append(" total)")
    return sb.toString()
}

@Preview(showBackground = true)
@Composable
private fun SkeletonPreview() {
    EncGalleryTheme {
        SkeletonContent()
    }
}

@Composable
private fun SortOptionsDialog(
    isAlbums: Boolean,
    inGroup: Boolean,
    currentAlbumOrder: AlbumSortOrder,
    currentEntryOrder: EntrySortOrder,
    onAlbumOrderSelected: (AlbumSortOrder) -> Unit,
    onEntryOrderSelected: (EntrySortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val title = stringResource(
        when {
            !isAlbums -> R.string.sort_dialog_title_entries
            inGroup -> R.string.sort_dialog_title_albums
            else -> R.string.sort_dialog_title_root
        }
    )
    InWindowDialog(
        onDismiss = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isAlbums) {
                    AlbumSortOrder.values().forEach { value ->
                        SortOptionRow(
                            label = stringResource(albumSortOptionLabel(value)),
                            selected = value == currentAlbumOrder,
                            onClick = { onAlbumOrderSelected(value) }
                        )
                    }
                } else {
                    EntrySortOrder.values().forEach { value ->
                        SortOptionRow(
                            label = stringResource(entrySortOptionLabel(value)),
                            selected = value == currentEntryOrder,
                            onClick = { onEntryOrderSelected(value) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sort_dialog_btn_close))
            }
        }
    )
}

@Composable
private fun SortOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HostSelectionTopBar(
    count: Int,
    onCancel: () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                ThemedIcon(
                    fallback = Icons.Default.Close,
                    medievalDrawable = MedievalIcons.Close,
                    contentDescription = stringResource(R.string.selection_cancel)
                )
            }
            Text(
                text = stringResource(R.string.selection_count, count),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun albumSortOptionLabel(order: AlbumSortOrder): Int = when (order) {
    AlbumSortOrder.DATE_DESC -> R.string.sort_option_date_desc
    AlbumSortOrder.DATE_ASC -> R.string.sort_option_date_asc
    AlbumSortOrder.NAME_AZ -> R.string.sort_option_name_az
    AlbumSortOrder.NAME_ZA -> R.string.sort_option_name_za
}

private fun entrySortOptionLabel(order: EntrySortOrder): Int = when (order) {
    EntrySortOrder.DATE_DESC -> R.string.sort_option_date_desc
    EntrySortOrder.DATE_ASC -> R.string.sort_option_date_asc
    EntrySortOrder.NAME_AZ -> R.string.sort_option_name_az
    EntrySortOrder.NAME_ZA -> R.string.sort_option_name_za
}
