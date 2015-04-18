package com.bubelov.coins.loader;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import com.bubelov.coins.App;
import com.bubelov.coins.database.Tables;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Author: Igor Bubelov
 * Date: 18/04/15 12:05
 */

public class MerchantsLoader extends SimpleCursorLoader {
    private static final String BOUNDS_KEY = "bounds";

    private LatLngBounds bounds;

    public static Bundle prepareArguments(LatLngBounds bounds) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(BOUNDS_KEY, bounds);
        return arguments;
    }

    public MerchantsLoader(Context context, Bundle arguments) {
        super(context);
        bounds = arguments.getParcelable(BOUNDS_KEY);
    }

    @Override
    public Cursor loadInBackground() {
        String[] projection = {
                Tables.Merchants._ID,
                Tables.Merchants.LATITUDE,
                Tables.Merchants.LONGITUDE,
                Tables.Merchants.NAME,
                Tables.Merchants.DESCRIPTION
        };

        return App.getInstance().getDatabaseHelper().getReadableDatabase().query(
                Tables.Merchants.TABLE_NAME,
                projection,
                "(latitude between ? and ?) and (longitude between ? and ?)",
                new String[] { String.valueOf(bounds.southwest.latitude), String.valueOf(bounds.northeast.latitude), String.valueOf(bounds.southwest.longitude), String.valueOf(bounds.northeast.longitude) },
                null,
                null,
                null
        );
    }
}