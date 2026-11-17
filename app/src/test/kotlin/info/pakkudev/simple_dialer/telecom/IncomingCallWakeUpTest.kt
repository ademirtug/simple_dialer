package info.pakkudev.simple_dialer.telecom

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncomingCallWakeUpTest {
    private class Screen : IncomingCallScreenWakeLock {
        override var isInteractive = false
        var wakes = 0
        var held = false
        override fun wake() { wakes++; held = true }
        override fun release() { held = false }
    }

    @Test fun `fallback waits for full screen notification and retries the call UI`() = runTest {
        val screen = Screen()
        var opens = 0
        val wakeUp = IncomingCallWakeUp(this, screen) { assertTrue(screen.held); opens++ }
        wakeUp.update(setOf("incoming"))
        advanceTimeBy(999)
        assertEquals(0, screen.wakes)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, screen.wakes)
        assertEquals(1, opens)
        wakeUp.update(emptySet())
        assertFalse(screen.held)
    }

    @Test fun `successful platform wake skips fallback`() = runTest {
        val screen = Screen()
        val wakeUp = IncomingCallWakeUp(this, screen) { fail("Already awake") }
        wakeUp.update(setOf("incoming"))
        screen.isInteractive = true
        advanceTimeBy(1_001)
        assertEquals(0, screen.wakes)
    }

    @Test fun `answer or disconnect before deadline cancels wake`() = runTest {
        val screen = Screen()
        val wakeUp = IncomingCallWakeUp(this, screen) { fail("Call no longer ringing") }
        wakeUp.update(setOf("incoming"))
        advanceTimeBy(500)
        wakeUp.update(emptySet())
        advanceTimeBy(1_001)
        assertEquals(0, screen.wakes)
    }

    @Test fun `caller updates do not postpone the fallback or repeatedly wake`() = runTest {
        val screen = Screen()
        var opens = 0
        val wakeUp = IncomingCallWakeUp(this, screen) { opens++ }
        wakeUp.update(setOf("incoming"))
        advanceTimeBy(600)
        wakeUp.update(setOf("incoming"))
        advanceTimeBy(401)
        assertEquals(1, opens)
        wakeUp.update(setOf("incoming"))
        advanceTimeBy(2_000)
        assertEquals(1, screen.wakes)
        wakeUp.close()
        assertFalse(screen.held)
    }

    @Test fun `service shutdown cancels pending wake`() = runTest {
        val screen = Screen()
        val wakeUp = IncomingCallWakeUp(this, screen) { fail("Service stopped") }
        wakeUp.update(setOf("incoming"))
        advanceTimeBy(500)
        wakeUp.close()
        advanceTimeBy(1_001)
        assertEquals(0, screen.wakes)
    }

    @Test fun `unlocked interactive phone is not interrupted`() = runTest {
        val screen = Screen().apply { isInteractive = true }
        val wakeUp = IncomingCallWakeUp(this, screen) { fail("Phone in use") }
        wakeUp.update(setOf("incoming"))
        advanceTimeBy(1_001)
        assertEquals(0, screen.wakes)
    }

    @Test fun `a later call can wake after the previous call ended`() = runTest {
        val screen = Screen()
        var opens = 0
        val wakeUp = IncomingCallWakeUp(this, screen) { opens++ }
        wakeUp.update(setOf("first"))
        advanceTimeBy(1_001)
        wakeUp.update(emptySet())
        assertFalse(screen.held)
        wakeUp.update(setOf("second"))
        advanceTimeBy(1_001)
        assertEquals(2, opens)
        wakeUp.close()
    }
}
