package com.bubelov.coins.repository.place

import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase

import com.bubelov.coins.model.Place
import com.bubelov.coins.database.DbContract
import java.util.*

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesDataSourceDb @Inject
internal constructor(private val db: SQLiteDatabase) {
    val places: MutableList<Place>
        get() = db.query(DbContract.Places.TABLE_NAME,
                null, null, null, null, null, null, null).use { cursor -> return getPlaces(cursor) }

    val cachedPlacesCount: Long
        get() = DatabaseUtils.queryNumEntries(db, DbContract.Places.TABLE_NAME)

    fun getPlace(id: Long): Place? {
        db.query(DbContract.Places.TABLE_NAME, null,
                "_id = ?",
                arrayOf(id.toString()), null, null, null, null).use { cursor -> return if (cursor.count == 1) getPlaces(cursor).iterator().next() else null }
    }

    fun insertOrUpdatePlace(place: Place) {
        batchInsert(setOf(place))
    }

    private fun getPlaces(cursor: Cursor): MutableList<Place> {
        val places = ArrayList<Place>()

        while (cursor.moveToNext()) {
            places.add(Place(
                    id = cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)),
                    name = cursor.getString(cursor.getColumnIndex(DbContract.Places.NAME)),
                    description = cursor.getString(cursor.getColumnIndex(DbContract.Places.DESCRIPTION)),
                    latitude = cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LATITUDE)),
                    longitude = cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LONGITUDE)),
                    categoryId = cursor.getLong(cursor.getColumnIndex(DbContract.Places.CATEGORY_ID)),
                    phone = cursor.getString(cursor.getColumnIndex(DbContract.Places.PHONE)),
                    website = cursor.getString(cursor.getColumnIndex(DbContract.Places.WEBSITE)),
                    openingHours = cursor.getString(cursor.getColumnIndex(DbContract.Places.OPENING_HOURS)),
                    visible = cursor.getLong(cursor.getColumnIndex(DbContract.Places.VISIBLE)) == 1L,
                    updatedAt = Date(cursor.getLong(cursor.getColumnIndex(DbContract.Places._UPDATED_AT)))
            ))
        }

        return places
    }

    internal fun batchInsert(places: Collection<Place>) {
        db.beginTransaction()

        try {
            val insertQuery = String.format("insert or replace into %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    DbContract.Places.TABLE_NAME,
                    DbContract.Places._ID,
                    DbContract.Places._UPDATED_AT,
                    DbContract.Places.LATITUDE,
                    DbContract.Places.LONGITUDE,
                    DbContract.Places.NAME,
                    DbContract.Places.DESCRIPTION,
                    DbContract.Places.PHONE,
                    DbContract.Places.WEBSITE,
                    DbContract.Places.CATEGORY_ID,
                    DbContract.Places.OPENING_HOURS,
                    DbContract.Places.VISIBLE)

            val insertStatement = db.compileStatement(insertQuery)

            for ((id, name, description, latitude, longitude, categoryId, phone, website, openingHours, visible, updatedAt) in places) {
                insertStatement.bindLong(1, id)
                insertStatement.bindLong(2, updatedAt.time)
                insertStatement.bindDouble(3, latitude)
                insertStatement.bindDouble(4, longitude)
                insertStatement.bindString(5, name)
                insertStatement.bindString(6, description)
                insertStatement.bindString(7, phone)
                insertStatement.bindString(8, website)
                insertStatement.bindLong(9, categoryId)
                insertStatement.bindString(10, openingHours)
                insertStatement.bindLong(11, (if (visible) 1 else 0).toLong())
                insertStatement.execute()
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}