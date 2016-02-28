package com.bubelov.coins.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;

import com.bubelov.coins.database.Database;
import com.bubelov.coins.database.DbContract;

/**
 * Author: Igor Bubelov
 * Date: 12/05/15 20:31
 */

public class CoinsProvider extends ContentProvider {
    private SQLiteDatabase db;

    private static final int MERCHANTS = 100;
    private static final int MERCHANT_ID = 101;
    private static final int CURRENCIES_FOR_MERCHANT = 102;

    private static final int CURRENCIES = 200;
    private static final int CURRENCY_ID = 201;

    private static final int CURRENCIES_MERCHANTS = 300;

    private static final int EXCHANGE_RATES = 400;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(DbContract.AUTHORITY, DbContract.Merchants.TABLE_NAME, MERCHANTS);
        sURIMatcher.addURI(DbContract.AUTHORITY, DbContract.Merchants.TABLE_NAME + "/#", MERCHANT_ID);
        sURIMatcher.addURI(DbContract.AUTHORITY, DbContract.Merchants.TABLE_NAME + "/#/currencies", CURRENCIES_FOR_MERCHANT);
        sURIMatcher.addURI(DbContract.AUTHORITY, DbContract.Currencies.TABLE_NAME, CURRENCIES);
        sURIMatcher.addURI(DbContract.AUTHORITY, DbContract.Currencies.TABLE_NAME + "/*", CURRENCY_ID);
        sURIMatcher.addURI(DbContract.AUTHORITY, DbContract.CurrenciesMerchants.TABLE_NAME, CURRENCIES_MERCHANTS);
        sURIMatcher.addURI(DbContract.AUTHORITY, DbContract.ExchangeRates.TABLE_NAME, EXCHANGE_RATES);
    }

    @Override
    public boolean onCreate() {
        db = Database.get(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (sURIMatcher.match(uri) == CURRENCIES_FOR_MERCHANT) {
            long merchantId = Long.valueOf(uri.getPathSegments().get(uri.getPathSegments().size() - 2));

            StringBuilder selectionBuilder = new StringBuilder();

            if (projection == null || projection.length == 0) {
                selectionBuilder.append("*");
            } else {
                for (int i = 0; i < projection.length; i++) {
                    selectionBuilder.append("c." + projection[i]);

                    if (i < projection.length - 1) {
                        selectionBuilder.append(", ");
                    }
                }
            }

            String query = String.format("select distinct %s from %s c join %s cm on cm.%s = c._id and cm.%s = ?",
                    selectionBuilder.toString(),
                    DbContract.Currencies.TABLE_NAME,
                    DbContract.CurrenciesMerchants.TABLE_NAME,
                    DbContract.CurrenciesMerchants.CURRENCY_ID,
                    DbContract.CurrenciesMerchants.MERCHANT_ID);

            return db.rawQuery(query, new String[]{String.valueOf(merchantId)});
        }

        return db.query(getTableName(uri),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (sURIMatcher.match(uri) == CURRENCIES_FOR_MERCHANT) {
            long merchantId = Long.valueOf(uri.getPathSegments().get(uri.getPathSegments().size() - 2));
            long currencyId = values.getAsLong(BaseColumns._ID);

            values.clear();
            values.put(DbContract.CurrenciesMerchants.MERCHANT_ID, merchantId);
            values.put(DbContract.CurrenciesMerchants.CURRENCY_ID, currencyId);

            db.insertWithOnConflict(DbContract.CurrenciesMerchants.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return Uri.parse(DbContract.Currencies.CONTENT_URI + "/" + currencyId);
        }

        String tableName = getTableName(uri);
        long id = db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(String.format("%s/%s/%s", DbContract.BASE_CONTENT_URI, tableName, id));
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        switch (sURIMatcher.match(uri)) {
            case MERCHANTS:
                db.beginTransaction();

                try {
                    String insertQuery = String.format("insert or replace into %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            DbContract.Merchants.TABLE_NAME,
                            DbContract.Merchants._ID,
                            DbContract.Merchants._CREATED_AT,
                            DbContract.Merchants._UPDATED_AT,
                            DbContract.Merchants.LATITUDE,
                            DbContract.Merchants.LONGITUDE,
                            DbContract.Merchants.NAME,
                            DbContract.Merchants.DESCRIPTION,
                            DbContract.Merchants.PHONE,
                            DbContract.Merchants.WEBSITE,
                            DbContract.Merchants.AMENITY,
                            DbContract.Merchants.OPENING_HOURS,
                            DbContract.Merchants.ADDRESS);

                    SQLiteStatement insertStatement = db.compileStatement(insertQuery);

                    for (ContentValues value : values) {
                        insertStatement.bindLong(1, value.getAsLong(DbContract.Merchants._ID));
                        insertStatement.bindLong(2, value.getAsLong(DbContract.Merchants._CREATED_AT));
                        insertStatement.bindLong(3, value.getAsLong(DbContract.Merchants._UPDATED_AT));
                        insertStatement.bindDouble(4, value.getAsDouble(DbContract.Merchants.LATITUDE));
                        insertStatement.bindDouble(5, value.getAsDouble(DbContract.Merchants.LONGITUDE));
                        insertStatement.bindString(6, getString(value, DbContract.Merchants.NAME));
                        insertStatement.bindString(7, getString(value, DbContract.Merchants.DESCRIPTION));
                        insertStatement.bindString(8, getString(value, DbContract.Merchants.PHONE));
                        insertStatement.bindString(9, getString(value, DbContract.Merchants.WEBSITE));
                        insertStatement.bindString(10, getString(value, DbContract.Merchants.AMENITY));
                        insertStatement.bindString(11, getString(value, DbContract.Merchants.OPENING_HOURS));
                        insertStatement.bindString(12, getString(value, DbContract.Merchants.ADDRESS));
                        insertStatement.execute();
                    }

                    db.setTransactionSuccessful();
                    return values.length;
                } finally {
                    db.endTransaction();
                }

            case CURRENCIES_MERCHANTS:
                db.beginTransaction();

                try {
                    String insertQuery = String.format("insert or replace into %s (%s, %s) values (?, ?)", DbContract.CurrenciesMerchants.TABLE_NAME, DbContract.CurrenciesMerchants.CURRENCY_ID, DbContract.CurrenciesMerchants.MERCHANT_ID);
                    SQLiteStatement insertStatement = db.compileStatement(insertQuery);

                    for (ContentValues value : values) {
                        insertStatement.bindLong(1, value.getAsLong(DbContract.CurrenciesMerchants.CURRENCY_ID));
                        insertStatement.bindLong(2, value.getAsLong(DbContract.CurrenciesMerchants.MERCHANT_ID));
                        insertStatement.execute();
                    }

                    db.setTransactionSuccessful();
                    return values.length;
                } finally {
                    db.endTransaction();
                }

            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return db.delete(getTableName(uri), selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (sURIMatcher.match(uri) == CURRENCY_ID) {
            selection = String.format("%s = ?", DbContract.Currencies._ID);
            selectionArgs = new String[]{uri.getLastPathSegment()};
        }

        if (sURIMatcher.match(uri) == MERCHANT_ID) {
            selection = String.format("%s = ?", DbContract.Merchants._ID);
            selectionArgs = new String[]{uri.getLastPathSegment()};
        }

        int result = db.update(getTableName(uri), values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    private String getTableName(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case MERCHANTS:
            case MERCHANT_ID:
                return DbContract.Merchants.TABLE_NAME;
            case CURRENCIES:
            case CURRENCY_ID:
                return DbContract.Currencies.TABLE_NAME;
            case CURRENCIES_MERCHANTS:
                return DbContract.CurrenciesMerchants.TABLE_NAME;
            case EXCHANGE_RATES:
                return DbContract.ExchangeRates.TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unsupported URI");
        }
    }

    private String getString(ContentValues contentValues, String key) {
        String string = contentValues.getAsString(key);

        if (string == null) {
            return "";
        } else {
            return string;
        }
    }
}