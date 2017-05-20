package com.bubelov.coins.repository.place;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import com.bubelov.coins.model.Place;
import com.bubelov.coins.database.DbContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class PlacesDataSourceDb {
    private final SQLiteDatabase db;

    @Inject
    PlacesDataSourceDb(SQLiteDatabase db) {
        this.db = db;
    }

    public List<Place> getPlaces() {
        try (Cursor cursor = db.query(DbContract.Places.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null)) {
            return getPlaces(cursor);
        }
    }

    public Place getPlace(long id) {
        try (Cursor cursor = db.query(DbContract.Places.TABLE_NAME,
                null,
                "_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null,
                null)) {
            return cursor.getCount() == 1 ? getPlaces(cursor).iterator().next() : null;
        }
    }

    public void insertOrUpdatePlace(Place place) {
        batchInsert(Collections.singleton(place));
    }

    private List<Place> getPlaces(@NonNull Cursor cursor) {
        List<Place> places = new ArrayList<>();

        while (cursor.moveToNext()) {
            places.add(Place.builder()
                    .id(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)))
                    .name(cursor.getString(cursor.getColumnIndex(DbContract.Places.NAME)))
                    .description(cursor.getString(cursor.getColumnIndex(DbContract.Places.DESCRIPTION)))
                    .latitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LATITUDE)))
                    .longitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LONGITUDE)))
                    .categoryId(cursor.getLong(cursor.getColumnIndex(DbContract.Places.CATEGORY_ID)))
                    .phone(cursor.getString(cursor.getColumnIndex(DbContract.Places.PHONE)))
                    .website(cursor.getString(cursor.getColumnIndex(DbContract.Places.WEBSITE)))
                    .openingHours(cursor.getString(cursor.getColumnIndex(DbContract.Places.OPENING_HOURS)))
                    .visible(cursor.getLong(cursor.getColumnIndex(DbContract.Places.VISIBLE)) == 1)
                    .openedClaims(cursor.getInt(cursor.getColumnIndex(DbContract.Places.OPENED_CLAIMS)))
                    .closedClaims(cursor.getInt(cursor.getColumnIndex(DbContract.Places.CLOSED_CLAIMS)))
                    .updatedAt(new Date(cursor.getLong(cursor.getColumnIndex(DbContract.Places._UPDATED_AT))))
                    .build());
        }

        return places;
    }

    void batchInsert(Collection<Place> places) {
        db.beginTransaction();

        try {
            String insertQuery = String.format("insert or replace into %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
                    DbContract.Places.VISIBLE,
                    DbContract.Places.OPENED_CLAIMS,
                    DbContract.Places.CLOSED_CLAIMS);

            SQLiteStatement insertStatement = db.compileStatement(insertQuery);

            for (Place place : places) {
                insertStatement.bindLong(1, place.id());
                insertStatement.bindLong(2, place.updatedAt().getTime());
                insertStatement.bindDouble(3, place.latitude());
                insertStatement.bindDouble(4, place.longitude());
                insertStatement.bindString(5, place.name());
                insertStatement.bindString(6, place.description());
                insertStatement.bindString(7, place.phone());
                insertStatement.bindString(8, place.website());
                insertStatement.bindLong(9, place.categoryId());
                insertStatement.bindString(10, place.openingHours());
                insertStatement.bindLong(11, place.visible() ? 1 : 0);
                insertStatement.bindLong(12, place.openedClaims());
                insertStatement.bindLong(13, place.closedClaims());
                insertStatement.execute();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}