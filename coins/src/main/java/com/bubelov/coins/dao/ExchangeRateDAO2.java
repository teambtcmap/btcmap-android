package com.bubelov.coins.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.ExchangeRate;

import org.joda.time.DateTime;

/**
 * Author: Igor Bubelov
 * Date: 10/13/15 9:25 PM
 */

public class ExchangeRateDAO2 {
    public static ExchangeRate queryForLast(Context context, Currency sourceCurrency, Currency targetCurrency) {
        if (sourceCurrency == null || targetCurrency == null) {
            return null;
        }

        Cursor cursor = context.getContentResolver().query(DbContract.ExchangeRates.CONTENT_URI,
                new String[]{DbContract.ExchangeRates._ID, DbContract.ExchangeRates.VALUE, DbContract.ExchangeRates._CREATED_AT, DbContract.ExchangeRates._UPDATED_AT},
                String.format("%s = ? and %s = ?", DbContract.ExchangeRates.SOURCE_CURRENCY_ID, DbContract.ExchangeRates.TARGET_CURRENCY_ID),
                new String[]{String.valueOf(sourceCurrency.getId()), String.valueOf(targetCurrency.getId())},
                DbContract.ExchangeRates._UPDATED_AT + " DESC");

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

    public static void insert(Context context, ExchangeRate exchangeRate) {
        ContentValues values = new ContentValues();
        values.put(DbContract.ExchangeRates.SOURCE_CURRENCY_ID, exchangeRate.getSourceCurrencyId());
        values.put(DbContract.ExchangeRates.TARGET_CURRENCY_ID, exchangeRate.getTargetCurrencyId());
        values.put(DbContract.ExchangeRates.VALUE, exchangeRate.getValue());
        values.put(DbContract.ExchangeRates._CREATED_AT, exchangeRate.getCreatedAt().getMillis());
        values.put(DbContract.ExchangeRates._UPDATED_AT, exchangeRate.getUpdatedAt().getMillis());

        context.getContentResolver().insert(DbContract.ExchangeRates.CONTENT_URI, values);
    }
}
