package com.bubelov.coins.loader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.bubelov.coins.App;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Author: Igor Bubelov
 * Date: 18/04/15 12:05
 */

public class MerchantsLoader extends SimpleCursorLoader {
    private static final String TAG = MerchantsLoader.class.getSimpleName();

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
        SQLiteDatabase db = App.getInstance().getDatabaseHelper().getReadableDatabase();

        return db.rawQuery("select m._id, m.latitude, m.longitude, m.name, m.description from merchants as m join merchants_to_currencies as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where (latitude between ? and ?) and (longitude between ? and ?) and c.show_on_map = 1 group by m._ID",
                new String[] { String.valueOf(bounds.southwest.latitude), String.valueOf(bounds.northeast.latitude), String.valueOf(bounds.southwest.longitude), String.valueOf(bounds.northeast.longitude) });
    }
}