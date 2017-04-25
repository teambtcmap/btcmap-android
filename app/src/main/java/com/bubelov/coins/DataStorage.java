package com.bubelov.coins;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Author: Igor Bubelov
 */

public class DataStorage {
    private SQLiteDatabase db;

    private SharedPreferences preferences;

    public DataStorage(SQLiteDatabase db, SharedPreferences preferences) {
        this.db = db;
        this.preferences = preferences;
    }

    public void doInTransaction(Runnable task) {
        db.beginTransaction();

        try {
            task.run();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Place getPlace(long id) {
        Cursor cursor = db.query(DbContract.Places.TABLE_NAME, null, "_id = ?", new String[]{String.valueOf(id)}, null, null, null);

        try {
            return cursor.moveToNext() ? getPlace(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public void insertPlaces(Collection<Place> places) {
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
    }

    public Place getLatestPlace() {
        Cursor cursor = db.query(DbContract.Places.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                DbContract.Places._UPDATED_AT + " desc",
                "1");

        try {
            return cursor.getCount() == 0 ? null : getPlace(cursor);
        } finally {
            cursor.close();
        }
    }

    private Place getPlace(Cursor cursor) {
        return Place.builder()
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
                .build();
    }

    public Currency getCurrency(String code) {
        Cursor cursor = db.query(DbContract.Currencies.TABLE_NAME,
                null,
                "code = ?",
                new String[]{code},
                null,
                null,
                null);

        try {
            return getCurrencies(cursor).get(0);
        } finally {
            cursor.close();
        }
    }

    public void insertCurrency(Currency currency) {
        db.insertWithOnConflict(DbContract.Currencies.TABLE_NAME,
                null,
                getContentValues(currency),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    private List<Currency> getCurrencies(@NonNull Cursor cursor) {
        List<Currency> currencies = new ArrayList<>();

        while (cursor.moveToNext()) {
            Currency currency = Currency.builder()
                    .id(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)))
                    .name(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.NAME)))
                    .code(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.CODE)))
                    .crypto(cursor.getInt(cursor.getColumnIndex(DbContract.Currencies.CRYPTO)) == 1)
                    .build();

            currencies.add(currency);
        }

        return currencies;
    }

    private ContentValues getContentValues(Currency currency) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Currencies._ID, currency.id());
        values.put(DbContract.Currencies.NAME, currency.name());
        values.put(DbContract.Currencies.CODE, currency.code());
        values.put(DbContract.Currencies.CRYPTO, currency.crypto());
        return values;
    }

    public void insertCurrencyForPlaces(Place place, Collection<Currency> currencies) {
        String insertQuery = String.format("insert or replace into %s (%s, %s) values (?, ?)", DbContract.CurrenciesPlaces.TABLE_NAME, DbContract.CurrenciesPlaces.CURRENCY_ID, DbContract.CurrenciesPlaces.PLACE_ID);
        SQLiteStatement insertStatement = db.compileStatement(insertQuery);

        for (Currency currency : currencies) {
            insertStatement.bindLong(1, currency.id());
            insertStatement.bindLong(2, place.id());
            insertStatement.execute();
        }
    }

    private String getEmptyStringIfNull(String string) {
        return string == null ? "" : string;
    }
}