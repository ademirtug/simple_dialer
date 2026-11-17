package info.pakkudev.simple_dialer.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformProfileTest {
    @Test fun `default dialer strategies match public API boundaries`() {
        listOf(23, 26, 28).forEach { assertEquals(DialerRoleApi.TELECOM, PlatformProfile.forSdk(it).dialerRoleApi) }
        listOf(29, 33, 34, 36).forEach { assertEquals(DialerRoleApi.ROLE_MANAGER, PlatformProfile.forSdk(it).dialerRoleApi) }
    }

    @Test fun `independent capability boundaries are retained`() {
        assertFalse(PlatformProfile.forSdk(25).usesNotificationChannels)
        assertTrue(PlatformProfile.forSdk(26).usesNotificationChannels)
        assertFalse(PlatformProfile.forSdk(26).usesLockScreenMethods)
        assertTrue(PlatformProfile.forSdk(27).usesLockScreenMethods)
        assertFalse(PlatformProfile.forSdk(32).needsNotificationPermission)
        assertTrue(PlatformProfile.forSdk(33).needsNotificationPermission)
        assertFalse(PlatformProfile.forSdk(33).usesCallEndpoints)
        assertTrue(PlatformProfile.forSdk(34).usesCallEndpoints)
        assertFalse(PlatformProfile.forSdk(33).needsFullScreenSettings)
        assertTrue(PlatformProfile.forSdk(34).needsFullScreenSettings)
        assertFalse(PlatformProfile.forSdk(32).supportsAtomicComponentUpdates)
        assertTrue(PlatformProfile.forSdk(33).supportsAtomicComponentUpdates)
    }

    @Test fun `versions below the supported minimum fail explicitly`() {
        listOf(15, 16, 19, 21, 22).forEach { sdk ->
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                PlatformProfile.forSdk(sdk)
            }
        }
    }
}
