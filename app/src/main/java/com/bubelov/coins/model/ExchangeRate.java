package com.bubelov.coins.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;

import java.util.Date;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 8:29 PM
 */

public class ExchangeRate extends AbstractEntity {
    private String currency;

    private String baseCurrency;

    private double value;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    // Database stuff

    public static ExchangeRate last(String currency, String baseCurrency) {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

        Cursor cursor = db.query(DbContract.ExchangeRates.TABLE_NAME,
                null,
                "currency = ? and base_currency = ?",
                new String[]{currency, baseCurrency},
                null,
                null,
                "_updated_at desc");

        try {
            if (cursor.moveToNext()) {
                ExchangeRate rate = new ExchangeRate();
                rate.setId(cursor.getLong(cursor.getColumnIndex(DbContract.ExchangeRates._ID)));
                rate.setCurrency(currency);
                rate.setBaseCurrency(baseCurrency);
                rate.setValue(cursor.getFloat(cursor.getColumnIndex(DbContract.ExchangeRates.VALUE)));
                rate.setUpdatedAt(new Date(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._UPDATED_AT))));

                return rate;
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    public void create() {
        ContentValues values = new ContentValues();

        values.put(DbContract.ExchangeRates.CURRENCY, currency);
        values.put(DbContract.ExchangeRates.BASE_CURRENCY, baseCurrency);
        values.put(DbContract.ExchangeRates.VALUE, value);
        values.put(DbContract.ExchangeRates._UPDATED_AT, updatedAt.getTime());

        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        db.insert(DbContract.ExchangeRates.TABLE_NAME, null, values);
    }
}