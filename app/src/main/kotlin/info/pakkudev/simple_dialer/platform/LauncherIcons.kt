package info.pakkudev.simple_dialer.platform

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import info.pakkudev.simple_dialer.domain.model.ColorPalette

/** Changes launcher aliases only. MainActivity remains enabled for dialing and explicit intents. */
class LauncherIcons(context: Context, private val profile: PlatformProfile) {
    private val manager = context.applicationContext.packageManager
    private val packageName = context.packageName

    fun component(palette: ColorPalette): ComponentName {
        val name = when (palette) {
            ColorPalette.MONOCHROME -> "Monochrome"
            ColorPalette.OCEAN -> "Ocean"
            ColorPalette.FOREST -> "Forest"
            ColorPalette.ROSE -> "Rose"
            ColorPalette.SUNSET -> "Sunset"
        }
        return ComponentName(packageName, "$packageName.launcher.$name")
    }

    @Synchronized
    fun apply(palette: ColorPalette) {
        val target = component(palette)
        val aliases = ColorPalette.entries.map(::component)
        val enabled = aliases.filter { alias ->
            when (manager.getComponentEnabledSetting(alias)) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> alias == component(ColorPalette.DEFAULT)
                else -> false
            }
        }
        if (enabled == listOf(target)) return
        if (profile.supportsAtomicComponentUpdates) updateAtomically(aliases, target)
        else {
            // Enable the replacement first so older Android never has a gap with no launcher entry.
            manager.setComponentEnabledSetting(target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            aliases.filter { it != target }.forEach {
                manager.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }

    @SuppressLint("NewApi") // Called only when PlatformProfile reports API 33+ support.
    private fun updateAtomically(aliases: List<ComponentName>, target: ComponentName) {
        manager.setComponentEnabledSettings(aliases.map { alias ->
            PackageManager.ComponentEnabledSetting(alias,
                if (alias == target) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        })
    }
}
