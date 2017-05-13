package com.bubelov.coins.repository.placecategory;

import com.bubelov.coins.model.PlaceCategory;

/**
 * @author Igor Bubelov
 */

public interface PlaceCategoriesDataSource {
    PlaceCategory getPlaceCategory(long id);
}