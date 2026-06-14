package dev.encgallery.gallery

import android.graphics.Bitmap
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

object GallerySession {

    var lastViewingIndex: Int? = null

    var lastGridFirstVisibleItem: Int = 0

    var lastGridFirstVisibleOffset: Int = 0

    var lastMosaicFirstVisibleItem: Int = 0
    var lastMosaicFirstVisibleOffset: Int = 0

    val albumDetailGridScroll: MutableMap<String, Pair<Int, Int>> = mutableMapOf()
    val albumDetailMosaicScroll: MutableMap<String, Pair<Int, Int>> = mutableMapOf()

    val groupAlbumsGridScroll: MutableMap<String, Pair<Int, Int>> = mutableMapOf()

    var albumsTabReturnAlbumUuid: String? = null

    var lastCoverPickerFirstVisibleItem: Int = 0
    var lastCoverPickerFirstVisibleOffset: Int = 0

    val mosaicAspectRatios: MutableMap<String, Float> =
        java.util.concurrent.ConcurrentHashMap()

    val entryIsVideo: MutableMap<String, Boolean> = mutableMapOf()

    var lastDevPanelScrollPx: Int = 0

    var lastUiVisible: Boolean = true

    var lastViewedEntryUuid: String? = null

    var lastViewerScale: Float = 1f

    var lastViewerOffsetX: Float = 0f

    var lastViewerOffsetY: Float = 0f

    var lastViewerVideoUuid: String? = null

    var lastViewerVideoPositionMs: Int = 0

    var lastViewerVideoWasPlaying: Boolean = true

    val pendingScrollToViewedUuid: androidx.compose.runtime.MutableState<String?> =
        androidx.compose.runtime.mutableStateOf(null)

    var lastViewerBitmap: Bitmap? = null

    var lastSelectedTab: GalleryTab = GalleryTab.ALL

    var lastOpenedAlbumUuid: String? = null

    var lastAlbumDialogType: AlbumDialogType = AlbumDialogType.NONE

    var lastAlbumDialogUuid: String? = null

    var lastViewerInfoUuid: String? = null

    var lastViewerDeleteUuid: String? = null

    private val _lastSelectedEntryUuids =
        androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet())
    var lastSelectedEntryUuids: Set<String>
        get() = _lastSelectedEntryUuids.value
        set(value) { _lastSelectedEntryUuids.value = value }

    private val _selectionRevision = mutableIntStateOf(0)
    var selectionRevision: Int
        get() = _selectionRevision.intValue
        set(value) { _selectionRevision.intValue = value }

    var lastEntryPickerOp: EntryPickerOp = EntryPickerOp.NONE

    var lastEntryPickerSelection: Set<String> = emptySet()

    private val _lastSelectedAlbumUuids =
        androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet())
    var lastSelectedAlbumUuids: Set<String>
        get() = _lastSelectedAlbumUuids.value
        set(value) { _lastSelectedAlbumUuids.value = value }

    private val _albumSelectionRevision = mutableIntStateOf(0)
    var albumSelectionRevision: Int
        get() = _albumSelectionRevision.intValue
        set(value) { _albumSelectionRevision.intValue = value }

    var lastAlbumSelectionStage: AlbumSelectionStage = AlbumSelectionStage.NONE

    var lastOpenedGroupUuid: String? = null

    var lastAlbumPickerOp: AlbumPickerOp = AlbumPickerOp.NONE

    var lastAlbumPickerSelection: Set<String> = emptySet()

    private val _lastGridActionStage = androidx.compose.runtime.mutableStateOf(GridActionStage.NONE)
    var lastGridActionStage: GridActionStage
        get() = _lastGridActionStage.value
        set(value) { _lastGridActionStage.value = value }

    var lastEntries: List<VaultEntry> = emptyList()

    var lastAlbumSummaries: List<AlbumSummary> = emptyList()

    var lastFabDialog: FabDialogState = FabDialogState.NONE

    var lastSearchActive: Boolean = false
    var lastSearchQuery: String = ""

    val entryFilenameCache: MutableMap<String, String?> = mutableMapOf()

    private val _entryFilenameCacheRevision = mutableIntStateOf(0)
    var entryFilenameCacheRevision: Int
        get() = _entryFilenameCacheRevision.intValue
        set(value) { _entryFilenameCacheRevision.intValue = value }

    private val _inAlbumSelectionMode = androidx.compose.runtime.mutableStateOf(false)
    var inAlbumSelectionMode: Boolean
        get() = _inAlbumSelectionMode.value
        set(value) { _inAlbumSelectionMode.value = value }

    private val _inEntrySelectionMode = androidx.compose.runtime.mutableStateOf(false)
    var inEntrySelectionMode: Boolean
        get() = _inEntrySelectionMode.value
        set(value) { _inEntrySelectionMode.value = value }

    var lastAlbumGroups: List<AlbumGroupSummary> = emptyList()

    var lastCoverPickerAlbumUuid: String? = null

    var lastTopMenuExpanded: Boolean = false

    var lastTopMenuAnchor: Rect? = null

    var lastTopMenuRootOffset: Offset? = null

    var lastBurgerMenuExpanded: Boolean = false

    var lastBurgerAnchor: Rect? = null

    var lastBurgerRootOffset: Offset? = null

    var lastViewerMenuAnchor: Rect? = null

    var lastViewerMenuRootOffset: Offset? = null

    var lastFabAnchor: Rect? = null

    var lastEntryOptionsAnchor: Rect? = null

    var lastAlbumOptionsAnchor: Rect? = null

    var lastSortDialogIsAlbums: Boolean? = null

    var lastSettingsOpen: Boolean = false

    var lastTrashOpen: Boolean = false

    var lastTrashViewingUuid: String? = null

    var lastViewerMenuExpanded: Boolean = false

    var lastGroupActionUuid: String? = null

    var lastGroupActionStage: GroupActionStage = GroupActionStage.NONE

    var lastTrashSelectedUuids: Set<String> = emptySet()

    var lastTrashShowDeleteForeverDialog: Boolean = false

    var lastTrashShowEmptyTrashDialog: Boolean = false

    var lastTrashPurgeDialog: Boolean = false

    var lastEntryPickerInnerSelection: Set<String> = emptySet()

    var lastEntryPickerOpenedGroupUuid: String? = null

    var lastEntryPickerOpenedAlbumUuid: String? = null

    var lastEntryPickerShowCommitDialog: Boolean = false

    var lastAlbumsGridFirstVisibleItem: Int = 0

    var lastAlbumsGridFirstVisibleOffset: Int = 0

    var lastCoverCropEntryUuid: String? = null

    var lastWallpaperCropEntryUuid: String? = null

    var lastWallpaperCropTarget: WallpaperTarget? = null

    var lastWallpaperCropFrameUuid: String? = null

    var lastWallpaperCropHPx: Float = 0f

    var lastWallpaperCropCenterX: Float = 0f

    var lastWallpaperCropCenterY: Float = 0f

    var lastWallpaperCropBitmap: Bitmap? = null

    var lastViewerWallpaperUuid: String? = null

    var lastViewerRenameUuid: String? = null

    val infoProbeCache: MutableMap<String, EntryInfoProbe> = mutableMapOf()

    fun reset() {
        lastViewingIndex = null
        lastGridFirstVisibleItem = 0
        lastGridFirstVisibleOffset = 0
        lastMosaicFirstVisibleItem = 0
        lastMosaicFirstVisibleOffset = 0
        lastCoverPickerFirstVisibleItem = 0
        lastCoverPickerFirstVisibleOffset = 0
        mosaicAspectRatios.clear()

        MosaicAspectStore.resetLoaded()
        albumDetailGridScroll.clear()
        albumDetailMosaicScroll.clear()
        groupAlbumsGridScroll.clear()
        albumsTabReturnAlbumUuid = null
        entryIsVideo.clear()
        lastDevPanelScrollPx = 0
        lastUiVisible = true
        lastViewedEntryUuid = null
        lastViewerScale = 1f
        lastViewerOffsetX = 0f
        lastViewerOffsetY = 0f
        lastViewerVideoUuid = null
        lastViewerVideoPositionMs = 0
        lastViewerVideoWasPlaying = true
        pendingScrollToViewedUuid.value = null
        lastViewerBitmap = null
        lastSelectedTab = GalleryTab.ALL
        lastOpenedAlbumUuid = null
        lastAlbumDialogType = AlbumDialogType.NONE
        lastAlbumDialogUuid = null
        lastViewerInfoUuid = null
        lastViewerDeleteUuid = null
        lastSelectedEntryUuids = emptySet()
        selectionRevision = 0
        lastGridActionStage = GridActionStage.NONE
        lastEntryPickerOp = EntryPickerOp.NONE
        lastEntryPickerSelection = emptySet()
        lastSelectedAlbumUuids = emptySet()
        albumSelectionRevision = 0
        lastAlbumSelectionStage = AlbumSelectionStage.NONE
        lastOpenedGroupUuid = null
        lastAlbumPickerOp = AlbumPickerOp.NONE
        lastAlbumPickerSelection = emptySet()
        lastEntries = emptyList()
        lastAlbumSummaries = emptyList()
        lastAlbumGroups = emptyList()
        lastFabDialog = FabDialogState.NONE
        lastSearchActive = false
        lastSearchQuery = ""
        entryFilenameCache.clear()
        entryFilenameCacheRevision = 0
        inAlbumSelectionMode = false
        inEntrySelectionMode = false
        infoProbeCache.clear()
        lastCoverPickerAlbumUuid = null
        lastCoverCropEntryUuid = null
        lastWallpaperCropEntryUuid = null
        lastWallpaperCropTarget = null
        lastWallpaperCropFrameUuid = null
        lastWallpaperCropHPx = 0f
        lastWallpaperCropCenterX = 0f
        lastWallpaperCropCenterY = 0f
        lastWallpaperCropBitmap = null
        lastViewerWallpaperUuid = null
        lastViewerRenameUuid = null
        lastTopMenuExpanded = false
        lastTopMenuAnchor = null
        lastTopMenuRootOffset = null
        lastBurgerMenuExpanded = false
        lastBurgerAnchor = null
        lastBurgerRootOffset = null
        lastViewerMenuAnchor = null
        lastFabAnchor = null
        lastEntryOptionsAnchor = null
        lastAlbumOptionsAnchor = null
        lastSortDialogIsAlbums = null
        lastSettingsOpen = false
        lastTrashOpen = false
        lastTrashViewingUuid = null
        lastViewerMenuExpanded = false
        lastGroupActionUuid = null
        lastGroupActionStage = GroupActionStage.NONE
        lastTrashSelectedUuids = emptySet()
        lastTrashShowDeleteForeverDialog = false
        lastTrashShowEmptyTrashDialog = false
        lastTrashPurgeDialog = false
        lastEntryPickerInnerSelection = emptySet()
        lastEntryPickerOpenedGroupUuid = null
        lastEntryPickerOpenedAlbumUuid = null
        lastEntryPickerShowCommitDialog = false
        lastAlbumsGridFirstVisibleItem = 0
        lastAlbumsGridFirstVisibleOffset = 0
    }
}

data class EntryInfoProbe(
    val mime: String?,
    val dimensions: Pair<Int, Int>?,
    val originalFilename: String?
)

enum class GalleryTab { ALL, ALBUMS }

enum class GridActionStage {
    NONE, MENU, MOVE_PICK, COPY_PICK, DELETE_CONFIRM, OPTIONS,

    RENAME,

    WALLPAPER
}

enum class EntryPickerOp { NONE, MOVE, COPY }

enum class AlbumSelectionStage { NONE, OPTIONS, GROUP_NAME, DELETE_CONFIRM, RENAME_NAME }

enum class AlbumPickerOp { NONE, MOVE }

enum class AlbumDialogType { NONE, CREATE, ACTION_MENU, RENAME, DELETE }

enum class FabDialogState { NONE, CHOICE, GROUP_NAME }
