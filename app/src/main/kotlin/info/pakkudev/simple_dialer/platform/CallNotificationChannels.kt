package info.pakkudev.simple_dialer.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.RequiresApi
import info.pakkudev.simple_dialer.R

const val CALL_NOTIFICATION_CHANNEL = "phone_calls"
const val ONGOING_CALL_NOTIFICATION_CHANNEL = "ongoing_phone_calls"

interface CallNotificationChannels { fun create() }

class NoCallNotificationChannels : CallNotificationChannels { override fun create() = Unit }

@RequiresApi(26)
class AndroidCallNotificationChannels(private val context: Context) : CallNotificationChannels {
    override fun create() {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CALL_NOTIFICATION_CHANNEL, context.getString(R.string.calls_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(ONGOING_CALL_NOTIFICATION_CHANNEL, context.getString(R.string.active_call), NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
    }
}
