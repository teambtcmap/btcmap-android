package com.bubelov.coins.repository.place

import android.content.Context
import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.model.Place
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.gson.Gson

import java.io.IOException

import javax.inject.Inject
import javax.inject.Singleton

import java.util.*

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesRepository @Inject
constructor(val networkDataSource: PlacesDataSourceApi, val dbDataSource: PlacesDataSourceDb, val userRepository: UserRepository, val context: Context, val gson: Gson) {
    private val cache: MutableList<Place> = mutableListOf()
        get() {
            if (field.isEmpty()) field.addAll(dbDataSource.getPlaces())
            return field
        }

    fun getPlaces(bounds: LatLngBounds) = cache.filter { bounds.contains(LatLng(it.latitude, it.longitude)) }

    fun getPlaces(searchQuery: String) = cache.filter { it.name.contains(searchQuery, ignoreCase = true) }

    fun getPlace(id: Long) = cache.firstOrNull { it.id == id }

    fun getRandomPlace() = if (cache.isEmpty()) null else cache[(Math.random() * cache.size).toInt()]

    fun addPlace(place: Place): Boolean {
        val createdPlace = networkDataSource.addPlace(place, userRepository.userAuthToken) ?: return false
        dbDataSource.insertOrReplace(createdPlace)
        cache.add(createdPlace)
        return true
    }

    fun updatePlace(place: Place): Boolean {
        val updatedPlace = networkDataSource.updatePlace(place, userRepository.userAuthToken) ?: return false
        dbDataSource.insertOrReplace(updatedPlace)
        cache.remove(updatedPlace)
        cache.add(updatedPlace)
        return true
    }

    @Throws(IOException::class)
    fun fetchNewPlaces(): List<Place> {
        val places = networkDataSource.getPlaces(gson.toJson(getLastUpdateDate()))

        if (!places.isEmpty()) {
            dbDataSource.insertOrReplace(places)
            cache.clear()
        }

        return places
    }

    fun setCache(places: Collection<Place>) {
        dbDataSource.insertOrReplace(places)
    }

    private fun getLastUpdateDate(): Date {
        val latestPlace = cache.maxBy { it.updatedAt }

        when (latestPlace) {
            null -> return Date(0)
            else -> return latestPlace.updatedAt
        }
    }
}