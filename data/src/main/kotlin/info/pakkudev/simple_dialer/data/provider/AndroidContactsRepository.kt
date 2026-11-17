package info.pakkudev.simple_dialer.data.provider

import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import info.pakkudev.simple_dialer.domain.model.Contact
import info.pakkudev.simple_dialer.domain.model.distinctContactNumbers
import info.pakkudev.simple_dialer.domain.repository.ContactsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AndroidContactsRepository(context: Context, private val io: CoroutineDispatcher) : ContactsRepository {
    private val resolver = context.applicationContext.contentResolver

    // Editors can notify contacts, raw_contacts, or data rather than data/phones.
    override fun observeContacts() = resolver.observeChanges(ContactsContract.AUTHORITY_URI).map {
        buildList {
            resolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone._ID, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.PHOTO_THUMBNAIL_URI, Phone.STARRED, Phone.CONTACT_ID, Phone.LOOKUP_KEY),
                "${Phone.NUMBER} IS NOT NULL", null,
                "${Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val number = cursor.getString(2).orEmpty()
                    if (number.isNotBlank()) add(Contact(cursor.getString(0), cursor.getString(1) ?: number, number,
                        cursor.getString(3), cursor.getInt(4) != 0, contactId = cursor.getString(5),
                        lookupUri = Contacts.getLookupUri(cursor.getLong(5), cursor.getString(6))?.toString()))
                }
            }
        }.let(::distinctContactNumbers)
    }.flowOn(io)
}
