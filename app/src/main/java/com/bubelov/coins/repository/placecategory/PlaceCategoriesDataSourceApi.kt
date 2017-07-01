package com.bubelov.coins.repository.placecategory

import com.bubelov.coins.api.coins.CoinsApi
import com.bubelov.coins.model.PlaceCategory

import java.io.IOException

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlaceCategoriesDataSourceApi @Inject
internal constructor(private val api: CoinsApi) {
    @Throws(IOException::class)
    fun getPlaceCategory(id: Long): PlaceCategory? = api.getPlaceCategory(id).execute().body()

    @Throws(IOException::class)
    fun getPlaceCategories(): Collection<PlaceCategory> = api.getPlaceCategories().execute().body()!!
}