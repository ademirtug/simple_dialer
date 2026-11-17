package info.pakkudev.simple_dialer.domain.model

import org.junit.Assert.*
import org.junit.Test

class DialerReadinessTest {
    @Test fun `optional permissions never gate manual calling`() {
        val ready = DialerReadiness(phone = AccessState.GRANTED, defaultPhoneApp = AccessState.GRANTED)
        assertTrue(ready.canPlaceCalls)
        assertFalse(ready.canReadContacts)
        assertFalse(ready.canReadHistory)
        assertFalse(ready.callAlertsReady)
    }
    @Test fun `losing the default role disables history despite a stale permission grant`() {
        val ready = DialerReadiness(defaultPhoneApp = AccessState.GRANTED, history = AccessState.GRANTED)
        assertTrue(ready.canReadHistory)
        assertFalse(ready.copy(defaultPhoneApp = AccessState.NOT_REQUESTED).canReadHistory)
    }
    @Test fun `first request is not classified as permanently denied`() {
        assertEquals(AccessState.NOT_REQUESTED, permissionAccessState(false, false, false))
        assertEquals(AccessState.DENIED, permissionAccessState(false, true, false))
        assertEquals(AccessState.REQUIRES_SETTINGS, permissionAccessState(false, true, true))
        assertEquals(AccessState.GRANTED, permissionAccessState(true, true, true))
    }
}
