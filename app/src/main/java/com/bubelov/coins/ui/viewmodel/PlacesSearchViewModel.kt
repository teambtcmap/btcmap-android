/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */

package com.bubelov.coins.ui.viewmodel

import android.arch.lifecycle.*
import android.content.Context
import android.location.Location
import android.preference.PreferenceManager

import com.bubelov.coins.R
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.placeicon.PlaceIconsRepository
import com.bubelov.coins.ui.model.PlacesSearchRow
import com.bubelov.coins.util.*
import java.text.NumberFormat
import javax.inject.Inject

class PlacesSearchViewModel @Inject constructor(
    private val context: Context,
    private val placesRepository: PlacesRepository,
    private val placeIconsRepository: PlaceIconsRepository
) : ViewModel() {

    private var userLocation: Location? = null

    private lateinit var currency: String

    val searchQuery = MutableLiveData<String>()

    val searchResults: LiveData<List<PlacesSearchRow>> =
        Transformations.switchMap(searchQuery) { query ->
            if (query.length < MIN_QUERY_LENGTH) {
                MutableLiveData<List<PlacesSearchRow>>().apply { value = emptyList() }
            } else {
                Transformations.map(placesRepository.getPlaces(query)) { places ->
                    places
                        .filter { it.currencies.contains(currency) }
                        .map { it.toRow() }
                        .sortedBy { it.distance }
                }
            }
        }

    fun init(userLocation: Location?, currency: String) {
        this.userLocation = userLocation
        this.currency = currency
    }

    private fun Place.toRow(): PlacesSearchRow {
        val userLocation = userLocation

        val distanceString = if (userLocation != null) DISTANCE_FORMAT.format(
            userLocation.distanceTo(
                latitude,
                longitude,
                getDistanceUnits()
            )
        ) + " ${getDistanceUnits().getShortName()}" else ""

        return PlacesSearchRow(
            placeId = id,
            name = name,
            distance = distanceString,
            icon = placeIconsRepository.getPlaceIcon(category)
        )
    }

    private fun getDistanceUnits(): DistanceUnits {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val distanceUnitsString = sharedPreferences.getString(
            context.getString(R.string.pref_distance_units_key),
            context.getString(R.string.pref_distance_units_automatic)
        )

        return if (distanceUnitsString == context.getString(R.string.pref_distance_units_automatic)) {
            DistanceUnits.default
        } else {
            DistanceUnits.valueOf(distanceUnitsString)
        }
    }

    private fun DistanceUnits.getShortName(): String {
        return when (this) {
            DistanceUnits.KILOMETERS -> context.getString(R.string.kilometers_short)
            DistanceUnits.MILES -> context.getString(R.string.miles_short)
        }
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 2

        private val DISTANCE_FORMAT =
            NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
    }
}