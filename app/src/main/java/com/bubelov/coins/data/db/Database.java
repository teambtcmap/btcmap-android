package com.bubelov.coins.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bubelov.coins.data.api.coins.model.Currency;
import com.bubelov.coins.data.api.coins.model.Place;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Igor Bubelov
 */

public class Database {
    private final SQLiteDatabase db;

    public Database(SQLiteDatabase db) {
        this.db = db;
    }

    public @Nullable
    Place getPlace(long id) {
        try (Cursor cursor = db.query(DbContract.Places.TABLE_NAME,
                null,
                "_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null)) {
            return cursor.getCount() != 0 ? getPlaces(cursor).iterator().next() : null;
        }
    }

    public @Nullable Place getLatestPlace() {
        try (Cursor cursor = db.query(DbContract.Places.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                DbContract.Places._UPDATED_AT + " desc",
                "1")) {
            return cursor.getCount() == 0 ? null : getPlaces(cursor).iterator().next();
        }
    }

    public @Nullable Place getRandomPlace() {
        int placesCount = (int) DatabaseUtils.queryNumEntries(db, DbContract.Places.TABLE_NAME);

        if (placesCount == 0) {
            return null;
        }

        try (Cursor cursor = db.query(DbContract.Places.TABLE_NAME,
                null,
                "_id = ?",
                new String[]{String.valueOf(new Random(System.currentTimeMillis()).nextInt(placesCount + 1))},
                null,
                null,
                null)) {
            return getPlaces(cursor).get(0);
        }
    }

    public List<Place> getAllPlaces() {
        try (Cursor cursor = db.query(DbContract.Places.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null)) {
            return cursor.getCount() == 0 ? Collections.EMPTY_LIST : getPlaces(cursor);
        }
    }

    public List<Place> getPlaces(String query) {
        try (Cursor cursor = db.query(DbContract.Places.TABLE_NAME,
                null,
                String.format("%s = 1 and (%s like ? or %s like ?)", DbContract.Places.VISIBLE, DbContract.Places.NAME, DbContract.Places.AMENITY),
                new String[]{"%" + query + "%", "%" + query + "%"},
                null,
                null,
                DbContract.Places._UPDATED_AT + " desc",
                null)) {
            return cursor.getCount() == 0 ? Collections.EMPTY_LIST : getPlaces(cursor);
        }
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

    public void insertPlaces(@NonNull Collection<Place> places) {
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
                insertStatement.bindString(5, getEmptyStringIfNull(place.name()));
                insertStatement.bindString(6, getEmptyStringIfNull(place.description()));
                insertStatement.bindString(7, getEmptyStringIfNull(place.phone()));
                insertStatement.bindString(8, getEmptyStringIfNull(place.website()));
                insertStatement.bindString(9, getEmptyStringIfNull(place.amenity()));
                insertStatement.bindString(10, getEmptyStringIfNull(place.openingHours()));
                insertStatement.bindString(11, getEmptyStringIfNull(place.address()));
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

    public @NonNull
    Currency getCurrency(@NonNull String code) {
        try (Cursor cursor = db.query(DbContract.Currencies.TABLE_NAME,
                null,
                "code = ?",
                new String[]{code},
                null,
                null,
                null)) {
            return getCurrency(cursor);
        }
    }

    private @NonNull
    Currency getCurrency(@NonNull Cursor cursor) {
        return Currency.builder()
                .id(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)))
                .name(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.NAME)))
                .code(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.CODE)))
                .crypto(cursor.getInt(cursor.getColumnIndex(DbContract.Currencies.CRYPTO)) == 1)
                .build();
    }

    public void insertCurrencies(@NonNull Collection<Currency> currencies) {
        db.beginTransaction();

        try {
            for (Currency currency : currencies) {
                db.insertWithOnConflict(DbContract.Currencies.TABLE_NAME,
                        null,
                        getContentValues(currency),
                        SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues getContentValues(@NonNull Currency currency) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Currencies._ID, currency.id());
        values.put(DbContract.Currencies.NAME, currency.name());
        values.put(DbContract.Currencies.CODE, currency.code());
        values.put(DbContract.Currencies.CRYPTO, currency.crypto());
        return values;
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

    private String getEmptyStringIfNull(String string) {
        return string == null ? "" : string;
    }
}