package db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import db.table.event.EventSchema
import db.table.place.PlaceSchema
import db.table.comment.CommentSchema

class DbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(PlaceSchema.toString())
        db.execSQL(EventSchema.toString())
        db.execSQL(CommentSchema.toString())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        throw SQLiteException("Can't downgrade database from version $oldVersion to $newVersion")
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "btcmap-2025-11-06.db"
    }
}