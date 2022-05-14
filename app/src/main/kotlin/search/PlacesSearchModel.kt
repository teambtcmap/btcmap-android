package search

import androidx.lifecycle.ViewModel
import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import org.btcmap.R
import location.Location
import settings.ConfRepository
import map.PlacesRepository
import map.PlaceIconsRepository
import common.DistanceUnits
import common.DistanceUtils
import db.Conf
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
    confRepo: ConfRepository,
    private val resources: Resources,
) : ViewModel() {

    private val location = MutableStateFlow<Location?>(null)

    private val searchString = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<List<PlacesSearchRow>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    init {
        combine(location, searchString, confRepo.select()) { location, searchString, conf ->
            if (searchString.length < MIN_QUERY_LENGTH) {
                _searchResults.update { emptyList() }
            } else {
                var places = placesRepo.selectBySearchString(searchString).first()

                if (location != null) {
                    places = places.sortedBy {
                        DistanceUtils.getDistance(
                            startLatitude = location.latitude,
                            startLongitude = location.longitude,
                            endLatitude = it.lat,
                            endLongitude = it.lon,
                        )
                    }
                }

                _searchResults.update { places.map { it.toRow(location, conf) } }
            }
        }.launchIn(viewModelScope)
    }

    fun setLocation(location: Location?) {
        this.location.update { location }
    }

    fun setSearchString(searchString: String) {
        this.searchString.update { searchString }
    }

    private fun Place.toRow(userLocation: Location?, conf: Conf): PlacesSearchRow {
        val distanceStringBuilder = StringBuilder()

        if (userLocation != null) {
            val placeLocation = Location(lat, lon)
            val distance = userLocation.distanceTo(placeLocation, conf.getDistanceUnits())

            distanceStringBuilder.apply {
                append(DISTANCE_FORMAT.format(distance))
                append(" ")
                append(conf.getDistanceUnits().getShortName())
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

    private fun Conf.getDistanceUnits(): DistanceUnits {
        return if (distanceUnits.isBlank()) {
            DistanceUnits.default
        } else {
            DistanceUnits.valueOf(distanceUnits)
        }
    }

    private fun DistanceUnits.getShortName(): String {
        return when (this) {
            DistanceUnits.KILOMETERS -> resources.getString(R.string.kilometers_short)
            DistanceUnits.MILES -> resources.getString(R.string.miles_short)
        }
    }

    private fun Location.distanceTo(anotherLocation: Location, units: DistanceUnits): Double {
        val distanceInKilometers = DistanceUtils.getDistance(
            latitude,
            longitude,
            anotherLocation.latitude,
            anotherLocation.longitude
        ) / 1000.0

        return when (units) {
            DistanceUnits.KILOMETERS -> distanceInKilometers
            DistanceUnits.MILES -> DistanceUtils.toMiles(distanceInKilometers)
        }
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 2

        private val DISTANCE_FORMAT = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }
    }
}