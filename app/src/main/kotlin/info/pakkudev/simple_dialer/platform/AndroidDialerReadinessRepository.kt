package info.pakkudev.simple_dialer.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import info.pakkudev.simple_dialer.domain.model.AccessState
import info.pakkudev.simple_dialer.domain.model.DialerReadiness
import info.pakkudev.simple_dialer.domain.repository.DialerReadinessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PhoneHardware { fun supportsCalls(): Boolean }

class AndroidPhoneHardware(private val context: Context) : PhoneHardware {
    override fun supportsCalls() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
}

class AndroidDialerReadinessRepository(
    private val permissions: RuntimePermissions,
    private val dialer: DefaultDialerAccess,
    private val notifications: NotificationAccess,
    private val fullScreen: FullScreenCallAccess,
    private val hardware: PhoneHardware,
    private val channels: CallNotificationChannels,
) : DialerReadinessRepository {
    private val mutableReadiness = MutableStateFlow(DialerReadiness())
    override val readiness = mutableReadiness.asStateFlow()

    override fun refresh(): DialerReadiness {
        channels.create()
        val notificationPermission = notifications.runtimePermission?.let(permissions::state) ?: AccessState.GRANTED
        val phoneSupported = hardware.supportsCalls()
        val state = DialerReadiness(
            phone = if (phoneSupported) permissions.state(Manifest.permission.CALL_PHONE) else AccessState.UNSUPPORTED,
            defaultPhoneApp = when {
                !phoneSupported || !dialer.supported -> AccessState.UNSUPPORTED
                dialer.isHeld() -> AccessState.GRANTED
                else -> AccessState.NOT_REQUESTED
            },
            notifications = when {
                notificationPermission != AccessState.GRANTED -> notificationPermission
                !notifications.alertsEnabled() -> AccessState.REQUIRES_SETTINGS
                else -> AccessState.GRANTED
            },
            lockScreen = if (fullScreen.isAllowed()) AccessState.GRANTED else AccessState.REQUIRES_SETTINGS,
            contacts = permissions.state(Manifest.permission.READ_CONTACTS),
            history = permissions.state(Manifest.permission.READ_CALL_LOG),
        )
        mutableReadiness.value = state
        return state
    }
}
