package com.bubelov.coins.repository.placecategory

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

import com.bubelov.coins.database.DbContract
import com.bubelov.coins.model.PlaceCategory

import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
class PlaceCategoriesDataSourceDb @Inject
internal constructor(private val db: SQLiteDatabase) : PlaceCategoriesDataSource {
    override fun getPlaceCategory(id: Long): PlaceCategory? {
        db.query(DbContract.PlaceCategories.TABLE_NAME,
                null,
                "_id = ?",
                arrayOf(id.toString()), null, null, null).use { cursor ->
            if (cursor.count == 0) {
                return null
            } else {
                cursor.moveToFirst()
                return PlaceCategory(
                        id = cursor.getLong(cursor.getColumnIndex(DbContract.PlaceCategories._ID)),
                        name = cursor.getString(cursor.getColumnIndex(DbContract.PlaceCategories.NAME))
                )
            }
        }
    }

    fun addPlaceCategory(category: PlaceCategory) {
        val values = ContentValues().apply {
            put(DbContract.PlaceCategories._ID, category.id)
            put(DbContract.PlaceCategories.NAME, category.name)
        }

        db.insertWithOnConflict(DbContract.PlaceCategories.TABLE_NAME, null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE)
    }
}