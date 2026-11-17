package info.pakkudev.simple_dialer.domain.model

enum class CallEndReason { BUSY, REJECTED, ERROR, RESTRICTED, REMOTE, LOCAL, CANCELED, MISSED, OTHER }

/** User-facing carrier text only; internal disconnect reasons must never be displayed. */
data class CallDisconnectInfo(val reason: CallEndReason, val label: String = "", val description: String = "")
