package info.pakkudev.simple_dialer

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.data.preferences.DataStorePreferencesRepository
import info.pakkudev.simple_dialer.domain.model.IncomingCallStyle
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.ui.labelResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.io.File

@RunWith(AndroidJUnit4::class)
class IncomingCallStyleUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun allStylesPersistSurviveRecreationAndHaveSafePreviews() {
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val calls = GlobalContext.get().get<CallRepository>()
        val original = runBlocking { settings.preferences.first().incomingCallStyle }
        val originalCalls = calls.calls.value
        fun label(id: Int) = compose.activity.getString(id)
        try {
            compose.onNodeWithContentDescription(label(R.string.more_options)).performClick()
            compose.onNodeWithText(label(R.string.admin_settings)).performClick()
            IncomingCallStyle.entries.forEach { style ->
                compose.onNodeWithText(label(style.labelResource())).performScrollTo().performClick().assertIsSelected()
                compose.waitUntil(5_000) { runBlocking { settings.preferences.first().incomingCallStyle } == style }
                assertEquals(style, runBlocking { DataStorePreferencesRepository(compose.activity).preferences.first().incomingCallStyle })
                compose.onNodeWithText(label(R.string.incoming_style_preview)).performScrollTo().performClick()
                compose.onNodeWithText(label(R.string.incoming_preview_description)).assertIsDisplayed()
                if (style == IncomingCallStyle.BUTTONS) compose.onNodeWithContentDescription(label(R.string.answer)).performClick()
                else {
                    val horizontal = style == IncomingCallStyle.HORIZONTAL_SLIDE
                    val track = compose.onNodeWithTag("incoming_call_track").fetchSemanticsNode().boundsInRoot
                    val handle = compose.onNodeWithTag("incoming_call_handle").fetchSemanticsNode().boundsInRoot
                    val distance = (if (horizontal) track.width - handle.width else track.height - handle.height) / 2
                    compose.onNodeWithTag("incoming_call_handle").performTouchInput {
                        swipe(center, center + if (horizontal) Offset(distance, 0f) else Offset(0f, -distance), durationMillis = 400)
                    }
                }
                compose.onNodeWithText(compose.activity.getString(R.string.incoming_preview_result, label(R.string.answer))).assertIsDisplayed()
                compose.waitForIdle()
                val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                screenshot?.let { bitmap ->
                    File(compose.activity.getExternalFilesDir(null), "incoming-style-${style.name.lowercase()}.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
                compose.onNodeWithText(label(R.string.incoming_preview_close)).performClick()
                assertEquals(originalCalls, calls.calls.value)
            }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(label(IncomingCallStyle.HORIZONTAL_SLIDE.labelResource())).performScrollTo().assertIsSelected()
        } finally {
            runBlocking { settings.setIncomingCallStyle(original) }
        }
    }
}
