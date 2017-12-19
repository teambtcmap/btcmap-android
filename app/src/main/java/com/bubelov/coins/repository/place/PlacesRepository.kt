package com.bubelov.coins.repository.place

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.os.SystemClock
import com.bubelov.coins.db.DatabaseConfig
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.Result
import com.bubelov.coins.util.Analytics
import com.bubelov.coins.util.toLatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.util.*

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesRepository @Inject constructor(
        private val api: PlacesApi,
        private val db: PlacesDb,
        private val assetsCache: PlacesAssetsCache,
        private val analytics: Analytics,
        databaseConfig: DatabaseConfig
) {
    private val allPlaces = db.all()

    private val initAssets = launch {
        if (db.count() == 0) {
            db.insertAll(assetsCache.getPlaces())
        }
    }

    init {
        if (databaseConfig.canUseMainThread) {
            runBlocking { initAssets.join() }
        }
    }

    fun getPlaces(bounds: LatLngBounds): LiveData<List<Place>>
            = Transformations.switchMap(allPlaces) { MutableLiveData<List<Place>>().apply { value = it.filter { bounds.contains(it.toLatLng()) } } }

    fun getPlaces(searchQuery: String) = db.findBySearchQuery(searchQuery)

    fun getCurrenciesToPlacesMap(): LiveData<Map<String, List<Place>>> = Transformations.switchMap(allPlaces) {
        val data = MutableLiveData<Map<String, List<Place>>>()
        val map = mutableMapOf<String, MutableList<Place>>()

        it?.forEach { place ->
            place.currencies.forEach { currency ->
                if (!map.containsKey(currency)) {
                    map.put(currency, mutableListOf())
                }

                map[currency]!!.add(place)
            }
        }

        data.apply { value = map }
    }

    fun getPlace(id: Long) = db.findById(id)

    fun getRandomPlace() = db.random()

    fun fetchNewPlaces(): Result<List<Place>> {
        try {
            runBlocking { initAssets.join() }

            while (db.count() == 0) {
                SystemClock.sleep(100)
            }

            val latestPlaceUpdatedAt = db.maxUpdatedAt() ?: Date(0)
            val response = api.getPlaces(Date(latestPlaceUpdatedAt.time + 1)).execute()

            return if (response.isSuccessful) {
                val places = response.body()!!

                if (!places.isEmpty()) {
                    db.insertAll(places)
                }

                Result.Success(places)
            } else {
                throw Exception("HTTP code: ${response.code()}, message: ${response.message()}")
            }
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    fun addPlace(place: Place): Result<Place> {
        return try {
            val response = api.addPlace(place).execute()

            if (response.isSuccessful) {
                val createdPlace = response.body()!!
                db.insert(createdPlace)
                analytics.logEvent("create_place")
                Result.Success(createdPlace)
            } else {
                throw Exception("HTTP code: ${response.code()}, message: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun updatePlace(place: Place): Result<Place> {
        return try {
            val response = api.updatePlace(place).execute()

            if (response.isSuccessful) {
                val updatedPlace = response.body()!!
                db.update(updatedPlace)
                analytics.logEvent("edit_place")
                Result.Success(updatedPlace)
            } else {
                throw Exception("HTTP code: ${response.code()}, message: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}