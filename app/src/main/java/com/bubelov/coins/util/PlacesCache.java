package com.bubelov.coins.util;

import android.os.AsyncTask;

import com.bubelov.coins.data.DataManager;
import com.bubelov.coins.data.api.coins.model.Place;
import com.bubelov.coins.data.model.PlaceCategory;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class PlacesCache {
    private final DataManager dataManager;

    private List<Place> places = new ArrayList<>();

    private Callback callback;

    public PlacesCache(DataManager dataManager) {
        this.dataManager = dataManager;
        invalidate();
    }

    public Collection<Place> getPlaces(LatLngBounds bounds, PlaceCategory category) {
        Timber.d("Retrieving places for area %s", bounds);
        long time = System.currentTimeMillis();
        Collection<Place> result = new ArrayList<>();

        for (Place place : places) {
            if (bounds.contains(place.getPosition())
                    && (category == null || category.name().equalsIgnoreCase(place.amenity()))) {
                result.add(place);
            }
        }

        time = System.currentTimeMillis() - time;
        Timber.d("%s places found. Time: %s", result.size(), time);
        return result;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void invalidate() {
        new InitCacheTask().executeOnExecutor(Executors.newSingleThreadExecutor());
    }

    private class InitCacheTask extends AsyncTask<Void, Void, List<Place>> {
        private long startTime;

        @Override
        protected void onPreExecute() {
            Timber.d("Cache initialization started");
            startTime = System.currentTimeMillis();
        }

        @Override
        protected List<Place> doInBackground(Void... params) {
            return dataManager.database().getAllPlaces();
        }

        @Override
        protected void onPostExecute(List<Place> places) {
            PlacesCache.this.places = places;

            Timber.d("Cache initialization finished. Total time: %s", System.currentTimeMillis() - startTime);

            if (callback != null) {
                callback.onPlacesCacheInitialized();
            }
        }
    }

    public interface Callback {
        void onPlacesCacheInitialized();
    }
}