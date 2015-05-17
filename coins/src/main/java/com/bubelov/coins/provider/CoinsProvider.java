package com.bubelov.coins.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
    private static final int MERCHANT_CURRENCIES = 102;

    private static final int CURRENCIES = 200;
    private static final int CURRENCY_ID = 201;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(Database.AUTHORITY, Database.Merchants.TABLE_NAME, MERCHANTS);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Merchants.TABLE_NAME + "/#", MERCHANT_ID);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Merchants.TABLE_NAME + "/#/currencies", MERCHANT_CURRENCIES);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Currencies.TABLE_NAME, CURRENCIES);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Currencies.TABLE_NAME + "/*", CURRENCY_ID);
    }

    @Override
    public boolean onCreate() {
        db = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (sURIMatcher.match(uri) == MERCHANT_CURRENCIES) {
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

            return db.getReadableDatabase().rawQuery("select " + selectionBuilder.toString() + " from currencies c join currencies_merchants cm on cm.currency_id = c._id and cm.merchant_id = " + merchantId, null);
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
        if (sURIMatcher.match(uri) == MERCHANT_CURRENCIES) {
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
            default:
                throw new IllegalArgumentException("Unsupported URI");
        }
    }
}