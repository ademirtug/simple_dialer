package info.pakkudev.simple_dialer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import info.pakkudev.simple_dialer.domain.model.ColorPalette

val LocalMonochrome = staticCompositionLocalOf { false }

private data class PaletteColors(
    val primary: Color,
    val container: Color,
    val onContainer: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
)

private fun colors(palette: ColorPalette, dark: Boolean): PaletteColors = when (palette) {
    ColorPalette.OCEAN -> if (dark) PaletteColors(
        Color(0xFFA8C7FA), Color(0xFF284777), Color(0xFFD8E7FF), Color(0xFF0C1728),
        Color(0xFF172E49), Color(0xFFE0EBFF), Color(0xFFBACBE3), Color(0xFF8196B2),
    ) else PaletteColors(
        Color(0xFF175DB5), Color(0xFFD3E3FD), Color(0xFF082F63), Color(0xFFF4F8FF),
        Color(0xFFDCEAFF), Color(0xFF16243A), Color(0xFF42536A), Color(0xFF697D99),
    )
    ColorPalette.FOREST -> if (dark) PaletteColors(
        Color(0xFF98D8AB), Color(0xFF1B5131), Color(0xFFBCEFCB), Color(0xFF0D1C14),
        Color(0xFF1A3826), Color(0xFFD9F1DD), Color(0xFFB5CCBA), Color(0xFF7E9C87),
    ) else PaletteColors(
        Color(0xFF256B43), Color(0xFFBCEFCB), Color(0xFF123D24), Color(0xFFF2FBF3),
        Color(0xFFD9EEDC), Color(0xFF122A1C), Color(0xFF425B49), Color(0xFF68816E),
    )
    ColorPalette.ROSE -> if (dark) PaletteColors(
        Color(0xFFFFB1D2), Color(0xFF722449), Color(0xFFFFD8E7), Color(0xFF24111C),
        Color(0xFF472236), Color(0xFFFFE3EE), Color(0xFFDFBCCA), Color(0xFFB58D9F),
    ) else PaletteColors(
        Color(0xFFA53465), Color(0xFFFFD8E7), Color(0xFF611336), Color(0xFFFFF5F8),
        Color(0xFFF9DFEA), Color(0xFF361522), Color(0xFF71505F), Color(0xFF987282),
    )
    ColorPalette.SUNSET -> if (dark) PaletteColors(
        Color(0xFFFFBD79), Color(0xFF6C3E0A), Color(0xFFFFDCB0), Color(0xFF24180E),
        Color(0xFF49311C), Color(0xFFFFE9D2), Color(0xFFDEC3A5), Color(0xFFB39878),
    ) else PaletteColors(
        Color(0xFF915000), Color(0xFFFFDCB0), Color(0xFF512B00), Color(0xFFFFF8EE),
        Color(0xFFFFE6C9), Color(0xFF321D0D), Color(0xFF725840), Color(0xFF927657),
    )
    ColorPalette.MONOCHROME -> if (dark) PaletteColors(
        Color(0xFFE8E8E8), Color(0xFF353535), Color(0xFFF0F0F0), Color(0xFF121212),
        Color(0xFF242424), Color(0xFFF0F0F0), Color(0xFFC4C4C4), Color(0xFF969696),
    ) else PaletteColors(
        Color(0xFF202020), Color(0xFFDDDDDD), Color(0xFF202020), Color(0xFFFAFAFA),
        Color(0xFFE6E6E6), Color(0xFF1C1C1C), Color(0xFF515151), Color(0xFF777777),
    )
}

/** Keep dark surfaces neutral while applying the palette to accents and action containers. */
fun paletteColorScheme(palette: ColorPalette, dark: Boolean): ColorScheme {
    val tones = colors(palette, dark)
    val surfaceTones = if (dark) colors(ColorPalette.MONOCHROME, true) else tones
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val monochrome = palette == ColorPalette.MONOCHROME
    return base.copy(
        primary = tones.primary,
        onPrimary = if (dark) surfaceTones.background else Color.White,
        primaryContainer = tones.container,
        onPrimaryContainer = tones.onContainer,
        inversePrimary = colors(palette, !dark).primary,
        secondary = tones.primary,
        onSecondary = if (dark) surfaceTones.background else Color.White,
        secondaryContainer = tones.container,
        onSecondaryContainer = tones.onContainer,
        tertiary = tones.primary,
        onTertiary = if (dark) surfaceTones.background else Color.White,
        tertiaryContainer = tones.container,
        onTertiaryContainer = tones.onContainer,
        background = surfaceTones.background,
        onBackground = surfaceTones.onSurface,
        surface = surfaceTones.background,
        onSurface = surfaceTones.onSurface,
        surfaceVariant = surfaceTones.surface,
        onSurfaceVariant = surfaceTones.onSurfaceVariant,
        surfaceTint = if (dark) Color.Transparent else tones.primary,
        surfaceDim = if (dark) surfaceTones.background else surfaceTones.surface,
        surfaceBright = if (dark) surfaceTones.surface else surfaceTones.background,
        surfaceContainerLowest = surfaceTones.background,
        surfaceContainerLow = lerp(surfaceTones.background, surfaceTones.surface, 0.45f),
        surfaceContainer = lerp(surfaceTones.background, surfaceTones.surface, 0.7f),
        surfaceContainerHigh = surfaceTones.surface,
        surfaceContainerHighest = lerp(surfaceTones.surface, surfaceTones.container, 0.2f),
        inverseSurface = surfaceTones.onSurface,
        inverseOnSurface = surfaceTones.background,
        outline = surfaceTones.outline,
        outlineVariant = lerp(surfaceTones.surface, surfaceTones.outline, 0.45f),
        error = if (monochrome) tones.primary else base.error,
        onError = if (monochrome) (if (dark) surfaceTones.background else Color.White) else base.onError,
        errorContainer = if (monochrome) tones.container else base.errorContainer,
        onErrorContainer = if (monochrome) tones.onContainer else base.onErrorContainer,
        primaryFixed = if (monochrome) Color(0xFFE0E0E0) else base.primaryFixed,
        primaryFixedDim = if (monochrome) Color(0xFFC4C4C4) else base.primaryFixedDim,
        onPrimaryFixed = if (monochrome) Color(0xFF202020) else base.onPrimaryFixed,
        onPrimaryFixedVariant = if (monochrome) Color(0xFF454545) else base.onPrimaryFixedVariant,
        secondaryFixed = if (monochrome) Color(0xFFE0E0E0) else base.secondaryFixed,
        secondaryFixedDim = if (monochrome) Color(0xFFC4C4C4) else base.secondaryFixedDim,
        onSecondaryFixed = if (monochrome) Color(0xFF202020) else base.onSecondaryFixed,
        onSecondaryFixedVariant = if (monochrome) Color(0xFF454545) else base.onSecondaryFixedVariant,
        tertiaryFixed = if (monochrome) Color(0xFFE0E0E0) else base.tertiaryFixed,
        tertiaryFixedDim = if (monochrome) Color(0xFFC4C4C4) else base.tertiaryFixedDim,
        onTertiaryFixed = if (monochrome) Color(0xFF202020) else base.onTertiaryFixed,
        onTertiaryFixedVariant = if (monochrome) Color(0xFF454545) else base.onTertiaryFixedVariant,
    )
}

@Composable
fun DialerTheme(palette: ColorPalette = ColorPalette.DEFAULT, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMonochrome provides (palette == ColorPalette.MONOCHROME)) {
        MaterialTheme(colorScheme = paletteColorScheme(palette, isSystemInDarkTheme()), content = content)
    }
}
