package dev.encgallery.featuresettings

object SettingsSession {

    var lastOpenCategory: SettingsCategory = SettingsCategory.HOME

    var lastTestsAndLogsScrollPx: Int = 0

    fun reset() {
        lastOpenCategory = SettingsCategory.HOME
        lastTestsAndLogsScrollPx = 0
    }
}

enum class SettingsCategory { HOME, SECURITY, APPEARANCE, LANGUAGE, APP_ICON, TRASH, SUPPORT, TESTS_AND_LOGS, ABOUT }
