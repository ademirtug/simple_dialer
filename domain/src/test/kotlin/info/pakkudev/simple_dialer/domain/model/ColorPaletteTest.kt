package info.pakkudev.simple_dialer.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPaletteTest {
    @Test fun `new preferences and missing or unknown saved values default to ocean`() {
        assertEquals(ColorPalette.OCEAN, DialerPreferences().colorPalette)
        assertEquals(ColorPalette.OCEAN, ColorPalette.fromStored(null))
        assertEquals(ColorPalette.OCEAN, ColorPalette.fromStored("removed-palette"))
    }

    @Test fun `changing the default preserves every explicitly saved palette`() {
        ColorPalette.entries.forEach { assertEquals(it, ColorPalette.fromStored(it.name)) }
    }
}
