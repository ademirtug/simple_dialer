package info.pakkudev.simple_dialer.domain.model

import org.junit.Assert.*
import org.junit.Test

class RecentCallsTest {
    private fun call(id: Long, number: String, time: Long = id, direction: CallDirection = CallDirection.INCOMING,
                     name: String = number) = RecentCall(id, name, number, time, direction)

    @Test fun `only latest call per number remains even when other callers intervene`() {
        val latestAda = call(3, "+12025550101")
        val ben = call(2, "+12025550102")
        val olderAda = call(1, "+12025550101")
        assertEquals(listOf(latestAda, ben), latestCallsByNumber(listOf(olderAda, ben, latestAda)))
    }

    @Test fun `formatted variants group and preserve the latest display and direction`() {
        val latest = call(2, "0542 333 33 33", direction = CallDirection.MISSED, name = "Ada")
        val older = call(1, "0542 3333333", direction = CallDirection.OUTGOING)
        assertEquals(listOf(latest), latestCallsByNumber(listOf(older, latest)))
    }

    @Test fun `newest timestamp wins rather than provider order or highest id`() {
        val latest = call(1, "123", time = 100)
        assertEquals(listOf(latest), latestCallsByNumber(listOf(call(2, "123", time = 50), latest)))
    }

    @Test fun `identical timestamps use highest call id consistently`() {
        val latest = call(2, "123", time = 100)
        assertEquals(listOf(latest), latestCallsByNumber(listOf(call(1, "123", time = 100), latest)))
    }

    @Test fun `same name with different numbers keeps both rows`() {
        val mobile = call(2, "123", name = "Ada")
        val work = call(1, "456", name = "Ada")
        assertEquals(listOf(mobile, work), latestCallsByNumber(listOf(mobile, work)))
    }

    @Test fun `unidentified callers and Android private number markers remain separate`() {
        val numbers = listOf("", " ", "Unknown", "Private", "-1", "-2", "-3")
        val calls = numbers.flatMapIndexed { index, number ->
            listOf(call(index * 2L, number), call(index * 2L + 1, number))
        }
        assertEquals(calls.reversed(), latestCallsByNumber(calls))
    }

    @Test fun `grouping leaves source history intact and supports an empty log`() {
        val calls = listOf(call(1, "123"), call(2, "123"))
        assertEquals(1, latestCallsByNumber(calls).size)
        assertEquals(2, calls.size)
        assertTrue(latestCallsByNumber(emptyList()).isEmpty())
    }
}
