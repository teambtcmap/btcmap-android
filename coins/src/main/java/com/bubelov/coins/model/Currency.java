package com.bubelov.coins.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Author: Igor Bubelov
 * Date: 03/07/14 22:54
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

    public static Currency findByCode(String code) {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        Cursor cursor = db.query(DbContract.Currencies.TABLE_NAME, null, "code = ?", new String[]{code}, null, null, null);

        try {
            return fromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    public static List<Currency> findByMerchant(Merchant merchant) {
        List<Currency> currencies = new ArrayList<>();
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        Cursor cursor = db.query(DbContract.CurrenciesMerchants.TABLE_NAME, null, "merchant_id = ?", new String[]{String.valueOf(merchant.getId())}, null, null, null);

        try {
            while (cursor.moveToNext()) {
                currencies.add(fromCursor(cursor));
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

        values.put(DbContract.Currencies._UPDATED_AT, getUpdatedAt().getMillis());
        values.put(DbContract.Currencies.NAME, getName());
        values.put(DbContract.Currencies.CODE, getCode());
        values.put(DbContract.Currencies.CRYPTO, isCrypto());

        return values;
    }

    private static Currency fromCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        Currency currency = new Currency();
        currency.setId(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)));
        currency.setName(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.NAME)));
        currency.setCode(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.CODE)));
        currency.setCrypto(cursor.getInt(cursor.getColumnIndex(DbContract.Currencies.CRYPTO)) == 1);
        currency.setCreatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._CREATED_AT))));
        currency.setUpdatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._UPDATED_AT))));
        return currency;
    }
}