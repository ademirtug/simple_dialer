package info.pakkudev.simple_dialer.platform

import android.content.Context

/** Stores permission request history. Grants themselves are always read from Android. */
class PermissionHistory(context: Context) {
    private val store = context.applicationContext.getSharedPreferences("permission_requests", Context.MODE_PRIVATE)
    fun wasRequested(permission: String) = store.getBoolean("requested:$permission", false)
    fun needsSettings(permission: String) = store.getBoolean("settings:$permission", false)
    fun recordRequest(permission: String) { store.edit().putBoolean("requested:$permission", true).apply() }
    fun recordResult(permission: String, granted: Boolean, canExplainAndRequestAgain: Boolean) {
        store.edit().putBoolean("requested:$permission", true)
            .putBoolean("settings:$permission", !granted && !canExplainAndRequestAgain).apply()
    }
    fun recordObservedGrant(permission: String) {
        if (needsSettings(permission)) store.edit().remove("settings:$permission").apply()
    }
}
