package dev.encgallery.featuresettings

import androidx.annotation.StringRes

enum class AlbumTileShape(@StringRes val labelRes: Int) {
    ROUNDED(R.string.settings_appearance_tile_shape_rounded),
    SQUARE(R.string.settings_appearance_tile_shape_square);

    companion object {
        val DEFAULT: AlbumTileShape = ROUNDED

        fun fromNameOrDefault(name: String?): AlbumTileShape {
            if (name == null) return DEFAULT
            return try {
                valueOf(name)
            } catch (_: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}
