package com.bubelov.coins.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.service.sync.merchants.MerchantsSyncService;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:18
 */

public class DbHelper extends SQLiteOpenHelper {
    private static final String TEXT = " TEXT";
    private static final String REAL = " REAL";
    private static final String INTEGER = " INTEGER";
    private static final String BOOLEAN = " BOOLEAN";

    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_MERCHANTS_TABLE =
            "CREATE TABLE " + DbContract.Merchants.TABLE_NAME + " (" +
                    DbContract.Merchants._ID + INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    DbContract.Merchants._CREATED_AT + INTEGER + COMMA_SEP +
                    DbContract.Merchants._UPDATED_AT + INTEGER + COMMA_SEP +
                    DbContract.Merchants.LATITUDE + REAL + COMMA_SEP +
                    DbContract.Merchants.LONGITUDE + REAL + COMMA_SEP +
                    DbContract.Merchants.NAME + TEXT + COMMA_SEP +
                    DbContract.Merchants.DESCRIPTION + TEXT + COMMA_SEP +
                    DbContract.Merchants.PHONE + TEXT + COMMA_SEP +
                    DbContract.Merchants.WEBSITE + TEXT + COMMA_SEP +
                    DbContract.Merchants.AMENITY + TEXT + COMMA_SEP +
                    DbContract.Merchants.OPENING_HOURS + TEXT + COMMA_SEP +
                    DbContract.Merchants.ADDRESS + TEXT +
            " )";

    private static final String SQL_CREATE_CURRENCIES_TABLE =
            "CREATE TABLE " + DbContract.Currencies.TABLE_NAME + " (" +
                    DbContract.Currencies._ID + INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    DbContract.Currencies._CREATED_AT + INTEGER + COMMA_SEP +
                    DbContract.Currencies._UPDATED_AT + INTEGER + COMMA_SEP +
                    DbContract.Currencies.NAME + TEXT + COMMA_SEP +
                    DbContract.Currencies.CODE + INTEGER + COMMA_SEP +
                    DbContract.Currencies.CRYPTO + BOOLEAN + " NOT NULL" + COMMA_SEP +
                    DbContract.Currencies.SHOW_ON_MAP + BOOLEAN + " NOT NULL DEFAULT 1" +
                    " )";

    private static final String SQL_CREATE_MERCHANTS_TO_CURRENCIES_TABLE =
            "CREATE TABLE " + DbContract.CurrenciesMerchants.TABLE_NAME + " (" +
                    DbContract.CurrenciesMerchants._ID + INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    DbContract.CurrenciesMerchants.MERCHANT_ID + TEXT + COMMA_SEP +
                    DbContract.CurrenciesMerchants.CURRENCY_ID + INTEGER +
                    " )";

    private static final String SQL_CREATE_EXCHANGE_RATES_TABLE =
            "CREATE TABLE " + DbContract.ExchangeRates.TABLE_NAME + " (" +
                    DbContract.ExchangeRates._ID + INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    DbContract.ExchangeRates._CREATED_AT + INTEGER + COMMA_SEP +
                    DbContract.ExchangeRates._UPDATED_AT + INTEGER + COMMA_SEP +
                    DbContract.ExchangeRates.SOURCE_CURRENCY_ID + INTEGER + COMMA_SEP +
                    DbContract.ExchangeRates.TARGET_CURRENCY_ID + INTEGER + COMMA_SEP +
                    DbContract.ExchangeRates.VALUE + REAL +
                    " )";

    private static final String SQL_DELETE_MERCHANTS_TABLE = "DROP TABLE IF EXISTS " + DbContract.Merchants.TABLE_NAME;
    private static final String SQL_DELETE_CURRENCIES_TABLE = "DROP TABLE IF EXISTS " + DbContract.Currencies.TABLE_NAME;
    private static final String SQL_DELETE_CURRENCIES_TO_MERCHANTS_TABLE = "DROP TABLE IF EXISTS " + DbContract.CurrenciesMerchants.TABLE_NAME;
    private static final String SQL_DELETE_EXCHANGE_RATES_TABLE = "DROP TABLE IF EXISTS " + DbContract.ExchangeRates.TABLE_NAME;

    public DbHelper(Context context) {
        super(context,
                context.getResources().getString(R.string.database_name),
                null,
                context.getResources().getInteger(R.integer.database_version));
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MERCHANTS_TABLE);
        db.execSQL(SQL_CREATE_CURRENCIES_TABLE);
        db.execSQL(SQL_CREATE_MERCHANTS_TO_CURRENCIES_TABLE);
        db.execSQL(SQL_CREATE_EXCHANGE_RATES_TABLE);

        db.execSQL(String.format("CREATE INDEX index_merchant_id_position ON merchants (%s, %s, %s)", DbContract.Merchants._ID, DbContract.Merchants.LATITUDE, DbContract.Merchants.LONGITUDE));
        db.execSQL(String.format("CREATE INDEX index_currency_id ON currencies (%s)", DbContract.Currencies._ID));
        db.execSQL(String.format("CREATE INDEX index_currency_merchant_id ON currencies_merchants (%s)", DbContract.CurrenciesMerchants._ID));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_MERCHANTS_TABLE);
        db.execSQL(SQL_DELETE_CURRENCIES_TABLE);
        db.execSQL(SQL_DELETE_CURRENCIES_TO_MERCHANTS_TABLE);
        db.execSQL(SQL_DELETE_EXCHANGE_RATES_TABLE);
        onCreate(db);

        Context context = App.getInstance();
        context.startService(MerchantsSyncService.makeIntent(context, true));
    }
}
