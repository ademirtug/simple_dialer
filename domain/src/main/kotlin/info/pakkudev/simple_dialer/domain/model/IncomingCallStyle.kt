package info.pakkudev.simple_dialer.domain.model

enum class IncomingCallStyle {
    BUTTONS,
    VERTICAL_SWIPE,
    HORIZONTAL_SLIDE;

    companion object {
        val DEFAULT = BUTTONS
        fun fromStored(value: String?): IncomingCallStyle = entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
