package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import android.preference.PreferenceManager

import com.bubelov.coins.App
import com.bubelov.coins.R
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.ui.model.PlacesSearchResult
import com.bubelov.coins.util.DistanceComparator
import com.bubelov.coins.util.DistanceUnits
import com.bubelov.coins.util.DistanceUtils
import com.bubelov.coins.util.appComponent
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.uiThread
import java.util.*
import java.util.concurrent.Future
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * @author Igor Bubelov
 */

class PlacesSearchViewModel(application: Application) : AndroidViewModel(application) {
    @Inject internal lateinit var placesRepository: PlacesRepository

    var userLocation: Location? = null

    var searchQuery: String by Delegates.observable("", { _, _, _ -> onSearchQueryChanged() })

    val searchResults = MutableLiveData<List<PlacesSearchResult>>()

    private var futureResults: Future<Any>? = null

    init {
        appComponent().inject(this)
    }

    private fun onSearchQueryChanged() {
        futureResults?.cancel(true)

        if (searchQuery.length >= MIN_QUERY_LENGTH) {
            futureResults = doAsyncResult {
                val places = placesRepository.getPlaces(searchQuery)

                if (userLocation != null) {
                    Collections.sort(places, DistanceComparator(userLocation!!))
                }

                val results = places.map { place ->
                    PlacesSearchResult(
                            placeId = place.id,
                            placeName = place.name,
                            distance = userLocation?.distanceTo(place.latitude, place.longitude, getDistanceUnits()),
                            distanceUnits = getDistanceUnits().getShortName(),
                            iconResId = R.drawable.ic_place
                    )
                }

                uiThread { searchResults.value = results }
            }
        } else {
            searchResults.value = emptyList()
        }
    }

    private fun getDistanceUnits(): DistanceUnits {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication<App>())
        val distanceUnitsString = sharedPreferences.getString(getApplication<App>().getString(R.string.pref_distance_units_key), getApplication<App>().getString(R.string.pref_distance_units_automatic))

        if (distanceUnitsString == getApplication<App>().getString(R.string.pref_distance_units_automatic)) {
            return DistanceUnits.default
        } else {
            return DistanceUnits.valueOf(distanceUnitsString)
        }
    }

    private fun Location.distanceTo(latitude: Double, longitude: Double, units: DistanceUnits): Double {
        val distanceInKilometers = DistanceUtils.getDistance(this.latitude, this.longitude, latitude, longitude) / 1000.0

        return when (units) {
            DistanceUnits.KILOMETERS -> distanceInKilometers
            DistanceUnits.MILES -> DistanceUtils.toMiles(distanceInKilometers)
        }
    }

    private fun DistanceUnits.getShortName(): String {
        return when (this) {
            DistanceUnits.KILOMETERS -> getApplication<App>().getString(R.string.kilometers_short)
            DistanceUnits.MILES -> getApplication<App>().getString(R.string.miles_short)
        }
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 2
    }
}