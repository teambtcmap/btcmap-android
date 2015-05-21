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

    private BitmapDescriptor createBitmapDescriptor(String amenity) {
        if (Amenity.ATM.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_atm);
        }

        if (Amenity.CAFE.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_cafe);
        }

        if (Amenity.RESTAURANT.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_restaurant);
        }

        if (Amenity.BAR.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_bar);
        }

        if (Amenity.HOTEL.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_hotel);
        }

        if (Amenity.CAR_WASH.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_car_wash);
        }

        if (Amenity.FUEL.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_fuel);
        }

        if (Amenity.HOSPITAL.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_hospital);
        }

        if (Amenity.DRY_CLEANING.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_dry_cleaning);
        }

        if (Amenity.CINEMA.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_cinema);
        }

        if (Amenity.PARKING.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_parking);
        }

        if (Amenity.PHARMACY.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_pharmacy);
        }

        if (Amenity.PIZZA.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_pizza);
        }

        if (Amenity.TAXI.name().equalsIgnoreCase(amenity)) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_taxi);
        }

        return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_empty);
    }
}
