package dev.encgallery.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dev.encgallery.logging.EncLog
import dev.encgallery.settings.AppSettings

object IconSwitcher {

    const val VARIANT_COUNT = 10

    const val DEFAULT_VARIANT = 7

    private const val ALIAS_PREFIX = "dev.encgallery.IconAlias"

    private const val TAG = "IconSwitcher"

    fun setActiveVariant(context: Context, target: Int) {
        require(target in 1..VARIANT_COUNT) { "variant out of range: $target" }
        val pm = context.packageManager
        val pkg = context.packageName

        val targetComp = ComponentName(pkg, "$ALIAS_PREFIX$target")
        pm.setComponentEnabledSetting(
            targetComp,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        for (n in 1..VARIANT_COUNT) {
            if (n == target) continue
            val comp = ComponentName(pkg, "$ALIAS_PREFIX$n")
            pm.setComponentEnabledSetting(
                comp,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        AppSettings.setActiveIconVariant(target)
        EncLog.i(TAG, "active icon variant set to $target")
    }

    fun activeVariant(): Int = AppSettings.activeIconVariant.value
}
