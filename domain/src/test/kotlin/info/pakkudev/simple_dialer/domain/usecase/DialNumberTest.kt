package info.pakkudev.simple_dialer.domain.usecase

import info.pakkudev.simple_dialer.domain.repository.CallLauncher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DialNumberTest {
    private val placed = mutableListOf<String>()
    private val dial = DialNumber(object : CallLauncher { override suspend fun placeCall(number: String) { placed += number } })
    @Test fun `valid call invokes platform with normalized number`() = runTest {
        assertEquals(DialResult.CallStarted, dial("+90 (555) 123-45-67"))
        assertEquals(listOf("+905551234567"), placed)
    }
    @Test fun `invalid input never invokes platform`() = runTest {
        listOf("", " ", "+", "*#", "1+23", "++123", "tel:123", "abc123", "9".repeat(41)).forEach {
            assertEquals(DialResult.InvalidNumber, dial(it))
        }
        assertTrue(placed.isEmpty())
    }
    @Test fun `service codes and international prefixes are retained`() {
        assertEquals("*123#", DialNumber.normalizeNumber("*123#"))
        assertEquals("+12025550123", DialNumber.normalizeNumber("+1 (202) 555-0123"))
        assertEquals("123", DialNumber.normalizeNumber("１２３"))
    }
    @Test fun `localized decimal digits become telephone digits`() {
        assertEquals("+905551234567", DialNumber.normalizeNumber("\u200f+٩٠ (٥٥٥) ١٢٣-٤٥٦٧"))
        assertEquals("123", DialNumber.normalizeNumber("۱۲۳"))
        assertEquals("123", DialNumber.normalizeNumber("१२३"))
        assertEquals("123", DialNumber.normalizeNumber("১২৩"))
    }
    @Test(expected = SecurityException::class) fun `platform denial reaches user feedback`() = runTest {
        DialNumber(object : CallLauncher { override suspend fun placeCall(number: String) { throw SecurityException("Permission denied") } })("555")
    }
}
