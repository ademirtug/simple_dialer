package info.pakkudev.simple_dialer

import android.app.KeyguardManager
import android.graphics.Bitmap
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import info.pakkudev.simple_dialer.domain.model.CallStatus
import info.pakkudev.simple_dialer.domain.model.IncomingCallStyle
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.telecom.CallActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Host must lock a disposable emulator and inject a simulated GSM call. Never launches the UI itself. */
@RunWith(AndroidJUnit4::class)
class EmulatorLockedCallTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun incomingCallWakesScreenAndCanBeAnsweredWithoutUnlocking() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("simulatedCall") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val power = context.getSystemService(PowerManager::class.java)
        val calls = GlobalContext.get().get<CallRepository>()
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first() }
        val style = IncomingCallStyle.fromStored(InstrumentationRegistry.getArguments().getString("incomingStyle"))
        try {
            runBlocking { settings.setAutoAnswer(false); settings.setAutoSpeaker(false); settings.setIncomingCallStyle(style) }
            assertTrue("Host must configure a secure emulator lock", keyguard.isDeviceSecure)
            assertTrue("Phone must start locked", keyguard.isKeyguardLocked)
            assertTrue("Phone must start with screen off", !power.isInteractive)
            File(context.getExternalFilesDir(null), "locked-call-ready").writeText("ready")
            waitUntil { calls.calls.value.any { it.status == CallStatus.RINGING } }
            waitUntil { power.isInteractive && callScreenResumed() }
            // Host uses a one-second screen timeout. Ringing must keep the display awake.
            SystemClock.sleep(1_500)
            assertTrue("Ringing call must keep the display on", power.isInteractive)
            assertTrue("Showing incoming call must retain keyguard", keyguard.isKeyguardLocked)
            assertTrue("Device must remain credential-locked", keyguard.isDeviceLocked)
            compose.waitUntil(5_000) {
                if (style == IncomingCallStyle.BUTTONS) compose.onAllNodesWithContentDescription(context.getString(R.string.answer)).fetchSemanticsNodes().isNotEmpty()
                else compose.onAllNodesWithTag("incoming_call_handle").fetchSemanticsNodes().isNotEmpty()
            }
            if (style == IncomingCallStyle.BUTTONS) compose.onNodeWithContentDescription(context.getString(R.string.answer)).assertIsDisplayed()
            else compose.onNodeWithTag("incoming_call_handle").assertIsDisplayed()
            compose.waitForIdle()
            if (InstrumentationRegistry.getArguments().getString("captureScreens") == "true") {
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    File(context.getExternalFilesDir(null), "incoming-locked-${style.name.lowercase()}.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
            }
            if (style == IncomingCallStyle.BUTTONS) compose.onNodeWithContentDescription(context.getString(R.string.answer)).performClick()
            else {
                val horizontal = style == IncomingCallStyle.HORIZONTAL_SLIDE
                val track = compose.onNodeWithTag("incoming_call_track").fetchSemanticsNode().boundsInRoot
                val handle = compose.onNodeWithTag("incoming_call_handle").fetchSemanticsNode().boundsInRoot
                val distance = (if (horizontal) track.width - handle.width else track.height - handle.height) / 2
                compose.onNodeWithTag("incoming_call_handle").performTouchInput {
                    swipe(center, center + if (horizontal) Offset(distance, 0f) else Offset(0f, -distance), durationMillis = 400)
                }
            }
            waitUntil { calls.calls.value.any { it.status == CallStatus.ACTIVE } }
            assertTrue("Answering must not dismiss keyguard", keyguard.isKeyguardLocked)
            assertTrue("Answering must not unlock device", keyguard.isDeviceLocked)
            assertTrue("Ongoing call must stay visible", callScreenResumed())
            compose.onNodeWithContentDescription(context.getString(R.string.end_call)).performClick()
            waitUntil { calls.calls.value.none { it.status != CallStatus.DISCONNECTED } }
            waitUntil { !callScreenResumed() }
            assertTrue("Ending the call must leave device locked", keyguard.isKeyguardLocked)
        } finally {
            instrumentation.runOnMainSync { calls.calls.value.forEach { calls.end(it.id) } }
            runBlocking { settings.setAutoAnswer(original.autoAnswer); settings.setAutoSpeaker(original.autoSpeaker); settings.setIncomingCallStyle(original.incomingCallStyle) }
        }
    }

    private fun callScreenResumed(): Boolean {
        var resumed = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).any { it is CallActivity }
        }
        return resumed
    }

    private fun waitUntil(predicate: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 25_000, condition = predicate)
    }
}
