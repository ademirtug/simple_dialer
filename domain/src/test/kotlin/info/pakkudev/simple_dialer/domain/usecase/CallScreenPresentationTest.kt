package info.pakkudev.simple_dialer.domain.usecase

import info.pakkudev.simple_dialer.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CallScreenPresentationTest {
    private val policy = CallScreenPresentation()
    private val dialing = ActiveCall("one", "555", "Caller", CallStatus.DIALING)
    private fun ended(reason: CallEndReason) = dialing.copy(status = CallStatus.DISCONNECTED, disconnectInfo = CallDisconnectInfo(reason))

    @Test fun `busy feedback survives call removal and expires without being restarted`() {
        policy.update(listOf(dialing), 0)
        assertEquals(5_000L, policy.update(listOf(ended(CallEndReason.BUSY)), 100).closeAfterMillis)
        assertEquals(CallEndReason.BUSY, policy.update(emptyList(), 500).calls.single().disconnectInfo?.reason)
        assertEquals(1L, policy.update(emptyList(), 5_099).closeAfterMillis)
        assertTrue(policy.update(emptyList(), 5_100).calls.isEmpty())
        assertTrue(policy.update(emptyList(), 10_000).calls.isEmpty())
    }

    @Test fun `terminal record preserves busy cause when raw disconnected update is skipped`() {
        policy.update(listOf(dialing), 0)
        val result = policy.update(emptyList(), 100, ended(CallEndReason.BUSY))
        assertEquals(CallEndReason.BUSY, result.calls.single().disconnectInfo?.reason)
        assertEquals(5_000L, result.closeAfterMillis)
    }

    @Test fun `ordinary remote hangup is visible briefly`() {
        policy.update(listOf(dialing.copy(status = CallStatus.ACTIVE)), 0)
        assertEquals(2_000L, policy.update(listOf(ended(CallEndReason.REMOTE)), 100).closeAfterMillis)
        assertTrue(policy.update(emptyList(), 2_100).calls.isEmpty())
    }

    @Test fun `local hangup stays immediate even with a remote failure code`() {
        policy.update(listOf(dialing), 0)
        policy.localEndRequested("one")
        assertTrue(policy.update(emptyList(), 100, ended(CallEndReason.BUSY)).calls.isEmpty())
    }

    @Test fun `failed local hangup does not suppress a later remote error`() {
        policy.update(listOf(dialing), 0)
        policy.localEndRequested("one")
        policy.localEndFailed("one")
        assertEquals(5_000L, policy.update(listOf(ended(CallEndReason.ERROR)), 100).closeAfterMillis)
    }

    @Test fun `local cancel and missed calls do not linger`() {
        listOf(CallEndReason.LOCAL, CallEndReason.CANCELED, CallEndReason.MISSED).forEach { reason ->
            val next = CallScreenPresentation()
            next.update(listOf(dialing), 0)
            assertTrue(next.update(listOf(ended(reason)), 100).calls.isEmpty())
        }
    }

    @Test fun `other active call takes priority over terminal feedback`() {
        val other = dialing.copy(id = "two", status = CallStatus.ACTIVE)
        policy.update(listOf(dialing, other), 0)
        val result = policy.update(listOf(ended(CallEndReason.BUSY), other), 100)
        assertEquals(listOf(other), result.calls)
        assertNull(result.closeAfterMillis)
    }

    @Test fun `new ringing call replaces busy feedback and does not inherit its deadline`() {
        policy.update(listOf(dialing), 0)
        policy.update(listOf(ended(CallEndReason.BUSY)), 100)
        val incoming = dialing.copy(id = "two", status = CallStatus.RINGING)
        assertEquals(listOf(incoming), policy.update(listOf(incoming), 1_000).calls)
        assertEquals(listOf(incoming), policy.update(listOf(incoming), 6_000).calls)
    }

    @Test fun `late failure details extend feedback from original disconnect time only`() {
        policy.update(listOf(dialing), 0)
        policy.update(listOf(ended(CallEndReason.OTHER)), 100)
        assertEquals(4_000L, policy.update(listOf(ended(CallEndReason.BUSY)), 1_100).closeAfterMillis)
        assertTrue(policy.update(emptyList(), 5_100).calls.isEmpty())
    }

    @Test fun `fresh screen never resurrects an unrelated old terminal record`() {
        assertTrue(policy.update(emptyList(), 0, ended(CallEndReason.BUSY)).calls.isEmpty())
    }
}
