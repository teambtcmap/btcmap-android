package com.bubelov.coins.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bubelov.coins.database.sync.DatabaseSyncService;

/**
 * @author Igor Bubelov
 */

public class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "database.db";

    private static final int DATABASE_VERSION = 1;

    private static final String CREATE_PLACES__SQL =
            "create table places (\n" +
            "    _id integer primary key,\n" +
            "    _updated_at integer not null,\n" +
            "    latitude real not null,\n" +
            "    longitude real not null,\n" +
            "    name text not null,\n" +
            "    description text not null,\n" +
            "    phone text not null,\n" +
            "    website text not null,\n" +
            "    category_id integer not null,\n" +
            "    opening_hours text not null,\n" +
            "    visible boolean not null\n" +
            ");";

    private static final String DROP_PLACES_SQL = "drop table if exists places;";

    private static final String CREATE_PLACE_CATEGORIES_SQL =
            "create table place_categories (\n" +
            "    _id integer primary key,\n" +
            "    name text not null,\n" +
            "    unique (name)\n" +
            ");";

    private static final String DROP_PLACE_CATEGORIES_SQL = "drop table if exists place_categories;";

    private static final String CREATE_CURRENCIES_SQL =
            "create table currencies (\n" +
            "    _id integer primary key,\n" +
            "    name text not null,\n" +
            "    code text not null,\n" +
            "    crypto boolean not null,\n" +
            "    unique (name),\n" +
            "    unique (code)\n" +
            ");";

    private static final String DROP_CURRENCIES_SQL = "drop table if exists currencies;";

    private static final String CREATE_CURRENCIES_PLACES_SQL =
            "create table currencies_places (\n" +
            "    _id integer primary key,\n" +
            "    currency_id integer not null,\n" +
            "    place_id integer not null,\n" +
            "    unique (currency_id, place_id)\n" +
            ");";

    private static final String DROP_CURRENCIES_PLACES_SQL = "drop table if exists currencies_places;";

    private Context context;

    public DbHelper(Context context) {
        super(context,
                DATABASE_NAME,
                null,
                DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PLACES__SQL);
        db.execSQL(CREATE_PLACE_CATEGORIES_SQL);
        db.execSQL(CREATE_CURRENCIES_SQL);
        db.execSQL(CREATE_CURRENCIES_PLACES_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_PLACES_SQL);
        db.execSQL(DROP_PLACE_CATEGORIES_SQL);
        db.execSQL(DROP_CURRENCIES_SQL);
        db.execSQL(DROP_CURRENCIES_PLACES_SQL);

        onCreate(db);

        DatabaseSyncService.Companion.start(context);
    }
}