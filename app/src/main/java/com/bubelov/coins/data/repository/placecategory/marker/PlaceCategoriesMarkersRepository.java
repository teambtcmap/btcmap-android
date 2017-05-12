package com.bubelov.coins.data.repository.placecategory.marker;

import android.support.annotation.Nullable;

import com.bubelov.coins.R;
import com.bubelov.coins.domain.PlaceCategory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlaceCategoriesMarkersRepository {
    private Map<PlaceCategory, BitmapDescriptor> cache = new HashMap<>();

    @Inject
    PlaceCategoriesMarkersRepository() {}

    public BitmapDescriptor getMarker(@Nullable PlaceCategory placeCategory) {
        if (!cache.containsKey(placeCategory)) {
            cache.put(placeCategory, createBitmapDescriptor(placeCategory));
        }

        return cache.get(placeCategory);
    }

    private BitmapDescriptor createBitmapDescriptor(@Nullable PlaceCategory placeCategory) {
        if (placeCategory == null) {
            return BitmapDescriptorFactory.fromResource(R.drawable.ic_place_empty);
        }

        int resourceId;

        switch (placeCategory.name().toLowerCase()) {
            case "atm":
                resourceId = R.drawable.ic_place_atm;
                break;
            case "restaurant":
                resourceId = R.drawable.ic_place_restaurant;
                break;
            case "café":
                resourceId = R.drawable.ic_place_cafe;
                break;
            case "bar":
                resourceId = R.drawable.ic_place_bar;
                break;
            case "hotel":
                resourceId = R.drawable.ic_place_hotel;
                break;
            case "pizza":
                resourceId = R.drawable.ic_place_pizza;
                break;
            case "fast food":
                resourceId = R.drawable.ic_place_empty; // TODO add icon
                break;
            case "hospital":
                resourceId = R.drawable.ic_place_hospital;
                break;
            case "pharmacy":
                resourceId = R.drawable.ic_place_pharmacy;
                break;
            case "taxi":
                resourceId = R.drawable.ic_place_taxi;
                break;
            case "gas station":
                resourceId = R.drawable.ic_place_fuel;
                break;
            default:
                resourceId = R.drawable.ic_place_empty;
        }

        return BitmapDescriptorFactory.fromResource(resourceId);
    }
}