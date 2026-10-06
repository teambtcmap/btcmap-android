package org.btcmap.ui.map

/** The Material Symbols glyph an area wears when it has no image. */
internal const val AREA_ICON = "public"

/** The area type a city carries, matching what the API sends. */
internal const val CITY_AREA_TYPE = "city"

/** The glyph a city wears, so it is told apart from the generic globe. */
internal const val CITY_ICON = "location_city"

/**
 * The glyph an area wears when it has no image to show. Cities get their own
 * icon so they are told apart from the globe used for countries and everything
 * else. Shared by the search results and the map's area chips, so an area looks
 * the same in both.
 */
internal fun areaIcon(areaType: String): String = when (areaType) {
    CITY_AREA_TYPE -> CITY_ICON
    else -> AREA_ICON
}
