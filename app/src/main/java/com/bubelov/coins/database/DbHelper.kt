package com.bubelov.coins.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bubelov.coins.dagger.Injector

import com.bubelov.coins.database.sync.DatabaseSyncService
import com.bubelov.coins.model.Place
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jetbrains.anko.doAsync
import java.io.InputStreamReader

/**
 * @author Igor Bubelov
 */

class DbHelper(private val context: Context, private val gson: Gson) : SQLiteOpenHelper(context, DbHelper.DATABASE_NAME, null, DbHelper.DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_PLACES__SQL)
        db.execSQL(CREATE_PLACE_CATEGORIES_SQL)
        db.execSQL(CREATE_CURRENCIES_SQL)
        db.execSQL(CREATE_CURRENCIES_PLACES_SQL)

        val placesFromAssets = getPlacesFromAssets()

        doAsync {
            Injector.mainComponent.placesRepository().setCache(placesFromAssets)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DROP_PLACES_SQL)
        db.execSQL(DROP_PLACE_CATEGORIES_SQL)
        db.execSQL(DROP_CURRENCIES_SQL)
        db.execSQL(DROP_CURRENCIES_PLACES_SQL)

        onCreate(db)

        DatabaseSyncService.start(context)
    }

    private fun getPlacesFromAssets(): Collection<Place> {
        val input = context.assets.open("places.json")
        val typeToken = object: TypeToken<List<Place>>(){}
        return gson.fromJson(InputStreamReader(input), typeToken.type)
    }

    companion object {
        private val DATABASE_NAME = "database.db"

        private val DATABASE_VERSION = 1

        private val CREATE_PLACES__SQL =
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
                        ");"

        private val DROP_PLACES_SQL = "drop table if exists places;"

        private val CREATE_PLACE_CATEGORIES_SQL =
                "create table place_categories (\n" +
                        "    _id integer primary key,\n" +
                        "    name text not null,\n" +
                        "    unique (name)\n" +
                        ");"

        private val DROP_PLACE_CATEGORIES_SQL = "drop table if exists place_categories;"

        private val CREATE_CURRENCIES_SQL =
                "create table currencies (\n" +
                        "    _id integer primary key,\n" +
                        "    name text not null,\n" +
                        "    code text not null,\n" +
                        "    crypto boolean not null,\n" +
                        "    unique (name),\n" +
                        "    unique (code)\n" +
                        ");"

        private val DROP_CURRENCIES_SQL = "drop table if exists currencies;"

        private val CREATE_CURRENCIES_PLACES_SQL =
                "create table currencies_places (\n" +
                        "    _id integer primary key,\n" +
                        "    currency_id integer not null,\n" +
                        "    place_id integer not null,\n" +
                        "    unique (currency_id, place_id)\n" +
                        ");"

        private val DROP_CURRENCIES_PLACES_SQL = "drop table if exists currencies_places;"
    }
}