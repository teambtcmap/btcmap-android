package com.bubelov.coins.database;

import android.content.Context;

import com.bubelov.coins.R;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * @author Igor Bubelov
 */

public class PreloadedDbHelper extends SQLiteAssetHelper {
    public PreloadedDbHelper(Context context) {
        super(context,
                context.getResources().getString(R.string.database_name),
                null,
                context.getResources().getInteger(R.integer.database_version));
        setForcedUpgrade();
    }
}
