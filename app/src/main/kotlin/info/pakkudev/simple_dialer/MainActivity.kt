package info.pakkudev.simple_dialer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.core.app.ActivityCompat
import info.pakkudev.simple_dialer.domain.model.AccessState
import info.pakkudev.simple_dialer.domain.model.DialerCapability
import info.pakkudev.simple_dialer.domain.repository.DialerReadinessRepository
import info.pakkudev.simple_dialer.platform.DefaultDialerAccess
import info.pakkudev.simple_dialer.platform.FullScreenCallAccess
import info.pakkudev.simple_dialer.platform.NotificationAccess
import info.pakkudev.simple_dialer.platform.RuntimePermissions
import info.pakkudev.simple_dialer.data.call.ProximityMonitor
import info.pakkudev.simple_dialer.domain.usecase.DialNumber
import info.pakkudev.simple_dialer.telecom.CallActivity
import info.pakkudev.simple_dialer.ui.DialerApp
import info.pakkudev.simple_dialer.ui.MainViewModel
import info.pakkudev.simple_dialer.ui.permissions.PermissionPrompt
import info.pakkudev.simple_dialer.ui.permissions.PermissionPromptAction
import info.pakkudev.simple_dialer.ui.permissions.PermissionPromptDialog
import info.pakkudev.simple_dialer.ui.theme.DialerTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModel()
    private val readiness: DialerReadinessRepository by inject()
    private val permissions: RuntimePermissions by inject()
    private val dialer: DefaultDialerAccess by inject()
    private val notifications: NotificationAccess by inject()
    private val fullScreen: FullScreenCallAccess by inject()
    private val proximity: ProximityMonitor by inject()
    private var externalNumber by mutableStateOf<String?>(null)
    private var openDialpadRequest by mutableStateOf(0)
    private var prompt by mutableStateOf<PermissionPrompt?>(null)
    private var pendingPermission: String? = null

    private val runtimeRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingPermission?.let { permission ->
            permissions.recordResult(permission, granted, ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
        }
        pendingPermission = null
        readiness.refresh()
        // Deliberately do not dial, chain another prompt, or open Settings after a result.
    }
    private val systemRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { readiness.refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingPermission = savedInstanceState?.getString("pending_permission")
        prompt = savedInstanceState?.getString("prompt_capability")?.let { capability ->
            savedInstanceState.getString("prompt_action")?.let { action ->
                PermissionPrompt(DialerCapability.valueOf(capability), PermissionPromptAction.valueOf(action))
            }
        }
        enableEdgeToEdge()
        externalNumber = intent.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart
        if (intent.getBooleanExtra(EXTRA_OPEN_DIALPAD, false)) openDialpadRequest++
        readiness.refresh()
        // Some vendor frame schedulers strand Compose updates after returning from Home.
        // Request a native frame when provider results change, only while the screen is started.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.contacts.combine(vm.recents) { _, _ -> Unit }.collect {
                    window.decorView.postInvalidateOnAnimation()
                }
            }
        }
        setContent {
            val preferences by vm.preferences.collectAsStateWithLifecycle()
            DialerTheme(palette = preferences.colorPalette) {
                DialerApp(
                    viewModel = vm, externalNumber = externalNumber, openDialpadRequest = openDialpadRequest,
                    proximityAvailable = proximity.available,
                    onDial = { number ->
                        val current = readiness.refresh()
                        when {
                            DialNumber.normalizeNumber(number) == null -> vm.dial(number)
                            current.defaultPhoneApp != AccessState.GRANTED -> requestAccess(DialerCapability.DEFAULT_PHONE_APP)
                            current.phone != AccessState.GRANTED -> requestAccess(DialerCapability.PHONE)
                            else -> vm.dial(number)
                        }
                    },
                    onRequestAccess = ::requestAccess,
                    onSystemPhoneSettings = { openSystem(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) },
                    onReturnToCall = { startActivity(Intent(this, CallActivity::class.java)) },
                    onCreateContact = { number -> openSystem(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).putExtra(ContactsContract.Intents.Insert.PHONE, number)) },
                    onEditContact = { lookupUri ->
                        openSystem(Intent(Intent.ACTION_EDIT)
                            .setDataAndType(Uri.parse(lookupUri), ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                            .putExtra("finishActivityOnSaveCompleted", true))
                    },
                )
                prompt?.let { current ->
                    PermissionPromptDialog(current, onDismiss = { prompt = null }, onConfirm = { confirmPrompt(current) })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Editors do not consistently return RESULT_OK or notify the same provider URI.
        vm.refreshContacts()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalNumber = intent.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart
        if (intent.getBooleanExtra(EXTRA_OPEN_DIALPAD, false)) openDialpadRequest++
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending_permission", pendingPermission)
        prompt?.let {
            outState.putString("prompt_capability", it.capability.name)
            outState.putString("prompt_action", it.action.name)
        }
        super.onSaveInstanceState(outState)
    }

    private fun requestAccess(capability: DialerCapability) {
        if (pendingPermission != null) return
        val current = readiness.refresh()
        if (capability == DialerCapability.HISTORY && current.defaultPhoneApp != AccessState.GRANTED) {
            requestAccess(DialerCapability.DEFAULT_PHONE_APP)
            return
        }
        when (current[capability]) {
            AccessState.GRANTED -> return
            AccessState.UNSUPPORTED -> { vm.reportFailure(getString(R.string.capability_unavailable)); return }
            else -> Unit
        }
        val action = when (capability) {
            DialerCapability.DEFAULT_PHONE_APP -> PermissionPromptAction.DEFAULT_PHONE_APP
            DialerCapability.LOCK_SCREEN -> PermissionPromptAction.LOCK_SCREEN_SETTINGS
            DialerCapability.NOTIFICATIONS -> {
                val permission = notifications.runtimePermission
                if (permission != null && permissions.state(permission) in setOf(AccessState.NOT_REQUESTED, AccessState.DENIED)) PermissionPromptAction.RUNTIME
                else PermissionPromptAction.NOTIFICATION_SETTINGS
            }
            else -> if (current[capability] == AccessState.REQUIRES_SETTINGS) PermissionPromptAction.APP_SETTINGS else PermissionPromptAction.RUNTIME
        }
        prompt = PermissionPrompt(capability, action)
    }

    private fun confirmPrompt(current: PermissionPrompt) {
        prompt = null
        when (current.action) {
            PermissionPromptAction.RUNTIME -> {
                val permission = runtimePermission(current.capability) ?: return
                // Recheck immediately: permissions can change while the explanation is open.
                when (permissions.state(permission)) {
                    AccessState.GRANTED -> readiness.refresh()
                    AccessState.REQUIRES_SETTINGS -> { prompt = current.copy(action = PermissionPromptAction.APP_SETTINGS) }
                    else -> try {
                        permissions.recordRequest(permission)
                        pendingPermission = permission
                        runtimeRequest.launch(permission)
                    } catch (error: RuntimeException) {
                        pendingPermission = null
                        vm.reportFailure(error.message.orEmpty())
                    }
                }
            }
            PermissionPromptAction.DEFAULT_PHONE_APP -> dialer.requestIntent()?.let(::openSystem)
                ?: vm.reportFailure(getString(R.string.default_unavailable))
            PermissionPromptAction.NOTIFICATION_SETTINGS -> openSystem(notifications.settingsIntent())
            PermissionPromptAction.LOCK_SCREEN_SETTINGS -> fullScreen.settingsIntent()?.let(::openSystem)
            PermissionPromptAction.APP_SETTINGS -> openSystem(appSettingsIntent())
        }
    }

    private fun runtimePermission(capability: DialerCapability): String? = when (capability) {
        DialerCapability.PHONE -> Manifest.permission.CALL_PHONE
        DialerCapability.CONTACTS -> Manifest.permission.READ_CONTACTS
        DialerCapability.HISTORY -> Manifest.permission.READ_CALL_LOG
        DialerCapability.NOTIFICATIONS -> notifications.runtimePermission
        else -> null
    }

    private fun appSettingsIntent() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

    private fun openSystem(intent: Intent) {
        try { systemRequest.launch(intent) }
        catch (error: RuntimeException) {
            if (intent.action?.startsWith("android.settings.") == true && intent.action != Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                try { systemRequest.launch(appSettingsIntent()) }
                catch (fallbackError: RuntimeException) { vm.reportFailure(fallbackError.message.orEmpty()) }
            } else vm.reportFailure(error.message.orEmpty())
        }
    }

    companion object { const val EXTRA_OPEN_DIALPAD = "open_dialpad" }
}
