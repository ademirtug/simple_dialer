package info.pakkudev.simple_dialer.platform

import android.app.NotificationManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.annotation.RequiresApi

interface FullScreenCallAccess {
    fun isAllowed(): Boolean
    fun settingsIntent(): Intent?
}

fun createFullScreenCallAccess(context: Context, profile: PlatformProfile): FullScreenCallAccess {
    val platform = if (Build.VERSION.SDK_INT >= 34 && profile.needsFullScreenSettings) Android14FullScreenCallAccess(context) else UnrestrictedFullScreenCallAccess()
    val editor = Intent("miui.intent.action.APP_PERM_EDITOR").setPackage("com.miui.securitycenter")
        .putExtra("extra_pkgname", context.packageName).putExtra("extra_package_uid", context.applicationInfo.uid)
    val xiaomi = listOf(Build.MANUFACTURER, Build.BRAND).any {
        it.equals("Xiaomi", true) || it.equals("Redmi", true) || it.equals("Poco", true)
    }
    return if (xiaomi) {
        CombinedFullScreenCallAccess(platform, MiuiCallScreenAccess(context, editor))
    } else platform
}

/** Both Android full-screen access and any vendor-specific lock-screen restrictions must allow calls. */
class CombinedFullScreenCallAccess(private val platform: FullScreenCallAccess, private val vendor: FullScreenCallAccess) : FullScreenCallAccess {
    override fun isAllowed() = platform.isAllowed() && vendor.isAllowed()
    override fun settingsIntent(): Intent? = if (!platform.isAllowed()) platform.settingsIntent() else vendor.settingsIntent()
}

private class MiuiCallScreenAccess(private val context: Context, private val editor: Intent) : FullScreenCallAccess {
    override fun isAllowed() = allowed(10020) && allowed(10021)
    override fun settingsIntent() = if (editor.resolveActivity(context.packageManager) != null) editor
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    // MIUI's Show on Lock screen (10020) and background windows (10021)
    // are vendor app-ops, separate from Android's full-screen notification access.
    private fun allowed(op: Int): Boolean = try {
        val manager = context.getSystemService(AppOpsManager::class.java)
        val method = AppOpsManager::class.java.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
        method.invoke(manager, op, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    } catch (_: ReflectiveOperationException) {
        // This check is Xiaomi-only. Unknown access needs manual review, not a ready status.
        false
    } catch (_: RuntimeException) {
        false
    }
}

class UnrestrictedFullScreenCallAccess : FullScreenCallAccess {
    override fun isAllowed() = true
    override fun settingsIntent(): Intent? = null
}

@RequiresApi(34)
class Android14FullScreenCallAccess(private val context: Context) : FullScreenCallAccess {
    override fun isAllowed() = context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    override fun settingsIntent() = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.fromParts("package", context.packageName, null))
}
