package dev.encgallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.encgallery.featuresettings.AccentColor
import dev.encgallery.featuresettings.ProvideThemeVariant
import dev.encgallery.featuresettings.ThemeVariant

@Composable
fun EncGalleryTheme(
    variant: ThemeVariant = ThemeVariant.DEFAULT,
    lightAccent: AccentColor = AccentColor.DEFAULT_LIGHT,
    darkAccent: AccentColor = AccentColor.DEFAULT_DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val scheme = colorSchemeFor(variant, lightAccent, darkAccent, systemDark)
    val typography = typographyFor(variant)
    MaterialTheme(
        colorScheme = scheme,
        typography = typography
    ) {

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

            ProvideThemeVariant(variant, content)
        }
    }
}

private fun colorSchemeFor(
    variant: ThemeVariant,
    lightAccent: AccentColor,
    darkAccent: AccentColor,
    systemDark: Boolean
): ColorScheme = when (variant) {
    ThemeVariant.LIGHT -> LightBaseScheme.copy(
        primary = lightAccent.primaryFor(false),
        onPrimary = lightAccent.onPrimaryFor(false)
    )
    ThemeVariant.DARK_DEFAULT -> DarkBaseScheme.copy(
        primary = darkAccent.primaryFor(true),
        onPrimary = darkAccent.onPrimaryFor(true)
    )
    ThemeVariant.HACKER -> HackerScheme
    ThemeVariant.BLOODY -> BloodyScheme
    ThemeVariant.MEDIEVAL -> MedievalScheme
}

private fun typographyFor(variant: ThemeVariant): Typography {
    if (variant != ThemeVariant.MEDIEVAL) return Typography()
    val base = Typography()
    val gothicFamily = FontFamily.Serif
    fun heavy(style: TextStyle): TextStyle = style.copy(
        fontFamily = gothicFamily,
        fontWeight = FontWeight.Black
    )
    return base.copy(
        displayLarge = heavy(base.displayLarge),
        displayMedium = heavy(base.displayMedium),
        displaySmall = heavy(base.displaySmall),
        headlineLarge = heavy(base.headlineLarge),
        headlineMedium = heavy(base.headlineMedium),
        headlineSmall = heavy(base.headlineSmall),
        titleLarge = heavy(base.titleLarge),
        titleMedium = heavy(base.titleMedium),
        titleSmall = heavy(base.titleSmall)
    )
}
