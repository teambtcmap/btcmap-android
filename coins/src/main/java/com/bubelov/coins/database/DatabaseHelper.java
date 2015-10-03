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

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TEXT = " TEXT";
    private static final String REAL = " REAL";
    private static final String INTEGER = " INTEGER";
    private static final String BOOLEAN = " BOOLEAN";

    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_MERCHANTS_TABLE =
            "CREATE TABLE " + Database.Merchants.TABLE_NAME + " (" +
                    Database.Merchants._ID + INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    Database.Merchants._CREATED_AT + INTEGER + COMMA_SEP +
                    Database.Merchants._UPDATED_AT + INTEGER + COMMA_SEP +
                    Database.Merchants.LATITUDE + REAL + COMMA_SEP +
                    Database.Merchants.LONGITUDE + REAL + COMMA_SEP +
                    Database.Merchants.NAME + TEXT + COMMA_SEP +
                    Database.Merchants.DESCRIPTION + TEXT + COMMA_SEP +
                    Database.Merchants.PHONE + TEXT + COMMA_SEP +
                    Database.Merchants.WEBSITE + TEXT + COMMA_SEP +
                    Database.Merchants.AMENITY + TEXT + COMMA_SEP +
                    Database.Merchants.OPENING_HOURS + TEXT + COMMA_SEP +
                    Database.Merchants.ADDRESS + TEXT +
            " )";

    private static final String SQL_CREATE_CURRENCIES_TABLE =
            "CREATE TABLE " + Database.Currencies.TABLE_NAME + " (" +
                    Database.Currencies._ID + INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    Database.Currencies._CREATED_AT + INTEGER + COMMA_SEP +
                    Database.Currencies._UPDATED_AT + INTEGER + COMMA_SEP +
                    Database.Currencies.NAME + TEXT + COMMA_SEP +
                    Database.Currencies.CODE + INTEGER + COMMA_SEP +
                    Database.Currencies.CRYPTO + BOOLEAN + " NOT NULL" + COMMA_SEP +
                    Database.Currencies.SHOW_ON_MAP + BOOLEAN + " NOT NULL DEFAULT 1" +
                    " )";

    private static final String SQL_CREATE_MERCHANTS_TO_CURRENCIES_TABLE =
            "CREATE TABLE " + Database.CurrenciesMerchants.TABLE_NAME + " (" +
                    Database.CurrenciesMerchants._ID + INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    Database.CurrenciesMerchants.MERCHANT_ID + TEXT + COMMA_SEP +
                    Database.CurrenciesMerchants.CURRENCY_ID + INTEGER +
                    " )";

    private static final String SQL_CREATE_EXCHANGE_RATES_TABLE =
            "CREATE TABLE " + Database.ExchangeRates.TABLE_NAME + " (" +
                    Database.ExchangeRates._ID + INTEGER + " PRIMARY KEY" + COMMA_SEP +
                    Database.ExchangeRates._CREATED_AT + INTEGER + COMMA_SEP +
                    Database.ExchangeRates._UPDATED_AT + INTEGER + COMMA_SEP +
                    Database.ExchangeRates.SOURCE_CURRENCY_ID + INTEGER + COMMA_SEP +
                    Database.ExchangeRates.TARGET_CURRENCY_ID + INTEGER + COMMA_SEP +
                    Database.ExchangeRates.VALUE + REAL +
                    " )";

    private static final String SQL_DELETE_MERCHANTS_TABLE = "DROP TABLE IF EXISTS " + Database.Merchants.TABLE_NAME;
    private static final String SQL_DELETE_CURRENCIES_TABLE = "DROP TABLE IF EXISTS " + Database.Currencies.TABLE_NAME;
    private static final String SQL_DELETE_CURRENCIES_TO_MERCHANTS_TABLE = "DROP TABLE IF EXISTS " + Database.CurrenciesMerchants.TABLE_NAME;
    private static final String SQL_DELETE_EXCHANGE_RATES_TABLE = "DROP TABLE IF EXISTS " + Database.ExchangeRates.TABLE_NAME;

    public DatabaseHelper(Context context) {
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

        db.execSQL(String.format("CREATE INDEX index_merchant_id_position ON merchants (%s, %s, %s)", Database.Merchants._ID, Database.Merchants.LATITUDE, Database.Merchants.LONGITUDE));
        db.execSQL(String.format("CREATE INDEX index_currency_id ON currencies (%s)", Database.Currencies._ID));
        db.execSQL(String.format("CREATE INDEX index_currency_merchant_id ON currencies_merchants (%s)", Database.CurrenciesMerchants._ID));
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
