package com.bubelov.coins.loader;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.bubelov.coins.App;
import com.bubelov.coins.database.Tables;
import com.bubelov.coins.model.Merchant;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Author: Igor Bubelov
 * Date: 03/07/14 22:43
 */

public class PlacesLoader extends AsyncTaskLoader<Collection<Merchant>> {
    private static final String TAG = PlacesLoader.class.getName();

    private static final String LOCATION_KEY = "location";

    private LatLngBounds bounds;

    public static Bundle prepareArguments(LatLngBounds bounds) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(LOCATION_KEY, bounds);
        return arguments;
    }

    public PlacesLoader(Context context, Bundle arguments) {
        super(context);
        bounds = arguments.getParcelable(LOCATION_KEY);
        onContentChanged();
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public Collection<Merchant> loadInBackground() {
        Collection<Merchant> merchants = new ArrayList<>();

        String[] projection = {
                Tables.Merchants._ID,
                Tables.Merchants.LATITUDE,
                Tables.Merchants.LONGITUDE,
                Tables.Merchants.NAME,
                Tables.Merchants.DESCRIPTION,
                Tables.Merchants.PHONE,
                Tables.Merchants.WEBSITE
        };

        App app = (App)getContext().getApplicationContext();

        Cursor cursor = app.getDatabaseHelper().getReadableDatabase().query(
                Tables.Merchants.TABLE_NAME,
                projection,
                "(latitude between ? and ?) and (longitude between ? and ?)",
                new String[] { String.valueOf(bounds.southwest.latitude), String.valueOf(bounds.northeast.latitude), String.valueOf(bounds.southwest.longitude), String.valueOf(bounds.northeast.longitude) },
                null,
                null,
                null
        );

        while (cursor.moveToNext()) {
            Merchant merchant = new Merchant();
            merchant.setId(cursor.getLong(0));
            merchant.setLatitude(cursor.getFloat(1));
            merchant.setLongitude(cursor.getFloat(2));
            merchant.setName(cursor.getString(3));
            merchant.setDescription(cursor.getString(4));
            merchant.setPhone(cursor.getString(5));
            merchant.setWebsite(cursor.getString(6));

            merchants.add(merchant);
        }

        cursor.close();

        Log.d(TAG, String.format("%s merchants found for area %s", merchants.size(), bounds.toString()));
        return merchants;
    }
}
