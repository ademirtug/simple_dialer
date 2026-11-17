package info.pakkudev.simple_dialer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.domain.model.IncomingCallStyle

@Composable
fun IncomingCallStylePicker(selected: IncomingCallStyle, onSelect: (IncomingCallStyle) -> Unit) {
    var preview by remember { mutableStateOf(false) }
    Text(stringResource(R.string.incoming_call_style), Modifier.padding(20.dp, 4.dp), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.incoming_style_description), Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(Modifier.selectableGroup()) {
        IncomingCallStyle.entries.forEach { style ->
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .selectable(selected == style, role = Role.RadioButton, onClick = { onSelect(style) })
                .padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButton(selected == style, onClick = null)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(style.labelResource()), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(style.hintResource()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    TextButton(onClick = { preview = true }, modifier = Modifier.padding(horizontal = 12.dp)) { Text(stringResource(R.string.incoming_style_preview)) }
    if (preview) {
        var generation by remember { mutableIntStateOf(0) }
        var lastAction by remember { mutableStateOf<Int?>(null) }
        AlertDialog(onDismissRequest = { preview = false }, title = { Text(stringResource(selected.labelResource())) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.incoming_preview_description))
                    key(selected, generation) {
                        IncomingCallControls(selected,
                            onAnswer = { lastAction = R.string.answer; generation++ },
                            onDecline = { lastAction = R.string.decline; generation++ })
                    }
                    lastAction?.let { Text(stringResource(R.string.incoming_preview_result, stringResource(it))) }
                }
            }, confirmButton = { TextButton(onClick = { preview = false }) { Text(stringResource(R.string.incoming_preview_close)) } })
    }
}
