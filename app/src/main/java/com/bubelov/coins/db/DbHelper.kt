package com.bubelov.coins.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * @author Igor Bubelov
 */

class DbHelper(context: Context) : SQLiteOpenHelper(context, DbHelper.DATABASE_NAME, null, DbHelper.DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_PLACES__SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DROP_PLACES_SQL)
        onCreate(db)
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
                        "    visible boolean not null,\n" +
                        "    currencies text not null,\n" +
                        "    opened_claims integer not null,\n" +
                        "    closed_claims integer not null\n" +
                        ");"

        private val DROP_PLACES_SQL = "drop table if exists places;"
    }
}