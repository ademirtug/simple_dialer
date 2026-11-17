package info.pakkudev.simple_dialer.domain.model

import info.pakkudev.simple_dialer.domain.usecase.DialNumber

/** Compare dialable numbers without display formatting, retaining prefixes and service codes. */
fun phoneNumberKey(number: String): String = DialNumber.normalizeNumber(number) ?: number.trim()

/** Android's phone query returns data rows, including rows from linked contact sources. */
fun distinctContactNumbers(contacts: List<Contact>): List<Contact> {
    val distinct = linkedMapOf<Pair<String, String>, Contact>()
    contacts.forEach { contact ->
        val key = contact.contactId to phoneNumberKey(contact.number)
        val previous = distinct[key]
        distinct[key] = if (previous == null) contact else previous.copy(
            photoUri = previous.photoUri ?: contact.photoUri,
            starred = previous.starred || contact.starred,
            lookupUri = previous.lookupUri ?: contact.lookupUri,
        )
    }
    return distinct.values.toList()
}
