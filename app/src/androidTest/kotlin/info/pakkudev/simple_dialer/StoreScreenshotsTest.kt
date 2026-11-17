package info.pakkudev.simple_dialer

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.model.ColorPalette
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Opt-in captures of the actual activity. Run scripts/capture-store-screenshots.ps1. */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun captureEnglishStoreScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("captureStoreScreenshots") == "true")
        assumeTrue(Build.MODEL.contains("sdk_gphone"))
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first() }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)
        ).bufferedReader().use { it.readText() }
        fun label(id: Int) = compose.activity.getString(id)
        fun capture(name: String) {
            compose.waitForIdle()
            instrumentation.waitForIdleSync()
            Thread.sleep(400)
            shell("screencap -p /sdcard/Download/simple-dialer-store/$name.png")
        }
        shell("pm grant info.pakkudev.simple_dialer android.permission.READ_CONTACTS")
        shell("mkdir -p /sdcard/Download/simple-dialer-store")
        try {
            runBlocking {
                settings.setColorPalette(ColorPalette.OCEAN)
                settings.setShowContacts(true)
                settings.setShowFavorites(false)
                settings.setShowRecents(false)
                settings.setShowContactDetails(false)
            }
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Ada Morgan").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("Ada Morgan").assertIsDisplayed()
            capture("01-simplified-contacts")

            runBlocking {
                settings.setShowFavorites(true)
                settings.setShowRecents(true)
                settings.setShowContactDetails(true)
            }
            compose.onNodeWithText(label(R.string.favorites)).assertIsDisplayed()
            capture("02-contacts")
            compose.onNodeWithText(label(R.string.favorites)).performClick()
            compose.onNodeWithText("Ada Morgan").assertIsDisplayed()
            capture("03-favorites")

            compose.onNodeWithText(label(R.string.contacts)).performClick()
            compose.onNodeWithContentDescription(label(R.string.open_dialpad)).performClick()
            compose.onNodeWithContentDescription(label(R.string.phone_number)).performTextReplacement("+1 202 555 0101")
            Espresso.closeSoftKeyboard()
            capture("04-dialpad")
            Espresso.pressBack()

            compose.onNodeWithContentDescription(label(R.string.more_options)).performClick()
            compose.onNodeWithText(label(R.string.admin_settings)).performClick()
            compose.onNodeWithText(label(R.string.visibility_title)).performScrollTo()
            val visibilityTop = compose.onNodeWithText(label(R.string.visibility_title)).fetchSemanticsNode().boundsInRoot.top
            compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, visibilityTop - 260f) }
            capture("05-visibility-settings")
            compose.onNodeWithText(label(R.string.appearance_title)).performScrollTo()
            capture("06-appearance-settings")
        } finally {
            runBlocking {
                settings.setColorPalette(original.colorPalette)
                settings.setShowContacts(original.showContacts)
                settings.setShowFavorites(original.showFavorites)
                settings.setShowRecents(original.showRecents)
                settings.setShowContactDetails(original.showContactDetails)
            }
        }
    }

    @Test fun captureVisibilitySettingsOnly() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("captureStoreScreenshots") == "true")
        assumeTrue(Build.MODEL.contains("sdk_gphone"))
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first() }
        fun label(id: Int) = compose.activity.getString(id)
        try {
            runBlocking {
                settings.setColorPalette(ColorPalette.OCEAN)
                settings.setShowContacts(true)
                settings.setShowFavorites(false)
                settings.setShowRecents(false)
                settings.setShowContactDetails(false)
            }
            compose.onNodeWithContentDescription(label(R.string.more_options)).performClick()
            compose.onNodeWithText(label(R.string.admin_settings)).performClick()
            compose.onNodeWithText(label(R.string.visibility_title)).performScrollTo()
            val top = compose.onNodeWithText(label(R.string.visibility_title)).fetchSemanticsNode().boundsInRoot.top
            compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, top - 260f) }
            compose.onNodeWithText(label(R.string.show_contacts)).assertIsDisplayed()
            compose.onNodeWithText(label(R.string.show_contact_details)).assertIsDisplayed()
            compose.waitForIdle()
            Thread.sleep(400)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "screencap -p /sdcard/Download/simple-dialer-store/05-visibility-settings.png"
            )).bufferedReader().use { it.readText() }
        } finally {
            runBlocking {
                settings.setColorPalette(original.colorPalette)
                settings.setShowContacts(original.showContacts)
                settings.setShowFavorites(original.showFavorites)
                settings.setShowRecents(original.showRecents)
                settings.setShowContactDetails(original.showContactDetails)
            }
        }
    }
}
