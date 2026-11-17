package info.pakkudev.simple_dialer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.luminance
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {
    @Test fun `every monochrome color role is neutral in both modes`() {
        val roles = ColorScheme::class.java.methods.filter {
            it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == java.lang.Long.TYPE
        }
        assertTrue("The color scheme must expose its color roles", roles.size >= 40)
        listOf(false, true).forEach { dark ->
            val scheme = paletteColorScheme(ColorPalette.MONOCHROME, dark)
            roles.forEach { role ->
                val color = Color((role.invoke(scheme) as Long).toULong())
                assertTrue("${role.name} dark=$dark must be grayscale", color.red == color.green && color.green == color.blue)
            }
        }
    }

    @Test fun `dark backgrounds stay neutral across every palette without elevation tint`() {
        ColorPalette.entries.forEach { palette ->
            val colors = paletteColorScheme(palette, dark = true)
            val surfaces = listOf(colors.background, colors.surface, colors.surfaceVariant,
                colors.surfaceDim, colors.surfaceBright, colors.surfaceContainerLowest,
                colors.surfaceContainerLow, colors.surfaceContainer, colors.surfaceContainerHigh,
                colors.surfaceContainerHighest)
            surfaces.forEach { surface ->
                assertTrue("$palette dark surface must be opaque and neutral",
                    surface.alpha == 1f && surface.red == surface.green && surface.green == surface.blue)
            }
            assertTrue("$palette must not tint elevated surfaces", colors.surfaceTint.alpha == 0f)
            if (palette != ColorPalette.MONOCHROME) {
                assertTrue("$palette must retain its accent color",
                    colors.primary.red != colors.primary.green || colors.primary.green != colors.primary.blue)
            }
        }
    }

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }

    @Test fun `all palettes keep readable text in light and dark modes`() {
        ColorPalette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val colors = paletteColorScheme(palette, dark)
                val surfaces = listOf(colors.background, colors.surface, colors.surfaceContainerLowest,
                    colors.surfaceContainerLow, colors.surfaceContainer, colors.surfaceContainerHigh, colors.surfaceContainerHighest)
                surfaces.forEach { surface ->
                    listOf(colors.onSurface, colors.onSurfaceVariant).forEach { text ->
                        assertTrue("$palette dark=$dark surface text contrast=${contrast(text, surface)}", contrast(text, surface) >= 4.5f)
                    }
                }
                listOf(colors.primary to colors.onPrimary, colors.primaryContainer to colors.onPrimaryContainer,
                    colors.secondaryContainer to colors.onSecondaryContainer, colors.error to colors.onError,
                    colors.inverseSurface to colors.inverseOnSurface).forEach { (background, text) ->
                    assertTrue("$palette dark=$dark control text contrast=${contrast(text, background)}", contrast(text, background) >= 4.5f)
                }
                assertTrue("$palette dark=$dark selected control contrast", contrast(colors.primary, colors.surface) >= 3f)
                assertTrue("$palette dark=$dark control outline contrast", contrast(colors.outline, colors.surface) >= 3f)
            }
        }
    }
}
