package info.pakkudev.simple_dialer.telecom

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import info.pakkudev.simple_dialer.R
import info.pakkudev.simple_dialer.domain.model.ActiveCall
import info.pakkudev.simple_dialer.domain.model.CallStatus
import info.pakkudev.simple_dialer.platform.CallNotificationChannels
import info.pakkudev.simple_dialer.platform.CALL_NOTIFICATION_CHANNEL
import info.pakkudev.simple_dialer.platform.ONGOING_CALL_NOTIFICATION_CHANNEL

class CallNotifications(private val context: Context, private val channels: CallNotificationChannels) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val visible = mutableSetOf<Int>()

    fun update(calls: List<ActiveCall>) {
        channels.create()
        val active = calls.filter { it.status != CallStatus.DISCONNECTED }
        val next = active.map { notificationId(it.id) }.toSet()
        (visible - next).forEach(manager::cancel)
        active.forEach { call ->
            val ringing = call.status == CallStatus.RINGING
            val person = Person.Builder().setName(call.displayName.ifBlank { call.number.ifBlank { context.getString(R.string.unknown_caller) } }).setImportant(true).build()
            val open = activityPendingIntent(notificationId(call.id), Intent(context, CallActivity::class.java).apply {
                data = Uri.fromParts("dialer-call", call.id, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            val end = action(call.id, ACTION_END)
            val notification = NotificationCompat.Builder(context, if (ringing) CALL_NOTIFICATION_CHANNEL else ONGOING_CALL_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_phone)
                .setContentTitle(person.name)
                .setContentText(context.getString(if (ringing) R.string.incoming_call else R.string.active_call))
                .setContentIntent(open)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(if (ringing) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .apply {
                    if (ringing) {
                        setStyle(NotificationCompat.CallStyle.forIncomingCall(person, end, action(call.id, ACTION_ANSWER)))
                        setFullScreenIntent(open, true)
                    } else {
                        // A system-bound InCallService is not a foreground service. On newer
                        // Android versions ongoing CallStyle is rejected for such services.
                        addAction(R.drawable.ic_phone_end, context.getString(R.string.end_call), end)
                    }
                }
                .build()
            manager.notify(notificationId(call.id), notification)
        }
        visible.clear()
        visible.addAll(next)
    }

    fun clear() { visible.forEach(manager::cancel); visible.clear() }

    private fun action(callId: String, action: String): PendingIntent {
        val target = if (action == ACTION_ANSWER) CallActivity::class.java else CallActionReceiver::class.java
        val intent = Intent(context, target).apply {
            this.action = action
            data = Uri.fromParts("dialer-action", callId, action)
            putExtra(EXTRA_CALL_ID, callId)
            if (action == ACTION_ANSWER) flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (action == ACTION_ANSWER) activityPendingIntent(notificationId(callId), intent)
        else PendingIntent.getBroadcast(context, notificationId(callId), intent, flags)
    }

    private fun activityPendingIntent(requestCode: Int, intent: Intent): PendingIntent {
        // These explicit, immutable intents open only our call UI. Android 15+
        // requires creator opt-in for launches while the phone is asleep.
        val options = if (Build.VERSION.SDK_INT >= 35) ActivityOptions.makeBasic().apply {
            setPendingIntentCreatorBackgroundActivityStartMode(
                if (Build.VERSION.SDK_INT >= 36) ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                else ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }.toBundle() else null
        return PendingIntent.getActivity(context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE, options)
    }

    private fun notificationId(id: String) = id.hashCode() and Int.MAX_VALUE

    companion object {
        const val ACTION_ANSWER = "info.pakkudev.simple_dialer.ANSWER"
        const val ACTION_END = "info.pakkudev.simple_dialer.END"
        const val EXTRA_CALL_ID = "call_id"
    }
}
