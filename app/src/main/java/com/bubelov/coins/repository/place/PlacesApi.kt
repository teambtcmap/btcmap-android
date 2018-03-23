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

import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.api.coins.AddPlaceArgs
import com.bubelov.coins.api.coins.UpdatePlaceArgs
import com.bubelov.coins.model.Place
import com.bubelov.coins.repository.user.UserRepository
import com.google.gson.Gson
import retrofit2.Call

import java.util.*

import javax.inject.Inject
import javax.inject.Singleton

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
        return api.addPlace(userRepository.userAuthToken, AddPlaceArgs(place))
    }

    fun updatePlace(place: Place): Call<Place> {
        return api.updatePlace(place.id, userRepository.userAuthToken, UpdatePlaceArgs(place))
    }
}