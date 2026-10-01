package org.btcmap.ui.map

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
import org.btcmap.search.SearchAdapterItem
import org.btcmap.search.nameMatchRank
import org.btcmap.util.isUpcoming
import org.maplibre.compose.map.MapState
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MIN_QUERY_LENGTH = 3
private const val MAX_RESULTS = 20
private const val SEARCH_DEBOUNCE_MS = 300L
private const val AREA_ICON = "public"

/**
 * The local search results for [query], ported from
 * `org.btcmap.map.SearchController`. Places, areas and events come from the
 * local cache only, so search needs no network call. Results are ordered like
 * the server: boosted places first, then exact, prefix and substring name
 * matches, with proximity breaking ties.
 */
@Composable
fun rememberSearchResults(
    db: Database,
    state: MapState,
    query: String,
    formatDistance: (Double) -> String,
): List<SearchAdapterItem> {
    val latestFormatDistance by rememberUpdatedState(formatDistance)
    var results by remember { mutableStateOf<List<SearchAdapterItem>>(emptyList()) }

    LaunchedEffect(db, state, query) {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            results = emptyList()
            return@LaunchedEffect
        }

        delay(SEARCH_DEBOUNCE_MS)
        val camera = state.cameraPosition
        results = withContext(Dispatchers.IO) {
            search(
                db = db,
                query = trimmed,
                referenceLat = camera?.target?.latitude ?: 0.0,
                referenceLon = camera?.target?.longitude ?: 0.0,
                formatDistance = latestFormatDistance,
            )
        }
    }

    return results
}

private fun AreaSearchProjection.searchBbox(): List<Double>? {
    val west = bboxWest ?: return null
    val south = bboxSouth ?: return null
    val east = bboxEast ?: return null
    val north = bboxNorth ?: return null
    return listOf(west, south, east, north)
}

private fun search(
    db: Database,
    query: String,
    referenceLat: Double,
    referenceLon: Double,
    formatDistance: (Double) -> String,
): List<SearchAdapterItem> {
    val now = ZonedDateTime.now()

    val areas = db.area.selectBySearchString(query).mapNotNull { area ->
        val bbox = area.searchBbox()
        val center = bbox?.let { (it[1] + it[3]) / 2 to (it[0] + it[2]) / 2 }

        localMatch(query, area.getSearchableNames(), center, referenceLat, referenceLon, formatDistance) { distance ->
            SearchAdapterItem.Area(
                areaId = area.id,
                bbox = bbox,
                iconUrl = area.icon,
                headerImageUrl = area.iconWide ?: area.icon,
                icon = AREA_ICON,
                name = area.getLocalizedName(),
                distanceToUser = distance,
            )
        }
    }

    val places = db.place.selectBySearchString(query).mapNotNull { place ->
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

    val events = db.event.selectBySearchString(query)
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
