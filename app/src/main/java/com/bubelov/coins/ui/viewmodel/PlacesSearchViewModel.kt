package com.bubelov.coins.ui.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import android.os.AsyncTask
import android.preference.PreferenceManager

import com.bubelov.coins.App
import com.bubelov.coins.R
import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.ui.model.PlacesSearchResult
import com.bubelov.coins.util.DistanceComparator
import com.bubelov.coins.util.DistanceUnits
import com.bubelov.coins.util.DistanceUtils
import java.util.*
import kotlin.properties.Delegates

/**
 * @author Igor Bubelov
 */

class PlacesSearchViewModel(application: Application) : AndroidViewModel(application) {
    var userLocation: Location? = null

    var searchQuery: String by Delegates.observable("", { _, _, _ -> search() })

    val searchResults = MutableLiveData<List<PlacesSearchResult>>()

    private var findPlacesTask: FindPlacesTask? = null

    private fun search() {
        findPlacesTask?.cancel(true)
        findPlacesTask = null

        if (searchQuery.length >= MIN_QUERY_LENGTH) {
            findPlacesTask = FindPlacesTask()
            findPlacesTask!!.execute(searchQuery)
        } else {
            searchResults.value = emptyList()
        }
    }

    private fun getDistanceUnits(): DistanceUnits {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication<App>())
        val distanceUnitsString = sharedPreferences.getString(getApplication<App>().getString(R.string.pref_distance_units_key), getApplication<App>().getString(R.string.pref_distance_units_automatic))

        if (distanceUnitsString == getApplication<App>().getString(R.string.pref_distance_units_automatic)) {
            return DistanceUnits.getDefault()
        } else {
            return DistanceUnits.valueOf(distanceUnitsString)
        }
    }

    fun Location.distanceTo(latitude: Double, longitude: Double, units: DistanceUnits): Double {
        val distanceInKilometers = DistanceUtils.getDistance(this.latitude, this.longitude, latitude, longitude) / 1000.0

        when (units) {
            DistanceUnits.KILOMETERS -> return distanceInKilometers
            DistanceUnits.MILES -> return DistanceUtils.toMiles(distanceInKilometers)
        }
    }

    fun DistanceUnits.getShortName(): String {
        when (this) {
            DistanceUnits.KILOMETERS -> return getApplication<App>().getString(R.string.kilometers_short)
            DistanceUnits.MILES -> return getApplication<App>().getString(R.string.miles_short)
        }
    }

    private inner class FindPlacesTask : AsyncTask<String, Void, List<PlacesSearchResult>>() {
        override fun doInBackground(vararg query: String): List<PlacesSearchResult> {
            val places = Injector.INSTANCE.mainComponent().placesRepository().getPlaces(query[0])

            if (userLocation != null) {
                Collections.sort(places, DistanceComparator(userLocation!!))
            }

            return places.map { (id, name, _, latitude, longitude) ->
                PlacesSearchResult(
                        placeId = id,
                        placeName = name,
                        distance = userLocation?.distanceTo(latitude, longitude, getDistanceUnits()),
                        distanceUnits = getDistanceUnits().getShortName(),
                        iconResId = R.drawable.ic_place
                )
            }
        }

        override fun onPostExecute(places: List<PlacesSearchResult>) {
            searchResults.value = places
        }
    }

    companion object {
        private val MIN_QUERY_LENGTH = 2
    }
}