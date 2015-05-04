package com.bubelov.coins.loader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;

import com.bubelov.coins.App;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Author: Igor Bubelov
 * Date: 18/04/15 12:05
 */

public class MerchantsLoader extends SimpleCursorLoader {
    private static final String TAG = MerchantsLoader.class.getSimpleName();

    private static final String BOUNDS_KEY = "bounds";

    private static final String AMENITY_KEY = "amenity";

    private LatLngBounds bounds;

    private String amenity;

    public static Bundle prepareArguments(LatLngBounds bounds, String amenity) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(BOUNDS_KEY, bounds);
        arguments.putString(AMENITY_KEY, amenity);
        return arguments;
    }

    public MerchantsLoader(Context context, Bundle arguments) {
        super(context);
        bounds = arguments.getParcelable(BOUNDS_KEY);
        amenity = arguments.getString(AMENITY_KEY);
    }

    @Override
    public Cursor loadInBackground() {
        SQLiteDatabase db = App.getInstance().getDatabaseHelper().getReadableDatabase();

        if (TextUtils.isEmpty(amenity)) {
            return db.rawQuery("select distinct m._id, m.latitude, m.longitude, m.amenity from merchants as m join currencies_merchants as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where (latitude between ? and ?) and (m.longitude between ? and ?) and c.show_on_map = 1",
                    new String[] { String.valueOf(bounds.southwest.latitude), String.valueOf(bounds.northeast.latitude), String.valueOf(bounds.southwest.longitude), String.valueOf(bounds.northeast.longitude) });
        } else {
            return db.rawQuery("select distinct m._id, m.latitude, m.longitude, m.amenity from merchants as m join currencies_merchants as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where (latitude between ? and ?) and (m.longitude between ? and ?) and m.amenity = ? and c.show_on_map = 1",
                    new String[] { String.valueOf(bounds.southwest.latitude), String.valueOf(bounds.northeast.latitude), String.valueOf(bounds.southwest.longitude), String.valueOf(bounds.northeast.longitude), amenity });
        }
    }
}