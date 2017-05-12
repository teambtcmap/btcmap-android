package com.bubelov.coins.data.repository.placecategory;

import com.bubelov.coins.domain.PlaceCategory;

/**
 * @author Igor Bubelov
 */

public interface PlaceCategoriesDataSource {
    PlaceCategory getPlaceCategory(long id);
}