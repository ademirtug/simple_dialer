package info.pakkudev.simple_dialer.platform

import android.Manifest
import android.content.Intent
import info.pakkudev.simple_dialer.domain.model.AccessState
import org.junit.Assert.*
import org.junit.Test

class AndroidDialerReadinessRepositoryTest {
    private val permissions = FakePermissions()
    private var role = true
    private var alertEnabled = true
    private var fullScreenAllowed = true
    private var phoneHardware = true
    private val repository = AndroidDialerReadinessRepository(permissions, object : DefaultDialerAccess {
        override val supported = true
        override fun isHeld() = role
        override fun requestIntent(): Intent? = null
    }, object : NotificationAccess {
        override val runtimePermission = Manifest.permission.POST_NOTIFICATIONS
        override fun alertsEnabled() = alertEnabled
        override fun settingsIntent(): Intent = error("Not used")
    }, object : FullScreenCallAccess {
        override fun isAllowed() = fullScreenAllowed
        override fun settingsIntent(): Intent? = null
    }, object : PhoneHardware { override fun supportsCalls() = phoneHardware }, object : CallNotificationChannels { override fun create() = Unit })

    @Test fun `notification permission alone does not make a blocked channel ready`() {
        alertEnabled = false
        val state = repository.refresh()
        assertEquals(AccessState.REQUIRES_SETTINGS, state.notifications)
        assertTrue(state.canPlaceCalls)
        assertFalse(state.callAlertsReady)
    }
    @Test fun `runtime denial is not hidden by enabled app notifications`() {
        permissions.values[Manifest.permission.POST_NOTIFICATIONS] = AccessState.DENIED
        assertEquals(AccessState.DENIED, repository.refresh().notifications)
    }
    @Test fun `role removal immediately removes history readiness`() {
        assertTrue(repository.refresh().canReadHistory)
        role = false
        assertFalse(repository.refresh().canReadHistory)
        assertFalse(repository.readiness.value.canPlaceCalls)
    }
    @Test fun `lock screen access is independent of outgoing calls`() {
        fullScreenAllowed = false
        val state = repository.refresh()
        assertTrue(state.canPlaceCalls)
        assertFalse(state.callAlertsReady)
        assertEquals(AccessState.REQUIRES_SETTINGS, state.lockScreen)
    }
    @Test fun `devices without telephony retain contacts access`() {
        phoneHardware = false
        val state = repository.refresh()
        assertEquals(AccessState.UNSUPPORTED, state.phone)
        assertTrue(state.canReadContacts)
    }
    private class FakePermissions : RuntimePermissions {
        val values = mutableMapOf<String, AccessState>()
        override fun state(permission: String) = values[permission] ?: AccessState.GRANTED
        override fun recordRequest(permission: String) = Unit
        override fun recordResult(permission: String, granted: Boolean, canRequestAgain: Boolean) = Unit
    }
}
