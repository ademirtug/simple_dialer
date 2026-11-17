package info.pakkudev.simple_dialer

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.Settings
import android.telecom.DisconnectCause
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import info.pakkudev.simple_dialer.data.call.toCallDisconnectInfo
import info.pakkudev.simple_dialer.domain.model.*
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.platform.DialpadTonePlayer
import info.pakkudev.simple_dialer.telecom.CallActivity
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

@RunWith(AndroidJUnit4::class)
class CallFeedbackTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun label(id: Int) = context.getString(id)

    private fun withCall(status: CallStatus = CallStatus.DIALING, test: (FakeCalls) -> Unit) {
        val original = GlobalContext.get().get<CallRepository>()
        val fake = FakeCalls(ActiveCall("test-call", "5550100", "Test caller", status))
        loadKoinModules(module { single<CallRepository> { fake } })
        try {
            ActivityScenario.launch<CallActivity>(Intent(context, CallActivity::class.java)).use {
                val initial = label(if (status == CallStatus.ACTIVE) R.string.active_call else R.string.calling)
                compose.waitUntil(5_000) { compose.onAllNodesWithText(initial).fetchSemanticsNodes().isNotEmpty() }
                test(fake)
            }
        } finally { loadKoinModules(module { single<CallRepository> { original } }) }
    }

    @Test fun busyLabelSurvivesBackendRemovalWhileFeedbackPlaysThenCloses() = withCall { fake ->
        val startedAt = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { fake.fail(CallDisconnectInfo(CallEndReason.BUSY)) }
        compose.onNodeWithText(label(R.string.call_line_busy)).assertIsDisplayed()
        SystemClock.sleep(1_200)
        compose.onNodeWithText(label(R.string.call_line_busy)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.mute)).assertDoesNotExist()
        compose.onNodeWithContentDescription(label(R.string.end_call)).assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText(label(R.string.close_call)).assertDoesNotExist()
        compose.waitUntil(7_000) { !callScreenResumed() }
        assertTrue("Busy feedback must remain for its tone interval", SystemClock.elapsedRealtime() - startedAt >= 4_500)
    }

    @Test fun carrierTextIsShownAndRedEndButtonDismissesWithoutOperatingOnEndedCall() = withCall { fake ->
        instrumentation.runOnMainSync { fake.fail(CallDisconnectInfo(CallEndReason.BUSY, "Carrier busy", "Please try later")) }
        compose.onNodeWithText("Carrier busy").assertIsDisplayed()
        compose.onNodeWithText("Please try later").assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.end_call)).assertIsEnabled().performClick()
        compose.waitUntil(1_500) { !callScreenResumed() }
        assertEquals(0, fake.endRequests)
    }

    @Test fun pressingEndCallStillClosesPromptly() = withCall(CallStatus.ACTIVE) { fake ->
        compose.onNodeWithContentDescription(label(R.string.end_call)).performClick()
        compose.waitUntil(1_500) { !callScreenResumed() }
        assertEquals(1, fake.endRequests)
    }

    @Test fun platformDisconnectMappingPreservesUserTextAndExcludesInternalReason() {
        val info = DisconnectCause(DisconnectCause.BUSY, "Carrier busy", "Try later", "internal diagnostic only").toCallDisconnectInfo()!!
        assertEquals(CallEndReason.BUSY, info.reason)
        assertEquals("Carrier busy", info.label)
        assertEquals("Try later", info.description)
        assertFalse(info.toString().contains("internal diagnostic"))
    }

    @Test fun numberEntryTonesRespectSystemSwitchAndCanBeReleasedRepeatedly() {
        // Only a disposable emulator may have its global tone setting changed by this test.
        assumeTrue(Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish"))
        val audio = context.getSystemService(AudioManager::class.java)
        assumeTrue(audio.ringerMode == AudioManager.RINGER_MODE_NORMAL && audio.getStreamVolume(AudioManager.STREAM_DTMF) > 0)
        val original = Settings.System.getString(context.contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING)?.toIntOrNull()
        val tones = DialpadTonePlayer(context)
        try {
            shell("settings put system dtmf_tone 0")
            assertFalse(tones.play('1'))
            shell("settings put system dtmf_tone 1")
            "0123456789*#".forEach { assertTrue("Native tone for $it should start", tones.play(it)) }
            assertFalse(tones.play('x'))
            tones.release()
            tones.release()
            assertTrue(tones.play('5'))
        } finally {
            tones.release()
            if (original == null) shell("settings delete system dtmf_tone") else shell("settings put system dtmf_tone $original")
        }
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }

    private fun callScreenResumed(): Boolean {
        var resumed = false
        instrumentation.runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).any { it is CallActivity }
        }
        return resumed
    }

    private class FakeCalls(call: ActiveCall) : CallRepository {
        override val calls = MutableStateFlow(listOf(call))
        override val lastDisconnectedCall = MutableStateFlow<ActiveCall?>(null)
        override val audio = MutableStateFlow(AudioState())
        override val canAddCall = MutableStateFlow(false)
        var endRequests = 0
        fun fail(info: CallDisconnectInfo) {
            lastDisconnectedCall.value = calls.value.single().copy(status = CallStatus.DISCONNECTED, disconnectInfo = info)
            calls.value = emptyList()
        }
        override fun end(id: String) { endRequests++; fail(CallDisconnectInfo(CallEndReason.REMOTE)) }
        override fun answer(id: String) = error("Unexpected answer")
        override fun setMuted(muted: Boolean) = error("Unexpected mute")
        override suspend fun setSpeakerEnabled(enabled: Boolean) = error("Unexpected route")
        override fun setHeld(id: String, held: Boolean) = error("Unexpected hold")
        override fun merge(id: String) = error("Unexpected merge")
        override fun swap(id: String) = error("Unexpected swap")
        override fun playDtmf(id: String, digit: Char) = error("Unexpected DTMF")
        override fun stopDtmf(id: String) = error("Unexpected DTMF")
    }
}
