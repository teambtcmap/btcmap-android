package com.bubelov.coins.repository.place

import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.api.coins.PlaceParams
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.user.UserRepository
import com.google.gson.Gson
import retrofit2.Call

import java.util.*

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesApi @Inject
internal constructor(
        private val api: CoinsApi,
        private val gson: Gson,
        private val userRepository: UserRepository
) {
    fun getPlaces(updatedAfter: Date): Call<List<Place>> {
        return api.getPlaces(gson.toJson(updatedAfter), Integer.MAX_VALUE)
    }

    fun addPlace(place: Place): Call<Place> {
        return api.addPlace(userRepository.userAuthToken, PlaceParams(place))
    }

    fun updatePlace(place: Place): Call<Place> {
        return api.updatePlace(place.id, userRepository.userAuthToken, PlaceParams(place))
    }
}