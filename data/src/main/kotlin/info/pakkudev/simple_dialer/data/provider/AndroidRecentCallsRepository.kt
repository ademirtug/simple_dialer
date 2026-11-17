package info.pakkudev.simple_dialer.data.provider

import android.content.Context
import android.provider.CallLog.Calls
import info.pakkudev.simple_dialer.domain.model.CallDirection
import info.pakkudev.simple_dialer.domain.model.RecentCall
import info.pakkudev.simple_dialer.domain.repository.RecentCallsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class AndroidRecentCallsRepository(context: Context, private val io: CoroutineDispatcher) : RecentCallsRepository {
    private val resolver = context.applicationContext.contentResolver

    override fun observeRecentCalls() = resolver.observeChanges(Calls.CONTENT_URI).map {
        buildList {
            resolver.query(
                Calls.CONTENT_URI,
                arrayOf(Calls._ID, Calls.CACHED_NAME, Calls.NUMBER, Calls.DATE, Calls.TYPE),
                null, null, "${Calls.DATE} DESC",
            )?.use { cursor ->
                while (cursor.moveToNext() && size < 100) {
                    val number = cursor.getString(2).orEmpty()
                    val direction = when (cursor.getInt(4)) {
                        Calls.INCOMING_TYPE -> CallDirection.INCOMING
                        Calls.OUTGOING_TYPE -> CallDirection.OUTGOING
                        Calls.MISSED_TYPE -> CallDirection.MISSED
                        else -> CallDirection.OTHER
                    }
                    add(RecentCall(cursor.getLong(0), cursor.getString(1) ?: number, number, cursor.getLong(3), direction))
                }
            }
        }
    }.flowOn(io)
}
