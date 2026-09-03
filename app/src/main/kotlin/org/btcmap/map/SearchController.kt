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
import org.btcmap.api.Api
import org.btcmap.api.SearchResult
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.btcmap.search.SearchAdapterItem
import java.text.NumberFormat

class SearchController(
    private val db: Database,
    private val api: Api,
    private val resources: Resources,
    private val isOnline: () -> Boolean,
) {
    private val _results = MutableStateFlow<List<SearchAdapterItem>>(emptyList())
    val results: StateFlow<List<SearchAdapterItem>> = _results.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentJob: Job? = null
    private var currentQuery: String? = null
    private val areaIconCache = mutableMapOf<Long, String?>()

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

        val rawResults = fetchResults(query, referenceLocation) ?: return
        if (currentQuery != query) return

        rawResults.filterIsInstance<SearchResult.Area>().forEach { area ->
            areaIconCache[area.id] = area.iconUrl
        }

        if (currentQuery != query) return
        _results.value = rawResults.map { it.toAdapterItem(referenceLocation) }
    }

    private suspend fun fetchResults(
        query: String,
        referenceLocation: LatLng,
    ): List<SearchResult>? {
        if (!isOnline()) {
            emitLocalPlaces(query, referenceLocation)
            return null
        }
        val apiResult = runCatching {
            withContext(Dispatchers.IO) {
                api.search(
                    query = query,
                    lat = referenceLocation.latitude,
                    lon = referenceLocation.longitude,
                    limit = 20,
                )
            }
        }
        return when {
            apiResult.isSuccess -> apiResult.getOrThrow()
            currentQuery == query -> {
                emitLocalPlaces(query, referenceLocation)
                null
            }
            else -> null
        }
    }

    private suspend fun emitLocalPlaces(query: String, referenceLocation: LatLng) {
        val unsortedPlaces = withContext(Dispatchers.IO) {
            db.place.selectBySearchString(query)
        }
        val sortedPlaces = unsortedPlaces.sortedBy {
            distanceInMeters(
                start = referenceLocation,
                end = LatLng(it.lat, it.lon),
            )
        }
        _results.value = sortedPlaces.map { it.toAdapterItem(referenceLocation) }
    }

    private fun SearchResult.toAdapterItem(referenceLocation: LatLng): SearchAdapterItem {
        return when (this) {
            is SearchResult.Area -> {
                val bounds = bbox?.takeIf { it.size == 4 }?.let {
                    LatLngBounds.from(
                        latNorth = it[3],
                        lonEast = it[2],
                        latSouth = it[1],
                        lonWest = it[0],
                    )
                }
                val meters = bounds?.center?.let { distanceInMeters(referenceLocation, it) }
                SearchAdapterItem.Area(
                    areaId = id,
                    bbox = bbox,
                    iconUrl = iconUrl ?: areaIconCache[id],
                    icon = AREA_ICON,
                    name = name,
                    distanceToUser = meters?.let { formatDistance(it) },
                )
            }

            is SearchResult.Place -> {
                val meters = distanceInMeters(referenceLocation, LatLng(lat, lon))
                SearchAdapterItem.Place(
                    placeId = id,
                    icon = icon,
                    name = name,
                    distanceToUser = formatDistance(meters),
                )
            }
        }
    }

    private fun Place.toAdapterItem(referenceLocation: LatLng): SearchAdapterItem {
        val meters = referenceLocation.distanceTo(LatLng(lat, lon))
        return SearchAdapterItem.Place(
            placeId = id,
            icon = icon,
            name = name ?: "",
            distanceToUser = formatDistance(meters),
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

    companion object {
        private const val MIN_QUERY_LENGTH = 3

        private const val AREA_ICON = "public"

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }
}
