package com.bubelov.coins.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;

import org.joda.time.DateTime;

/**
 * Author: Igor Bubelov
 * Date: 10/13/15 9:25 PM
 */

public class ExchangeRateDAO2 {
    public static ExchangeRate queryForLast(Currency sourceCurrency, Currency targetCurrency) {
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

    public static void insert(ExchangeRate exchangeRate) {
        ContentValues values = new ContentValues();

        values.put(DbContract.ExchangeRates.BASE_CURRENCY_ID, exchangeRate.getSourceCurrencyId());
        values.put(DbContract.ExchangeRates.CURRENCY_ID, exchangeRate.getTargetCurrencyId());
        values.put(DbContract.ExchangeRates.VALUE, exchangeRate.getValue());
        values.put(DbContract.ExchangeRates._CREATED_AT, exchangeRate.getCreatedAt().getMillis());
        values.put(DbContract.ExchangeRates._UPDATED_AT, exchangeRate.getUpdatedAt().getMillis());

        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        db.insert(DbContract.ExchangeRates.TABLE_NAME, null, values);
    }
}
