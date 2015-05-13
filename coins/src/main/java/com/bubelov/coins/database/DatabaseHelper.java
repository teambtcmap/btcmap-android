package com.bubelov.coins.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bubelov.coins.App;
import com.bubelov.coins.R;
import com.bubelov.coins.receiver.SyncMerchantsWakefulReceiver;
import com.bubelov.coins.service.MerchantsSyncService;

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
            "CREATE TABLE " + Database.Merchants.TABLE_NAME + " (" +
                    Database.Merchants._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Database.Merchants._CREATED_AT + INTEGER_TYPE + COMMA_SEP +
                    Database.Merchants._UPDATED_AT + INTEGER_TYPE + COMMA_SEP +
                    Database.Merchants.LATITUDE + REAL_TYPE + COMMA_SEP +
                    Database.Merchants.LONGITUDE + REAL_TYPE + COMMA_SEP +
                    Database.Merchants.NAME + TEXT_TYPE + COMMA_SEP +
                    Database.Merchants.DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                    Database.Merchants.PHONE + TEXT_TYPE + COMMA_SEP +
                    Database.Merchants.WEBSITE + TEXT_TYPE + COMMA_SEP +
                    Database.Merchants.AMENITY + TEXT_TYPE +
            " )";

    private static final String SQL_CREATE_CURRENCIES_TABLE =
            "CREATE TABLE " + Database.Currencies.TABLE_NAME + " (" +
                    Database.Currencies._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Database.Currencies._CREATED_AT + INTEGER_TYPE + COMMA_SEP +
                    Database.Currencies._UPDATED_AT + INTEGER_TYPE + COMMA_SEP +
                    Database.Currencies.NAME + TEXT_TYPE + COMMA_SEP +
                    Database.Currencies.CODE + INTEGER_TYPE + COMMA_SEP +
                    Database.Currencies.SHOW_ON_MAP + BOOLEAN_TYPE + " NOT NULL DEFAULT 1" + COMMA_SEP +
                    Database.Currencies.PRICE + REAL_TYPE + COMMA_SEP +
                    Database.Currencies.PRICE_LAST_CHECK + INTEGER_TYPE +
                    " )";

    private static final String SQL_CREATE_MERCHANTS_TO_CURRENCIES_TABLE =
            "CREATE TABLE " + Database.CurrenciesMerchants.TABLE_NAME + " (" +
                    Database.CurrenciesMerchants._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Database.CurrenciesMerchants.MERCHANT_ID + TEXT_TYPE + COMMA_SEP +
                    Database.CurrenciesMerchants.CURRENCY_ID + INTEGER_TYPE +
                    " )";

    private static final String SQL_DELETE_MERCHANTS_TABLE = "DROP TABLE IF EXISTS " + Database.Merchants.TABLE_NAME;
    private static final String SQL_DELETE_CURRENCIES_TABLE = "DROP TABLE IF EXISTS " + Database.Currencies.TABLE_NAME;
    private static final String SQL_DELETE_MERCHANTS_TO_CURRENCIES_TABLE = "DROP TABLE IF EXISTS " + Database.CurrenciesMerchants.TABLE_NAME;

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

        db.execSQL("CREATE INDEX index_merchant_id_position ON merchants (_id, latitude, longitude)");
        db.execSQL("CREATE INDEX index_currency_id ON currencies (_id)");
        db.execSQL("CREATE INDEX index_currency_merchant_id ON currencies_merchants (_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_MERCHANTS_TABLE);
        db.execSQL(SQL_DELETE_CURRENCIES_TABLE);
        db.execSQL(SQL_DELETE_MERCHANTS_TO_CURRENCIES_TABLE);
        onCreate(db);

        Context context = App.getInstance();
        SyncMerchantsWakefulReceiver.startWakefulService(context, MerchantsSyncService.makeIntent(context));
    }
}
