package search

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import db.Database
import org.btcmap.R
import db.Location
import icons.PlaceIconsRepository
import db.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import location.AndroidLocation
import org.koin.android.annotation.KoinViewModel
import java.text.NumberFormat
import kotlin.system.measureTimeMillis

@KoinViewModel
class PlacesSearchModel(
    private val placeIconsRepo: PlaceIconsRepository,
    private val app: Application,
    private val db: Database,
) : ViewModel() {

    companion object {
        private const val TAG = "search"

        private const val MIN_QUERY_LENGTH = 3

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }

    private val location = MutableStateFlow<Location?>(null)

    private val searchString = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<List<PlacesSearchAdapter.Item>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    init {
        combine(location, searchString) { location, searchString ->
            if (searchString.length < MIN_QUERY_LENGTH) {
                _searchResults.update { emptyList() }
            } else {
                var places: List<Place>

                val queryTimeMillis = measureTimeMillis {
                    places = db.placeQueries.selectBySearchString(searchString).asFlow().mapToList().first()
                }

                Log.d(TAG, "Search string: $searchString")
                Log.d(TAG, "Queried ${places.size} places in $queryTimeMillis ms")

                if (location != null) {
                    val sortTimeMillis = measureTimeMillis {
                        places = places.sortedBy {
                            getDistance(
                                startLatitude = location.lat,
                                startLongitude = location.lon,
                                endLatitude = it.lat,
                                endLongitude = it.lon,
                            )
                        }
                    }

                    Log.d(TAG, "Sorted ${places.size} places by distance in $sortTimeMillis ms")
                }

                _searchResults.update { places.map { it.toRow(location) } }
            }
        }.launchIn(viewModelScope)
    }

    fun setLocation(location: Location?) {
        this.location.update { location }
    }

    fun setSearchString(searchString: String) {
        this.searchString.update { searchString }
    }

    private fun Place.toRow(userLocation: Location?): PlacesSearchAdapter.Item {
        val distanceStringBuilder = StringBuilder()

        if (userLocation != null) {
            val placeLocation = Location(lat, lon)
            val distanceKm = userLocation.distanceInKmTo(placeLocation)

            distanceStringBuilder.apply {
                append(DISTANCE_FORMAT.format(distanceKm))
                append(" ")
                append(app.resources.getString(R.string.kilometers_short))
            }
        }

        return PlacesSearchAdapter.Item(
            place = this,
            icon = placeIconsRepo.getIcon(this),
            name = tags["name"]?.jsonPrimitive?.contentOrNull ?: "Unnamed",
            distanceToUser = distanceStringBuilder.toString(),
        )
    }

    private fun Location.distanceInKmTo(location: Location): Double {
        return getDistance(
            lat,
            lon,
            location.lat,
            location.lon,
        ) / 1000.0
    }

    private fun getDistance(
        startLatitude: Double, startLongitude: Double, endLatitude: Double, endLongitude: Double
    ): Double {
        val distance = FloatArray(1)
        AndroidLocation.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance)
        return distance[0].toDouble()
    }
}