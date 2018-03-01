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

package com.bubelov.coins.repository.place

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.Result
import com.bubelov.coins.util.Analytics
import com.bubelov.coins.util.toLatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.*

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlacesRepository @Inject constructor(
    private val api: PlacesApi,
    private val db: PlacesDb,
    private val assetsCache: PlacesAssetsCache,
    private val analytics: Analytics
) {
    private val allPlaces = db.all()

    init {
        db.count().observeForever { count ->
            if (count == 0) {
                db.insert(assetsCache.getPlaces())
            }
        }
    }

    fun all() = allPlaces

    fun find(id: Long) = db.find(id)

    fun findBySearchQuery(searchQuery: String) = db.findBySearchQuery(searchQuery)

    fun findRandom() = db.findRandom()

    fun getPlaces(bounds: LatLngBounds): LiveData<List<Place>> =
        Transformations.switchMap(allPlaces) {
            MutableLiveData<List<Place>>().apply {
                value = it.filter { bounds.contains(it.toLatLng()) }
            }
        }

    fun fetchNewPlaces(): Result<List<Place>> {
        try {
            val latestPlaceUpdatedAt = db.maxUpdatedAt() ?: Date(0)
            val response = api.getPlaces(Date(latestPlaceUpdatedAt.time + 1)).execute()

            return if (response.isSuccessful) {
                val places = response.body()!!

                if (!places.isEmpty()) {
                    db.insert(places)
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
                db.insert(listOf(createdPlace))
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