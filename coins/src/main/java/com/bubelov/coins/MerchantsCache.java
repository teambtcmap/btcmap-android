package com.bubelov.coins;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Amenity;
import com.bubelov.coins.model.Merchant;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 04/05/15 11:13
 */

public class MerchantsCache {
    private final SQLiteDatabase db;

    private final Collection<Merchant> data;

    private volatile boolean initialized;

    private List<MerchantsCacheListener> listeners = new ArrayList<>();

    public MerchantsCache(SQLiteDatabase db) {
        this.db = db;
        data = new ArrayList<>();
        initialize();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Collection<Merchant> getMerchants(LatLngBounds bounds, Amenity amenity) {
        Collection<Merchant> merchants = new ArrayList<>();

        for (Merchant merchant : data) {
            if (bounds.contains(merchant.getPosition())
                    && (amenity == null || amenity.name().equalsIgnoreCase(merchant.getAmenity()))) {
                merchants.add(merchant);
            }
        }

        return merchants;
    }

    public void invalidate() {
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
            List<Merchant> newestData = new ArrayList<>();

            Cursor cursor = db.rawQuery("select distinct m._id, m.latitude, m.longitude, m.amenity from merchants as m join currencies_merchants as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where c.show_on_map = 1", null);

            while (cursor.moveToNext()) {
                Merchant merchant = new Merchant();
                merchant.setId(cursor.getLong(cursor.getColumnIndex(Database.Merchants._ID)));
                merchant.setLatitude(cursor.getDouble(cursor.getColumnIndex(Database.Merchants.LATITUDE)));
                merchant.setLongitude(cursor.getDouble(cursor.getColumnIndex(Database.Merchants.LONGITUDE)));
                merchant.setAmenity(cursor.getString(cursor.getColumnIndex(Database.Merchants.AMENITY)));
                newestData.add(merchant);
            }

            cursor.close();

            return newestData;
        }

        @Override
        protected void onPostExecute(List<Merchant> newestData) {
            data.clear();
            data.addAll(newestData);
            initialized = true;

            for (MerchantsCacheListener listener : listeners) {
                listener.onMerchantsCacheInitialized();
            }
        }
    }

    public interface MerchantsCacheListener {
        void onMerchantsCacheInitialized();
    }
}