package com.bubelov.coins.data.repository.placecategory;

import android.annotation.SuppressLint;

import com.bubelov.coins.domain.PlaceCategory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlaceCategoriesDataSourceMemory implements PlaceCategoriesDataSource {
    @SuppressLint("UseSparseArrays")
    private Map<Long, PlaceCategory> categories = new HashMap<>();

    @Inject
    PlaceCategoriesDataSourceMemory() {}

    @Override
    public PlaceCategory getPlaceCategory(long id) {
        return categories.get(id);
    }

    public void addPlaceCategory(PlaceCategory category) {
        categories.put(category.id(), category);
    }
}