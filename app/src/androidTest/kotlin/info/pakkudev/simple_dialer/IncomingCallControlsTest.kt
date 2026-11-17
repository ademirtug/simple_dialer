package info.pakkudev.simple_dialer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.model.IncomingCallStyle
import info.pakkudev.simple_dialer.ui.IncomingCallControls
import info.pakkudev.simple_dialer.ui.theme.DialerTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingCallControlsTest {
    @get:Rule val compose = createComposeRule()
    private var answers = 0
    private var declines = 0

    private fun show(style: IncomingCallStyle, direction: LayoutDirection = LayoutDirection.Ltr) {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                DialerTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            IncomingCallControls(style, { answers++ }, { declines++ })
                        }
                    }
                }
            }
        }
    }

    private fun drag(horizontal: Boolean, sign: Int, fraction: Float = 1f, cancelGesture: Boolean = false) {
        val track = compose.onNodeWithTag("incoming_call_track").fetchSemanticsNode().boundsInRoot
        val handle = compose.onNodeWithTag("incoming_call_handle").fetchSemanticsNode().boundsInRoot
        val distance = ((if (horizontal) track.width - handle.width else track.height - handle.height) / 2) * sign * fraction
        compose.onNodeWithTag("incoming_call_handle").performTouchInput {
            val end = center + if (horizontal) Offset(distance, 0f) else Offset(0f, distance)
            if (cancelGesture) { down(center); moveTo(end); cancel() } else swipe(center, end, durationMillis = 300)
        }
    }

    private fun assertActions(answerCount: Int, declineCount: Int) = compose.runOnIdle {
        assertEquals(answerCount, answers)
        assertEquals(declineCount, declines)
    }

    @Test fun horizontalRightAnswersOnce() {
        show(IncomingCallStyle.HORIZONTAL_SLIDE)
        drag(true, 1)
        assertActions(1, 0)
        compose.onNodeWithTag("incoming_call_handle").assertIsNotEnabled()
    }

    @Test fun horizontalLeftDeclines() {
        show(IncomingCallStyle.HORIZONTAL_SLIDE)
        drag(true, -1)
        assertActions(0, 1)
    }

    @Test fun verticalUpAnswers() {
        show(IncomingCallStyle.VERTICAL_SWIPE)
        drag(false, -1)
        assertActions(1, 0)
    }

    @Test fun verticalDownDeclines() {
        show(IncomingCallStyle.VERTICAL_SWIPE)
        drag(false, 1)
        assertActions(0, 1)
    }

    @Test fun tapsShortDragsAndCanceledHorizontalDragsNeverAct() {
        show(IncomingCallStyle.HORIZONTAL_SLIDE)
        compose.onNodeWithTag("incoming_call_handle").performTouchInput { click(center) }
        drag(true, 1, fraction = 0.3f)
        assertActions(0, 0)
        drag(true, -1, cancelGesture = true)
        assertActions(0, 0)
        drag(true, 1)
        assertActions(1, 0)
    }

    @Test fun tapsShortDragsAndCanceledVerticalDragsNeverAct() {
        show(IncomingCallStyle.VERTICAL_SWIPE)
        compose.onNodeWithTag("incoming_call_handle").performTouchInput { click(center) }
        drag(false, -1, fraction = 0.3f)
        assertActions(0, 0)
        drag(false, 1, cancelGesture = true)
        assertActions(0, 0)
        drag(false, -1)
        assertActions(1, 0)
    }

    @Test fun rightStillAnswersInRightToLeftLayout() {
        show(IncomingCallStyle.HORIZONTAL_SLIDE, LayoutDirection.Rtl)
        drag(true, 1)
        assertActions(1, 0)
    }

    @Test fun screenReaderCanDeclineWithoutDraggingAndCannotSubmitTwice() {
        show(IncomingCallStyle.VERTICAL_SWIPE)
        val decline = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.decline)
        val actions = compose.onNodeWithTag("incoming_call_handle").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle {
            val action = actions.single { it.label == decline }
            assertTrue(action.action())
            assertFalse(action.action())
        }
        assertActions(0, 1)
    }
}
