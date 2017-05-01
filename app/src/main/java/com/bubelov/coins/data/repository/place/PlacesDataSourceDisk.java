package com.bubelov.coins.data.repository.place;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import com.bubelov.coins.domain.Currency;
import com.bubelov.coins.domain.Place;
import com.bubelov.coins.data.database.DbContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
class PlacesDataSourceDisk implements PlacesDataSource {
    private final SQLiteDatabase db;

    @Inject
    PlacesDataSourceDisk(SQLiteDatabase db) {
        this.db = db;
    }

    @Override
    public Place get(long id) {
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

    @Override
    public void add(Place place) {
        batchInsert(Collections.singleton(place));
    }

    @Override
    public void update(Place place) {
        batchInsert(Collections.singleton(place));
    }

    Collection<Place> getAll() {
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

    private Collection<Place> getPlaces(@NonNull Cursor cursor) {
        List<Place> places = new ArrayList<>();

        while (cursor.moveToNext()) {
            places.add(Place.builder()
                    .id(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)))
                    .name(cursor.getString(cursor.getColumnIndex(DbContract.Places.NAME)))
                    .description(cursor.getString(cursor.getColumnIndex(DbContract.Places.DESCRIPTION)))
                    .latitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LATITUDE)))
                    .longitude(cursor.getDouble(cursor.getColumnIndex(DbContract.Places.LONGITUDE)))
                    .amenity(cursor.getString(cursor.getColumnIndex(DbContract.Places.AMENITY)))
                    .phone(cursor.getString(cursor.getColumnIndex(DbContract.Places.PHONE)))
                    .website(cursor.getString(cursor.getColumnIndex(DbContract.Places.WEBSITE)))
                    .openingHours(cursor.getString(cursor.getColumnIndex(DbContract.Places.OPENING_HOURS)))
                    .address(cursor.getString(cursor.getColumnIndex(DbContract.Places.ADDRESS)))
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
            String insertQuery = String.format("insert or replace into %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    DbContract.Places.TABLE_NAME,
                    DbContract.Places._ID,
                    DbContract.Places._UPDATED_AT,
                    DbContract.Places.LATITUDE,
                    DbContract.Places.LONGITUDE,
                    DbContract.Places.NAME,
                    DbContract.Places.DESCRIPTION,
                    DbContract.Places.PHONE,
                    DbContract.Places.WEBSITE,
                    DbContract.Places.AMENITY,
                    DbContract.Places.OPENING_HOURS,
                    DbContract.Places.ADDRESS,
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
                insertStatement.bindString(9, place.amenity());
                insertStatement.bindString(10, place.openingHours());
                insertStatement.bindString(11, place.address());
                insertStatement.bindLong(12, place.visible() ? 1 : 0);
                insertStatement.bindLong(13, place.openedClaims());
                insertStatement.bindLong(14, place.closedClaims());
                insertStatement.execute();
            }

            Map<Long, Collection<Place>> currencyIdsToPlaces = new HashMap<>();

            for (Place place : places) {
                for (Currency currency : place.currencies) {
                    if (!currencyIdsToPlaces.containsKey(currency.id())) {
                        currencyIdsToPlaces.put(currency.id(), new HashSet<>());
                    }

                    currencyIdsToPlaces.get(currency.id()).add(place);
                }
            }

            for (Long currencyId : currencyIdsToPlaces.keySet()) {
                insertCurrencyForPlaces(currencyIdsToPlaces.get(currencyId), currencyId);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertCurrencyForPlaces(@NonNull Collection<Place> places, long currencyId) {
        String insertQuery = String.format("insert or replace into %s (%s, %s) values (?, ?)", DbContract.CurrenciesPlaces.TABLE_NAME, DbContract.CurrenciesPlaces.CURRENCY_ID, DbContract.CurrenciesPlaces.PLACE_ID);
        SQLiteStatement insertStatement = db.compileStatement(insertQuery);

        for (Place place : places) {
            insertStatement.bindLong(1, currencyId);
            insertStatement.bindLong(2, place.id());
            insertStatement.execute();
        }
    }
}