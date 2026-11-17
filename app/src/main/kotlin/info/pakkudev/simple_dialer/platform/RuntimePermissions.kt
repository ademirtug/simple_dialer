package info.pakkudev.simple_dialer.platform

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import info.pakkudev.simple_dialer.domain.model.AccessState
import info.pakkudev.simple_dialer.domain.model.permissionAccessState

interface RuntimePermissions {
    fun state(permission: String): AccessState
    fun recordRequest(permission: String)
    fun recordResult(permission: String, granted: Boolean, canRequestAgain: Boolean)
}

class AndroidRuntimePermissions(private val context: Context, private val history: PermissionHistory) : RuntimePermissions {
    override fun state(permission: String): AccessState {
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) history.recordObservedGrant(permission)
        return permissionAccessState(granted, history.wasRequested(permission), history.needsSettings(permission))
    }
    override fun recordRequest(permission: String) = history.recordRequest(permission)
    override fun recordResult(permission: String, granted: Boolean, canRequestAgain: Boolean) = history.recordResult(permission, granted, canRequestAgain)
}
