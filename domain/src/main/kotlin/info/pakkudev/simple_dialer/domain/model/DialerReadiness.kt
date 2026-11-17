package info.pakkudev.simple_dialer.domain.model

enum class AccessState { GRANTED, NOT_REQUESTED, DENIED, REQUIRES_SETTINGS, UNSUPPORTED }
enum class DialerCapability { PHONE, DEFAULT_PHONE_APP, NOTIFICATIONS, LOCK_SCREEN, CONTACTS, HISTORY }

data class DialerReadiness(
    val phone: AccessState = AccessState.NOT_REQUESTED,
    val defaultPhoneApp: AccessState = AccessState.NOT_REQUESTED,
    val notifications: AccessState = AccessState.NOT_REQUESTED,
    val lockScreen: AccessState = AccessState.NOT_REQUESTED,
    val contacts: AccessState = AccessState.NOT_REQUESTED,
    val history: AccessState = AccessState.NOT_REQUESTED,
) {
    val canPlaceCalls get() = phone == AccessState.GRANTED && defaultPhoneApp == AccessState.GRANTED
    val canReadContacts get() = contacts == AccessState.GRANTED
    // A stale runtime grant after losing the role does not authorize call-log access.
    val canReadHistory get() = history == AccessState.GRANTED && defaultPhoneApp == AccessState.GRANTED
    val callAlertsReady get() = notifications == AccessState.GRANTED && lockScreen == AccessState.GRANTED
    operator fun get(capability: DialerCapability): AccessState = when (capability) {
        DialerCapability.PHONE -> phone
        DialerCapability.DEFAULT_PHONE_APP -> defaultPhoneApp
        DialerCapability.NOTIFICATIONS -> notifications
        DialerCapability.LOCK_SCREEN -> lockScreen
        DialerCapability.CONTACTS -> contacts
        DialerCapability.HISTORY -> when {
            defaultPhoneApp == AccessState.UNSUPPORTED -> AccessState.UNSUPPORTED
            defaultPhoneApp != AccessState.GRANTED -> AccessState.NOT_REQUESTED
            else -> history
        }
    }
}

/** A first request has no rationale either. Only a received denial can mark settings recovery. */
fun permissionAccessState(granted: Boolean, requested: Boolean, settingsRequired: Boolean): AccessState = when {
    granted -> AccessState.GRANTED
    settingsRequired -> AccessState.REQUIRES_SETTINGS
    requested -> AccessState.DENIED
    else -> AccessState.NOT_REQUESTED
}
