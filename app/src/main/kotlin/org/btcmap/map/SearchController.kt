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
import org.btcmap.i18n.getLocalizedName
import org.btcmap.i18n.getSearchableNames
import org.btcmap.place.isBoosted
import org.btcmap.search.SearchAdapterItem
import org.btcmap.search.nameMatchRank
import org.btcmap.util.isUpcoming
import org.maplibre.android.geometry.LatLng
import java.text.NumberFormat
import java.time.ZonedDateTime

/**
 * Searches the local cache only. Places, areas and events are kept in SQLite by
 * [org.btcmap.Sync], so search needs no network call and works offline.
 *
 * Each entity is matched with a case-insensitive substring on any of its names,
 * including the per-language translations, and is shown under the device
 * language. Results are ordered like the server's `GET /v4/search`: exact name
 * matches first, then prefix matches, then substring matches, with proximity
 * breaking ties. Boosted places are promoted above that relevance order,
 * matching the website, so a boost is visible in search.
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

            val areas = db.area.selectBySearchString(query).mapNotNull { area ->
                val bbox = area.searchBbox()
                val center = bbox?.let { LatLng((it[1] + it[3]) / 2, (it[0] + it[2]) / 2) }
                localMatch(query, area.getSearchableNames(), center, referenceLocation) { distanceToUser ->
                    SearchAdapterItem.Area(
                        areaId = area.id,
                        bbox = bbox,
                        iconUrl = area.icon,
                        headerImageUrl = area.iconWide ?: area.icon,
                        icon = AREA_ICON,
                        name = area.getLocalizedName(),
                        distanceToUser = distanceToUser,
                    )
                }
            }

            val places = db.place.selectBySearchString(query).mapNotNull { place ->
                val boosted = place.isBoosted(now)
                localMatch(
                    query = query,
                    names = place.getSearchableNames(),
                    location = LatLng(place.lat, place.lon),
                    referenceLocation = referenceLocation,
                    boosted = boosted,
                ) { distanceToUser ->
                    SearchAdapterItem.Place(
                        placeId = place.id,
                        icon = place.icon,
                        name = place.getLocalizedName(),
                        distanceToUser = distanceToUser,
                        boosted = boosted,
                    )
                }
            }

            val events = db.event.selectBySearchString(query)
                .filter { it.startsAt.isUpcoming(now) }
                .mapNotNull { event ->
                    localMatch(query, listOf(event.name), LatLng(event.lat, event.lon), referenceLocation) { distanceToUser ->
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
                    // Boosted places are promoted above the relevance order, like
                    // the website: premium placement, then the server's ranking.
                    { if (it.boosted) 0 else 1 },
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
     * e.g. an area without a bounding box). Returns null when none of [names]
     * matches, so a row the SQL pre-filter returned for an unrelated reason is
     * dropped.
     */
    private inline fun localMatch(
        query: String,
        names: List<String>,
        location: LatLng?,
        referenceLocation: LatLng,
        boosted: Boolean = false,
        item: (String?) -> SearchAdapterItem,
    ): LocalMatch? {
        val rank = nameMatchRank(names, query) ?: return null
        val distance = location?.let { distanceInMeters(referenceLocation, it) }
        return LocalMatch(
            rank = rank,
            distance = distance,
            boosted = boosted,
            item = item(distance?.let { formatDistance(it) }),
        )
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
        val boosted: Boolean,
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
