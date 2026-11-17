package info.pakkudev.simple_dialer.platform

import android.Manifest
import android.content.Context
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat

interface NotificationAccess {
    val runtimePermission: String?
    fun alertsEnabled(): Boolean
    fun settingsIntent(): Intent
}

class LegacyNotificationAccess(private val context: Context) : NotificationAccess {
    override val runtimePermission: String? = null
    override fun alertsEnabled() = NotificationManagerCompat.from(context).areNotificationsEnabled()
    override fun settingsIntent() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
}

@RequiresApi(26)
class ChannelNotificationAccess(private val context: Context, override val runtimePermission: String? = null) : NotificationAccess {
    override fun alertsEnabled(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        val importance = manager.getNotificationChannel(CALL_NOTIFICATION_CHANNEL)?.importance ?: NotificationManager.IMPORTANCE_HIGH
        return NotificationManagerCompat.from(context).areNotificationsEnabled() && importance >= NotificationManager.IMPORTANCE_HIGH
    }
    override fun settingsIntent(): Intent = if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, CALL_NOTIFICATION_CHANNEL)
}

@RequiresApi(33)
class RuntimeNotificationAccess(context: Context) : NotificationAccess by ChannelNotificationAccess(context, Manifest.permission.POST_NOTIFICATIONS)
