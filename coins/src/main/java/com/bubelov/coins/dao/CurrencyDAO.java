package com.bubelov.coins.dao;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;

import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 10/13/15 9:16 PM
 */

public class CurrencyDAO {
    private static String[] FULL_PROJECTION = new String[]{Database.Currencies._ID, Database.Currencies.NAME, Database.Currencies.CODE, Database.Currencies.CRYPTO, Database.Currencies._CREATED_AT, Database.Currencies._UPDATED_AT};

    public static Currency query(Context context, String code) {
        Cursor cursor = context.getContentResolver().query(Database.Currencies.CONTENT_URI,
                FULL_PROJECTION,
                String.format("%s = ?", Database.Currencies.CODE),
                new String[]{code},
                null);

        Currency currency = null;

        if (cursor.moveToNext()) {
            currency = getCurrency(cursor);
        }

        cursor.close();
        return currency;
    }

    public static List<Currency> query(Context context, Merchant merchant) {
        List<Currency> currencies = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(Database.Merchants.CONTENT_URI.buildUpon().appendPath(String.valueOf(merchant.getId())).appendPath("currencies").build(),
                FULL_PROJECTION,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            Currency currency = new Currency();
            currency.setName(cursor.getString(0));
            currencies.add(getCurrency(cursor));
        }

        cursor.close();
        return currencies;
    }

    public static void insert(Context context, List<Currency> currencies) throws Exception {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        for (Currency currency : currencies) {
            operations.add(ContentProviderOperation
                    .newInsert(Database.Currencies.CONTENT_URI)
                    .withValue(Database.Currencies._ID, currency.getId())
                    .withValue(Database.Currencies._CREATED_AT, currency.getCreatedAt().getMillis())
                    .withValue(Database.Currencies._UPDATED_AT, currency.getUpdatedAt().getMillis())
                    .withValue(Database.Currencies.NAME, currency.getName())
                    .withValue(Database.Currencies.CODE, currency.getCode())
                    .withValue(Database.Currencies.CRYPTO, currency.isCrypto())
                    .build());
        }

        context.getContentResolver().applyBatch(Database.AUTHORITY, operations);
    }

    private static Currency getCurrency(Cursor cursor) {
        Currency currency = new Currency();
        currency.setId(cursor.getLong(cursor.getColumnIndex(Database.Currencies._ID)));
        currency.setName(cursor.getString(cursor.getColumnIndex(Database.Currencies.NAME)));
        currency.setCode(cursor.getString(cursor.getColumnIndex(Database.Currencies.CODE)));
        currency.setCrypto(cursor.getInt(cursor.getColumnIndex(Database.Currencies.CRYPTO)) == 1);
        currency.setCreatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(Database.Currencies._CREATED_AT))));
        currency.setUpdatedAt(new DateTime(cursor.getLong(cursor.getColumnIndex(Database.Currencies._UPDATED_AT))));
        return currency;
    }
}
