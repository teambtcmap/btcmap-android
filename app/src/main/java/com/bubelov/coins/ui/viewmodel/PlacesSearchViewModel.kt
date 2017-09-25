package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.location.Location
import android.preference.PreferenceManager

import com.bubelov.coins.App
import com.bubelov.coins.R
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.ui.model.PlacesSearchResult
import com.bubelov.coins.util.*
import javax.inject.Inject

/**
 * @author Igor Bubelov
 */

class PlacesSearchViewModel(application: Application) : AndroidViewModel(application) {
    @Inject internal lateinit var placesRepository: PlacesRepository

    var userLocation: Location? = null

    val searchQuery = MutableLiveData<String>()

    val searchResults = Transformations.switchMap(searchQuery) {
        if (it.length <= MIN_QUERY_LENGTH) {
            return@switchMap MutableLiveData<List<PlacesSearchResult>>()
        }

        Transformations.switchMap(placesRepository.getPlaces(it)) {
            val results = it.map { place ->
                PlacesSearchResult(
                        placeId = place.id,
                        placeName = place.name,
                        distance = userLocation?.distanceTo(place.latitude, place.longitude, getDistanceUnits()),
                        distanceUnits = getDistanceUnits().getShortName(),
                        iconResId = R.drawable.ic_place
                )
            }

            MutableLiveData<List<PlacesSearchResult>>().apply { value = results }
        }
    }

    init {
        appComponent().inject(this)
    }

    private fun getDistanceUnits(): DistanceUnits {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication<App>())
        val distanceUnitsString = sharedPreferences.getString(getApplication<App>().getString(R.string.pref_distance_units_key), getApplication<App>().getString(R.string.pref_distance_units_automatic))

        return if (distanceUnitsString == getApplication<App>().getString(R.string.pref_distance_units_automatic)) {
            DistanceUnits.default
        } else {
            DistanceUnits.valueOf(distanceUnitsString)
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