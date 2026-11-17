package info.pakkudev.simple_dialer.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import info.pakkudev.simple_dialer.ui.theme.paletteColorScheme

fun ColorPalette.labelResource() = when (this) {
    ColorPalette.OCEAN -> R.string.palette_ocean
    ColorPalette.FOREST -> R.string.palette_forest
    ColorPalette.ROSE -> R.string.palette_rose
    ColorPalette.SUNSET -> R.string.palette_sunset
    ColorPalette.MONOCHROME -> R.string.palette_monochrome
}

@Composable
fun PalettePicker(selected: ColorPalette, onSelect: (ColorPalette) -> Unit) {
    Text(stringResource(R.string.color_palette), Modifier.padding(20.dp, 4.dp), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.palette_description), Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(Modifier.selectableGroup()) {
        ColorPalette.entries.forEach { palette ->
            val preview = paletteColorScheme(palette, isSystemInDarkTheme())
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .selectable(selected == palette, role = Role.RadioButton, onClick = { onSelect(palette) })
                .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButton(selected == palette, onClick = null)
                Text(stringResource(palette.labelResource()), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(preview.primary, preview.primaryContainer, preview.surfaceContainerHigh).forEachIndexed { index, color ->
                        Surface(Modifier.size(22.dp).testTag("palette_swatch_${palette.name}_$index"),
                            shape = CircleShape, color = color) {}
                    }
                }
            }
        }
    }
}
