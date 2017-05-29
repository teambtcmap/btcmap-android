package com.bubelov.coins.repository.placecategory

import android.annotation.SuppressLint

import com.bubelov.coins.model.PlaceCategory

import java.util.HashMap

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlaceCategoriesDataSourceMemory @Inject
internal constructor() : PlaceCategoriesDataSource {
    @SuppressLint("UseSparseArrays")
    private val categories = HashMap<Long, PlaceCategory>()

    override fun getPlaceCategory(id: Long): PlaceCategory? {
        return categories[id]
    }

    fun addPlaceCategory(category: PlaceCategory) {
        categories.put(category.id, category)
    }
}