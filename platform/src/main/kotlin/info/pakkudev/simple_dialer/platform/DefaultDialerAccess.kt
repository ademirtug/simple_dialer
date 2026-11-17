package info.pakkudev.simple_dialer.platform

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi

/** Android-specific port: callers launch the returned intent with the Activity Result API. */
interface DefaultDialerAccess {
    val supported: Boolean
    fun isHeld(): Boolean
    fun requestIntent(): Intent?
}

@RequiresApi(29)
class RoleManagerDialerAccess(context: Context) : DefaultDialerAccess {
    private val roles = context.getSystemService(RoleManager::class.java)
    override val supported get() = roles?.isRoleAvailable(RoleManager.ROLE_DIALER) == true
    override fun isHeld() = supported && roles.isRoleHeld(RoleManager.ROLE_DIALER)
    override fun requestIntent(): Intent? =
        if (supported) roles.createRequestRoleIntent(RoleManager.ROLE_DIALER) else null
}

@RequiresApi(23)
class TelecomDialerAccess(context: Context) : DefaultDialerAccess {
    private val app = context.applicationContext
    private val telecom = app.getSystemService(TelecomManager::class.java)
    override val supported get() = telecom != null && requestIntent().resolveActivity(app.packageManager) != null
    override fun isHeld() = telecom?.defaultDialerPackage == app.packageName
    override fun requestIntent(): Intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
        .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, app.packageName)
}
