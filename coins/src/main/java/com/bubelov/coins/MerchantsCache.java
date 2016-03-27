package com.bubelov.coins;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.Merchant;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import timber.log.Timber;

/**
 * Author: Igor Bubelov
 * Date: 04/05/15 11:13
 */

public class MerchantsCache {
    @Inject
    Lazy<SQLiteDatabase> db;

    private final Collection<Merchant> data = new ArrayList<>();

    private volatile boolean initialized;

    private List<MerchantsCacheListener> listeners = new ArrayList<>();

    public MerchantsCache() {
        Timber.d("Creating...");
        Injector.INSTANCE.getAppComponent().inject(this);
        initialize();
        Timber.d("Created");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Collection<Merchant> getMerchants(LatLngBounds bounds, Amenity amenity) {
        Timber.d("getMerchants called");
        long time = System.currentTimeMillis();

        Collection<Merchant> merchants = new ArrayList<>();

        for (Merchant merchant : data) {
            if (bounds.contains(merchant.getPosition())
                    && (amenity == null || amenity.name().equalsIgnoreCase(merchant.getAmenity()))) {
                merchants.add(merchant);
            }
        }

        time = System.currentTimeMillis() - time;
        Timber.d("%s merchants found. Time: %s", merchants.size(), time);

        return merchants;
    }

    public void invalidate() {
        Timber.d("Invalidating...");
        initialized = false;
        initialize();
    }

    public List<MerchantsCacheListener> getListeners() {
        return listeners;
    }

    private void initialize() {
        new InitCacheTask().execute();
    }

    private class InitCacheTask extends AsyncTask<Void, Void, List<Merchant>> {
        @Override
        protected List<Merchant> doInBackground(Void... params) {
            Timber.d("Querying data from DB");
            long time = System.currentTimeMillis();
            Cursor cursor = db.get().rawQuery("select distinct m._id, m.latitude, m.longitude, m.amenity from merchants as m join currencies_merchants as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where c.show_on_map = 1", null);
            Timber.d("Query time: %s", System.currentTimeMillis() - time);

            List<Merchant> newestData = new ArrayList<>(7500);

            while (cursor.moveToNext()) {
                Merchant merchant = new Merchant();
                merchant.setId(cursor.getLong(cursor.getColumnIndex(DbContract.Merchants._ID)));
                merchant.setLatitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Merchants.LATITUDE)));
                merchant.setLongitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Merchants.LONGITUDE)));
                merchant.setAmenity(cursor.getString(cursor.getColumnIndex(DbContract.Merchants.AMENITY)));
                newestData.add(merchant);
            }

            Timber.d("%s merchants loaded. Overall time: %s", newestData.size(), System.currentTimeMillis() - time);
            cursor.close();
            return newestData;
        }

        @Override
        protected void onPostExecute(List<Merchant> newestData) {
            data.clear();
            data.addAll(newestData);
            initialized = true;

            for (MerchantsCacheListener listener : new ArrayList<>(listeners)) {
                listener.onMerchantsCacheInitialized();
            }
        }
    }

    public interface MerchantsCacheListener {
        void onMerchantsCacheInitialized();
    }
}