package com.bubelov.coins.database;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Author: Igor Bubelov
 * Date: 9/24/15 8:12 PM
 */

public class DatabaseFactory {
    public static SQLiteOpenHelper newHelper(Context context) {
        return new PreloadedDatabaseHelper(context);
    }
}
