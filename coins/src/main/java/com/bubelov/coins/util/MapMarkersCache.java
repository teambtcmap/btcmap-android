package com.bubelov.coins.util;

import com.bubelov.coins.R;
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

    private BitmapDescriptor createBitmapDescriptor(String amenity) {
        if ("atm".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_atm);
        }

        if ("cafe".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_cafe);
        }

        if ("restaurant".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_restaurant);
        }

        if ("bar".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_bar);
        }

        if ("hotel".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_hotel);
        }

        if ("car_wash".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_car_wash);
        }

        if ("fuel".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_fuel);
        }

        if ("hospital".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_hospital);
        }

        if ("dry_cleaning".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_dry_cleaning);
        }

        if ("cinema".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_cinema);
        }

        if ("parking".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_parking);
        }

        if ("pharmacy".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_pharmacy);
        }

        if ("pizza".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_pizza);
        }

        if ("taxi".equals(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_taxi);
        }

        return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_empty);
    }
}
