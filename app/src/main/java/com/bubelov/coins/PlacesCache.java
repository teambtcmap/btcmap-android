package com.bubelov.coins;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Place;
import com.bubelov.coins.model.PlaceCategory;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class PlacesCache {
    private final SQLiteDatabase db;

    private Place[] places = new Place[0];

    private Callback callback;

    @Inject
    public PlacesCache(SQLiteDatabase db) {
        this.db = db;
        invalidate();
    }

    public Collection<Place> getPlaces(LatLngBounds bounds, PlaceCategory category) {
        Timber.d("Retrieving places for area %s", bounds);
        long time = System.currentTimeMillis();
        Collection<Place> result = new ArrayList<>();
        int placeCount = this.places.length;

        for (int i = 0; i < placeCount; i++) {
            Place place = this.places[i];

            if (bounds.contains(place.getPosition())
                    && (category == null || category.name().equalsIgnoreCase(place.getAmenity()))) {
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

    private class InitCacheTask extends AsyncTask<Void, Void, Place[]> {
        private long startTime;

        @Override
        protected void onPreExecute() {
            Timber.d("Cache initialization started");
            startTime = System.currentTimeMillis();
        }

        @Override
        protected Place[] doInBackground(Void... params) {
            long time = System.currentTimeMillis();
            Cursor cursor = db.rawQuery("select _id, latitude, longitude, amenity from places where visible = 1", null);
            Timber.d("%s places retrieved from DB", cursor.getCount());
            Timber.d("DB query time: %s", System.currentTimeMillis() - time);
            time = System.currentTimeMillis();

            Place[] places = new Place[cursor.getCount()];
            int index = 0;

            while (cursor.moveToNext()) {
                Place place = new Place();
                place.setId(cursor.getLong(cursor.getColumnIndex(DbContract.Places._ID)));
                place.setLatitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LATITUDE)));
                place.setLongitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LONGITUDE)));
                place.setAmenity(cursor.getString(cursor.getColumnIndex(DbContract.Places.AMENITY)));
                places[index++] = place;
            }

            Timber.d("Object mapping time: %s", System.currentTimeMillis() - time);
            cursor.close();
            return places;
        }

        @Override
        protected void onPostExecute(Place[] places) {
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