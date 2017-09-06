package com.bubelov.coins.repository.place

import com.bubelov.coins.repository.user.UserRepository
import com.bubelov.coins.model.Place
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

import javax.inject.Inject
import javax.inject.Singleton

import java.util.*

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesRepository @Inject
constructor(
        private val networkDataSource: PlacesDataSourceApi,
        private val dbDataSource: PlacesDataSourceDb,
        private val assetsDataSource: PlacesDataSourceAssets,
        private val userRepository: UserRepository
) {
    private val cache: MutableList<Place> = mutableListOf()
        get() {
            if (field.isEmpty()) {
                val placesFromDb = dbDataSource.getPlaces()

                if (placesFromDb.isEmpty()) {
                    val placesFromAssets = assetsDataSource.getPlaces()
                    dbDataSource.insertOrReplace(placesFromAssets)
                    field.addAll(placesFromAssets)
                } else {
                    field.addAll(placesFromDb)
                }
            }

            return field
        }

    fun getPlaces(bounds: LatLngBounds) = cache.filter { bounds.contains(LatLng(it.latitude, it.longitude)) }

    fun getPlaces(searchQuery: String) = cache.filter { it.name.contains(searchQuery, ignoreCase = true) }

    fun getPlace(id: Long) = cache.firstOrNull { it.id == id }

    fun getRandomPlace() = if (cache.isEmpty()) null else cache[(Math.random() * cache.size).toInt()]

    fun fetchNewPlaces(): List<Place> {
        val lastSyncDate = cache.maxBy { it.updatedAt }?.updatedAt ?: Date(0)
        val places = networkDataSource.getPlaces(Date(lastSyncDate.time + 1))

        if (!places.isEmpty()) {
            dbDataSource.insertOrReplace(places)
            cache.clear()
        }

        return places
    }

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
}