package com.bubelov.coins.api.coins

import com.bubelov.coins.model.Place

import java.util.HashMap

/**
 * @author Igor Bubelov
 */

class PlaceParams(place: Place) {
    private val place: MutableMap<String, Any>

    init {
        this.place = HashMap<String, Any>()
        this.place.put("name", place.name)
        this.place.put("description", place.description)
        this.place.put("latitude", place.latitude)
        this.place.put("longitude", place.longitude)
        this.place.put("phone", place.phone)
        this.place.put("website", place.website)
        this.place.put("opening_hours", place.openingHours)
        this.place.put("visible", place.visible)
    }
}