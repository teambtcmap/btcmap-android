package com.bubelov.coins.data.repository.placecategory;

import com.bubelov.coins.domain.PlaceCategory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlaceCategoriesRepository implements PlaceCategoriesDataSource {
    private final PlaceCategoriesDataSourceNetwork networkSource;

    private final PlaceCategoriesDataSourceDb dbSource;

    private final PlaceCategoriesDataSourceMemory memorySource;

    @Inject
    PlaceCategoriesRepository(PlaceCategoriesDataSourceNetwork networkSource, PlaceCategoriesDataSourceDb dbSource, PlaceCategoriesDataSourceMemory memorySource) {
        this.networkSource = networkSource;
        this.dbSource = dbSource;
        this.memorySource = memorySource;
    }

    @Override
    public PlaceCategory getPlaceCategory(long id) {
        PlaceCategory category = memorySource.getPlaceCategory(id);

        if (category != null) {
            return category;
        }

        category = dbSource.getPlaceCategory(id);

        if (category != null) {
            memorySource.addPlaceCategory(category);
            return category;
        }

        category = networkSource.getPlaceCategory(id);

        if (category != null) {
            dbSource.addPlaceCategory(category);
            memorySource.addPlaceCategory(category);
        }

        return category;
    }

    public void reloadFromNetworkSource() {

    }
}