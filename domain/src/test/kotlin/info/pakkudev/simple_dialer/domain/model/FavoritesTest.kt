package info.pakkudev.simple_dialer.domain.model

import org.junit.Assert.*
import org.junit.Test

class FavoritesTest {
    private val formatted = Contact("1", "Contact", "0542 333 33 33", photoUri = "content://photo/1")
    private val compact = formatted.copy(id = "2", number = "0542 3333333")

    @Test fun `duplicate starred contact formats produce one favorite`() {
        val first = formatted.copy(starred = true)
        assertEquals(listOf(first), favoriteContacts(listOf(first, compact.copy(starred = true)), DialerPreferences()))
    }

    @Test fun `local favorite matches device contact and preserves its details`() {
        val preferences = DialerPreferences(favoriteNumbers = setOf(compact.number))
        assertEquals(listOf(formatted), favoriteContacts(listOf(formatted, compact), preferences))
        assertTrue(isFavorite(formatted, preferences))
    }

    @Test fun `starred contact and saved formatted variants produce one favorite`() {
        val contact = formatted.copy(starred = true)
        val preferences = DialerPreferences(favoriteNumbers = setOf(formatted.number, compact.number, "05423333333"))
        assertEquals(listOf(contact), favoriteContacts(listOf(contact), preferences))
    }

    @Test fun `saved variants collapse without contacts access`() {
        val preferences = DialerPreferences(favoriteNumbers = setOf(formatted.number, compact.number))
        val favorites = favoriteContacts(emptyList(), preferences)
        assertEquals(1, favorites.size)
        assertEquals("05423333333", favorites.single().number)
        assertTrue(isFavorite(favorites.single(), preferences))
    }

    @Test fun `removing a favorite clears all saved variants and excludes starred variants`() {
        val preferences = DialerPreferences(favoriteNumbers = setOf(formatted.number, compact.number, "123"))
            .withFavorite("05423333333", false)
        assertEquals(setOf("123"), preferences.favoriteNumbers)
        assertEquals(setOf("05423333333"), preferences.excludedFavorites)
        assertFalse(isFavorite(formatted.copy(starred = true), preferences))
        assertFalse(isFavorite(compact.copy(starred = true), preferences))
    }

    @Test fun `adding a favorite clears all excluded variants`() {
        val preferences = DialerPreferences(excludedFavorites = setOf(formatted.number, compact.number, "123"))
            .withFavorite(formatted.number, true)
            .withFavorite(compact.number, true)
        assertEquals(setOf("05423333333"), preferences.favoriteNumbers)
        assertEquals(setOf("123"), preferences.excludedFavorites)
        assertEquals(listOf(formatted), favoriteContacts(listOf(formatted), preferences))
    }

    @Test fun `old formatted exclusions suppress both device and local favorites`() {
        val preferences = DialerPreferences(
            favoriteNumbers = setOf(compact.number),
            excludedFavorites = setOf(formatted.number),
        )
        assertFalse(isFavorite(compact.copy(starred = true), preferences))
        assertTrue(favoriteContacts(listOf(compact.copy(starred = true)), preferences).isEmpty())
        assertTrue(favoriteContacts(emptyList(), preferences).isEmpty())
    }

    @Test fun `reading existing preferences collapses formats in favorites and exclusions`() {
        val preferences = DialerPreferences(
            favoriteNumbers = setOf(formatted.number, compact.number),
            excludedFavorites = setOf("(123) 45-67", "1234567"),
            showRecents = false,
        ).normalizedFavorites()
        assertEquals(setOf("05423333333"), preferences.favoriteNumbers)
        assertEquals(setOf("1234567"), preferences.excludedFavorites)
        assertFalse(preferences.showRecents)
    }

    @Test fun `format matching supports localized digits without merging distinct dialing sequences`() {
        assertEquals("05423333333", phoneNumberKey("\u200f\u0660\u0665\u0664\u0662 (333) 33-33"))
        val numbers = setOf("05423333333", "+905423333333", "905423333333", "*123#", "123", "05423333334")
        assertEquals(numbers, favoriteContacts(emptyList(), DialerPreferences(favoriteNumbers = numbers)).map { it.number }.toSet())
        assertNotEquals(phoneNumberKey("123,45"), phoneNumberKey("12345"))
    }
}
