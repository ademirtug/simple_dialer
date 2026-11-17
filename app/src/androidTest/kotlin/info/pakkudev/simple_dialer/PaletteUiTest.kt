package info.pakkudev.simple_dialer

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.pakkudev.simple_dialer.data.preferences.DataStorePreferencesRepository
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.ui.labelResource
import info.pakkudev.simple_dialer.platform.LauncherIcons
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.io.File

@RunWith(AndroidJUnit4::class)
class PaletteUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun monochromeModePreservesColorInOtherPalettePreviews() {
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first().colorPalette }
        try {
            runBlocking { settings.setColorPalette(ColorPalette.MONOCHROME) }
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.more_options)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.admin_settings)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.palette_monochrome)).assertIsSelected()
            ColorPalette.entries.forEach { palette ->
                compose.onNodeWithText(compose.activity.getString(palette.labelResource())).performScrollTo()
                val pixels = compose.onNodeWithTag("palette_swatch_${palette.name}_0", useUnmergedTree = true).captureToImage().toPixelMap()
                val color = pixels[pixels.width / 2, pixels.height / 2]
                val chromatic = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue) > 0.03f
                assertEquals("$palette preview must show its own color", palette != ColorPalette.MONOCHROME, chromatic)
            }
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(compose.activity.getExternalFilesDir(null), "monochrome-palette-previews.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { runBlocking { settings.setColorPalette(original) } }
    }

    @Test fun paletteSelectionAppliesImmediatelyAndSurvivesRecreation() {
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first().colorPalette }
        val icons = GlobalContext.get().get<LauncherIcons>()
        val context = compose.activity.applicationContext
        fun launchers() = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName), 0)
        fun assertLauncher(palette: ColorPalette) {
            val expected = icons.component(palette)
            compose.waitUntil(5_000) {
                launchers().map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) } == listOf(expected)
            }
            assertEquals(MainActivity::class.java.name, launchers().single().activityInfo.targetActivity)
            val dialers = context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:123")).setPackage(context.packageName), 0)
            assertTrue("Dialing must remain available after an icon change", dialers.any { it.activityInfo.name == MainActivity::class.java.name })
        }
        try {
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.more_options)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.admin_settings)).performClick()
            ColorPalette.entries.forEach { palette ->
                val name = compose.activity.getString(palette.labelResource())
                compose.onNodeWithText(name).performScrollTo().performClick()
                compose.waitUntil(5_000) {
                    compose.onAllNodes(hasText(name) and isSelected()).fetchSemanticsNodes().size == 1
                }
                assertEquals(palette, runBlocking { DataStorePreferencesRepository(compose.activity).preferences.first().colorPalette })
                assertLauncher(palette)
            }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(compose.activity.getString(ColorPalette.entries.last().labelResource())).performScrollTo().assertIsSelected()
            assertLauncher(ColorPalette.entries.last())
        } finally {
            runBlocking { settings.setColorPalette(original) }
            assertLauncher(original)
        }
    }
}
