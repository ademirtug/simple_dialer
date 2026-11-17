package info.pakkudev.simple_dialer

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.model.*
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.telecom.CallActivity
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.io.File

@RunWith(AndroidJUnit4::class)
class CallKeypadUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun label(id: Int) = context.getString(id)
    private fun button(id: Int) = compose.onNodeWithContentDescription(label(id))

    private fun withCall(hidden: Set<CallControl> = emptySet(), palette: ColorPalette = ColorPalette.DEFAULT,
                         test: (FakeCalls, ActivityScenario<CallActivity>) -> Unit) {
        val koin = GlobalContext.get()
        val originalCalls = koin.get<CallRepository>()
        val originalSettings = koin.get<PreferencesRepository>()
        val fake = FakeCalls()
        val settings = object : PreferencesRepository by originalSettings {
            override val preferences = MutableStateFlow(DialerPreferences(hiddenControls = hidden, colorPalette = palette))
        }
        loadKoinModules(module {
            single<CallRepository> { fake }
            single<PreferencesRepository> { settings }
        })
        try {
            ActivityScenario.launch<CallActivity>(Intent(context, CallActivity::class.java)).use { scenario ->
                compose.waitUntil(5_000) { compose.onAllNodesWithText(label(R.string.active_call)).fetchSemanticsNodes().isNotEmpty() }
                test(fake, scenario)
            }
        } finally {
            loadKoinModules(module {
                single<CallRepository> { originalCalls }
                single<PreferencesRepository> { originalSettings }
            })
        }
    }

    @Test fun keypadKeepsPrimaryActionsAndOverflowUsable() = withCall { fake, _ ->
        button(R.string.keypad).performClick().assertIsSelected()
        val keyHeight = compose.onNodeWithText("5").fetchSemanticsNode().boundsInRoot.height
        "123456789*0#".forEach { digit ->
            val key = compose.onNode(hasText(digit.toString()) and hasClickAction()).assertIsDisplayed()
            assertEquals("Key $digit must be fully visible", keyHeight, key.fetchSemanticsNode().boundsInRoot.height, 0.5f)
        }
        button(R.string.mute).assertIsDisplayed().performClick().assertIsSelected()
        button(R.string.speaker).assertIsDisplayed().performClick().assertIsSelected()
        button(R.string.end_call).assertIsDisplayed()
        compose.onNodeWithText("5").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(listOf('5'), fake.digits)
            assertEquals(true, fake.audio.value.muted)
            assertEquals(true, fake.audio.value.speakerEnabled)
        }
        button(R.string.more_options).assertIsDisplayed().performClick()
        compose.onNodeWithText(label(R.string.add_call)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.hold)).assertIsDisplayed().performClick()
        button(R.string.hide_keypad).assertIsDisplayed()
        compose.onNode(hasText("5") and hasClickAction()).assertIsNotEnabled()
        button(R.string.more_options).performClick()
        compose.onNodeWithText(label(R.string.resume_call)).assertIsDisplayed().performClick()
        compose.onNode(hasText("5") and hasClickAction()).assertIsEnabled()
        capture("keypad-controls")
        button(R.string.hide_keypad).performClick()
        compose.onNodeWithText("5").assertDoesNotExist()
        button(R.string.keypad).assertIsNotSelected()
        button(R.string.mute).assertIsDisplayed()
    }

    @Test fun keypadToggleBackAndRecreationPreserveCallControls() = withCall { _, scenario ->
        button(R.string.keypad).performClick()
        compose.onNodeWithText("8").performClick()
        scenario.recreate()
        button(R.string.hide_keypad).assertIsDisplayed()
        button(R.string.keypad).assertIsSelected().performClick()
        button(R.string.hide_keypad).assertDoesNotExist()
        button(R.string.keypad).performClick()
        androidx.test.espresso.Espresso.pressBack()
        button(R.string.hide_keypad).assertDoesNotExist()
        button(R.string.end_call).assertIsDisplayed()
    }

    @Test fun hiddenSecondaryControlsDoNotProduceOverflow() = withCall(
        hidden = setOf(CallControl.ADD_CALL, CallControl.HOLD, CallControl.SWAP)
    ) { _, _ ->
        button(R.string.more_options).assertDoesNotExist()
        button(R.string.keypad).performClick()
        button(R.string.mute).assertIsDisplayed()
        button(R.string.speaker).assertIsDisplayed()
        button(R.string.end_call).assertIsDisplayed()
    }

    @Test fun hiddenPrimaryControlsStayHiddenWhenKeypadOpens() = withCall(
        hidden = setOf(CallControl.MUTE, CallControl.SPEAKER, CallControl.ADD_CALL, CallControl.SWAP)
    ) { _, _ ->
        button(R.string.keypad).performClick()
        button(R.string.mute).assertDoesNotExist()
        button(R.string.speaker).assertDoesNotExist()
        button(R.string.more_options).performClick()
        compose.onNodeWithText(label(R.string.hold)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.add_call)).assertDoesNotExist()
        compose.onNodeWithText(label(R.string.swap_calls)).assertDoesNotExist()
    }

    @Test fun endCallRemainsUsableWithKeypadOpen() = withCall { fake, _ ->
        button(R.string.keypad).performClick()
        button(R.string.end_call).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(emptyList<ActiveCall>(), fake.calls.value) }
    }

    @Test fun monochromeActiveControlsHaveHighContrastAndToggleBack() = withCall(palette = ColorPalette.MONOCHROME) { _, _ ->
        val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val activeBackground = if (dark) Color.White else Color.Black
        val activeIcon = if (dark) Color.Black else Color.White
        fun speakerImage() = compose.onRoot().captureToImage().toPixelMap() to
            button(R.string.speaker).fetchSemanticsNode().boundsInRoot
        fun background(): Int {
            val (pixels, bounds) = speakerImage()
            return pixels[bounds.center.x.toInt(), (bounds.top + bounds.height / 8).toInt()].toArgb()
        }
        // Captures may differ by a few channel levels due to GPU color conversion.
        fun near(actual: Int, expected: Int) = listOf(0, 8, 16).all { shift ->
            kotlin.math.abs(((actual shr shift) and 255) - ((expected shr shift) and 255)) <= 5
        }
        val inactive = background()
        button(R.string.speaker).performClick().assertIsSelected()
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        assertTrue("Active speaker background must be high contrast", near(background(), activeBackground.toArgb()))
        compose.waitUntil(3_000) {
            val (pixels, bounds) = speakerImage()
            val iconPixels = ((bounds.top + bounds.height / 4).toInt() until (bounds.bottom - bounds.height / 4).toInt()).sumOf { y ->
                ((bounds.left + bounds.width / 4).toInt() until (bounds.right - bounds.width / 4).toInt()).count { x ->
                    near(pixels[x, y].toArgb(), activeIcon.toArgb())
                }
            }
            iconPixels > 20
        }
        button(R.string.mute).performClick().assertIsSelected()
        button(R.string.keypad).performClick().assertIsSelected()
        capture(if (dark) "monochrome-active-controls-dark" else "monochrome-active-controls-light")
        button(R.string.speaker).performClick().assertIsNotSelected()
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        assertTrue("Speaker must return to its inactive color", near(background(), inactive))
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private class FakeCalls : CallRepository {
        override val calls = MutableStateFlow(listOf(ActiveCall("keypad-test", "5550100", "Test caller", CallStatus.ACTIVE,
            connectedAtMillis = System.currentTimeMillis(), canHold = true, canSwap = true)))
        override val lastDisconnectedCall = MutableStateFlow<ActiveCall?>(null)
        override val audio = MutableStateFlow(AudioState())
        override val canAddCall = MutableStateFlow(true)
        val digits = mutableListOf<Char>()
        override fun setMuted(muted: Boolean) { audio.value = audio.value.copy(muted = muted) }
        override suspend fun setSpeakerEnabled(enabled: Boolean) { audio.value = audio.value.copy(speakerEnabled = enabled) }
        override fun setHeld(id: String, held: Boolean) { calls.value = calls.value.map { it.copy(status = if (held) CallStatus.HOLDING else CallStatus.ACTIVE) } }
        override fun playDtmf(id: String, digit: Char) { digits += digit }
        override fun stopDtmf(id: String) = Unit
        override fun answer(id: String) = error("Unexpected answer")
        override fun end(id: String) { calls.value = emptyList() }
        override fun merge(id: String) = error("Unexpected merge")
        override fun swap(id: String) = Unit
    }
}
