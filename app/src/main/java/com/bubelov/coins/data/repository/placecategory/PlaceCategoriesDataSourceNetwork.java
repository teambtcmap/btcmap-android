package com.bubelov.coins.data.repository.placecategory;

import com.bubelov.coins.data.api.coins.CoinsApi;
import com.bubelov.coins.domain.PlaceCategory;
import com.google.firebase.crash.FirebaseCrash;

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
}