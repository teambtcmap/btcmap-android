package com.bubelov.coins.dao;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
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
    public static Currency query(String code) {
        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();

        Cursor cursor = db.query(DbContract.Currencies.TABLE_NAME, null, "code = ?", new String[]{code}, null, null, null);

        if (cursor.moveToNext()) {
            Currency currency = getCurrency(cursor);
            cursor.close();
            return currency;
        } else {
            cursor.close();
            return null;
        }
    }

    public static List<Currency> query(Context context, Merchant merchant) {
        List<Currency> currencies = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(DbContract.Merchants.CONTENT_URI.buildUpon().appendPath(String.valueOf(merchant.getId())).appendPath("currencies").build(),
                null,
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
                    .newInsert(DbContract.Currencies.CONTENT_URI)
                    .withValue(DbContract.Currencies._ID, currency.getId())
                    .withValue(DbContract.Currencies._CREATED_AT, currency.getCreatedAt().getMillis())
                    .withValue(DbContract.Currencies._UPDATED_AT, currency.getUpdatedAt().getMillis())
                    .withValue(DbContract.Currencies.NAME, currency.getName())
                    .withValue(DbContract.Currencies.CODE, currency.getCode())
                    .withValue(DbContract.Currencies.CRYPTO, currency.isCrypto())
                    .build());
        }

        context.getContentResolver().applyBatch(DbContract.AUTHORITY, operations);
    }

    private static Currency getCurrency(Cursor cursor) {
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
