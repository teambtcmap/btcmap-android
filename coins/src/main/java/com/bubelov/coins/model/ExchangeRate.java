package com.bubelov.coins.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;

import org.joda.time.DateTime;

/**
 * Author: Igor Bubelov
 * Date: 10/4/15 8:29 PM
 */

public class ExchangeRate extends AbstractEntity {
    private long sourceCurrencyId;

    private long targetCurrencyId;

    private float value;

    public long getSourceCurrencyId() {
        return sourceCurrencyId;
    }

    public void setSourceCurrencyId(long sourceCurrencyId) {
        this.sourceCurrencyId = sourceCurrencyId;
    }

    public long getTargetCurrencyId() {
        return targetCurrencyId;
    }

    public void setTargetCurrencyId(long targetCurrencyId) {
        this.targetCurrencyId = targetCurrencyId;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    // Database stuff

    public static ExchangeRate last(Currency sourceCurrency, Currency targetCurrency) {
        if (sourceCurrency == null || targetCurrency == null) {
            return null;
        }

        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

        Cursor cursor = db.query(DbContract.ExchangeRates.TABLE_NAME,
                null,
                "base_currency_id = ? and currency_id = ?",
                new String[]{String.valueOf(sourceCurrency.getId()), String.valueOf(targetCurrency.getId())},
                null,
                null,
                "_updated_at desc");

        try {
            if (cursor.moveToNext()) {
                ExchangeRate rate = new ExchangeRate();
                rate.setId(cursor.getLong(cursor.getColumnIndex(DbContract.ExchangeRates._ID)));
                rate.setSourceCurrencyId(sourceCurrency.getId());
                rate.setTargetCurrencyId(targetCurrency.getId());
                rate.setValue(cursor.getFloat(cursor.getColumnIndex(DbContract.ExchangeRates.VALUE)));
                rate.setCreatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._CREATED_AT))));
                rate.setUpdatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._UPDATED_AT))));

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

        values.put(DbContract.ExchangeRates.BASE_CURRENCY_ID, getSourceCurrencyId());
        values.put(DbContract.ExchangeRates.CURRENCY_ID, getTargetCurrencyId());
        values.put(DbContract.ExchangeRates.VALUE, getValue());
        values.put(DbContract.ExchangeRates._CREATED_AT, getCreatedAt().getMillis());
        values.put(DbContract.ExchangeRates._UPDATED_AT, getUpdatedAt().getMillis());

        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        db.insert(DbContract.ExchangeRates.TABLE_NAME, null, values);
    }
}