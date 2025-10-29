package search

import android.app.Application
import android.location.Location
import androidx.lifecycle.ViewModel
import db.db
import db.table.place.Place
import db.table.place.PlaceQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.btcmap.R
import org.maplibre.android.geometry.LatLng
import java.text.NumberFormat

class SearchModel(
    private val app: Application,
) : ViewModel() {

    companion object {
        private const val MIN_QUERY_LENGTH = 3

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }

    private val _searchResults = MutableStateFlow<List<SearchAdapterItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    suspend fun search(referenceLocation: LatLng, searchString: String) {
        if (searchString.length < MIN_QUERY_LENGTH) {
            _searchResults.update { emptyList() }
        } else {
            val unsortedPlaces = withContext(Dispatchers.IO) {
                PlaceQueries.selectBySearchString(searchString, db)
            }

            val sortedPlaces = unsortedPlaces.sortedBy {
                getDistanceInMeters(
                    startLatitude = referenceLocation.latitude,
                    startLongitude = referenceLocation.longitude,
                    endLatitude = it.lat,
                    endLongitude = it.lon,
                )
            }

            _searchResults.update { sortedPlaces.map { it.toAdapterItem(referenceLocation) } }
        }
    }

    private fun Place.toAdapterItem(referenceLocation: LatLng): SearchAdapterItem {
        val distanceMeters = referenceLocation.distanceTo(LatLng(lat, lon))

        val distanceString = if (distanceMeters < 1_000) {
            app.resources.getString(R.string.s_m, DISTANCE_FORMAT.format(distanceMeters))
        } else {
            app.resources.getString(R.string.s_km, DISTANCE_FORMAT.format(distanceMeters / 1_000))
        }

        return SearchAdapterItem(
            placeId = this.id,
            icon = this.icon,
            name = this.name ?: "",
            distanceToUser = distanceString,
        )
    }

    private fun getDistanceInMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Double {
        val distance = FloatArray(1)
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance)
        return distance[0].toDouble()
    }
}