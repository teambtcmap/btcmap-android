package com.bubelov.coins.repository.place

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.os.SystemClock
import com.bubelov.coins.database.DatabaseConfig
import com.bubelov.coins.database.dao.PlaceDao
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.Result
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
class PlacesRepository @Inject
constructor(
        private val networkDataSource: PlacesDataSourceApi,
        private val dao: PlaceDao,
        private val assetsDataSource: PlacesDataSourceAssets,
        databaseConfig: DatabaseConfig
) {
    private val allPlaces = dao.all()

    private val initAssets = launch {
        if (dao.count() == 0) {
            dao.insertAll(assetsDataSource.getPlaces())
        }
    }

    init {
        if (databaseConfig.canUseMainThread) {
            runBlocking { initAssets.join() }
        }
    }

    fun getPlaces(bounds: LatLngBounds): LiveData<List<Place>>
            = Transformations.switchMap(allPlaces) { MutableLiveData<List<Place>>().apply { value = it.filter { bounds.contains(it.toLatLng()) } } }

    fun getPlaces(searchQuery: String) = dao.findBySearchQuery("%$searchQuery%")

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

    fun getPlace(id: Long) = dao.findById(id)

    fun getRandomPlace() = dao.random()

    fun fetchNewPlaces(): Result<List<Place>> {
        try {
            runBlocking { initAssets.join() }

            while (dao.count() == 0) {
                SystemClock.sleep(100)
            }

            val latestPlaceUpdatedAt = dao.maxUpdatedAt() ?: Date(0)
            val response = networkDataSource.getPlaces(Date(latestPlaceUpdatedAt.time + 1)).execute()

            return if (response.isSuccessful) {
                val places = response.body()!!

                if (!places.isEmpty()) {
                    dao.insertAll(places)
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
            val response = networkDataSource.addPlace(place).execute()

            if (response.isSuccessful) {
                val createdPlace = response.body()!!
                dao.insert(createdPlace)
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
            val response = networkDataSource.updatePlace(place).execute()

            if (response.isSuccessful) {
                val updatedPlace = response.body()!!
                dao.update(updatedPlace)
                Result.Success(updatedPlace)
            } else {
                throw Exception("HTTP code: ${response.code()}, message: ${response.message()}")
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}