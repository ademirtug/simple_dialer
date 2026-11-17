package info.pakkudev.simple_dialer.domain.model

/** One latest call per number, newest first. Unidentified callers remain separate. */
fun latestCallsByNumber(calls: List<RecentCall>): List<RecentCall> {
    val seen = mutableSetOf<String>()
    return calls.sortedWith(compareByDescending<RecentCall> { it.timestampMillis }.thenByDescending { it.id })
        .filter { call ->
            val number = call.number.trim()
            if (number in setOf("-1", "-2", "-3") || number.none { it.isDigit() }) {
                true
            } else {
                seen.add(phoneNumberKey(number))
            }
        }
}
