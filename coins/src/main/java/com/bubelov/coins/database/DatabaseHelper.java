package com.bubelov.coins.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:18
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String BOOLEAN_TYPE = " BOOLEAN";

    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_MERCHANTS_TABLE =
            "CREATE TABLE " + Tables.Merchants.TABLE_NAME + " (" +
                    Tables.Merchants._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Tables.Merchants.LATITUDE + REAL_TYPE + COMMA_SEP +
                    Tables.Merchants.LONGITUDE + REAL_TYPE + COMMA_SEP +
                    Tables.Merchants.NAME + TEXT_TYPE + COMMA_SEP +
                    Tables.Merchants.DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                    Tables.Merchants.PHONE + TEXT_TYPE + COMMA_SEP +
                    Tables.Merchants.WEBSITE + TEXT_TYPE +
            " )";

    private static final String SQL_CREATE_CURRENCIES_TABLE =
            "CREATE TABLE " + Tables.Currencies.TABLE_NAME + " (" +
                    Tables.Currencies._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Tables.Currencies.NAME + TEXT_TYPE + COMMA_SEP +
                    Tables.Currencies.CODE + INTEGER_TYPE + COMMA_SEP +
                    Tables.Currencies.SHOW_ON_MAP + BOOLEAN_TYPE + " NOT NULL DEFAULT 1" +
                    " )";

    private static final String SQL_CREATE_MERCHANTS_TO_CURRENCIES_TABLE =
            "CREATE TABLE " + Tables.MerchantsToCurrencies.TABLE_NAME + " (" +
                    Tables.MerchantsToCurrencies._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Tables.MerchantsToCurrencies.MERCHANT_ID + TEXT_TYPE + COMMA_SEP +
                    Tables.MerchantsToCurrencies.CURRENCY_ID + INTEGER_TYPE +
                    " )";

    private static final String SQL_DELETE_MERCHANTS_TABLE = "DROP TABLE IF EXISTS " + Tables.Merchants.TABLE_NAME;
    private static final String SQL_DELETE_CURRENCIES_TABLE = "DROP TABLE IF EXISTS " + Tables.Currencies.TABLE_NAME;
    private static final String SQL_DELETE_MERCHANTS_TO_CURRENCIES_TABLE = "DROP TABLE IF EXISTS " + Tables.MerchantsToCurrencies.TABLE_NAME;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "coins.db";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MERCHANTS_TABLE);
        db.execSQL(SQL_CREATE_CURRENCIES_TABLE);
        db.execSQL(SQL_CREATE_MERCHANTS_TO_CURRENCIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_MERCHANTS_TABLE);
        db.execSQL(SQL_DELETE_CURRENCIES_TABLE);
        db.execSQL(SQL_DELETE_MERCHANTS_TO_CURRENCIES_TABLE);
        onCreate(db);
    }
}
