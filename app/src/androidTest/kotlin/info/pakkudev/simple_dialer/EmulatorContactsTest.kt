package info.pakkudev.simple_dialer

import android.graphics.Bitmap
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import info.pakkudev.simple_dialer.domain.repository.ContactsRepository
import info.pakkudev.simple_dialer.domain.repository.PreferencesRepository
import info.pakkudev.simple_dialer.data.preferences.DataStorePreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.hamcrest.Matchers.allOf
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.io.File

/** Run after scripts/seed-emulator-contacts.ps1, with READ_CONTACTS granted to the app. */
@RunWith(AndroidJUnit4::class)
class EmulatorContactsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun deletingATemporaryContactUpdatesTheVisibleList() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seededContacts") == "true")
        assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone"))
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand(command)
        ).bufferedReader().use { it.readText() }
        shell("pm grant info.pakkudev.simple_dialer android.permission.READ_CONTACTS")
        compose.activityRule.scenario.recreate()
        val source = "refresh-test-${java.util.UUID.randomUUID()}"
        shell("content insert --uri content://com.android.contacts/raw_contacts --bind account_type:n: --bind account_name:n: --bind sourceid:s:$source")
        val inserted = shell("content query --uri content://com.android.contacts/raw_contacts --projection _id --where sourceid='$source'")
        val rawId = checkNotNull(Regex("_id=(\\d+)").find(inserted)?.groupValues?.get(1)) { inserted }
        val rawUri = "content://com.android.contacts/raw_contacts/$rawId"
        try {
            fun addData(mime: String, value: String) {
                shell("content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$rawId --bind mimetype:s:$mime --bind data1:s:$value")
            }
            addData(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, "RefreshTemporaryContact")
            addData(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, "+12025550198")
            val fresh = runBlocking { GlobalContext.get().get<ContactsRepository>().observeContacts().first() }
            org.junit.Assert.assertTrue("Fixture missing from provider", fresh.any { it.name == "RefreshTemporaryContact" })
            compose.onNode(hasSetTextAction()).performTextReplacement("RefreshTemporary")
            compose.waitUntil(10_000) { compose.onAllNodesWithText("RefreshTemporaryContact").fetchSemanticsNodes().size == 1 }
            // Delete through the same provider used by Android's contact editor.
            shell("content delete --uri $rawUri")
            compose.waitUntil(10_000) { compose.onAllNodesWithText(compose.activity.getString(R.string.no_results)).fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("RefreshTemporaryContact").assertDoesNotExist()
        } finally {
            shell("content delete --uri $rawUri")
        }
    }

    @Test fun contactsRemainUsableAfterFifteenSecondsInBackground() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seededContacts") == "true")
        compose.onNode(hasSetTextAction()).performTextReplacement("Debug ")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Debug Ada").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText("+1 202 555 0101").assertDoesNotExist()

        // Exceed WhileSubscribed's stop timeout, as pressing Home does.
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(15_000)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Debug Ada").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText("Debug Ada").assertIsDisplayed()
        compose.onNodeWithText("Debug Ben").assertIsDisplayed()
        compose.onNodeWithText("+1 202 555 0101").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextReplacement("Ben")
        compose.onNodeWithText("Debug Ben").assertIsDisplayed()
        compose.onNodeWithText("Debug Ada").assertDoesNotExist()
    }

    @Test fun contactPanelCanBeDisabledAndTheChoiceSurvivesRecreation() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seededContacts") == "true")
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first() }
        try {
            runBlocking { settings.setShowContactDetails(true); settings.setShowFavorites(true) }
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.more_options)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.admin_settings)).performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText(compose.activity.getString(R.string.show_contact_details)) and isOn()).fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithText(compose.activity.getString(R.string.show_contact_details)).performScrollTo().performClick()
            runBlocking { withTimeout(5_000) { settings.preferences.first { !it.showContactDetails } } }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(compose.activity.getString(R.string.show_contact_details)).performScrollTo().assertIsOff()
            assertEquals(false, runBlocking { DataStorePreferencesRepository(compose.activity).preferences.first().showContactDetails })
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
            compose.onNode(hasSetTextAction()).performTextReplacement("Ada")
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Debug Ada").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("Debug Ada").assertHasNoClickAction().performTouchInput { click() }
            compose.onNodeWithText(compose.activity.getString(R.string.edit_contact)).assertDoesNotExist()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.call_person, "Debug Ada")).assertIsDisplayed()
            compose.onNodeWithText(compose.activity.getString(R.string.favorites)).performClick()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.contact_details)).assertDoesNotExist()
        } finally {
            runBlocking { settings.setShowContactDetails(original.showContactDetails); settings.setShowFavorites(original.showFavorites) }
        }
    }

    @Test fun editingAContactOpensTheCorrectSystemEditorFromContactsAndFavorites() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seededContacts") == "true")
        val settings = GlobalContext.get().get<PreferencesRepository>()
        val original = runBlocking { settings.preferences.first() }
        runBlocking { settings.setShowContactDetails(true); settings.setShowFavorites(true) }
        compose.activityRule.scenario.recreate()
        val repository = GlobalContext.get().get<ContactsRepository>()
        val contact = runBlocking { withTimeout(10_000) { repository.observeContacts().first() } }
            .single { it.name == "Debug Ada" }
        assertNotNull(contact.lookupUri)
        Intents.init()
        try {
            Intents.intending(hasAction(Intent.ACTION_EDIT)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
            compose.onNode(hasSetTextAction()).performTextReplacement("Ada")
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Debug Ada").fetchSemanticsNodes().size == 1 }
            compose.onNodeWithText("Debug Ada").performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.edit_contact)).performClick()
            val expected = allOf(hasAction(Intent.ACTION_EDIT), hasData(contact.lookupUri!!),
                hasType(Contacts.CONTENT_ITEM_TYPE), hasExtra("finishActivityOnSaveCompleted", true))
            Intents.intended(expected)

            compose.onNodeWithText(compose.activity.getString(R.string.favorites)).performClick()
            compose.onNodeWithContentDescription(compose.activity.getString(R.string.contact_details)).performClick()
            compose.onNodeWithText(compose.activity.getString(R.string.edit_contact)).performClick()
            Intents.intended(expected, androidx.test.espresso.intent.VerificationModes.times(2))
        } finally {
            Intents.release()
            runBlocking { settings.setShowContactDetails(original.showContactDetails); settings.setShowFavorites(original.showFavorites) }
        }
    }

    @Test fun uniqueFakeContactsAppearOnceInTheActualApp() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("seededContacts") == "true")
        val expected = listOf("Debug Ada", "Debug Ben", "Debug Cara", "Debug Deniz", "Debug Ece")
        val repository = GlobalContext.get().get<ContactsRepository>()
        val contacts = runBlocking { withTimeout(10_000) { repository.observeContacts().first() } }
            .filter { it.name in expected }
        assertEquals(expected, contacts.map { it.name })
        assertEquals(5, contacts.map { it.contactId }.toSet().size)
        assertEquals(5, contacts.map { it.number }.toSet().size)
        assertEquals(listOf("Debug Ada"), contacts.filter { it.starred }.map { it.name })

        compose.onNode(hasSetTextAction()).performTextInput("Debug ")
        expected.forEach { name ->
            compose.waitUntil(10_000) { compose.onAllNodesWithText(name).fetchSemanticsNodes().size == 1 }
            compose.onAllNodesWithText(name).assertCountEquals(1)
        }
        if (InstrumentationRegistry.getArguments().getString("captureScreens") == "true") {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            File(instrumentation.targetContext.getExternalFilesDir(null), "contacts.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }
}
