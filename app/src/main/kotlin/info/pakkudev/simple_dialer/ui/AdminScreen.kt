package info.pakkudev.simple_dialer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.domain.model.*
import info.pakkudev.simple_dialer.ui.permissions.titleResource

fun CallControl.labelResource() = when (this) {
    CallControl.MUTE -> R.string.mute
    CallControl.KEYPAD -> R.string.keypad
    CallControl.SPEAKER -> R.string.speaker
    CallControl.ADD_CALL -> R.string.add_call
    CallControl.HOLD -> R.string.hold
    CallControl.SWAP -> R.string.swap_calls
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(preferences: DialerPreferences, access: DialerReadiness, proximityAvailable: Boolean, vm: MainViewModel,
                onBack: () -> Unit, onRequest: (DialerCapability) -> Unit, onSystemPhoneSettings: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.admin_settings)) }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            SectionTitle(R.string.appearance_title)
            PalettePicker(preferences.colorPalette, vm::setColorPalette)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle(R.string.call_handling_title)
            IncomingCallStylePicker(preferences.incomingCallStyle, vm::setIncomingCallStyle)
            AdminSwitch(R.string.auto_answer, R.string.auto_answer_description, preferences.autoAnswer, vm::setAutoAnswer, proximityAvailable)
            AdminSwitch(R.string.auto_speaker, R.string.auto_speaker_description, preferences.autoSpeaker, vm::setAutoSpeaker, proximityAvailable)
            if (!proximityAvailable) Text(stringResource(R.string.proximity_unavailable), Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle(R.string.visibility_title)
            AdminSwitch(R.string.show_contacts, null, preferences.showContacts, vm::setShowContacts)
            AdminSwitch(R.string.show_favorites, null, preferences.showFavorites, vm::setShowFavorites)
            AdminSwitch(R.string.show_recents, null, preferences.showRecents, vm::setShowRecents)
            AdminSwitch(R.string.show_contact_details, R.string.contact_details_description, preferences.showContactDetails, vm::setShowContactDetails)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle(R.string.call_controls_title)
            CallControl.entries.forEach { control ->
                AdminSwitch(control.labelResource(), null, control !in preferences.hiddenControls, { vm.setControlVisible(control, it) })
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            SectionTitle(R.string.permissions_title)
            DialerCapability.entries.forEach { capability ->
                val state = access[capability]
                ListItem(headlineContent = { Text(stringResource(capability.titleResource())) }, supportingContent = {
                    Text(stringResource(when (state) {
                        AccessState.GRANTED -> R.string.access_ready
                        AccessState.REQUIRES_SETTINGS -> R.string.access_settings_needed
                        AccessState.UNSUPPORTED -> R.string.capability_unavailable
                        AccessState.DENIED -> R.string.access_declined
                        AccessState.NOT_REQUESTED -> R.string.access_not_set_up
                    }))
                }, trailingContent = {
                    if (state != AccessState.GRANTED && state != AccessState.UNSUPPORTED) TextButton(onClick = { onRequest(capability) }) { Text(stringResource(R.string.set_up_access)) }
                })
            }
            if (access.defaultPhoneApp == AccessState.GRANTED && !access.callAlertsReady) {
                Text(stringResource(R.string.call_alerts_need_attention), Modifier.padding(20.dp))
                TextButton(onClick = onSystemPhoneSettings, modifier = Modifier.padding(horizontal = 12.dp)) { Text(stringResource(R.string.choose_system_phone)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: Int) { Text(stringResource(title), Modifier.padding(20.dp, 12.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }

@Composable
private fun AdminSwitch(title: Int, description: Int?, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(headlineContent = { Text(stringResource(title)) }, supportingContent = description?.let { { Text(stringResource(it)) } },
        trailingContent = { Switch(checked, null, enabled = enabled) },
        modifier = Modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange))
}
