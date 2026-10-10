package org.btcmap.settings

/**
 * One of the map's customizable colors: the key it is stored under, its default
 * ARGB value and whether the picker offers a reset back to that default.
 *
 * Shared by both hosts so the stored keys and the defaults cannot drift; the
 * labels and the picker itself stay with the platform. The Android app used to
 * keep a getter/setter per color here, each carrying its own copy of the key and
 * the default.
 */
enum class MapColor(
    val key: String,
    val defaultArgb: Int,
    val resettable: Boolean = false,
) {
    MarkerBackground("markerBackgroundColor", 0xFF0E95AF.toInt(), resettable = true),
    MarkerIcon("markerIconColor", 0xFFFFFFFF.toInt(), resettable = true),
    BoostedMarkerBackground("boostedMarkerBackgroundColor", 0xFFF7931A.toInt(), resettable = true),
    BoostedMarkerIcon("boostedMarkerIconColor", 0xFFFFFFFF.toInt(), resettable = true),
    BadgeBackground("badgeBackgroundColor", 0xFF00A63E.toInt()),
    BadgeText("badgeTextColor", 0xFFFFFFFF.toInt()),
    ButtonBackground("buttonBackgroundColor", 0xFF1F2937.toInt()),
    ButtonIcon("buttonIconColor", 0xFFFFFFFF.toInt()),

    /**
     * Tints the selected marker-filter icon. It has no colour of its own by
     * default: it follows [BoostedMarkerBackground] until the user picks one, so
     * changing the boosted colour also changes the accent (see [mapColor]).
     * [defaultArgb] mirrors the boosted default for callers that need a standalone
     * value, and [key] keeps the name the colour had as the button border so an
     * override set back then still applies.
     */
    ButtonAccent("buttonBorderColor", 0xFFF7931A.toInt(), resettable = true),
    ;

    companion object {
        /** The color stored under [key], or null when [key] is not a map color. */
        fun fromKey(key: String): MapColor? = entries.firstOrNull { it.key == key }
    }
}

/** The stored color for [color], or its default when the user has not overridden it. */
fun Settings.mapColor(color: MapColor): Int = getIntOrNull(color.key) ?: when (color) {
    // The button accent has no default of its own: it tracks the boosted marker
    // colour unless the user has picked a colour for it.
    MapColor.ButtonAccent -> mapColor(MapColor.BoostedMarkerBackground)
    else -> color.defaultArgb
}

/** Stores [argb] for [color]; null clears the override back to the default. */
fun Settings.setMapColor(color: MapColor, argb: Int?) = putInt(color.key, argb)
