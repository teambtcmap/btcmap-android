package com.bubelov.coins.repository.place

import android.database.Cursor
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
    fun getPlaces(): List<Place> = db.query(DbContract.Places.TABLE_NAME,
            null, null, null, null, null, null, null).use { cursor -> return cursor.toPlaces() }

    fun insertOrReplace(place: Place) {
        insertOrReplace(setOf(place))
    }

    fun insertOrReplace(places: Collection<Place>) {
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

    private fun Cursor.toPlaces() = mutableListOf<Place>().apply {
        moveToFirst()

        while (moveToNext()) {
            add(toPlace())
        }
    }

    private fun Cursor.toPlace() = Place(
            id = getLong(getColumnIndex(DbContract.Currencies._ID)),
            name = getString(getColumnIndex(DbContract.Places.NAME)),
            description = getString(getColumnIndex(DbContract.Places.DESCRIPTION)),
            latitude = getDouble(getColumnIndex(DbContract.Places.LATITUDE)),
            longitude = getDouble(getColumnIndex(DbContract.Places.LONGITUDE)),
            categoryId = getLong(getColumnIndex(DbContract.Places.CATEGORY_ID)),
            phone = getString(getColumnIndex(DbContract.Places.PHONE)),
            website = getString(getColumnIndex(DbContract.Places.WEBSITE)),
            openingHours = getString(getColumnIndex(DbContract.Places.OPENING_HOURS)),
            visible = getLong(getColumnIndex(DbContract.Places.VISIBLE)) == 1L,
            updatedAt = Date(getLong(getColumnIndex(DbContract.Places._UPDATED_AT)))
    )
}