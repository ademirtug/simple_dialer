package info.pakkudev.simple_dialer

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import info.pakkudev.simple_dialer.ui.ContactAvatar
import info.pakkudev.simple_dialer.ui.PalettePicker
import info.pakkudev.simple_dialer.ui.PhoneDialpad
import info.pakkudev.simple_dialer.ui.PhoneIcons
import info.pakkudev.simple_dialer.telecom.CallActionButton
import info.pakkudev.simple_dialer.ui.theme.DialerTheme
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class MonochromeUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun photoKeepsItsColorWhilePalettePreviewsStayNeutralInBothModes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photo = File.createTempFile("monochrome-photo-", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val dark = mutableStateOf(false)
        try {
            compose.setContent {
                val config = Configuration(LocalConfiguration.current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                        if (dark.value) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
                CompositionLocalProvider(LocalConfiguration provides config) {
                    DialerTheme(ColorPalette.MONOCHROME) {
                        Surface {
                            Column {
                                Box(Modifier.testTag("photo")) { ContactAvatar("Photo", Uri.fromFile(photo).toString(), 80.dp) }
                                Column(Modifier.testTag("previews")) { PalettePicker(ColorPalette.MONOCHROME, {}) }
                            }
                        }
                    }
                }
            }
            // Wait for the original red image, rather than checking the initial-letter placeholder.
            compose.waitUntil(10_000) {
                val pixels = compose.onNodeWithTag("photo").captureToImage().toPixelMap()
                val center = pixels[pixels.width / 2, pixels.height / 2]
                center.red > 0.99f && center.green < 0.01f && center.blue < 0.01f
            }
            listOf(false, true).forEach { night ->
                compose.runOnIdle { dark.value = night }
                val photoPixels = compose.onNodeWithTag("photo").captureToImage().toPixelMap()
                val center = photoPixels[photoPixels.width / 2, photoPixels.height / 2]
                assertTrue("Photo must retain red dark=$night", center.red > 0.99f && center.green < 0.01f && center.blue < 0.01f)
                val pixels = compose.onNodeWithTag("previews").captureToImage().toPixelMap()
                for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                    val color = pixels[x, y]
                    assertTrue("Colored pixel at $x,$y dark=$night", abs(color.red - color.green) <= 1f / 255 && abs(color.green - color.blue) <= 1f / 255)
                }
            }
        } finally {
            photo.delete()
        }
    }

    @Test fun dialingAnswerAndEndButtonsKeepTheirColorsInBothModes() {
        val dark = mutableStateOf(false)
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark.value) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides config) {
                DialerTheme(ColorPalette.MONOCHROME) {
                    Surface {
                        Column {
                            Row {
                                Box(Modifier.testTag("answer")) { CallActionButton(PhoneIcons.Call, R.string.answer, Color(0xFF188038)) {} }
                                Box(Modifier.testTag("end")) { CallActionButton(PhoneIcons.CallEnd, R.string.end_call, Color(0xFFC5221F)) {} }
                            }
                            Box(Modifier.testTag("dialpad")) { PhoneDialpad("123", {}, {}, {}) }
                        }
                    }
                }
            }
        }
        listOf(false, true).forEach { night ->
            compose.runOnIdle { dark.value = night }
            listOf("answer", "end", "dialpad").forEach { tag ->
                val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
                var coloredPixels = 0
                for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                    val color = pixels[x, y]
                    if (if (tag == "end") color.red > color.green * 2 && color.red > color.blue * 2
                        else color.green > color.red * 2 && color.green > color.blue * 1.5f) coloredPixels++
                }
                assertTrue("$tag must keep its action color dark=$night", coloredPixels > 100)
            }
        }
    }
}
