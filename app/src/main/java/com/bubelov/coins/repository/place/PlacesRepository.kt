package com.bubelov.coins.repository.place

import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.ApiResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import timber.log.Timber

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
        private val assetsDataSource: PlacesDataSourceAssets
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

    fun fetchNewPlaces(): ApiResult<List<Place>> {
        try {
            val lastSyncDate = cache.maxBy { it.updatedAt }?.updatedAt ?: Date(0)
            val response = networkDataSource.getPlaces(Date(lastSyncDate.time + 1)).execute()

            return if (response.isSuccessful) {
                val places = response.body()!!

                if (!places.isEmpty()) {
                    dbDataSource.insertOrReplace(places)
                    cache.clear()
                }

                ApiResult.Success(places)
            } else {
                throw Exception("HTTP code: ${response.code()}, message: ${response.message()}")
            }
        } catch (e: Exception) {
            return ApiResult.Error(e)
        }
    }

    fun addPlace(place: Place): Boolean {
        return try {
            val response = networkDataSource.addPlace(place).execute()

            if (response.isSuccessful) {
                val createdPlace = response.body()!!
                dbDataSource.insertOrReplace(createdPlace)
                cache.add(createdPlace)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    fun updatePlace(place: Place): Boolean {
        return try {
            val response = networkDataSource.updatePlace(place).execute()

            if (response.isSuccessful) {
                val updatedPlace = response.body()!!
                dbDataSource.insertOrReplace(updatedPlace)
                cache.remove(updatedPlace)
                cache.add(updatedPlace)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }
}