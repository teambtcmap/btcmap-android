package com.bubelov.coins.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;

import com.bubelov.coins.database.DatabaseHelper;
import com.bubelov.coins.database.Database;

/**
 * Author: Igor Bubelov
 * Date: 12/05/15 20:31
 */

public class CoinsProvider extends ContentProvider {
    private DatabaseHelper db;

    private static final int MERCHANTS = 100;
    private static final int MERCHANT_ID = 101;
    private static final int CURRENCIES_FOR_MERCHANT = 102;

    private static final int CURRENCIES = 200;
    private static final int CURRENCY_ID = 201;

    private static final int CURRENCIES_MERCHANTS = 300;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(Database.AUTHORITY, Database.Merchants.TABLE_NAME, MERCHANTS);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Merchants.TABLE_NAME + "/#", MERCHANT_ID);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Merchants.TABLE_NAME + "/#/currencies", CURRENCIES_FOR_MERCHANT);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Currencies.TABLE_NAME, CURRENCIES);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Currencies.TABLE_NAME + "/*", CURRENCY_ID);
        sURIMatcher.addURI(Database.AUTHORITY, Database.CurrenciesMerchants.TABLE_NAME, CURRENCIES_MERCHANTS);
    }

    @Override
    public boolean onCreate() {
        db = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uri.getPathSegments().contains(SearchManager.SUGGEST_URI_PATH_QUERY)) {
            MatrixCursor resultCursor = new MatrixCursor(new String[]{BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID});

            if (uri.getLastPathSegment().equals(SearchManager.SUGGEST_URI_PATH_QUERY)) {
                return resultCursor;
            }

            Cursor merchantsCursor = db.getReadableDatabase().query(Database.Merchants.TABLE_NAME,
                    new String[]{Database.Merchants._ID, Database.Merchants.NAME},
                    String.format("%s like ? or %s like ?", Database.Merchants.NAME, Database.Merchants.AMENITY),
                    new String[]{"%" + uri.getLastPathSegment() + "%", "%" + uri.getLastPathSegment() + "%"},
                    null,
                    null,
                    null,
                    null);

            int i = 1;

            while (merchantsCursor.moveToNext()) {
                resultCursor.addRow(new Object[]{i++, merchantsCursor.getString(1), merchantsCursor.getLong(0)});
            }

            merchantsCursor.close();

            return resultCursor;
        }

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
                    Database.Currencies.TABLE_NAME,
                    Database.CurrenciesMerchants.TABLE_NAME,
                    Database.CurrenciesMerchants.CURRENCY_ID,
                    Database.CurrenciesMerchants.MERCHANT_ID);

            return db.getReadableDatabase().rawQuery(query, new String[]{String.valueOf(merchantId)});
        }

        return db.getReadableDatabase().query(getTableName(uri),
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
            values.put(Database.CurrenciesMerchants.MERCHANT_ID, merchantId);
            values.put(Database.CurrenciesMerchants.CURRENCY_ID, currencyId);

            db.getWritableDatabase().insertWithOnConflict(Database.CurrenciesMerchants.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return Uri.parse(Database.Currencies.CONTENT_URI + "/" + currencyId);
        }

        String tableName = getTableName(uri);
        long id = db.getWritableDatabase().insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(String.format("%s/%s/%s", Database.BASE_CONTENT_URI, tableName, id));
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase database = db.getWritableDatabase();

        switch (sURIMatcher.match(uri)) {
            case MERCHANTS:
                database.beginTransaction();

                try {
                    String insertQuery = String.format("insert or replace into %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Database.Merchants.TABLE_NAME,
                            Database.Merchants._ID,
                            Database.Merchants._CREATED_AT,
                            Database.Merchants._UPDATED_AT,
                            Database.Merchants.LATITUDE,
                            Database.Merchants.LONGITUDE,
                            Database.Merchants.NAME,
                            Database.Merchants.DESCRIPTION,
                            Database.Merchants.PHONE,
                            Database.Merchants.WEBSITE,
                            Database.Merchants.AMENITY,
                            Database.Merchants.OPENING_HOURS,
                            Database.Merchants.ADDRESS);

                    SQLiteStatement insertStatement = database.compileStatement(insertQuery);

                    for (ContentValues value : values) {
                        insertStatement.bindLong(1, value.getAsLong(Database.Merchants._ID));
                        insertStatement.bindLong(2, value.getAsLong(Database.Merchants._CREATED_AT));
                        insertStatement.bindLong(3, value.getAsLong(Database.Merchants._UPDATED_AT));
                        insertStatement.bindDouble(4, value.getAsDouble(Database.Merchants.LATITUDE));
                        insertStatement.bindDouble(5, value.getAsDouble(Database.Merchants.LONGITUDE));
                        insertStatement.bindString(6, getString(value, Database.Merchants.NAME));
                        insertStatement.bindString(7, getString(value, Database.Merchants.DESCRIPTION));
                        insertStatement.bindString(8, getString(value, Database.Merchants.PHONE));
                        insertStatement.bindString(9, getString(value, Database.Merchants.WEBSITE));
                        insertStatement.bindString(10, getString(value, Database.Merchants.AMENITY));
                        insertStatement.bindString(11, getString(value, Database.Merchants.OPENING_HOURS));
                        insertStatement.bindString(12, getString(value, Database.Merchants.ADDRESS));
                        insertStatement.execute();
                    }

                    database.setTransactionSuccessful();
                    return values.length;
                } finally {
                    database.endTransaction();
                }

            case CURRENCIES_MERCHANTS:
                database.beginTransaction();

                try {
                    String insertQuery = String.format("insert or replace into %s (%s, %s) values (?, ?)", Database.CurrenciesMerchants.TABLE_NAME, Database.CurrenciesMerchants.CURRENCY_ID, Database.CurrenciesMerchants.MERCHANT_ID);
                    SQLiteStatement insertStatement = database.compileStatement(insertQuery);

                    for (ContentValues value : values) {
                        insertStatement.bindLong(1, value.getAsLong(Database.CurrenciesMerchants.CURRENCY_ID));
                        insertStatement.bindLong(2, value.getAsLong(Database.CurrenciesMerchants.MERCHANT_ID));
                        insertStatement.execute();
                    }

                    database.setTransactionSuccessful();
                    return values.length;
                } finally {
                    database.endTransaction();
                }

            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return db.getWritableDatabase().delete(getTableName(uri), selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (sURIMatcher.match(uri) == CURRENCY_ID) {
            selection = String.format("%s = ?", Database.Currencies._ID);
            selectionArgs = new String[]{uri.getLastPathSegment()};
        }

        if (sURIMatcher.match(uri) == MERCHANT_ID) {
            selection = String.format("%s = ?", Database.Merchants._ID);
            selectionArgs = new String[]{uri.getLastPathSegment()};
        }

        int result = db.getWritableDatabase().update(getTableName(uri), values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    private String getTableName(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case MERCHANTS:
            case MERCHANT_ID:
                return Database.Merchants.TABLE_NAME;
            case CURRENCIES:
            case CURRENCY_ID:
                return Database.Currencies.TABLE_NAME;
            case CURRENCIES_MERCHANTS:
                return Database.CurrenciesMerchants.TABLE_NAME;
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