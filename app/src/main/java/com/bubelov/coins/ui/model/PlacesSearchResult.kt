package com.bubelov.coins.ui.model

/**
 * @author Igor Bubelov
 */

data class PlacesSearchResult(
        val placeId: Long,
        val placeName: String,
        val distance: Double?,
        val distanceUnits: String?,
        val iconResId: Int
)