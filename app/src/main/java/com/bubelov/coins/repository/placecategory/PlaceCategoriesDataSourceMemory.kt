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
internal constructor() {
    @SuppressLint("UseSparseArrays")
    private val categories = HashMap<Long, PlaceCategory>()

    fun getPlaceCategory(id: Long): PlaceCategory? {
        return categories[id]
    }

    fun addPlaceCategory(category: PlaceCategory) {
        categories.put(category.id, category)
    }
}