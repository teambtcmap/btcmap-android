package com.bubelov.coins.data.repository.placecategory;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.data.database.DbContract;
import com.bubelov.coins.domain.PlaceCategory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlaceCategoriesDataSourceDb implements PlaceCategoriesDataSource {
    private final SQLiteDatabase db;

    @Inject
    PlaceCategoriesDataSourceDb(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public PlaceCategory getPlaceCategory(long id) {
        try (Cursor cursor = db.query(DbContract.PlaceCategories.TABLE_NAME,
                null,
                "_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null)) {
            if (cursor.getCount() == 0) {
                return null;
            } else {
                cursor.moveToFirst();
                return PlaceCategory.builder()
                        .id(cursor.getLong(cursor.getColumnIndex(DbContract.PlaceCategories._ID)))
                        .name(cursor.getString(cursor.getColumnIndex(DbContract.PlaceCategories.NAME)))
                        .build();
            }
        }
    }

    public void addPlaceCategory(PlaceCategory category) {
        ContentValues values = new ContentValues();
        values.put(DbContract.PlaceCategories._ID, category.id());
        values.put(DbContract.PlaceCategories.NAME, category.name());

        db.insertWithOnConflict(DbContract.PlaceCategories.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }
}