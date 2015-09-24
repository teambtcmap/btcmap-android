package com.bubelov.coins.database;

import android.content.Context;

import com.bubelov.coins.R;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Author: Igor Bubelov
 * Date: 9/22/15 10:00 PM
 */

public class PreloadedDatabaseHelper extends SQLiteAssetHelper {
    public PreloadedDatabaseHelper(Context context) {
        super(context,
                context.getResources().getString(R.string.database_name),
                null,
                context.getResources().getInteger(R.integer.database_version));
    }
}
