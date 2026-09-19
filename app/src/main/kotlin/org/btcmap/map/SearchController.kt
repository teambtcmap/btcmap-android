package org.btcmap.map

import android.content.res.Resources
import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.search.SearchAdapterItem
import org.maplibre.android.geometry.LatLng
import java.text.NumberFormat
import java.time.ZonedDateTime

/**
 * Searches the local cache only. Places, areas and events are kept in SQLite by
 * [org.btcmap.Sync], so search needs no network call and works offline.
 *
 * Each entity is matched with a case-insensitive substring on its name. Results
 * are ordered like the server's `GET /v4/search`: exact name matches first, then
 * prefix matches, then substring matches, with proximity breaking ties.
 */
class SearchController(
    private val db: Database,
    private val resources: Resources,
) {
    private val _results = MutableStateFlow<List<SearchAdapterItem>>(emptyList())
    val results: StateFlow<List<SearchAdapterItem>> = _results.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentJob: Job? = null
    private var currentQuery: String? = null

    fun search(referenceLocation: LatLng, query: String) {
        val trimmed = query.trim()
        currentQuery = trimmed
        currentJob?.cancel("superseded by newer search")
        currentJob = scope.launch {
            run(trimmed, referenceLocation)
        }
    }

    fun clear() {
        currentJob?.cancel("cleared")
        currentJob = null
        currentQuery = null
        _results.value = emptyList()
    }

    fun dispose() {
        currentJob?.cancel("disposed")
        scope.cancel()
    }

    private suspend fun run(query: String, referenceLocation: LatLng) {
        if (query.length < MIN_QUERY_LENGTH) {
            _results.value = emptyList()
            return
        }

        val matches = withContext(Dispatchers.IO) {
            val now = ZonedDateTime.now()

            val areas = db.area.selectBySearchString(query).map { area ->
                val bbox = area.searchBbox()
                val center = bbox?.let { LatLng((it[1] + it[3]) / 2, (it[0] + it[2]) / 2) }
                localMatch(query, area.name, center, referenceLocation) { distanceToUser ->
                    SearchAdapterItem.Area(
                        areaId = area.id,
                        bbox = bbox,
                        iconUrl = area.icon,
                        headerImageUrl = area.iconWide ?: area.icon,
                        icon = AREA_ICON,
                        name = area.name,
                        distanceToUser = distanceToUser,
                    )
                }
            }

            val places = db.place.selectBySearchString(query).map { place ->
                val name = place.name ?: ""
                localMatch(query, name, LatLng(place.lat, place.lon), referenceLocation) { distanceToUser ->
                    SearchAdapterItem.Place(
                        placeId = place.id,
                        icon = place.icon,
                        name = name,
                        distanceToUser = distanceToUser,
                    )
                }
            }

            val events = db.event.selectBySearchString(query)
                .filter { it.isUpcomingOrUndated(now) }
                .map { event ->
                    localMatch(query, event.name, LatLng(event.lat, event.lon), referenceLocation) { distanceToUser ->
                        SearchAdapterItem.Event(
                            eventId = event.id,
                            icon = EVENT_ICON,
                            name = event.name,
                            distanceToUser = distanceToUser,
                        )
                    }
                }

            areas + places + events
        }

        if (currentQuery != query) return

        _results.value = matches
            .sortedWith(
                compareBy(
                    { it.rank },
                    { it.distance ?: Double.MAX_VALUE },
                )
            )
            .take(MAX_RESULTS)
            .map { it.item }
    }

    /**
     * Builds a [LocalMatch], computing the relevance rank and the formatted
     * distance to [location] (null when the entity has no location to measure,
     * e.g. an area without a bounding box).
     */
    private inline fun localMatch(
        query: String,
        name: String,
        location: LatLng?,
        referenceLocation: LatLng,
        item: (String?) -> SearchAdapterItem,
    ): LocalMatch {
        val distance = location?.let { distanceInMeters(referenceLocation, it) }
        return LocalMatch(
            rank = matchRank(name, query),
            distance = distance,
            item = item(distance?.let { formatDistance(it) }),
        )
    }

    /**
     * Mirrors the server's ranking: exact name match, then name prefix, then a
     * name substring. Everything local matches on the name, so there is no
     * lower "matched another tag" rank.
     */
    private fun matchRank(name: String, query: String): Int {
        return when {
            name.equals(query, ignoreCase = true) -> 0
            name.startsWith(query, ignoreCase = true) -> 1
            else -> 2
        }
    }

    private fun Area.searchBbox(): List<Double>? {
        return listOf(
            bboxWest ?: return null,
            bboxSouth ?: return null,
            bboxEast ?: return null,
            bboxNorth ?: return null,
        )
    }

    private fun formatDistance(meters: Double): String {
        return if (meters < 1_000) {
            resources.getString(R.string.s_m, DISTANCE_FORMAT.format(meters))
        } else {
            resources.getString(R.string.s_km, DISTANCE_FORMAT.format(meters / 1_000))
        }
    }

    private fun distanceInMeters(start: LatLng, end: LatLng): Double {
        val result = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            result,
        )
        return result[0].toDouble()
    }

    private class LocalMatch(
        val rank: Int,
        val distance: Double?,
        val item: SearchAdapterItem,
    )

    companion object {
        private const val MIN_QUERY_LENGTH = 3
        private const val MAX_RESULTS = 20

        private const val AREA_ICON = "public"

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }
}
