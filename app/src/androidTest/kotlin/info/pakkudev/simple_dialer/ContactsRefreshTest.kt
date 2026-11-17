package info.pakkudev.simple_dialer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.model.Contact
import info.pakkudev.simple_dialer.domain.repository.ContactsRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** Deliberately emits no change notifications: refresh must perform a fresh query. */
@RunWith(AndroidJUnit4::class)
class ContactsRefreshTest {
    private val repository = SnapshotContacts()
    @get:Rule(order = 0) val dependencies = object : ExternalResource() {
        private lateinit var original: ContactsRepository
        private lateinit var settings: PreferencesRepository
        private var showContacts = true
        override fun before() {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "pm grant info.pakkudev.simple_dialer android.permission.READ_CONTACTS"
                )
            ).bufferedReader().use { it.readText() }
            original = GlobalContext.get().get()
            settings = GlobalContext.get().get()
            showContacts = runBlocking { settings.preferences.first().showContacts }
            runBlocking { settings.setShowContacts(true) }
            loadKoinModules(module { single<ContactsRepository> { repository } })
        }
        override fun after() {
            loadKoinModules(module { single<ContactsRepository> { original } })
            runBlocking { settings.setShowContacts(showContacts) }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun pullAtTopReloadsAndKeepsTheSearchQuery() {
        awaitContact("Refresh Ada")
        compose.onNode(hasSetTextAction()).performTextReplacement("Refresh")
        repository.snapshot = listOf(contact("Refresh Ben"))
        pull()
        awaitContact("Refresh Ben")
        compose.onNodeWithText("Refresh Ada").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).assertTextEquals("Refresh")
    }

    @Test fun pullWorksWithNoContactsAndNoSearchResults() {
        awaitContact("Refresh Ada")
        repository.snapshot = emptyList()
        pull()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(label(R.string.no_contacts)).fetchSemanticsNodes().size == 1 }
        repository.snapshot = listOf(contact("Refresh Ben"))
        pull()
        awaitContact("Refresh Ben")
        compose.onNode(hasSetTextAction()).performTextReplacement("Missing")
        compose.onNodeWithText(label(R.string.no_results)).assertIsDisplayed()
        repository.snapshot = listOf(contact("Missing Person"))
        pull()
        awaitContact("Missing Person")
    }

    @Test fun quickReturnRemovesDeletedContactWithoutProviderNotification() {
        awaitContact("Refresh Ada")
        // Stay within the five-second subscription timeout, like a quick editor visit.
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        repository.snapshot = emptyList()
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(label(R.string.no_contacts)).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("Refresh Ada").assertDoesNotExist()
    }

    private fun label(id: Int) = compose.activity.getString(id)
    private fun awaitContact(name: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(name).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText(name).assertIsDisplayed()
    }
    private fun pull() {
        compose.onNodeWithTag("contacts_refresh").performTouchInput {
            swipe(Offset(center.x, height * .15f), Offset(center.x, height * .85f), 600)
        }
    }
    private class SnapshotContacts : ContactsRepository {
        @Volatile var snapshot = listOf(contact("Refresh Ada"))
        override fun observeContacts() = flow {
            delay(150)
            emit(snapshot)
            awaitCancellation()
        }
    }
    companion object {
        private fun contact(name: String) = Contact("refresh", name, "+1 202 555 0199")
    }
}
