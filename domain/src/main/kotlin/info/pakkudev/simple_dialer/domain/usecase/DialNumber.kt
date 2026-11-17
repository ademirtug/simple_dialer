package info.pakkudev.simple_dialer.domain.usecase

import info.pakkudev.simple_dialer.domain.repository.CallLauncher

sealed interface DialResult {
    data object InvalidNumber : DialResult
    data object CallStarted : DialResult
}

/** Validates the number before invoking the platform dialer. */
class DialNumber(
    private val calls: CallLauncher,
) {
    suspend operator fun invoke(input: String): DialResult {
        val number = normalizeNumber(input) ?: return DialResult.InvalidNumber
        calls.placeCall(number)
        return DialResult.CallStarted
    }

    companion object {
        fun normalizeNumber(input: String): String? {
            // Phone keyboards and contacts may use Arabic, Persian, Bengali or other decimal digits.
            val number = buildString {
                input.forEach { character ->
                    val digit = Character.digit(character, 10)
                    append(if (digit in 0..9) '0' + digit else character)
                }
            }.filterNot { it.isWhitespace() || it in "()-" || it in "\u200e\u200f\u061c" }
            if (number.isEmpty() || number.length > 40) return null
            if (number.none { it in '0'..'9' }) return null
            if (number.any { it !in "0123456789+*#" }) return null
            if (number.count { it == '+' } > 1 || '+' in number.drop(1)) return null
            return number
        }
    }
}
