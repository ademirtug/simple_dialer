package info.pakkudev.simple_dialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import info.pakkudev.simple_dialer.domain.repository.CallRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CallActionReceiver : BroadcastReceiver(), KoinComponent {
    private val calls: CallRepository by inject()
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(CallNotifications.EXTRA_CALL_ID) ?: return
        // A notification can outlive the call; ignore stale actions.
        if (calls.calls.value.none { it.id == id }) return
        try {
            when (intent.action) {
                CallNotifications.ACTION_ANSWER -> calls.answer(id)
                CallNotifications.ACTION_END -> calls.end(id)
            }
        } catch (error: RuntimeException) { Log.w("CallActionReceiver", "Call action could not be completed", error) }
    }
}
