package info.pakkudev.simple_dialer.platform

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test

class CombinedFullScreenCallAccessTest {
    private class Access(var allowed: Boolean) : FullScreenCallAccess {
        var settingsOpened = false
        override fun isAllowed() = allowed
        override fun settingsIntent(): Intent? { settingsOpened = true; return null }
    }

    @Test fun `vendor denial is not hidden by Android full screen permission`() {
        val platform = Access(true)
        val vendor = Access(false)
        val access = CombinedFullScreenCallAccess(platform, vendor)
        assertFalse(access.isAllowed())
        access.settingsIntent()
        assertTrue(vendor.settingsOpened)
        assertFalse(platform.settingsOpened)
        vendor.allowed = true
        assertTrue(access.isAllowed())
    }

    @Test fun `Android full screen denial is fixed before vendor restrictions`() {
        val platform = Access(false)
        val vendor = Access(false)
        val access = CombinedFullScreenCallAccess(platform, vendor)
        assertFalse(access.isAllowed())
        access.settingsIntent()
        assertTrue(platform.settingsOpened)
        assertFalse(vendor.settingsOpened)
    }
}
