package info.pakkudev.simple_dialer.platform

import android.app.Activity
import android.view.WindowManager
import androidx.annotation.RequiresApi

interface CallWindowPolicy { fun apply(activity: Activity) }

@RequiresApi(27)
class ModernCallWindowPolicy : CallWindowPolicy {
    override fun apply(activity: Activity) {
        activity.setShowWhenLocked(true)
        activity.setTurnScreenOn(true)
    }
}

@Suppress("DEPRECATION")
class LegacyCallWindowPolicy : CallWindowPolicy {
    override fun apply(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }
}
