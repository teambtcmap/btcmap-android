package com.bubelov.coins.repository.place

import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.model.Place
import com.google.android.gms.maps.model.LatLngBounds

import java.io.IOException
import java.util.Date

import javax.inject.Inject
import javax.inject.Singleton

import timber.log.Timber

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesRepository @Inject
constructor(private val networkDataSource: PlacesDataSourceApi, private val dbDataSource: PlacesDataSourceDb, private val memoryDataSource: PlacesDataSourceRam, private val userRepository: UserRepository) {
    fun getPlaces(bounds: LatLngBounds): List<Place> {
        if (memoryDataSource.places.isEmpty()) {
            memoryDataSource.places = dbDataSource.places
        }

        return memoryDataSource.getPlaces(bounds)
    }

    fun getPlaces(searchQuery: String): List<Place> {
        if (memoryDataSource.places.isEmpty()) {
            memoryDataSource.places = dbDataSource.places
        }

        return memoryDataSource.places.filter { it.name.toLowerCase().contains(searchQuery.toLowerCase()) }
    }

    val randomPlace: Place?
        get() {
            if (memoryDataSource.places.isEmpty()) {
                memoryDataSource.places = dbDataSource.places
            }

            if (!memoryDataSource.places.isEmpty()) {
                return memoryDataSource.places[(Math.random() * memoryDataSource.places.size).toInt()]
            }

            return null
        }

    fun getPlace(id: Long): Place? {
        if (memoryDataSource.places.isEmpty()) {
            memoryDataSource.places = dbDataSource.places
        }

        return memoryDataSource.getPlace(id)
    }

    fun add(place: Place): Boolean {
        val result = networkDataSource.addPlace(place, userRepository.userAuthToken) ?: return false
        dbDataSource.insertOrUpdatePlace(result)
        memoryDataSource.places = dbDataSource.places
        return true
    }

    fun update(place: Place): Boolean {
        val result = networkDataSource.updatePlace(place, userRepository.userAuthToken) ?: return false
        dbDataSource.insertOrUpdatePlace(result)
        memoryDataSource.places = dbDataSource.places
        return true
    }

    val cachedPlacesCount: Long
        get() = dbDataSource.cachedPlacesCount

    fun fetchNewPlaces(): List<Place> {
        if (memoryDataSource.places.isEmpty()) {
            memoryDataSource.places = dbDataSource.places
        }

        var latestUpdatedAt = Date(0)

        for ((_, _, _, _, _, _, _, _, _, _, updatedAt) in memoryDataSource.places) {
            if (updatedAt.after(latestUpdatedAt)) {
                latestUpdatedAt = updatedAt
            }
        }

        try {
            val places = networkDataSource.getPlaces(latestUpdatedAt)

            if (!places.isEmpty()) {
                dbDataSource.batchInsert(places)
                memoryDataSource.places = dbDataSource.places
            }

            return places
        } catch (e: IOException) {
            Timber.e(e, "Couldn't fetch new places")
            return emptyList()
        }
    }
}