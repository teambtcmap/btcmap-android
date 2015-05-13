package com.bubelov.coins.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.bubelov.coins.database.DatabaseHelper;
import com.bubelov.coins.database.Database;

/**
 * Author: Igor Bubelov
 * Date: 12/05/15 20:31
 */

public class CurrenciesProvider extends ContentProvider {
    private DatabaseHelper db;

    private static final int CURRENCIES = 100;
    private static final int CURRENCY_ID = 101;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(Database.AUTHORITY, Database.Currencies.TABLE_NAME , CURRENCIES);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Currencies.TABLE_NAME + "/*" , CURRENCY_ID);
    }

    @Override
    public boolean onCreate() {
        db = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return db.getReadableDatabase().query(Database.Currencies.TABLE_NAME,
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
        long id = db.getWritableDatabase().insertWithOnConflict(Database.Currencies.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(Database.Currencies.CONTENT_URI + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return db.getWritableDatabase().delete(Database.Currencies.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (sURIMatcher.match(uri) == CURRENCY_ID) {
            selection = String.format("%s = ?", Database.Currencies._ID);
            selectionArgs = new String[] { uri.getLastPathSegment() };
        }

        int result = db.getWritableDatabase().update(Database.Currencies.TABLE_NAME, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }
}