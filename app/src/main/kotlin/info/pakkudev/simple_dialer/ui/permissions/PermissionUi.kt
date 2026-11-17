package info.pakkudev.simple_dialer.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.domain.model.AccessState
import info.pakkudev.simple_dialer.domain.model.DialerCapability

enum class PermissionPromptAction { RUNTIME, DEFAULT_PHONE_APP, APP_SETTINGS, NOTIFICATION_SETTINGS, LOCK_SCREEN_SETTINGS }
data class PermissionPrompt(val capability: DialerCapability, val action: PermissionPromptAction)

fun DialerCapability.titleResource(): Int = when (this) {
    DialerCapability.PHONE -> R.string.phone_access_title
    DialerCapability.DEFAULT_PHONE_APP -> R.string.default_phone_title
    DialerCapability.NOTIFICATIONS -> R.string.call_alerts_title
    DialerCapability.LOCK_SCREEN -> R.string.lock_screen_title
    DialerCapability.CONTACTS -> R.string.contacts
    DialerCapability.HISTORY -> R.string.recents
}

fun DialerCapability.descriptionResource(): Int = when (this) {
    DialerCapability.PHONE -> R.string.phone_access_description
    DialerCapability.DEFAULT_PHONE_APP -> R.string.default_phone_description
    DialerCapability.NOTIFICATIONS -> R.string.call_alerts_description
    DialerCapability.LOCK_SCREEN -> R.string.lock_screen_description
    DialerCapability.CONTACTS -> R.string.contacts_access_description
    DialerCapability.HISTORY -> R.string.history_access_description
}

@Composable
fun PermissionPromptDialog(prompt: PermissionPrompt, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val settings = prompt.action in setOf(PermissionPromptAction.APP_SETTINGS, PermissionPromptAction.NOTIFICATION_SETTINGS, PermissionPromptAction.LOCK_SCREEN_SETTINGS)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(prompt.capability.titleResource())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(prompt.capability.descriptionResource()))
                if (settings) Text(stringResource(R.string.settings_recovery_description))
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(if (settings) R.string.open_app_settings else R.string.continue_setup)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) } },
    )
}

@Composable
fun CapabilityCard(capability: DialerCapability, state: AccessState, onRequest: () -> Unit, prerequisite: String? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(capability.titleResource()), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(capability.descriptionResource()))
            Text(stringResource(when (state) {
                AccessState.GRANTED -> R.string.access_ready
                AccessState.NOT_REQUESTED -> R.string.access_not_set_up
                AccessState.DENIED -> R.string.access_declined
                AccessState.REQUIRES_SETTINGS -> R.string.access_settings_needed
                AccessState.UNSUPPORTED -> R.string.capability_unavailable
            }), style = MaterialTheme.typography.labelLarge)
            prerequisite?.let { Text(it) }
            if (state != AccessState.GRANTED && state != AccessState.UNSUPPORTED) {
                OutlinedButton(onClick = onRequest, enabled = prerequisite == null, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(if (state == AccessState.REQUIRES_SETTINGS) R.string.open_app_settings else R.string.set_up_access))
                }
            }
        }
    }
}
