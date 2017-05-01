package com.bubelov.coins.data.repository.currency;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.domain.Currency;
import com.bubelov.coins.data.database.DbContract;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Igor Bubelov
 */

@Singleton
public class CurrenciesDataSourceDisk {
    private SQLiteDatabase db;

    @Inject
    CurrenciesDataSourceDisk(SQLiteDatabase db) {
        this.db = db;
    }

    public Currency getCurrency(String code) {
        try (Cursor cursor = db.query(DbContract.Currencies.TABLE_NAME,
                null,
                "code = ?",
                new String[]{code},
                null,
                null,
                null,
                null)) {
            return cursor.getCount() == 1 ? fromCursor(cursor) : null;
        }
    }

    void insert(Collection<Currency> currencies) {
        db.beginTransaction();

        try {
            for (Currency currency : currencies) {
                db.insertWithOnConflict(DbContract.Currencies.TABLE_NAME,
                        null,
                        toContentValues(currency),
                        SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private Currency fromCursor(Cursor cursor) {
        return Currency.builder()
                .id(cursor.getLong(cursor.getColumnIndex(DbContract.Currencies._ID)))
                .name(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.NAME)))
                .code(cursor.getString(cursor.getColumnIndex(DbContract.Currencies.CODE)))
                .crypto(cursor.getInt(cursor.getColumnIndex(DbContract.Currencies.CRYPTO)) == 1)
                .build();
    }

    private ContentValues toContentValues(Currency currency) {
        ContentValues values = new ContentValues();
        values.put(DbContract.Currencies._ID, currency.id());
        values.put(DbContract.Currencies.NAME, currency.name());
        values.put(DbContract.Currencies.CODE, currency.code());
        values.put(DbContract.Currencies.CRYPTO, currency.crypto());
        return values;
    }
}