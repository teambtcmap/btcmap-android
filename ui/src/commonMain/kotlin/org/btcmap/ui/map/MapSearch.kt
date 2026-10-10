package org.btcmap.ui.map

import org.btcmap.ui.runDbBlocking
import kotlin.time.Clock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.area.AreaSearchProjection
import org.btcmap.i18n.getLocalizedName
import org.btcmap.i18n.getSearchableNames
import org.btcmap.map.EVENT_ICON
import org.btcmap.place.isBoosted
import org.btcmap.search.NominatimPlace
import org.btcmap.search.NominatimSearch
import org.btcmap.search.NominatimViewbox
import org.btcmap.search.SearchAdapterItem
import org.btcmap.search.nameMatchRank
import org.btcmap.util.isUpcoming
import org.btcmap.util.rethrowIfCancellation
import org.maplibre.compose.map.MapState
import kotlin.time.Instant
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MAX_RESULTS = 20
private const val SEARCH_DEBOUNCE_MS = 300L

/**
 * The minimum search-query weight. A Latin character counts one, but a character
 * from a script whose glyphs carry more information — a CJK ideograph, a
 * Japanese kana or a Hangul syllable — counts two, so "肉饼" (two characters, a
 * complete term) is searched like a three-letter word while "ab" is still too
 * broad to run.
 */
private const val MIN_QUERY_WEIGHT = 3

/**
 * Whether [query] carries enough to search for. A plain character count is wrong
 * for the dense scripts, where two characters are a whole word; see
 * [MIN_QUERY_WEIGHT].
 */
internal fun isSearchableQuery(query: String): Boolean =
    query.trim().sumOf { if (it.isDenseScript()) 2 else 1 } >= MIN_QUERY_WEIGHT

/**
 * Whether the character is from a script where one grapheme carries more than a
 * Latin letter: a CJK ideograph (including the compatibility and extension A
 * blocks), Japanese kana, or a Hangul syllable or Jamo.
 */
private fun Char.isDenseScript(): Boolean = when (code) {
    in 0x1100..0x11FF, // Hangul Jamo
    in 0x3040..0x30FF, // Hiragana and Katakana
    in 0x3400..0x4DBF, // CJK Unified Ideographs Extension A
    in 0x4E00..0x9FFF, // CJK Unified Ideographs
    in 0xAC00..0xD7AF, // Hangul Syllables
    in 0xF900..0xFAFF, // CJK Compatibility Ideographs
    -> true
    else -> false
}

/** The search results, whether a search is running, and whether the query is
 * long enough that one has been run (so "no results" can be told apart from
 * "not searching"). The local [items] come from the cache; [nominatim] holds
 * the separate OpenStreetMap group shown below them, and [nominatimLoading]
 * reports whether that group is still being fetched. */
data class SearchResults(
    val items: List<SearchAdapterItem> = emptyList(),
    val nominatim: List<SearchAdapterItem> = emptyList(),
    val loading: Boolean = false,
    val nominatimLoading: Boolean = false,
    val active: Boolean = false,
)

/**
 * The local search results for [query], ported from the app's Views search.
 * Places, areas and events come from the
 * local cache only, so search needs no network call. Results are ordered like
 * the server: boosted places first, then exact, prefix and substring name
 * matches, with proximity breaking ties. A query that is a `lat,lon` pair is
 * offered as a single coordinate result instead.
 *
 * When [nominatimSearch] is given, the same query is also sent to OpenStreetMap
 * through Nominatim and shown as a second group under the local hits. That call
 * is best-effort and paced by the searcher, so it never blocks or fails the
 * local results, which are set as soon as the cache has answered. The request is
 * restricted to the visible map and its rows sorted by distance, so the group
 * holds the OpenStreetMap features around the map, nearest first; when nothing
 * is found in view it falls back to a global search, so a named feature just
 * outside the viewport is still offered.
 */
@Composable
fun rememberSearchResults(
    db: Database,
    state: MapState,
    query: String,
    formatDistance: (Double) -> String,
    nominatimSearch: NominatimSearch? = null,
): SearchResults {
    val latestFormatDistance by rememberUpdatedState(formatDistance)
    var results by remember { mutableStateOf(SearchResults()) }

    LaunchedEffect(db, state, query, nominatimSearch) {
        val trimmed = query.trim()
        if (!isSearchableQuery(trimmed)) {
            results = SearchResults()
            return@LaunchedEffect
        }

        // A typed coordinate pair is a result on its own, with no cache lookup
        // and no debounce: the map can move there straight away.
        coordinateSearchItem(trimmed)?.let { coordinate ->
            results = SearchResults(items = listOf(coordinate), active = true)
            return@LaunchedEffect
        }

        results = SearchResults(loading = true, active = true)
        delay(SEARCH_DEBOUNCE_MS)
        val camera = state.cameraPosition
        val referenceLat = camera?.target?.latitude ?: 0.0
        val referenceLon = camera?.target?.longitude ?: 0.0

        val items = withContext(Dispatchers.IO) {
            search(
                db = db,
                query = trimmed,
                referenceLat = referenceLat,
                referenceLon = referenceLon,
                formatDistance = latestFormatDistance,
            )
        }

        if (nominatimSearch == null) {
            results = SearchResults(items = items, active = true)
            return@LaunchedEffect
        }

        // The local results are already shown; the OpenStreetMap group loads
        // into the same panel once its paced request completes.
        results = SearchResults(items = items, active = true, nominatimLoading = true)
        // Restrict the remote search to what the user can see. A bare viewbox is
        // only a ranking boost, which a generic term ignores in favour of
        // globally-named features, so the request is bounded to turn "cafe" into
        // the cafes around the map rather than any feature named "Cafe".
        val viewbox = state.viewport?.visibleBounds?.let { visible ->
            val bounds = ViewportBounds.expand(visible, scaleFactor = 1.0)
            NominatimViewbox(
                west = bounds.west,
                south = bounds.south,
                east = bounds.east,
                north = bounds.north,
            )
        }
        val nominatim = nominatimResults(
            search = { box, bounded ->
                nominatimSearch.search(trimmed, viewbox = box, bounded = bounded)
            },
            referenceLat = referenceLat,
            referenceLon = referenceLon,
            formatDistance = latestFormatDistance,
            viewbox = viewbox,
        )
        results = SearchResults(items = items, nominatim = nominatim, active = true)
    }

    return results
}

/**
 * Runs [search] and maps its hits to search rows, measuring each one from the
 * map centre and ordering them nearest first. [search] is called bounded to the
 * viewbox first; when that finds nothing the view is empty of matches, so it is
 * called again with the viewbox only as a soft bias, so a named feature just
 * outside the viewport (or elsewhere) is still offered. The remote service only
 * ever adds to the local results, so any failure is swallowed and an empty group
 * is returned.
 */
internal suspend fun nominatimResults(
    search: suspend (viewbox: NominatimViewbox?, bounded: Boolean) -> List<NominatimPlace>,
    referenceLat: Double,
    referenceLon: Double,
    formatDistance: (Double) -> String,
    viewbox: NominatimViewbox?,
): List<SearchAdapterItem> = try {
    val local = search(viewbox, viewbox != null)
    val places = if (local.isEmpty() && viewbox != null) search(viewbox, false) else local

    places
        .map { place -> place to distanceMeters(referenceLat, referenceLon, place.lat, place.lon) }
        // Nominatim's order is not by distance, so sort explicitly to guarantee
        // nearest-first, matching the local group's proximity order.
        .sortedBy { (_, distance) -> distance }
        .map { (place, distance) ->
            SearchAdapterItem.Nominatim(
                lat = place.lat,
                lon = place.lon,
                icon = NOMINATIM_ICON,
                name = place.name,
                distanceToUser = formatDistance(distance),
            )
        }
} catch (t: Throwable) {
    t.rethrowIfCancellation()
    emptyList()
}

/** The glyph on an OpenStreetMap group row. */
private const val NOMINATIM_ICON = "travel_explore"

private fun AreaSearchProjection.searchBbox(): List<Double>? {
    val west = bboxWest ?: return null
    val south = bboxSouth ?: return null
    val east = bboxEast ?: return null
    val north = bboxNorth ?: return null
    return listOf(west, south, east, north)
}

internal fun search(
    db: Database,
    query: String,
    referenceLat: Double,
    referenceLon: Double,
    formatDistance: (Double) -> String,
): List<SearchAdapterItem> {
    val now = Clock.System.now()

    val areas = runDbBlocking { db.area.selectBySearchString(query) }.mapNotNull { area ->
        val bbox = area.searchBbox()
        val center = bbox?.let { (it[1] + it[3]) / 2 to (it[0] + it[2]) / 2 }

        localMatch(query, area.getSearchableNames(), center, referenceLat, referenceLon, formatDistance) { distance ->
            SearchAdapterItem.Area(
                areaId = area.id,
                bbox = bbox,
                iconUrl = area.icon,
                headerImageUrl = area.iconWide ?: area.icon,
                icon = areaIcon(area.type),
                name = area.getLocalizedName(),
                distanceToUser = distance,
            )
        }
    }

    val places = runDbBlocking { db.place.selectBySearchString(query) }.mapNotNull { place ->
        val boosted = place.isBoosted(now)
        localMatch(
            query = query,
            names = place.getSearchableNames(),
            location = place.lat to place.lon,
            referenceLat = referenceLat,
            referenceLon = referenceLon,
            formatDistance = formatDistance,
            boosted = boosted,
        ) { distance ->
            SearchAdapterItem.Place(
                placeId = place.id,
                icon = place.icon,
                name = place.getLocalizedName(),
                distanceToUser = distance,
                boosted = boosted,
            )
        }
    }

    val events = runDbBlocking { db.event.selectBySearchString(query) }
        .filter { it.startsAt.isUpcoming(now) }
        .mapNotNull { event ->
            localMatch(
                query = query,
                names = listOf(event.name),
                location = event.lat to event.lon,
                referenceLat = referenceLat,
                referenceLon = referenceLon,
                formatDistance = formatDistance,
            ) { distance ->
                SearchAdapterItem.Event(
                    eventId = event.id,
                    icon = EVENT_ICON,
                    name = event.name,
                    distanceToUser = distance,
                )
            }
        }

    return (areas + places + events)
        .sortedWith(
            compareBy(
                { if (it.boosted) 0 else 1 },
                { it.rank },
                { it.distance ?: Double.MAX_VALUE },
            )
        )
        .take(MAX_RESULTS)
        .map { it.item }
}

/**
 * Builds a [LocalMatch], computing the relevance rank and the formatted distance
 * to the map centre. Returns null when none of [names] matches, so a row the SQL
 * pre-filter returned for an unrelated reason is dropped.
 */
private inline fun localMatch(
    query: String,
    names: List<String>,
    location: Pair<Double, Double>?,
    referenceLat: Double,
    referenceLon: Double,
    formatDistance: (Double) -> String,
    boosted: Boolean = false,
    item: (String?) -> SearchAdapterItem,
): LocalMatch? {
    val rank = nameMatchRank(names, query) ?: return null
    val distance = location?.let {
        distanceMeters(referenceLat, referenceLon, it.first, it.second)
    }
    return LocalMatch(
        rank = rank,
        distance = distance,
        boosted = boosted,
        item = item(distance?.let(formatDistance)),
    )
}

private class LocalMatch(
    val rank: Int,
    val distance: Double?,
    val boosted: Boolean,
    val item: SearchAdapterItem,
)

/** Distance in metres between two points, on a spherical earth. */
private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * earthRadius * asin(sqrt(a))
}
