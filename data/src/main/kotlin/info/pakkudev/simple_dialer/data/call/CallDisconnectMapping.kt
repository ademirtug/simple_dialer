package info.pakkudev.simple_dialer.data.call

import android.media.ToneGenerator
import android.telecom.DisconnectCause
import info.pakkudev.simple_dialer.domain.model.CallDisconnectInfo
import info.pakkudev.simple_dialer.domain.model.CallEndReason

fun DisconnectCause?.toCallDisconnectInfo(): CallDisconnectInfo? {
    val cause = this ?: return null
    val reason = when (cause.code) {
        DisconnectCause.LOCAL -> CallEndReason.LOCAL
        DisconnectCause.CANCELED -> CallEndReason.CANCELED
        DisconnectCause.MISSED -> CallEndReason.MISSED
        DisconnectCause.BUSY -> CallEndReason.BUSY
        DisconnectCause.REJECTED -> CallEndReason.REJECTED
        DisconnectCause.ERROR -> CallEndReason.ERROR
        DisconnectCause.RESTRICTED -> CallEndReason.RESTRICTED
        else -> if (cause.tone == ToneGenerator.TONE_SUP_BUSY) CallEndReason.BUSY
            else if (cause.code == DisconnectCause.REMOTE) CallEndReason.REMOTE else CallEndReason.OTHER
    }
    return CallDisconnectInfo(reason, cause.label?.toString().orEmpty(), cause.description?.toString().orEmpty())
}
