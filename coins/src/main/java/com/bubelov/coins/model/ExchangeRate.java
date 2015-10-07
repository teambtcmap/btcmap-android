package com.bubelov.coins.model;

import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.Database;

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
}