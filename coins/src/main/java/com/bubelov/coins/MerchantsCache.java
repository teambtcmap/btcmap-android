package com.bubelov.coins;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.bubelov.coins.database.Tables;
import com.bubelov.coins.model.Merchant;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Author: Igor Bubelov
 * Date: 04/05/15 11:13
 */

public class MerchantsCache {
    private static final String TAG = MerchantsCache.class.getSimpleName();

    private final SQLiteDatabase db;

    private final Collection<Merchant> data;

    private volatile boolean initialized;

    public MerchantsCache(SQLiteDatabase db) {
        this.db = db;
        data = new ArrayList<>();
        initialize();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Collection<Merchant> getMerchants(LatLngBounds bounds, String amenity) {
        Collection<Merchant> merchants = new ArrayList<>();

        for (Merchant merchant : data) {
            if (merchant.getLatitude() >= bounds.southwest.latitude
                    && merchant.getLatitude() <= bounds.northeast.latitude
                    && merchant.getLongitude() >= bounds.southwest.longitude
                    && merchant.getLongitude() <= bounds.northeast.longitude) {
                if (TextUtils.isEmpty(amenity) || amenity.equals(merchant.getAmenity())) {
                    merchants.add(merchant);
                }
            }
        }

        return merchants;
    }

    public void invalidate() {
        initialized = false;
        data.clear();
        initialize();
    }

    private void initialize() {
        Log.d(TAG, "Initializing cache");

        Cursor cursor = db.query(Tables.Merchants.TABLE_NAME,
                new String[] { Tables.Merchants._ID, Tables.Merchants.LATITUDE, Tables.Merchants.LONGITUDE, Tables.Merchants.AMENITY },
                null,
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Merchant merchant = new Merchant();
            merchant.setId(cursor.getLong(cursor.getColumnIndex(Tables.Merchants._ID)));
            merchant.setLatitude(cursor.getDouble(cursor.getColumnIndex(Tables.Merchants.LATITUDE)));
            merchant.setLongitude(cursor.getDouble(cursor.getColumnIndex(Tables.Merchants.LONGITUDE)));
            merchant.setAmenity(cursor.getString(cursor.getColumnIndex(Tables.Merchants.AMENITY)));
            data.add(merchant);
        }

        initialized = true;
        cursor.close();

        Log.d(TAG, "Cache initialized");
    }
}