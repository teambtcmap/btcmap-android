package org.btcmap.place

import org.btcmap.db.table.area.AreaGeometry
import org.btcmap.db.table.place.Place
import java.time.ZonedDateTime

fun Place.isMerchant(): Boolean {
    return icon != "local_atm" && icon != "currency_exchange"
}

/**
 * Whether the place's boost is still active at [now]. A place with no boost, or
 * one whose boost expired, is not boosted.
 */
fun Place.isBoosted(now: ZonedDateTime = ZonedDateTime.now()): Boolean {
    return boostedUntil?.isAfter(now) == true
}

/**
 * Whether the place falls inside the area geometry. Mirrors
 * [org.btcmap.db.table.event.isWithin]: the caller pre-filters on the area's
 * bbox and this repeats the point-in-polygon test the server applies.
 */
internal fun Place.isWithin(geometry: AreaGeometry): Boolean = geometry.contains(lat, lon)

/**
 * The place's OpenStreetMap page, e.g. "https://www.openstreetmap.org/node/123".
 * Null when no OSM id is known.
 */
fun Place.osmUrl(): String? = osmId?.toOsmUrl()

/**
 * Builds an OpenStreetMap page URL from a "type:id" identifier, e.g.
 * "node:123" becomes "https://www.openstreetmap.org/node/123". The v4 API
 * stores [Place.osmId] in this colon-separated form. Null when either part is
 * missing.
 */
internal fun String.toOsmUrl(): String? {
    val parts = split(':', limit = 2)
    val type = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
    val id = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    return "https://www.openstreetmap.org/$type/$id"
}
