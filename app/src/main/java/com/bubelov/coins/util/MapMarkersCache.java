package com.bubelov.coins.util;

import com.bubelov.coins.R;
import com.bubelov.coins.model.Amenity;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: Igor Bubelov
 * Date: 20/05/15 10:46
 */

public class MapMarkersCache {
    private Map<String, BitmapDescriptor> cache = new HashMap<>();

    public BitmapDescriptor getMarker(String amenity) {
        if (!cache.containsKey(amenity)) {
            cache.put(amenity, createBitmapDescriptor(amenity));
        }

        return cache.get(amenity);
    }

    private BitmapDescriptor createBitmapDescriptor(String amenityString) {
        for (Amenity amenity : Amenity.values()) {
            if (amenityString.equalsIgnoreCase(amenity.name())) {
                return BitmapDescriptorFactory.fromResource(amenity.getMarkerIconId());
            }
        }

        return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_empty);
    }
}
