package com.bubelov.coins.repository.placecategory;

import com.bubelov.coins.api.coins.CoinsApi;
import com.bubelov.coins.model.PlaceCategory;
import com.google.firebase.crash.FirebaseCrash;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlaceCategoriesDataSourceNetwork implements PlaceCategoriesDataSource {
    private final CoinsApi api;

    @Inject
    PlaceCategoriesDataSourceNetwork(CoinsApi api) {
        this.api = api;
    }

    @Override
    public PlaceCategory getPlaceCategory(long id) {
        try {
            return api.getPlaceCategory(id).execute().body();
        } catch (Exception e) {
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't load place category");
            return null;
        }
    }

    public List<PlaceCategory> getPlaceCategories() {
        try {
            return api.getPlaceCategories().execute().body();
        } catch (Exception e) {
            FirebaseCrash.report(e);
            Timber.e(e, "Couldn't load place categories");
            return null;
        }
    }
}