package info.pakkudev.simple_dialer.domain.model

fun DialerPreferences.normalizedFavorites(): DialerPreferences = copy(
    favoriteNumbers = favoriteNumbers.map(::phoneNumberKey).toSet(),
    excludedFavorites = excludedFavorites.map(::phoneNumberKey).toSet(),
)

fun DialerPreferences.withFavorite(number: String, favorite: Boolean): DialerPreferences {
    val normalized = normalizedFavorites()
    val key = phoneNumberKey(number)
    return normalized.copy(
        favoriteNumbers = if (favorite) normalized.favoriteNumbers + key else normalized.favoriteNumbers - key,
        excludedFavorites = if (favorite) normalized.excludedFavorites - key else normalized.excludedFavorites + key,
    )
}

fun isFavorite(contact: Contact, preferences: DialerPreferences): Boolean {
    val key = phoneNumberKey(contact.number)
    return preferences.excludedFavorites.none { phoneNumberKey(it) == key } &&
        (contact.starred || preferences.favoriteNumbers.any { phoneNumberKey(it) == key })
}

/** Prefer device contact details to number-only local entries, with one entry per number. */
fun favoriteContacts(contacts: List<Contact>, preferences: DialerPreferences): List<Contact> {
    val normalized = preferences.normalizedFavorites()
    val favorites = linkedMapOf<String, Contact>()
    contacts.forEach { contact ->
        val key = phoneNumberKey(contact.number)
        if (key !in favorites && key !in normalized.excludedFavorites &&
            (contact.starred || key in normalized.favoriteNumbers)) {
            favorites[key] = contact
        }
    }
    normalized.favoriteNumbers.forEach { key ->
        if (key !in favorites && key !in normalized.excludedFavorites) {
            favorites[key] = Contact("favorite:$key", key, key)
        }
    }
    return favorites.values.toList()
}
