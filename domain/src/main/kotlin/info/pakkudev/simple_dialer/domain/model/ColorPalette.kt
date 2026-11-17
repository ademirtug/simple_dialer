package info.pakkudev.simple_dialer.domain.model

enum class ColorPalette {
    MONOCHROME, OCEAN, FOREST, ROSE, SUNSET;

    companion object {
        val DEFAULT = OCEAN
        fun fromStored(value: String?): ColorPalette = entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
