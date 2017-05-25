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
import com.bubelov.coins.model.Place
import com.bubelov.coins.util.DistanceComparator
import com.bubelov.coins.util.DistanceUnits
import java.util.*

/**
 * @author Igor Bubelov
 */

class FindPlaceViewModel(application: Application) : AndroidViewModel(application) {
    var userLocation: Location? = null
        private set

    val searchResults = MutableLiveData<List<Place>>()

    private var findPlacesTask: FindPlacesTask? = null

    fun init(userLocation: Location?) {
        this.userLocation = userLocation
    }

    var searchQuery: String = ""
        set(value) {
            field = value
            onSearchQueryChanged()
        }

    val distanceUnits: DistanceUnits
        get() {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication<App>())
            val distanceUnitsString = sharedPreferences.getString(getApplication<App>().getString(R.string.pref_distance_units_key), getApplication<App>().getString(R.string.pref_distance_units_automatic))

            if (distanceUnitsString == getApplication<App>().getString(R.string.pref_distance_units_automatic)) {
                return DistanceUnits.getDefault()
            } else {
                return DistanceUnits.valueOf(distanceUnitsString)
            }
        }

    private fun onSearchQueryChanged() {
        if (findPlacesTask != null) {
            findPlacesTask!!.cancel(true)
            findPlacesTask = null
            searchResults.value = emptyList<Place>()
        }

        if (searchQuery.length >= MIN_QUERY_LENGTH) {
            findPlacesTask = FindPlacesTask()
            findPlacesTask!!.execute(searchQuery)
        }
    }

    private inner class FindPlacesTask : AsyncTask<String, Void, List<Place>>() {
        override fun doInBackground(vararg query: String): List<Place> {
            val places = Injector.INSTANCE.mainComponent().placesRepository().getPlaces(query[0])

            if (userLocation != null) {
                Collections.sort(places, DistanceComparator(userLocation))
            }

            return places
        }

        override fun onPostExecute(places: List<Place>) {
            searchResults.value = places
        }
    }

    companion object {
        private val MIN_QUERY_LENGTH = 2
    }
}