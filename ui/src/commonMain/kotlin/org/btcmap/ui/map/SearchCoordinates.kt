package org.btcmap.ui.map

import org.btcmap.search.SearchAdapterItem

/** The glyph shown for a typed coordinate: a filled location pin. */
private const val COORDINATE_ICON = "place"

/** A latitude/longitude pair parsed from the search field. */
internal data class SearchCoordinates(val lat: Double, val lon: Double)

/**
 * Parses [query] as a `lat,lon` pair typed into the search field, or null when
 * it is not a plain coordinate pair, so ordinary names fall through to the
 * local search.
 *
 * The two numbers may be separated by a comma or whitespace, and a URL-encoded
 * comma (`%2C`), as copied from a map link, is accepted too. Both must be in
 * range, so a stray pair of numbers is never mistaken for a valid location.
 */
internal fun parseCoordinates(query: String): SearchCoordinates? {
    val normalized = query.trim().replace("%2C", ",", ignoreCase = true)
    val match = COORDINATES.matchEntire(normalized) ?: return null
    val lat = match.groupValues[1].toDoubleOrNull() ?: return null
    val lon = match.groupValues[2].toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return SearchCoordinates(lat = lat, lon = lon)
}

/**
 * The search row for [query] when it is a coordinate pair, or null otherwise.
 * The row carries the coordinates so the map can move to them on tap.
 */
internal fun coordinateSearchItem(query: String): SearchAdapterItem.Coordinate? {
    val coordinates = parseCoordinates(query) ?: return null
    return SearchAdapterItem.Coordinate(
        lat = coordinates.lat,
        lon = coordinates.lon,
        icon = COORDINATE_ICON,
        name = "${coordinates.lat}, ${coordinates.lon}",
        distanceToUser = null,
    )
}

private val COORDINATES = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*[, ]\s*(-?\d+(?:\.\d+)?)\s*$""")
