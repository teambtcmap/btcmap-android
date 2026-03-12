package org.btcmap.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.btcmap.db.table.event.EventSchema
import org.btcmap.db.table.place.PlaceSchema
import org.btcmap.db.table.comment.CommentSchema

class DbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(PlaceSchema.toString())
        db.execSQL(EventSchema.toString())
        db.execSQL(CommentSchema.toString())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            Log.d("db", "upgrading schema v1 to v2")
            db.execSQL("ALTER TABLE place ADD COLUMN localized_name TEXT;")
            db.execSQL("UPDATE place SET updated_at = '2000-01-01T00:00:00Z';")
            Log.d("db", "upgraded schema v1 to v2")
        }

        if (oldVersion == 2 && newVersion == 3) {
            Log.d("db", "upgrading schema v2 to v3")
            db.execSQL("ALTER TABLE place ADD COLUMN localized_opening_hours TEXT;")
            db.execSQL("UPDATE place SET updated_at = '2000-01-01T00:00:00Z';")
            Log.d("db", "upgraded schema v2 to v3")
        }
    }

    companion object {
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "btcmap-2025-11-06.db"
    }
}