package com.bubelov.coins.repository.placecategory

import com.bubelov.coins.model.PlaceCategory

import java.io.IOException

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlaceCategoriesRepository @Inject
internal constructor(private val networkSource: PlaceCategoriesDataSourceApi, private val dbSource: PlaceCategoriesDataSourceDb, private val memorySource: PlaceCategoriesDataSourceMemory) {

    fun getPlaceCategory(id: Long): PlaceCategory? {
        var category = memorySource.getPlaceCategory(id)

        if (category != null) {
            return category
        }

        category = dbSource.getPlaceCategory(id)

        if (category != null) {
            memorySource.addPlaceCategory(category)
        }

        return category
    }

    @Throws(IOException::class)
    fun reloadFromApi() {
        val categories = networkSource.getPlaceCategories()

        for (category in categories) {
            dbSource.addPlaceCategory(category)
            memorySource.addPlaceCategory(category)
        }
    }
}