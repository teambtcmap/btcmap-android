package search

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.btcmap.R
import location.Location
import map.PlacesRepository
import map.PlaceIconsRepository
import db.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import java.text.NumberFormat

class PlacesSearchModel(
    private val placesRepo: PlacesRepository,
    private val placeIconsRepo: PlaceIconsRepository,
    private val app: Application,
) : ViewModel() {

    private val location = MutableStateFlow<Location?>(null)

    private val searchString = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<List<PlacesSearchRow>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    init {
        combine(location, searchString) { location, searchString ->
            if (searchString.length < MIN_QUERY_LENGTH) {
                _searchResults.update { emptyList() }
            } else {
                var places = placesRepo.selectBySearchString(searchString).first()

                if (location != null) {
                    places = places.sortedBy {
                        getDistance(
                            startLatitude = location.latitude,
                            startLongitude = location.longitude,
                            endLatitude = it.lat,
                            endLongitude = it.lon,
                        )
                    }
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

    private fun Place.toRow(userLocation: Location?): PlacesSearchRow {
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

        val name = if (tags.has("name")) {
            tags["name"].asString
        } else {
            "Unnamed"
        }

        return PlacesSearchRow(
            place = this,
            icon = placeIconsRepo.getPlaceIcon(this),
            name = name,
            distanceToUser = distanceStringBuilder.toString(),
        )
    }

    private fun Location.distanceInKmTo(location: Location): Double {
        return getDistance(
            latitude,
            longitude,
            location.latitude,
            location.longitude,
        ) / 1000.0
    }

    private fun getDistance(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val distance = FloatArray(1)
        android.location.Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance)
        return distance[0].toDouble()
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 2

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }
}