package com.bubelov.coins.database;

import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.App;

/**
 * Author: Igor Bubelov
 * Date: 09/02/16 13:20
 */

public class Database {
    private static SQLiteDatabase db;

    public static SQLiteDatabase get() {
        if (db == null) {
            db = new PreloadedDbHelper(App.getInstance()).getWritableDatabase();
        }

        return db;
    }
}