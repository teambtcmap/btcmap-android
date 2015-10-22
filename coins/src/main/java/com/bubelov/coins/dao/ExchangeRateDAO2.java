package com.bubelov.coins.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.Database;
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

        Cursor cursor = context.getContentResolver().query(Database.ExchangeRates.CONTENT_URI,
                new String[]{Database.ExchangeRates._ID, Database.ExchangeRates.VALUE, Database.ExchangeRates._CREATED_AT, Database.ExchangeRates._UPDATED_AT},
                String.format("%s = ? and %s = ?", Database.ExchangeRates.SOURCE_CURRENCY_ID, Database.ExchangeRates.TARGET_CURRENCY_ID),
                new String[]{String.valueOf(sourceCurrency.getId()), String.valueOf(targetCurrency.getId())},
                Database.ExchangeRates._UPDATED_AT + " DESC");

        try {
            if (cursor.moveToNext()) {
                ExchangeRate rate = new ExchangeRate();
                rate.setId(cursor.getLong(cursor.getColumnIndex(Database.ExchangeRates._ID)));
                rate.setSourceCurrencyId(sourceCurrency.getId());
                rate.setTargetCurrencyId(targetCurrency.getId());
                rate.setValue(cursor.getFloat(cursor.getColumnIndex(Database.ExchangeRates.VALUE)));
                rate.setCreatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(Database.Currencies._CREATED_AT))));
                rate.setUpdatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(Database.Currencies._UPDATED_AT))));

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
        values.put(Database.ExchangeRates.SOURCE_CURRENCY_ID, exchangeRate.getSourceCurrencyId());
        values.put(Database.ExchangeRates.TARGET_CURRENCY_ID, exchangeRate.getTargetCurrencyId());
        values.put(Database.ExchangeRates.VALUE, exchangeRate.getValue());
        values.put(Database.ExchangeRates._CREATED_AT, exchangeRate.getCreatedAt().getMillis());
        values.put(Database.ExchangeRates._UPDATED_AT, exchangeRate.getUpdatedAt().getMillis());

        context.getContentResolver().insert(Database.ExchangeRates.CONTENT_URI, values);
    }
}
