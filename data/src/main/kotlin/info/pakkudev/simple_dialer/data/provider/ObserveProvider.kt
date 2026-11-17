package info.pakkudev.simple_dialer.data.provider

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/** Cancelling a collector unregisters its platform observer. Queries run on an IO dispatcher. */
internal fun ContentResolver.observeChanges(uri: Uri): Flow<Unit> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { trySend(Unit) }
    }
    registerContentObserver(uri, true, observer)
    trySend(Unit)
    awaitClose { unregisterContentObserver(observer) }
}.buffer(Channel.CONFLATED)
