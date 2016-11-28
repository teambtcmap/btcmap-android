package com.bubelov.coins;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.Place;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class PlacesCache {
    @Inject
    Lazy<SQLiteDatabase> db;

    private final Collection<Place> data = new ArrayList<>();

    private volatile boolean initialized;

    private List<PlacesCacheListener> listeners = new ArrayList<>();

    public PlacesCache() {
        Timber.d("Creating...");
        Injector.INSTANCE.getAppComponent().inject(this);
        initialize();
        Timber.d("Created");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Collection<Place> getPlaces(LatLngBounds bounds, Amenity amenity) {
        Timber.d("getPlaces called");
        long time = System.currentTimeMillis();

        Collection<Place> places = new ArrayList<>();

        for (Place place : data) {
            if (bounds.contains(place.getPosition())
                    && (amenity == null || amenity.name().equalsIgnoreCase(place.getAmenity()))) {
                places.add(place);
            }
        }

        time = System.currentTimeMillis() - time;
        Timber.d("%s places found. Time: %s", places.size(), time);

        return places;
    }

    public void invalidate() {
        Timber.d("Invalidating...");
        initialized = false;
        initialize();
    }

    public List<PlacesCacheListener> getListeners() {
        return listeners;
    }

    private void initialize() {
        new InitCacheTask().execute();
    }

    private class InitCacheTask extends AsyncTask<Void, Void, List<Place>> {
        @Override
        protected List<Place> doInBackground(Void... params) {
            Timber.d("Querying data from DB");
            long time = System.currentTimeMillis();
            Cursor cursor = db.get().rawQuery("select distinct p._id, p.latitude, p.longitude, p.amenity from places as p join currencies_places as cp on p._id = cp.place_id join currencies c on c._id = cp.currency_id where c.show_on_map = 1", null);
            Timber.d("Query time: %s", System.currentTimeMillis() - time);

            List<Place> newestData = new ArrayList<>(7500);

            while (cursor.moveToNext()) {
                Place place = new Place();
                place.setId(cursor.getLong(cursor.getColumnIndex(DbContract.Places._ID)));
                place.setLatitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LATITUDE)));
                place.setLongitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LONGITUDE)));
                place.setAmenity(cursor.getString(cursor.getColumnIndex(DbContract.Places.AMENITY)));
                newestData.add(place);
            }

            Timber.d("%s places loaded. Overall time: %s", newestData.size(), System.currentTimeMillis() - time);
            cursor.close();
            return newestData;
        }

        @Override
        protected void onPostExecute(List<Place> newestData) {
            data.clear();
            data.addAll(newestData);
            initialized = true;

            for (PlacesCacheListener listener : new ArrayList<>(listeners)) {
                listener.onPlacesCacheInitialized();
            }
        }
    }

    public interface PlacesCacheListener {
        void onPlacesCacheInitialized();
    }
}