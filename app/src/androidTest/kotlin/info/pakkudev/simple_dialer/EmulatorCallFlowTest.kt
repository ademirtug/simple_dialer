package info.pakkudev.simple_dialer

import android.os.SystemClock
import android.content.Intent
import android.graphics.Bitmap
import info.pakkudev.simple_dialer.telecom.CallActivity
import java.io.File
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.model.CallStatus
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Opt-in test: the host must inject a simulated GSM call into a disposable emulator. */
@RunWith(AndroidJUnit4::class)
class EmulatorCallFlowTest {
    @Test fun simulatedIncomingCallCanBeObservedAnsweredRoutedAndEnded() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("simulatedCall") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val repository = GlobalContext.get().get<CallRepository>()
        waitUntil { repository.calls.value.any { it.status == CallStatus.RINGING } }
        val call = repository.calls.value.first { it.status == CallStatus.RINGING }
        var screen: ActivityScenario<CallActivity>? = null
        try {
            if (InstrumentationRegistry.getArguments().getString("captureScreens") == "true") {
                screen = ActivityScenario.launch(Intent(instrumentation.targetContext, CallActivity::class.java))
                capture("incoming.png")
            }
            instrumentation.runOnMainSync { repository.answer(call.id) }
            waitUntil { repository.calls.value.any { it.id == call.id && it.status == CallStatus.ACTIVE } }
            instrumentation.runOnMainSync { repository.setMuted(true) }
            waitUntil { repository.audio.value.muted }
            runBlocking { withContext(Dispatchers.Main.immediate) { repository.setSpeakerEnabled(true) } }
            waitUntil { repository.audio.value.speakerEnabled }
            if (InstrumentationRegistry.getArguments().getString("captureScreens") == "true") capture("ongoing.png")
            instrumentation.runOnMainSync { repository.playDtmf(call.id, '7'); repository.stopDtmf(call.id) }
            if (repository.calls.value.firstOrNull { it.id == call.id }?.canHold == true) {
                instrumentation.runOnMainSync { repository.setHeld(call.id, true) }
                waitUntil { repository.calls.value.any { it.id == call.id && it.status == CallStatus.HOLDING } }
                instrumentation.runOnMainSync { repository.setHeld(call.id, false) }
                waitUntil { repository.calls.value.any { it.id == call.id && it.status == CallStatus.ACTIVE } }
            }
        } finally {
            instrumentation.runOnMainSync { repository.end(call.id) }
            screen?.close()
        }
        waitUntil { repository.calls.value.none { it.id == call.id } }
    }

    private fun capture(name: String) {
        SystemClock.sleep(1_000)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cmd statusbar collapse")).use { it.readBytes() }
        SystemClock.sleep(500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun waitUntil(predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 25_000
        while (!predicate()) {
            check(SystemClock.elapsedRealtime() < deadline) { "Timed out waiting for the simulated call state." }
            SystemClock.sleep(100)
        }
    }
}
