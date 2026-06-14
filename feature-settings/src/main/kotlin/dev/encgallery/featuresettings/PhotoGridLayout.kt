package dev.encgallery.featuresettings

import androidx.annotation.StringRes

enum class PhotoGridLayout(@StringRes val labelRes: Int) {
    STANDARD(R.string.settings_appearance_photo_layout_standard),
    ENLARGED(R.string.settings_appearance_photo_layout_enlarged);

    companion object {
        val DEFAULT: PhotoGridLayout = STANDARD

        fun fromNameOrDefault(name: String?): PhotoGridLayout {
            if (name == null) return DEFAULT
            return try {
                valueOf(name)
            } catch (_: IllegalArgumentException) {
                DEFAULT
            }
        }
    }
}
