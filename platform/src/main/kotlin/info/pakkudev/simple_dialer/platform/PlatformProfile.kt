package info.pakkudev.simple_dialer.platform

import android.content.Context
import android.os.Build

enum class DialerRoleApi { TELECOM, ROLE_MANAGER }

class PlatformProfile private constructor(val sdk: Int) {
    val dialerRoleApi: DialerRoleApi get() = when {
        sdk >= 29 -> DialerRoleApi.ROLE_MANAGER
        else -> DialerRoleApi.TELECOM
    }
    val usesCallEndpoints get() = sdk >= 34
    val usesLockScreenMethods get() = sdk >= 27
    val needsNotificationPermission get() = sdk >= 33
    val usesNotificationChannels get() = sdk >= 26
    val needsFullScreenSettings get() = sdk >= 34
    val supportsAtomicComponentUpdates get() = sdk >= 33

    companion object {
        fun forSdk(sdk: Int): PlatformProfile {
            require(sdk >= 23) { "Android 6.0 (API 23) is the oldest supported platform." }
            return PlatformProfile(sdk)
        }
        fun current(): PlatformProfile = forSdk(Build.VERSION.SDK_INT)
    }
}

/** This factory is called once by the app's Koin module. */
@Suppress("NewApi")
fun createDefaultDialerAccess(context: Context, profile: PlatformProfile): DefaultDialerAccess =
    when (profile.dialerRoleApi) {
        DialerRoleApi.ROLE_MANAGER -> RoleManagerDialerAccess(context)
        DialerRoleApi.TELECOM -> TelecomDialerAccess(context)
    }
