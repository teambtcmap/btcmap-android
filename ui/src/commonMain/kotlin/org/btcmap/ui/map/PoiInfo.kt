package org.btcmap.ui.map

import kotlin.math.roundToLong
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point

/**
 * A non-BTC-Map OpenStreetMap feature under a map tap, read from one of the
 * basemap's POI symbol layers rather than from the app's own marker layers.
 *
 * Only the fields the info sheet shows are kept: the name in the app's language,
 * the feature's original name, the OSM `class`/`subclass` tokens, and the
 * point's coordinates, which a later "add as place/note" action will need.
 */
data class PoiInfo(
    /** The name in the app's language, or the basemap's own label as a fallback. */
    val name: String,
    val classId: String?,
    val subclass: String?,
    val lat: Double,
    val lon: Double,
    /**
     * The feature's original-language name, shown beside [name] when the tile
     * carries one and it is not already part of the label.
     */
    val localName: String? = null,
)

/**
 * The basemap style layers that draw POI symbols, across every bundled style.
 * A style that does not declare a given id simply matches nothing, so one set
 * serves them all. The place and road label layers are deliberately left out, so
 * a tap on a street or city name is not read as a place.
 */
internal val POI_LAYER_IDS: Set<String> = setOf(
    "poi_r20",
    "poi_r7",
    "poi_r1",
    "poi_transit",
    "poi_stadium",
    "poi_park",
)

/**
 * Reads a queried basemap feature into a [PoiInfo], or null when it carries no
 * usable name or is not a point.
 *
 * [language] is the app's current language: the feature's name for it is used
 * when the tile carries one, and otherwise the basemap's own label is. The
 * original-language name is kept alongside when the tile has one.
 */
internal fun Feature<Geometry, JsonObject?>.toPoiInfo(language: String): PoiInfo? {
    val properties = properties ?: return null
    val name = properties.poiName(language) ?: return null
    val point = geometry as? Point ?: return null
    return PoiInfo(
        name = name,
        classId = properties.string("class"),
        subclass = properties.string("subclass"),
        lat = point.latitude,
        lon = point.longitude,
        localName = properties.poiLocalName(name),
    )
}

/**
 * The feature's name in [language] — the tile carries these as `name:<lang>`
 * (and `name_<lang>` for a few, such as English and German) — falling back to
 * the basemap's own label.
 */
private fun JsonObject.poiName(language: String): String? =
    string("name:$language") ?: string("name_$language") ?: mapLabel()

/**
 * The name the tile's label draws, mirroring its `text-field` expression: the
 * Latin and non-Latin names together where a non-Latin name exists, otherwise
 * the English name or the plain name. The Latin name on its own is not used,
 * because a tile can carry a short `name:latin` beside a fuller plain `name`.
 */
private fun JsonObject.mapLabel(): String? {
    val nonLatin = string("name:nonlatin")
    if (nonLatin != null) {
        val latin = string("name:latin")
        return if (latin != null && latin != nonLatin) "$latin · $nonLatin" else nonLatin
    }
    return string("name_en") ?: string("name:en") ?: string("name") ?: string("name:latin")
}

/** The local, non-Latin `name` when it adds to the label [displayName], else null. */
private fun JsonObject.poiLocalName(displayName: String): String? {
    // An OSM element's own-language name can live in any of these, and a name
    // already inside the label (its non-Latin half) must not be repeated.
    val candidates = listOf("name:nonlatin", "name", "name:zh-Hans", "name:zh-Hant", "name:zh")
    return candidates.firstNotNullOfOrNull { key ->
        string(key)?.takeIf { it.isNonLatin() && !displayName.contains(it) }
    }
}

/** Whether [this] holds a character outside the Latin script. */
private fun String.isNonLatin(): Boolean = any { it.code > 0x024F }

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

/**
 * The OSM `class`/`subclass` tokens as a readable label, e.g. `Lodging · Hotel`,
 * or null when the feature carries neither. A generic `subclass` of `yes` is
 * dropped, and a `subclass` that repeats the class is not shown twice.
 */
internal fun PoiInfo.categoryLabel(): String? {
    val tokens = listOfNotNull(
        classId,
        subclass?.takeIf { it != classId && it != "yes" },
    )
    if (tokens.isEmpty()) return null
    return tokens.joinToString(" · ") { token ->
        token.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
    }
}

/**
 * Whether the feature's name matches [query] leniently: case, spaces and
 * punctuation are ignored and a substring on either side counts, so a Nominatim
 * hit matches the shortened or localized label the tile carries. A
 * comma-separated [query] also matches on any of its segments, because a
 * Nominatim `display_name` ("Foo Bar, City, Country") reorders the name so the
 * whole string is not a substring of the feature's own name.
 */
internal fun PoiInfo.matchesName(query: String): Boolean {
    val haystacks = listOfNotNull(name, localName).map { it.matchKey() }.filter { it.isNotEmpty() }
    if (haystacks.isEmpty()) return false

    val needle = query.matchKey()
    if (needle.isEmpty()) return false
    if (haystacks.any { it.contains(needle) || needle.contains(it) }) return true

    return query.split(',')
        .map { it.matchKey() }
        .filter { it.length >= MIN_MATCH_SEGMENT }
        .any { segment -> haystacks.any { it.contains(segment) || segment.contains(it) } }
}

/** The shortest query segment that may match on its own, so a stray word does not. */
private const val MIN_MATCH_SEGMENT = 2

/** [this] reduced to its letters and digits, lowercased, for lenient comparison. */
private fun String.matchKey(): String = filter { it.isLetterOrDigit() }.lowercase()

/**
 * The feature's OSM category token to pre-fill a place submission with, e.g.
 * `cafe` or `hotel`, or null when the feature carries neither token.
 *
 * The basemap's `subclass` is the specific value (the tiles carry a hotel as
 * `class=lodging, subclass=hotel`) and matches the API's OSM-style categories, so
 * it is preferred; the broader `class` is the fallback, with a `subclass` that is
 * generic (`yes`) or merely repeats the class dropped.
 */
fun PoiInfo.categoryToken(): String? =
    subclass?.takeIf { it.isNotBlank() && it != "yes" && it != classId }
        ?: classId?.takeIf { it.isNotBlank() }

/** A Material Symbols glyph matching the feature's OSM class, or a generic pin. */
internal fun PoiInfo.categoryGlyph(): String = when (classId) {
    "bar", "pub", "nightclub", "biergarten" -> "local_bar"
    "restaurant", "fast_food", "food_court", "cafe", "ice_cream" -> "restaurant"
    "lodging", "hotel", "hostel", "motel", "guest_house", "chalet" -> "hotel"
    "bank", "atm", "bureau_de_change" -> "account_balance"
    "hospital", "clinic", "doctors", "pharmacy", "dentist", "veterinary" -> "medical_services"
    "school", "college", "university", "kindergarten", "library" -> "school"
    "shop", "supermarket", "convenience", "grocery", "bakery", "clothes", "mall" -> "storefront"
    "fuel", "charging_station", "car_repair" -> "local_gas_station"
    "parking", "bicycle_parking" -> "local_parking"
    "museum", "gallery" -> "museum"
    "park", "garden", "playground" -> "park"
    "attraction", "artwork", "viewpoint", "monument", "castle" -> "attractions"
    "airport", "rail", "bus", "transit", "station", "halt", "subway" -> "directions_transit"
    else -> "place"
}

/** The point as `lat, lon`, rounded to five decimals, which is about a metre. */
internal fun PoiInfo.coordinatesLabel(): String = "${round(lat)}, ${round(lon)}"

/**
 * The feature's position on openstreetmap.org. The basemap's tiles carry no OSM
 * element id (the `poi` layer has only `class`, `subclass`, `name` and `rank`), so
 * the map knows where the feature is but not which element it is; the link points
 * at the position rather than at a specific element.
 */
fun PoiInfo.osmUrl(): String =
    "https://www.openstreetmap.org/?mlat=${round(lat)}&mlon=${round(lon)}" +
        "#map=19/${round(lat)}/${round(lon)}"

/** The openstreetmap.org iD editor opened at the feature's position (see [osmUrl]). */
fun PoiInfo.osmEditUrl(): String =
    "https://www.openstreetmap.org/edit#map=19/${round(lat)}/${round(lon)}"

private fun round(value: Double): String = ((value * 100_000).roundToLong() / 100_000.0).toString()
