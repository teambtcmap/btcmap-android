package com.bubelov.coins.api.coins;

import com.bubelov.coins.model.Place;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Igor Bubelov
 */

public class PlaceParams {
    private final Map<String, Object> place;

    public PlaceParams(Place place) {
        this.place = new HashMap<>();
        this.place.put("name", place.name());
        this.place.put("description", place.description());
        this.place.put("latitude", place.latitude());
        this.place.put("longitude", place.longitude());
        this.place.put("phone", place.phone());
        this.place.put("website", place.website());
        this.place.put("opening_hours", place.openingHours());
        this.place.put("visible", place.visible());
    }
}