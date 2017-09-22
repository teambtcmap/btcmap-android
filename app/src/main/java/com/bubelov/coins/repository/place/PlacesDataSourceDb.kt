package com.bubelov.coins.repository.place

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import com.bubelov.coins.model.Place
import com.bubelov.coins.database.DbContract
import com.google.gson.Gson
import java.util.*

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesDataSourceDb @Inject
internal constructor(private val db: SQLiteDatabase, private val gson: Gson) {
    fun getPlaces(): List<Place> = db.query(DbContract.Places.TABLE_NAME,
            null, null, null, null, null, null, null).use { cursor -> return cursor.toPlaces() }

    fun insertOrReplace(place: Place) {
        insertOrReplace(setOf(place))
    }

    fun insertOrReplace(places: Collection<Place>) {
        db.beginTransaction()

        try {
            val insertQuery = String.format("insert or replace into %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    DbContract.Places.TABLE_NAME,
                    DbContract.Places._ID,
                    DbContract.Places.NAME,
                    DbContract.Places.LATITUDE,
                    DbContract.Places.LONGITUDE,
                    DbContract.Places.CATEGORY,
                    DbContract.Places.DESCRIPTION,
                    DbContract.Places.CURRENCIES,
                    DbContract.Places.OPENED_CLAIMS,
                    DbContract.Places.CLOSED_CLAIMS,
                    DbContract.Places.PHONE,
                    DbContract.Places.WEBSITE,
                    DbContract.Places.OPENING_HOURS,
                    DbContract.Places.VISIBLE,
                    DbContract.Places._UPDATED_AT
            )

            val insertStatement = db.compileStatement(insertQuery)

            for (place in places) {
                insertStatement.apply {
                    bindLong(1, place.id)
                    bindString(2, place.name)
                    bindDouble(3, place.latitude)
                    bindDouble(4, place.longitude)
                    bindString(5, place.category)
                    bindString(6, place.description)
                    bindString(7, gson.toJson(place.currencies))
                    bindLong(8, place.openedClaims.toLong())
                    bindLong(9, place.closedClaims.toLong())
                    bindString(10, place.phone)
                    bindString(11, place.website)
                    bindString(12, place.openingHours)
                    bindLong(13, (if (place.visible) 1 else 0).toLong())
                    bindLong(14, place.updatedAt.time)
                    execute()
                }
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
            id = getLong(getColumnIndex(DbContract.Places._ID)),
            name = getString(getColumnIndex(DbContract.Places.NAME)),
            latitude = getDouble(getColumnIndex(DbContract.Places.LATITUDE)),
            longitude = getDouble(getColumnIndex(DbContract.Places.LONGITUDE)),
            category = getString(getColumnIndex(DbContract.Places.CATEGORY)),
            description = getString(getColumnIndex(DbContract.Places.DESCRIPTION)),
            currencies = gson.fromJson(getString(getColumnIndex(DbContract.Places.CURRENCIES)), ArrayList<String>().javaClass),
            openedClaims = getLong(getColumnIndex(DbContract.Places.OPENED_CLAIMS)).toInt(),
            closedClaims = getLong(getColumnIndex(DbContract.Places.CLOSED_CLAIMS)).toInt(),
            phone = getString(getColumnIndex(DbContract.Places.PHONE)),
            website = getString(getColumnIndex(DbContract.Places.WEBSITE)),
            openingHours = getString(getColumnIndex(DbContract.Places.OPENING_HOURS)),
            visible = getLong(getColumnIndex(DbContract.Places.VISIBLE)) == 1L,
            updatedAt = Date(getLong(getColumnIndex(DbContract.Places._UPDATED_AT)))
    )
}