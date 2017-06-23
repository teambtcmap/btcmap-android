package com.bubelov.coins.repository.placecategory

import android.content.ContentValues
import android.database.Cursor
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
internal constructor(private val db: SQLiteDatabase) {
    fun getPlaceCategory(id: Long): PlaceCategory? {
        db.query(DbContract.PlaceCategories.TABLE_NAME,
                null,
                "_id = ?",
                arrayOf(id.toString()), null, null, null).use { cursor ->
            return if (cursor.moveToNext()) cursor.toPlaceCategory() else null
        }
    }

    fun addPlaceCategory(category: PlaceCategory) {
        db.insertWithOnConflict(DbContract.PlaceCategories.TABLE_NAME, null,
                category.toContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun PlaceCategory.toContentValues() = ContentValues().apply {
        put(DbContract.PlaceCategories._ID, id)
        put(DbContract.PlaceCategories.NAME, name)
    }

    private fun Cursor.toPlaceCategory() = PlaceCategory(
            id = getLong(getColumnIndex(DbContract.PlaceCategories._ID)),
            name = getString(getColumnIndex(DbContract.PlaceCategories.NAME))
    )
}