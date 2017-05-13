package com.bubelov.coins.repository.placecategory;

import com.bubelov.coins.model.PlaceCategory;

import java.util.List;

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
        }

        return category;
    }

    public boolean reloadFromNetwork() {
        List<PlaceCategory> categories = networkSource.getPlaceCategories();

        if (categories != null) {
            for (PlaceCategory category : categories) {
                dbSource.addPlaceCategory(category);
                memorySource.addPlaceCategory(category);
            }

            return true;
        } else {
            return false;
        }
    }
}