package org.btcmap.place

import kotlin.time.Clock
import kotlin.time.Instant
import org.btcmap.db.table.area.AreaGeometry
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.place.SearchPlace

fun Place.isMerchant(): Boolean {
    return icon != "local_atm" && icon != "currency_exchange"
}

/**
 * Whether the place's boost is still active at [now]. A place with no boost, or
 * one whose boost expired, is not boosted.
 */
fun Place.isBoosted(now: Instant = Clock.System.now()): Boolean {
    return boostedUntil?.let { it > now } == true
}

/** [isBoosted] for a search row, which omits fields search does not use. */
fun SearchPlace.isBoosted(now: Instant = Clock.System.now()): Boolean {
    return boostedUntil?.let { it > now } == true
}

/**
 * Whether the place falls inside the area geometry. Mirrors
 * [org.btcmap.db.table.event.isWithin]: the caller pre-filters on the area's
 * bbox and this repeats the point-in-polygon test the server applies.
 */
fun Place.isWithin(geometry: AreaGeometry): Boolean = geometry.contains(lat, lon)

/** The place on btcmap.org, e.g. "https://btcmap.org/merchant/123". */
fun Place.btcmapUrl(): String = "https://btcmap.org/merchant/$id"

/**
 * OpenStreetMap centred on the place. The Android app's directions action uses a
 * `geo:` URI instead, which a desktop browser cannot act on.
 */
fun Place.osmMapUrl(): String =
    "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=17/$lat/$lon"

/**
 * The place's OpenStreetMap page, e.g. "https://www.openstreetmap.org/node/123".
 * Null when no OSM id is known.
 */
fun Place.osmUrl(): String? = osmId?.toOsmUrl()

/**
 * The place's OpenStreetMap editor page, e.g.
 * "https://www.openstreetmap.org/edit?node=123", which the web editor opens
 * with the element selected. Null when no OSM id is known.
 */
fun Place.osmEditUrl(): String? = osmId?.toOsmEditUrl()

/**
 * Builds an OpenStreetMap page URL from a "type:id" identifier, e.g.
 * "node:123" becomes "https://www.openstreetmap.org/node/123". The v4 API
 * stores [Place.osmId] in this colon-separated form. Null when either part is
 * missing.
 */
fun String.toOsmUrl(): String? {
    val parts = split(':', limit = 2)
    val type = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
    val id = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    return "https://www.openstreetmap.org/$type/$id"
}

/**
 * Builds an OpenStreetMap editor URL from a "type:id" identifier, e.g.
 * "node:123" becomes "https://www.openstreetmap.org/edit?node=123". Null when
 * either part is missing.
 */
fun String.toOsmEditUrl(): String? {
    val parts = split(':', limit = 2)
    val type = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
    val id = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
    return "https://www.openstreetmap.org/edit?$type=$id"
}
