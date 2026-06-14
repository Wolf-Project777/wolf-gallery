package dev.encgallery.featuresettings

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

enum class AccentColor(
    @StringRes val nameRes: Int,
    val lightSwatch: Color,
    val darkSwatch: Color,
    val onLight: Color,
    val onDark: Color
) {
    GREEN(
        nameRes = R.string.settings_appearance_accent_green,
        lightSwatch = Color(0xFF2E7D32),
        darkSwatch = Color(0xFF4ADE80),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF052E16)
    ),
    BLUE(
        nameRes = R.string.settings_appearance_accent_blue,
        lightSwatch = Color(0xFF1565C0),
        darkSwatch = Color(0xFF60A5FA),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF0B1F3D)
    ),
    YELLOW(
        nameRes = R.string.settings_appearance_accent_yellow,
        lightSwatch = Color(0xFFD18C00),
        darkSwatch = Color(0xFFFACC15),
        onLight = Color(0xFF1A1100),
        onDark = Color(0xFF1A1100)
    ),
    MAGENTA(
        nameRes = R.string.settings_appearance_accent_magenta,
        lightSwatch = Color(0xFFC2185B),
        darkSwatch = Color(0xFFF472B6),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF2D0A1B)
    ),
    VIOLET(
        nameRes = R.string.settings_appearance_accent_violet,
        lightSwatch = Color(0xFF7B1FA2),
        darkSwatch = Color(0xFFC084FC),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF24062E)
    ),
    ORANGE(
        nameRes = R.string.settings_appearance_accent_orange,
        lightSwatch = Color(0xFFE65100),
        darkSwatch = Color(0xFFFB923C),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF2A1207)
    ),
    CORAL(
        nameRes = R.string.settings_appearance_accent_coral,
        lightSwatch = Color(0xFFD84747),
        darkSwatch = Color(0xFFFB7185),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF2C0710)
    ),
    TEAL(
        nameRes = R.string.settings_appearance_accent_teal,
        lightSwatch = Color(0xFF00838F),
        darkSwatch = Color(0xFF2DD4BF),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF042F2E)
    ),
    LIME(
        nameRes = R.string.settings_appearance_accent_lime,
        lightSwatch = Color(0xFF558B2F),
        darkSwatch = Color(0xFFA3E635),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF132610)
    ),
    CYAN(
        nameRes = R.string.settings_appearance_accent_cyan,
        lightSwatch = Color(0xFF0277BD),
        darkSwatch = Color(0xFF67E8F9),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF052730)
    ),
    INDIGO(
        nameRes = R.string.settings_appearance_accent_indigo,
        lightSwatch = Color(0xFF303F9F),
        darkSwatch = Color(0xFF818CF8),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF0C1126)
    ),
    PURPLE(
        nameRes = R.string.settings_appearance_accent_purple,
        lightSwatch = Color(0xFF5E35B1),
        darkSwatch = Color(0xFFA78BFA),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF1A0B33)
    ),
    RED(
        nameRes = R.string.settings_appearance_accent_red,
        lightSwatch = Color(0xFFB71C1C),
        darkSwatch = Color(0xFFEF4444),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF2A0606)
    ),
    AMBER(
        nameRes = R.string.settings_appearance_accent_amber,
        lightSwatch = Color(0xFFB37A00),
        darkSwatch = Color(0xFFFCD34D),
        onLight = Color(0xFF1A1100),
        onDark = Color(0xFF1A1100)
    ),
    OLIVE(
        nameRes = R.string.settings_appearance_accent_olive,
        lightSwatch = Color(0xFF6B7B23),
        darkSwatch = Color(0xFFBEF264),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF1A1F08)
    ),
    BROWN(
        nameRes = R.string.settings_appearance_accent_brown,
        lightSwatch = Color(0xFF6D4C41),
        darkSwatch = Color(0xFFD4A574),
        onLight = Color(0xFFFFFFFF),
        onDark = Color(0xFF2A1A0F)
    );

    fun primaryFor(darkTheme: Boolean): Color = if (darkTheme) darkSwatch else lightSwatch
    fun onPrimaryFor(darkTheme: Boolean): Color = if (darkTheme) onDark else onLight

    companion object {
        val DEFAULT_LIGHT: AccentColor = BLUE
        val DEFAULT_DARK: AccentColor = BLUE

        val PICKER_ORDER: List<AccentColor> = entries.toList()

        fun fromNameOrDefault(name: String?, darkTheme: Boolean): AccentColor {
            if (name == null) return if (darkTheme) DEFAULT_DARK else DEFAULT_LIGHT
            return try {
                valueOf(name)
            } catch (_: IllegalArgumentException) {
                if (darkTheme) DEFAULT_DARK else DEFAULT_LIGHT
            }
        }
    }
}
