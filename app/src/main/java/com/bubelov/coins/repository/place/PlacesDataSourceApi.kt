package com.bubelov.coins.repository.place

import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.api.coins.PlaceParams
import com.bubelov.coins.model.Place

import java.io.IOException
import java.util.Date

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesDataSourceApi @Inject
internal constructor(private val api: CoinsApi) {
    @Throws(IOException::class)
    fun getPlaces(updatedAfter: Date): List<Place> {
        return api.getPlaces(updatedAfter, Integer.MAX_VALUE).execute().body()
    }

    @Throws(IOException::class)
    fun addPlace(place: Place, authToken: String): Place? {
        return api.addPlace(authToken, PlaceParams(place)).execute().body()
    }

    @Throws(IOException::class)
    fun updatePlace(place: Place, authToken: String): Place? {
        return api.updatePlace(place.id, authToken, PlaceParams(place)).execute().body()
    }
}