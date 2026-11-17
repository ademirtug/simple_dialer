package info.pakkudev.simple_dialer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class PhoneUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val settings get() = GlobalContext.get().get<PreferencesRepository>()
    private fun label(resource: Int) = compose.activity.getString(resource)

    @Test fun adminCanHideTabsAndContactsSurvivesRecreation() {
        val original = runBlocking { settings.preferences.first() }
        try {
            runBlocking { settings.setShowContacts(true); settings.setShowFavorites(true); settings.setShowRecents(true) }
            compose.onNodeWithText(label(R.string.favorites)).assertIsDisplayed()
            compose.onNodeWithText(label(R.string.recents)).assertIsDisplayed()
            openSettings()
            compose.onNodeWithText(label(R.string.show_favorites)).performScrollTo().performClick()
            compose.onNodeWithText(label(R.string.show_recents)).performScrollTo().performClick()
            compose.onNodeWithContentDescription(label(R.string.back)).performClick()
            assertOnlyContactsVisible()
            compose.activityRule.scenario.recreate()
            assertOnlyContactsVisible()
        } finally {
            restoreVisibility(original)
        }
    }

    @Test fun hidingContactsFallsBackAndAllTabsCanStayHiddenAfterRecreation() {
        val original = runBlocking { settings.preferences.first() }
        try {
            runBlocking { settings.setShowContacts(true); settings.setShowFavorites(true); settings.setShowRecents(true) }
            openSettings()
            compose.onNodeWithText(label(R.string.show_contacts)).performScrollTo().performClick()
            compose.onNodeWithText(label(R.string.show_favorites)).performScrollTo().performClick()
            compose.onNodeWithContentDescription(label(R.string.back)).performClick()
            compose.onNodeWithText(label(R.string.contacts)).assertDoesNotExist()
            compose.onNodeWithText(label(R.string.favorites)).assertDoesNotExist()
            compose.onNodeWithText(label(R.string.recents)).assertIsDisplayed()
            compose.onNodeWithText(label(R.string.search_contacts)).assertDoesNotExist()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(label(R.string.contacts)).assertDoesNotExist()
            compose.onNodeWithText(label(R.string.recents)).assertIsDisplayed()

            openSettings()
            compose.onNodeWithText(label(R.string.show_contacts)).performScrollTo().assertIsOff()
            compose.onNodeWithText(label(R.string.show_recents)).performScrollTo().performClick()
            compose.onNodeWithContentDescription(label(R.string.back)).performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(label(R.string.contacts)).assertDoesNotExist()
            compose.onNodeWithText(label(R.string.favorites)).assertDoesNotExist()
            compose.onNodeWithText(label(R.string.recents)).assertDoesNotExist()
            compose.onNodeWithContentDescription(label(R.string.more_options)).assertIsDisplayed()
            compose.onNodeWithContentDescription(label(R.string.open_dialpad)).performClick()
            compose.onNodeWithText("1", useUnmergedTree = true).performClick()
            compose.onNodeWithText("2", useUnmergedTree = true).performClick()
            compose.onNodeWithText("12").assertIsDisplayed()
        } finally {
            restoreVisibility(original)
        }
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription(label(R.string.more_options)).performClick()
        compose.onNodeWithText(label(R.string.admin_settings)).performClick()
    }

    private fun assertOnlyContactsVisible() {
        compose.onNodeWithText(label(R.string.favorites)).assertDoesNotExist()
        compose.onNodeWithText(label(R.string.recents)).assertDoesNotExist()
        compose.onNodeWithText(label(R.string.search_contacts)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.open_dialpad)).assertIsDisplayed()
    }

    private fun restoreVisibility(original: info.pakkudev.simple_dialer.domain.model.DialerPreferences) {
        runBlocking {
            settings.setShowContacts(original.showContacts)
            settings.setShowFavorites(original.showFavorites)
            settings.setShowRecents(original.showRecents)
        }
    }

    @Test fun standardDialpadEditsNumberWithoutPlacingACall() {
        compose.onNodeWithContentDescription(label(R.string.open_dialpad)).performClick()
        compose.onNodeWithText("1", useUnmergedTree = true).performClick()
        compose.onNodeWithText("2", useUnmergedTree = true).performClick()
        compose.onNodeWithText("3", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription(label(R.string.delete_digit)).performClick()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.call), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun contactSearchIsHiddenOnOtherTabsAndRetainedOnReturn() {
        val original = runBlocking { settings.preferences.first() }
        try {
            runBlocking { settings.setShowContacts(true); settings.setShowFavorites(true); settings.setShowRecents(true) }
            compose.onNode(hasSetTextAction()).performTextInput("search test")
            compose.onNodeWithText(label(R.string.favorites)).performClick()
            compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
            compose.onNodeWithText(label(R.string.search_contacts)).assertDoesNotExist()
            compose.onNodeWithText(label(R.string.recents)).performClick()
            compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
            compose.onNodeWithText(label(R.string.search_contacts)).assertDoesNotExist()
            compose.onNodeWithText(label(R.string.contacts)).performClick()
            compose.onNode(hasSetTextAction()).assertTextContains("search test")
        } finally {
            restoreVisibility(original)
        }
    }
}
