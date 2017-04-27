package com.bubelov.coins.util;

import com.bubelov.coins.R;
import com.bubelov.coins.data.model.PlaceCategory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Igor Bubelov
 */

public class MapMarkersCache {
    private Map<String, BitmapDescriptor> cache = new HashMap<>();

    public BitmapDescriptor getMarker(String amenity) {
        if (!cache.containsKey(amenity)) {
            cache.put(amenity, createBitmapDescriptor(amenity));
        }

        return cache.get(amenity);
    }

    private BitmapDescriptor createBitmapDescriptor(String categoryString) {
        for (PlaceCategory category : PlaceCategory.values()) {
            if (category.name().equalsIgnoreCase(categoryString)) {
                return BitmapDescriptorFactory.fromResource(category.getMarkerIconId());
            }
        }

        return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_empty);
    }
}
