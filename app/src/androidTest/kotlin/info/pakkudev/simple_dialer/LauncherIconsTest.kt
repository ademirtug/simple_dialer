package info.pakkudev.simple_dialer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.platform.LauncherIcons
import info.pakkudev.simple_dialer.platform.PlatformProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class LauncherIconsTest {
    @Test fun preAndroid13SwitchingRetainsOneLauncherAndTheDialActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first().colorPalette }
        val icons = LauncherIcons(context, PlatformProfile.forSdk(23))
        fun entries() = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName), 0)
        fun await(palette: ColorPalette) {
            val deadline = SystemClock.elapsedRealtime() + 5_000
            while (entries().map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) } != listOf(icons.component(palette))) {
                check(SystemClock.elapsedRealtime() < deadline) { "Launcher did not switch to $palette" }
                SystemClock.sleep(50)
            }
        }
        try {
            // Synchronize the application observer before exercising the older sequential update path.
            val initial = ColorPalette.entries.first { it != original }
            runBlocking { settings.setColorPalette(initial) }
            await(initial)
            ColorPalette.entries.forEach { palette ->
                icons.apply(palette)
                await(palette)
                assertEquals(MainActivity::class.java.name, entries().single().activityInfo.targetActivity)
                assertTrue(context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0).enabled)
                val dialers = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_DIAL, Uri.parse("tel:123")).setPackage(context.packageName), 0)
                assertTrue(dialers.any { it.activityInfo.name == MainActivity::class.java.name })
            }
        } finally {
            runBlocking { settings.setColorPalette(original) }
            await(original)
        }
    }
}
