package com.bubelov.coins.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class Currency extends AbstractEntity implements Serializable {
    private String name;

    private String code;

    private boolean crypto;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isCrypto() {
        return crypto;
    }

    public void setCrypto(boolean crypto) {
        this.crypto = crypto;
    }

    // Database stuff

    public static long getCount() {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        return DatabaseUtils.queryNumEntries(db, DbContract.Currencies.TABLE_NAME);
    }

    public static Currency find(long id) {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        Cursor cursor = db.query(DbContract.Currencies.TABLE_NAME, null, "_id = ?", new String[]{String.valueOf(id)}, null, null, null);

        try {
            return fromCursor(cursor).get(0);
        } finally {
            cursor.close();
        }
    }

    public static List<Currency> findByPlace(Place place) {
        List<Currency> currencies = new ArrayList<>();
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        Cursor cursor = db.query(DbContract.CurrenciesPlaces.TABLE_NAME, null, "place_id = ?", new String[]{String.valueOf(place.getId())}, null, null, null);

        try {
            while (cursor.moveToNext()) {
                long currencyId = cursor.getLong(cursor.getColumnIndex(DbContract.CurrenciesPlaces.CURRENCY_ID));
                currencies.add(find(currencyId));
            }
        } catch (Exception e) {
            Timber.e(e, "Failed to fetch currencies");
        } finally {
            cursor.close();
        }

        return currencies;
    }

    public static void insert(List<Currency> currencies) throws Exception {
        for (Currency currency : currencies) {
            currency.insert();
        }
    }

    public boolean insert() {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        long rowId = db.insertWithOnConflict(DbContract.Currencies.TABLE_NAME, null, toValues(), SQLiteDatabase.CONFLICT_REPLACE);

        if (rowId == -1) {
            return false;
        } else {
            setId(rowId);
            return true;
        }
    }

    private ContentValues toValues() {
        ContentValues values = new ContentValues();

        if (getId() > 0) {
            values.put(DbContract.Currencies._ID, getId());
        }

        values.put(DbContract.Currencies._UPDATED_AT, updatedAt.getTime());
        values.put(DbContract.Currencies.NAME, getName());
        values.put(DbContract.Currencies.CODE, getCode());
        values.put(DbContract.Currencies.CRYPTO, isCrypto());

        return values;
    }

    private static List<Currency> fromCursor(@NonNull Cursor cursor) {
        List<Currency> currencies = new ArrayList<>();

        while (cursor.moveToNext()) {
            Currency currency = new Currency();
            currency.setId(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)));
            currency.setName(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.NAME)));
            currency.setCode(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.CODE)));
            currency.setCrypto(cursor.getInt(cursor.getColumnIndex(DbContract.Currencies.CRYPTO)) == 1);
            currency.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._UPDATED_AT))));
            currencies.add(currency);
        }

        return currencies;
    }
}