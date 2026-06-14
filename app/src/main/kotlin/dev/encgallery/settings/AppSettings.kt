package dev.encgallery.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import dev.encgallery.crypto.BruteForceConfig
import dev.encgallery.crypto.CryptoMode
import dev.encgallery.crypto.LockMethod
import dev.encgallery.featuresettings.AccentColor
import dev.encgallery.featuresettings.AlbumTileShape
import dev.encgallery.featuresettings.PhotoGridLayout
import dev.encgallery.featuresettings.ThemeVariant
import dev.encgallery.gallery.AlbumSortOrder
import dev.encgallery.gallery.EntrySortOrder
import dev.encgallery.logging.EncLog

object AppSettings {

    private const val PREFS_NAME = "encgallery_prefs"
    private const val KEY_LOGGING_ENABLED = "logging_enabled"
    private const val KEY_SCREENSHOTS_ALLOWED = "screenshots_allowed"
    private const val KEY_WIZARD_COMPLETE = "wizard_complete"
    private const val KEY_CRYPTO_MODE = "crypto_mode"
    private const val KEY_LOCK_METHOD = "lock_method"
    private const val KEY_FORCE_DARK_THEME = "force_dark_theme"
    private const val KEY_LOCK_TIMEOUT_MINUTES = "lock_timeout_minutes"
    private const val KEY_BF_BACKOFF_ENABLED = "bf_backoff_enabled"
    private const val KEY_BF_WIPE_ENABLED = "bf_wipe_enabled"
    private const val KEY_BF_WIPE_AFTER_N = "bf_wipe_after_n"
    private const val KEY_FAILURE_COUNT = "failure_count"
    private const val KEY_LAST_FAILURE_AT = "last_failure_at"

    private const val KEY_LAST_TAB = "last_tab"

    private const val KEY_TRASH_ENABLED = "trash_enabled"

    private const val KEY_TAB_BAR_AT_BOTTOM = "tab_bar_at_bottom"

    private const val KEY_ALBUM_SORT_ORDER = "album_sort_order"
    private const val KEY_ENTRY_SORT_ORDER = "entry_sort_order"

    private const val KEY_ENTRY_SORT_BY_SECTION = "entry_sort_by_section"
    private const val KEY_ALBUM_SORT_BY_SECTION = "album_sort_by_section"

    private const val KEY_THUMBNAIL_GEN = "thumbnail_gen"

    private const val KEY_ACTIVE_ICON_VARIANT = "active_icon_variant"

    private const val KEY_LIGHT_ACCENT = "light_accent"
    private const val KEY_DARK_ACCENT = "dark_accent"

    private const val KEY_THEME_VARIANT = "theme_variant"

    private const val KEY_APP_LANGUAGE = "app_language"
    const val LANGUAGE_SYSTEM = "system"

    private const val KEY_ALBUM_COLUMNS = "album_columns"
    private const val KEY_ALBUM_TILE_SHAPE = "album_tile_shape"

    private const val KEY_PHOTO_GRID_LAYOUT = "photo_grid_layout"

    val ALBUM_COLUMN_CHOICES = listOf(2, 3)
    private const val DEFAULT_ALBUM_COLUMNS = 3

    private lateinit var prefs: SharedPreferences

    val loggingEnabled: MutableState<Boolean> = mutableStateOf(true)

    val screenshotsAllowed: MutableState<Boolean> = mutableStateOf(false)

    val wizardComplete: MutableState<Boolean> = mutableStateOf(false)

    val cryptoMode: MutableState<CryptoMode> = mutableStateOf(CryptoMode.DEFAULT)

    val lockMethod: MutableState<LockMethod> = mutableStateOf(LockMethod.DEFAULT)

    val forceDarkTheme: MutableState<Boolean> = mutableStateOf(false)

    val lockTimeoutMinutes: MutableState<Int> = mutableStateOf(0)

    val bruteForceConfig: MutableState<BruteForceConfig> =
        mutableStateOf(BruteForceConfig.DEFAULT)

    val failureCount: MutableState<Int> = mutableStateOf(0)

    val lastFailureAt: MutableState<Long> = mutableStateOf(0L)

    val lastTabName: MutableState<String> = mutableStateOf("ALL")

    val trashEnabled: MutableState<Boolean> = mutableStateOf(true)

    val tabBarAtBottom: MutableState<Boolean> = mutableStateOf(false)

    val albumSortOrder: MutableState<AlbumSortOrder> =
        mutableStateOf(AlbumSortOrder.DEFAULT)
    val entrySortOrder: MutableState<EntrySortOrder> =
        mutableStateOf(EntrySortOrder.DEFAULT)

    val entrySortBySection = mutableStateMapOf<String, EntrySortOrder>()
    val albumSortBySection = mutableStateMapOf<String, AlbumSortOrder>()
    val entrySortRevision = mutableIntStateOf(0)
    val albumSortRevision = mutableIntStateOf(0)

    val activeIconVariant: MutableState<Int> = mutableStateOf(7)

    val lightAccent: MutableState<AccentColor> =
        mutableStateOf(AccentColor.DEFAULT_LIGHT)
    val darkAccent: MutableState<AccentColor> =
        mutableStateOf(AccentColor.DEFAULT_DARK)

    val themeVariant: MutableState<ThemeVariant> =
        mutableStateOf(ThemeVariant.DEFAULT)

    val appLanguage: MutableState<String> = mutableStateOf(LANGUAGE_SYSTEM)

    val albumColumns: MutableState<Int> = mutableStateOf(DEFAULT_ALBUM_COLUMNS)

    val albumTileShape: MutableState<AlbumTileShape> =
        mutableStateOf(AlbumTileShape.DEFAULT)

    val photoGridLayout: MutableState<PhotoGridLayout> =
        mutableStateOf(PhotoGridLayout.DEFAULT)

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loggingEnabled.value = prefs.getBoolean(KEY_LOGGING_ENABLED, true)
        screenshotsAllowed.value = prefs.getBoolean(KEY_SCREENSHOTS_ALLOWED, false)
        wizardComplete.value = prefs.getBoolean(KEY_WIZARD_COMPLETE, false)
        cryptoMode.value = readCryptoMode(prefs)
        lockMethod.value = readLockMethod(prefs)
        forceDarkTheme.value = prefs.getBoolean(KEY_FORCE_DARK_THEME, false)
        appLanguage.value = prefs.getString(KEY_APP_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        lockTimeoutMinutes.value = prefs.getInt(KEY_LOCK_TIMEOUT_MINUTES, 0)
        bruteForceConfig.value = BruteForceConfig(
            backoffEnabled = prefs.getBoolean(
                KEY_BF_BACKOFF_ENABLED,
                BruteForceConfig.DEFAULT.backoffEnabled
            ),
            wipeEnabled = prefs.getBoolean(
                KEY_BF_WIPE_ENABLED,
                BruteForceConfig.DEFAULT.wipeEnabled
            ),
            wipeAfterN = prefs.getInt(
                KEY_BF_WIPE_AFTER_N,
                BruteForceConfig.DEFAULT.wipeAfterN
            )
        )
        failureCount.value = prefs.getInt(KEY_FAILURE_COUNT, 0)
        lastFailureAt.value = prefs.getLong(KEY_LAST_FAILURE_AT, 0L)
        lastTabName.value = prefs.getString(KEY_LAST_TAB, "ALL") ?: "ALL"
        trashEnabled.value = prefs.getBoolean(KEY_TRASH_ENABLED, true)
        tabBarAtBottom.value = prefs.getBoolean(KEY_TAB_BAR_AT_BOTTOM, false)
        albumSortOrder.value = readAlbumSortOrder(prefs)
        entrySortOrder.value = readEntrySortOrder(prefs)
        loadEntrySortBySection(prefs)
        loadAlbumSortBySection(prefs)
        activeIconVariant.value = prefs.getInt(KEY_ACTIVE_ICON_VARIANT, 7)
        lightAccent.value = AccentColor.fromNameOrDefault(
            prefs.getString(KEY_LIGHT_ACCENT, null),
            darkTheme = false
        )
        darkAccent.value = AccentColor.fromNameOrDefault(
            prefs.getString(KEY_DARK_ACCENT, null),
            darkTheme = true
        )

        themeVariant.value = readThemeVariant(prefs)

        albumColumns.value = prefs.getInt(KEY_ALBUM_COLUMNS, DEFAULT_ALBUM_COLUMNS)
            .let { if (it in ALBUM_COLUMN_CHOICES) it else DEFAULT_ALBUM_COLUMNS }
        albumTileShape.value = AlbumTileShape.fromNameOrDefault(
            prefs.getString(KEY_ALBUM_TILE_SHAPE, null)
        )
        photoGridLayout.value = PhotoGridLayout.fromNameOrDefault(
            prefs.getString(KEY_PHOTO_GRID_LAYOUT, null)
        )

        EncLog.enabled = loggingEnabled.value
    }

    private fun readAlbumSortOrder(prefs: SharedPreferences): AlbumSortOrder {
        val name = prefs.getString(KEY_ALBUM_SORT_ORDER, null) ?: return AlbumSortOrder.DEFAULT
        return try {
            AlbumSortOrder.valueOf(name)
        } catch (_: IllegalArgumentException) {
            EncLog.w(
                "AppSettings",
                "unknown AlbumSortOrder '$name' in prefs, falling back to ${AlbumSortOrder.DEFAULT.name}"
            )
            AlbumSortOrder.DEFAULT
        }
    }

    private fun readEntrySortOrder(prefs: SharedPreferences): EntrySortOrder {
        val name = prefs.getString(KEY_ENTRY_SORT_ORDER, null) ?: return EntrySortOrder.DEFAULT
        return try {
            EntrySortOrder.valueOf(name)
        } catch (_: IllegalArgumentException) {
            EncLog.w(
                "AppSettings",
                "unknown EntrySortOrder '$name' in prefs, falling back to ${EntrySortOrder.DEFAULT.name}"
            )
            EntrySortOrder.DEFAULT
        }
    }

    fun entrySortOrderFor(section: String): EntrySortOrder =
        entrySortBySection[section] ?: entrySortOrder.value

    fun albumSortOrderFor(section: String): AlbumSortOrder =
        albumSortBySection[section] ?: albumSortOrder.value

    private fun loadEntrySortBySection(prefs: SharedPreferences) {
        entrySortBySection.clear()
        val raw = prefs.getString(KEY_ENTRY_SORT_BY_SECTION, null) ?: return
        raw.split('\n').forEach { line ->
            val sep = line.indexOf('=')
            if (sep <= 0) return@forEach
            val key = line.substring(0, sep)
            val order = try {
                EntrySortOrder.valueOf(line.substring(sep + 1))
            } catch (_: IllegalArgumentException) {
                return@forEach
            }
            entrySortBySection[key] = order
        }
    }

    private fun loadAlbumSortBySection(prefs: SharedPreferences) {
        albumSortBySection.clear()
        val raw = prefs.getString(KEY_ALBUM_SORT_BY_SECTION, null) ?: return
        raw.split('\n').forEach { line ->
            val sep = line.indexOf('=')
            if (sep <= 0) return@forEach
            val key = line.substring(0, sep)
            val order = try {
                AlbumSortOrder.valueOf(line.substring(sep + 1))
            } catch (_: IllegalArgumentException) {
                return@forEach
            }
            albumSortBySection[key] = order
        }
    }

    private fun readCryptoMode(prefs: SharedPreferences): CryptoMode {
        val name = prefs.getString(KEY_CRYPTO_MODE, null) ?: return CryptoMode.DEFAULT
        return try {
            CryptoMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            EncLog.w(
                "AppSettings",
                "unknown CryptoMode '$name' in prefs, falling back to ${CryptoMode.DEFAULT.name}"
            )
            CryptoMode.DEFAULT
        }
    }

    private fun readLockMethod(prefs: SharedPreferences): LockMethod {
        val name = prefs.getString(KEY_LOCK_METHOD, null) ?: return LockMethod.DEFAULT
        return try {
            LockMethod.valueOf(name)
        } catch (_: IllegalArgumentException) {
            EncLog.w(
                "AppSettings",
                "unknown LockMethod '$name' in prefs, falling back to ${LockMethod.DEFAULT.name}"
            )
            LockMethod.DEFAULT
        }
    }

    fun setLoggingEnabled(value: Boolean) {
        loggingEnabled.value = value
        prefs.edit().putBoolean(KEY_LOGGING_ENABLED, value).apply()
        EncLog.enabled = value
        EncLog.i("AppSettings", "loggingEnabled set to $value")
    }

    fun setScreenshotsAllowed(value: Boolean) {
        screenshotsAllowed.value = value
        prefs.edit().putBoolean(KEY_SCREENSHOTS_ALLOWED, value).apply()
        EncLog.i("AppSettings", "screenshotsAllowed set to $value")
    }

    fun setWizardComplete(value: Boolean) {
        wizardComplete.value = value
        prefs.edit().putBoolean(KEY_WIZARD_COMPLETE, value).apply()
        EncLog.i("AppSettings", "wizardComplete set to $value")
    }

    fun setCryptoMode(value: CryptoMode) {
        val alreadyPersisted = prefs.contains(KEY_CRYPTO_MODE)
        if (alreadyPersisted && cryptoMode.value == value) return
        cryptoMode.value = value
        prefs.edit().putString(KEY_CRYPTO_MODE, value.name).apply()
        EncLog.i("AppSettings", "cryptoMode set to ${value.name}")
    }

    fun setLockMethod(value: LockMethod) {
        val alreadyPersisted = prefs.contains(KEY_LOCK_METHOD)
        if (alreadyPersisted && lockMethod.value == value) return
        lockMethod.value = value
        prefs.edit().putString(KEY_LOCK_METHOD, value.name).apply()
        EncLog.i("AppSettings", "lockMethod set to ${value.name}")
    }

    fun setForceDarkTheme(value: Boolean) {
        forceDarkTheme.value = value
        prefs.edit().putBoolean(KEY_FORCE_DARK_THEME, value).apply()
        EncLog.i("AppSettings", "forceDarkTheme set to $value")
    }

    fun setAppLanguage(value: String) {
        appLanguage.value = value
        prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()
        EncLog.i("AppSettings", "appLanguage set to $value")
    }

    fun persistedLanguage(ctx: Context): String =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM

    fun setLockTimeoutMinutes(value: Int) {
        val clamped = value.coerceAtLeast(0)
        if (lockTimeoutMinutes.value == clamped) return
        lockTimeoutMinutes.value = clamped
        prefs.edit().putInt(KEY_LOCK_TIMEOUT_MINUTES, clamped).apply()
        EncLog.i("AppSettings", "lockTimeoutMinutes set to $clamped")
    }

    fun setBruteForceConfig(value: BruteForceConfig) {
        val clamped = value.copy(wipeAfterN = value.wipeAfterN.coerceAtLeast(1))
        if (bruteForceConfig.value == clamped) return
        bruteForceConfig.value = clamped
        prefs.edit()
            .putBoolean(KEY_BF_BACKOFF_ENABLED, clamped.backoffEnabled)
            .putBoolean(KEY_BF_WIPE_ENABLED, clamped.wipeEnabled)
            .putInt(KEY_BF_WIPE_AFTER_N, clamped.wipeAfterN)
            .apply()
        EncLog.i("AppSettings", "bruteForceConfig set to $clamped")
    }

    fun incrementFailure() {
        failureCount.value += 1
        lastFailureAt.value = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_FAILURE_COUNT, failureCount.value)
            .putLong(KEY_LAST_FAILURE_AT, lastFailureAt.value)
            .apply()
        EncLog.i(
            "AppSettings",
            "failureCount=${failureCount.value}, lastFailureAt=${lastFailureAt.value}"
        )
    }

    fun resetFailureState() {
        if (failureCount.value == 0 && lastFailureAt.value == 0L) return
        failureCount.value = 0
        lastFailureAt.value = 0L
        prefs.edit()
            .remove(KEY_FAILURE_COUNT)
            .remove(KEY_LAST_FAILURE_AT)
            .apply()
        EncLog.i("AppSettings", "failure state reset")
    }

    fun wipeVaultPrefs() {
        prefs.edit()
            .remove(KEY_WIZARD_COMPLETE)
            .remove(KEY_CRYPTO_MODE)
            .remove(KEY_LOCK_METHOD)
            .remove(KEY_BF_BACKOFF_ENABLED)
            .remove(KEY_BF_WIPE_ENABLED)
            .remove(KEY_BF_WIPE_AFTER_N)
            .remove(KEY_FAILURE_COUNT)
            .remove(KEY_LAST_FAILURE_AT)
            .apply()
        wizardComplete.value = false
        cryptoMode.value = CryptoMode.DEFAULT
        lockMethod.value = LockMethod.DEFAULT
        bruteForceConfig.value = BruteForceConfig.DEFAULT
        failureCount.value = 0
        lastFailureAt.value = 0L
        EncLog.i("AppSettings", "vault prefs wiped")
    }

    fun setLastTabName(value: String) {
        if (value == lastTabName.value) return
        lastTabName.value = value
        prefs.edit().putString(KEY_LAST_TAB, value).apply()
    }

    fun setTrashEnabled(value: Boolean) {
        if (trashEnabled.value == value) return
        trashEnabled.value = value
        prefs.edit().putBoolean(KEY_TRASH_ENABLED, value).apply()
        EncLog.i("AppSettings", "trashEnabled set to $value")
    }

    fun setTabBarAtBottom(value: Boolean) {
        if (tabBarAtBottom.value == value) return
        tabBarAtBottom.value = value
        prefs.edit().putBoolean(KEY_TAB_BAR_AT_BOTTOM, value).apply()
        EncLog.i("AppSettings", "tabBarAtBottom set to $value")
    }

    fun setAlbumSortOrder(value: AlbumSortOrder) {
        if (albumSortOrder.value == value) return
        albumSortOrder.value = value
        prefs.edit().putString(KEY_ALBUM_SORT_ORDER, value.name).apply()
        EncLog.i("AppSettings", "albumSortOrder set to ${value.name}")
    }

    fun setEntrySortOrder(value: EntrySortOrder) {
        if (entrySortOrder.value == value) return
        entrySortOrder.value = value
        prefs.edit().putString(KEY_ENTRY_SORT_ORDER, value.name).apply()
        EncLog.i("AppSettings", "entrySortOrder set to ${value.name}")
    }

    fun setEntrySortOrderFor(section: String, value: EntrySortOrder) {
        if (entrySortBySection[section] == value) return
        entrySortBySection[section] = value
        entrySortRevision.intValue++
        persistSortMap(KEY_ENTRY_SORT_BY_SECTION, entrySortBySection)
        EncLog.i("AppSettings", "entrySortOrder[$section] set to ${value.name}")
    }

    fun setAlbumSortOrderFor(section: String, value: AlbumSortOrder) {
        if (albumSortBySection[section] == value) return
        albumSortBySection[section] = value
        albumSortRevision.intValue++
        persistSortMap(KEY_ALBUM_SORT_BY_SECTION, albumSortBySection)
        EncLog.i("AppSettings", "albumSortOrder[$section] set to ${value.name}")
    }

    fun thumbnailGen(): Int = prefs.getInt(KEY_THUMBNAIL_GEN, 0)

    fun setThumbnailGen(gen: Int) {
        prefs.edit().putInt(KEY_THUMBNAIL_GEN, gen).apply()
        EncLog.i("AppSettings", "thumbnailGen set to $gen")
    }

    private fun persistSortMap(key: String, map: Map<String, Enum<*>>) {
        val raw = map.entries.joinToString("\n") { "${it.key}=${it.value.name}" }
        prefs.edit().putString(key, raw).apply()
    }

    fun setActiveIconVariant(value: Int) {
        if (activeIconVariant.value == value) return
        activeIconVariant.value = value
        prefs.edit().putInt(KEY_ACTIVE_ICON_VARIANT, value).apply()
        EncLog.i("AppSettings", "activeIconVariant set to $value")
    }

    fun setLightAccent(value: AccentColor) {
        if (lightAccent.value == value) return
        lightAccent.value = value
        prefs.edit().putString(KEY_LIGHT_ACCENT, value.name).apply()
        EncLog.i("AppSettings", "lightAccent set to ${value.name}")
    }

    fun setDarkAccent(value: AccentColor) {
        if (darkAccent.value == value) return
        darkAccent.value = value
        prefs.edit().putString(KEY_DARK_ACCENT, value.name).apply()
        EncLog.i("AppSettings", "darkAccent set to ${value.name}")
    }

    fun setAlbumColumns(value: Int) {

        if (value !in ALBUM_COLUMN_CHOICES) return
        if (albumColumns.value == value) return
        albumColumns.value = value
        prefs.edit().putInt(KEY_ALBUM_COLUMNS, value).apply()
        EncLog.i("AppSettings", "albumColumns set to $value")
    }

    fun setAlbumTileShape(value: AlbumTileShape) {
        if (albumTileShape.value == value) return
        albumTileShape.value = value
        prefs.edit().putString(KEY_ALBUM_TILE_SHAPE, value.name).apply()
        EncLog.i("AppSettings", "albumTileShape set to ${value.name}")
    }

    fun setPhotoGridLayout(value: PhotoGridLayout) {
        if (photoGridLayout.value == value) return
        photoGridLayout.value = value
        prefs.edit().putString(KEY_PHOTO_GRID_LAYOUT, value.name).apply()
        EncLog.i("AppSettings", "photoGridLayout set to ${value.name}")
    }

    fun setThemeVariant(value: ThemeVariant) {
        if (themeVariant.value == value) return
        themeVariant.value = value

        prefs.edit()
            .putString(KEY_THEME_VARIANT, value.name)
            .putBoolean(KEY_FORCE_DARK_THEME, value.isDark)
            .apply()

        forceDarkTheme.value = value.isDark
        EncLog.i("AppSettings", "themeVariant set to ${value.name}")
    }

    private fun readThemeVariant(prefs: SharedPreferences): ThemeVariant {
        val newKey = prefs.getString(KEY_THEME_VARIANT, null)
        if (newKey != null) return ThemeVariant.fromNameOrDefault(newKey)

        return if (prefs.getBoolean(KEY_FORCE_DARK_THEME, false))
            ThemeVariant.DARK_DEFAULT else ThemeVariant.LIGHT
    }
}
