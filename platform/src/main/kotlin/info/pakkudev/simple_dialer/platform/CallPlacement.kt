package info.pakkudev.simple_dialer.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi

interface CallPlacement {
    fun place(number: String)
}

@RequiresApi(23)
class TelecomCallPlacement(context: Context, private val dialer: DefaultDialerAccess) : CallPlacement {
    private val app = context.applicationContext
    override fun place(number: String) {
        check(dialer.isHeld()) { "Choose Dialer as your default phone app first." }
        check(app.checkCallingOrSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            "Phone permission is needed to place a call."
        }
        app.getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", number, null), Bundle())
    }
}
