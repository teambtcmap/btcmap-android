package com.bubelov.coins.repository.place

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.os.SystemClock
import com.bubelov.coins.database.dao.PlaceDao
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.ApiResult
import com.bubelov.coins.util.toLatLng
import com.google.android.gms.maps.model.LatLngBounds
import org.jetbrains.anko.doAsync
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
        assetsDataSource: PlacesDataSourceAssets
) {
    val allPlaces = dao.all()

    private var assetsInitialized = false

    init {
        doAsync {
            if (dao.count() == 0) {
                dao.insertAll(assetsDataSource.getPlaces())
            }

            assetsInitialized = true
        }
    }

    fun getPlaces(bounds: LatLngBounds): LiveData<List<Place>>
            = Transformations.switchMap(allPlaces) { MutableLiveData<List<Place>>().apply { value = it.filter { bounds.contains(it.toLatLng()) } } }

    fun getPlaces(searchQuery: String) = dao.findBySearchQuery("%$searchQuery%")

    fun getPlace(id: Long) = dao.findById(id)

    fun getRandomPlace() = dao.random()

    fun fetchNewPlaces(): ApiResult<List<Place>> {
        try {
            while (!assetsInitialized) {
                SystemClock.sleep(100)
            }

            val latestPlaceUpdatedAt = dao.maxUpdatedAt() ?: Date(0)
            val response = networkDataSource.getPlaces(Date(latestPlaceUpdatedAt.time + 1)).execute()

            return if (response.isSuccessful) {
                val places = response.body()!!

                if (!places.isEmpty()) {
                    dao.insertAll(places)
                }

                ApiResult.Success(places)
            } else {
                throw Exception("HTTP code: ${response.code()}, message: ${response.message()}")
            }
        } catch (e: Exception) {
            return ApiResult.Error(e)
        }
    }

    fun addPlace(place: Place): ApiResult<Place> {
        return try {
            val response = networkDataSource.addPlace(place).execute()

            if (response.isSuccessful) {
                val createdPlace = response.body()!!
                dao.insert(createdPlace)
                ApiResult.Success(createdPlace)
            } else {
                throw Exception("HTTP code: ${response.code()}, message: ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    fun updatePlace(place: Place): ApiResult<Place> {
        return try {
            val response = networkDataSource.updatePlace(place).execute()

            if (response.isSuccessful) {
                val updatedPlace = response.body()!!
                dao.update(updatedPlace)
                ApiResult.Success(updatedPlace)
            } else {
                throw Exception("HTTP code: ${response.code()}, message: ${response.message()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }
}