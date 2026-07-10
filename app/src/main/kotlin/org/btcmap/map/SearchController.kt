package org.btcmap.map

import android.content.res.Resources
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.maplibre.android.geometry.LatLng
import org.btcmap.search.SearchAdapterItem
import java.text.NumberFormat

class SearchController(
    private val db: Database,
    private val resources: Resources,
) {
    private val _results = MutableStateFlow<List<SearchAdapterItem>>(emptyList())
    val results: StateFlow<List<SearchAdapterItem>> = _results.asStateFlow()

    suspend fun search(referenceLocation: LatLng, query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            _results.update { emptyList() }
            return
        }

        val unsortedPlaces = withContext(Dispatchers.IO) {
            db.place.selectBySearchString(query)
        }

        val sortedPlaces = unsortedPlaces.sortedBy {
            distanceInMeters(
                start = referenceLocation,
                end = LatLng(it.lat, it.lon),
            )
        }

        _results.update { sortedPlaces.map { it.toAdapterItem(referenceLocation) } }
    }

    private fun Place.toAdapterItem(referenceLocation: LatLng): SearchAdapterItem {
        val meters = referenceLocation.distanceTo(LatLng(lat, lon))
        val distance = if (meters < 1_000) {
            resources.getString(R.string.s_m, DISTANCE_FORMAT.format(meters))
        } else {
            resources.getString(R.string.s_km, DISTANCE_FORMAT.format(meters / 1_000))
        }

        return SearchAdapterItem(
            placeId = id,
            icon = icon,
            name = name ?: "",
            distanceToUser = distance,
        )
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

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }
}
