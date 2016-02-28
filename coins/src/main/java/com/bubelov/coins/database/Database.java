package com.bubelov.coins.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.App;

/**
 * Author: Igor Bubelov
 * Date: 09/02/16 13:20
 */

public class Database {
    private static SQLiteDatabase db;

    public static SQLiteDatabase get() {
        return get(App.getInstance());
    }

    public static SQLiteDatabase get(Context context) {
        if (db == null) {
            db = new DbHelper(context).getWritableDatabase();
        }

        return db;
    }
}