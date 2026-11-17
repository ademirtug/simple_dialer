package info.pakkudev.simple_dialer.domain.model

import org.junit.Assert.*
import org.junit.Test

class ContactsTest {
    private val contact = Contact("phone-row-1", "Ada", "0542 333 33 33", contactId = "person-1")

    @Test fun `linked provider rows show the contact number once`() {
        val linked = contact.copy(id = "phone-row-2", number = "0542 3333333")
        assertEquals(listOf(contact), distinctContactNumbers(listOf(contact, linked)))
    }

    @Test fun `identical provider phone rows show once`() {
        assertEquals(listOf(contact), distinctContactNumbers(listOf(contact, contact.copy(id = "phone-row-2"))))
    }

    @Test fun `different numbers for one contact remain available`() {
        val secondNumber = contact.copy(id = "phone-row-2", number = "0542 333 33 34")
        assertEquals(listOf(contact, secondNumber), distinctContactNumbers(listOf(contact, secondNumber)))
    }

    @Test fun `different people sharing a number are preserved even with matching names`() {
        val differentPerson = contact.copy(id = "phone-row-2", contactId = "person-2")
        assertEquals(listOf(contact, differentPerson), distinctContactNumbers(listOf(contact, differentPerson)))
    }

    @Test fun `linked rows retain photo and starred status`() {
        val linked = contact.copy(id = "phone-row-2", starred = true, photoUri = "content://photo/1")
        val merged = distinctContactNumbers(listOf(contact, linked)).single()
        assertEquals(contact.copy(starred = true, photoUri = linked.photoUri), merged)
        assertEquals(listOf(merged), favoriteContacts(listOf(merged), DialerPreferences()))
    }

    @Test fun `unique contacts remain unchanged and in provider order`() {
        val contacts = (1..5).map { Contact("row-$it", "Person $it", "+1202555010$it", contactId = "person-$it") }
        assertEquals(contacts, distinctContactNumbers(contacts))
        assertTrue(distinctContactNumbers(emptyList()).isEmpty())
    }

    @Test fun `merging linked phone rows retains the editable contact lookup`() {
        val linked = contact.copy(id = "phone-row-2", lookupUri = "content://com.android.contacts/contacts/lookup/abc/1")
        assertEquals(linked.lookupUri, distinctContactNumbers(listOf(contact, linked)).single().lookupUri)
    }
}
