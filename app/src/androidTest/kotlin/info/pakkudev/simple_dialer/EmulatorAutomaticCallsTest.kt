package info.pakkudev.simple_dialer

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.model.CallStatus
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.platform.PlatformProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class EmulatorAutomaticCallsTest {
    @Test fun proximityAnswersAndSwitchesSpeaker() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("simulatedCall") == "true")
        val calls = GlobalContext.get().get<CallRepository>()
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first() }
        try {
            runBlocking { settings.setAutoAnswer(true); settings.setAutoSpeaker(true) }
            waitUntil { calls.calls.value.any { it.status == CallStatus.RINGING } }
            waitUntil { calls.calls.value.any { it.status == CallStatus.ACTIVE } }
            if (GlobalContext.get().get<PlatformProfile>().usesCallEndpoints) waitUntil { calls.audio.value.availableOutputs > 0 }
            if (calls.audio.value.earpieceAvailable) waitUntil { !calls.audio.value.speakerEnabled }
            else println("This emulator exposes no earpiece; near-to-ear routing requires a phone with an earpiece.")
            waitUntil { calls.audio.value.speakerEnabled }
        } finally {
            runBlocking { settings.setAutoAnswer(original.autoAnswer); settings.setAutoSpeaker(original.autoSpeaker) }
            InstrumentationRegistry.getInstrumentation().runOnMainSync { calls.calls.value.forEach { calls.end(it.id) } }
        }
        waitUntil { calls.calls.value.isEmpty() }
    }
    private fun waitUntil(predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 25_000
        while (!predicate()) {
            check(SystemClock.elapsedRealtime() < deadline) { "Timed out waiting for automatic call handling." }
            SystemClock.sleep(100)
        }
    }
}
