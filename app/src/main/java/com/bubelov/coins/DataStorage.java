package com.bubelov.coins;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Place;

import java.util.ArrayList;
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

    public Currency getCurrency(long id) {
        Cursor cursor = db.query(DbContract.Currencies.TABLE_NAME,
                null,
                "_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null);

        try {
            return getCurrencies(cursor).get(0);
        } finally {
            cursor.close();
        }
    }

    public List<Currency> getCurrencies(Place place) {
        List<Currency> currencies = new ArrayList<>();

        Cursor cursor = db.query(DbContract.CurrenciesPlaces.TABLE_NAME,
                null,
                "place_id = ?",
                new String[]{String.valueOf(place.getId())},
                null,
                null,
                null);

        try {
            while (cursor.moveToNext()) {
                long currencyId = cursor.getLong(cursor.getColumnIndex(DbContract.CurrenciesPlaces.CURRENCY_ID));
                currencies.add(getCurrency(currencyId));
            }
        } finally {
            cursor.close();
        }

        return currencies;
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
}