package com.bubelov.coins.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.bubelov.coins.database.Database;
import com.bubelov.coins.database.DatabaseHelper;

/**
 * Author: Igor Bubelov
 * Date: 16/05/15 02:15
 */

public class MerchantsProvider extends ContentProvider {
    private DatabaseHelper db;

    private static final int MERCHANTS = 100;
    private static final int MERCHANT_ID = 101;

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(Database.AUTHORITY, Database.Merchants.TABLE_NAME , MERCHANTS);
        sURIMatcher.addURI(Database.AUTHORITY, Database.Merchants.TABLE_NAME + "/*" , MERCHANT_ID);
    }

    @Override
    public boolean onCreate() {
        db = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return db.getReadableDatabase().query(Database.Merchants.TABLE_NAME,
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
        long id = db.getWritableDatabase().insertWithOnConflict(Database.Merchants.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(Database.Merchants.CONTENT_URI + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return db.getWritableDatabase().delete(Database.Merchants.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (sURIMatcher.match(uri) == MERCHANT_ID) {
            selection = String.format("%s = ?", Database.Merchants._ID);
            selectionArgs = new String[] { uri.getLastPathSegment() };
        }

        int result = db.getWritableDatabase().update(Database.Merchants.TABLE_NAME, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }
}
