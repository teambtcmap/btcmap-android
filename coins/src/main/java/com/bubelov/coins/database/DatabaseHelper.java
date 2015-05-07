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
            "CREATE TABLE " + Tables.Merchants.TABLE_NAME + " (" +
                    Tables.Merchants._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Tables.Merchants._CREATED_AT + INTEGER_TYPE + COMMA_SEP +
                    Tables.Merchants._UPDATED_AT + INTEGER_TYPE + COMMA_SEP +
                    Tables.Merchants.LATITUDE + REAL_TYPE + COMMA_SEP +
                    Tables.Merchants.LONGITUDE + REAL_TYPE + COMMA_SEP +
                    Tables.Merchants.NAME + TEXT_TYPE + COMMA_SEP +
                    Tables.Merchants.DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                    Tables.Merchants.PHONE + TEXT_TYPE + COMMA_SEP +
                    Tables.Merchants.WEBSITE + TEXT_TYPE + COMMA_SEP +
                    Tables.Merchants.AMENITY + TEXT_TYPE +
            " )";

    private static final String SQL_CREATE_CURRENCIES_TABLE =
            "CREATE TABLE " + Tables.Currencies.TABLE_NAME + " (" +
                    Tables.Currencies._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Tables.Currencies._CREATED_AT + INTEGER_TYPE + COMMA_SEP +
                    Tables.Currencies._UPDATED_AT + INTEGER_TYPE + COMMA_SEP +
                    Tables.Currencies.NAME + TEXT_TYPE + COMMA_SEP +
                    Tables.Currencies.CODE + INTEGER_TYPE + COMMA_SEP +
                    Tables.Currencies.SHOW_ON_MAP + BOOLEAN_TYPE + " NOT NULL DEFAULT 1" +
                    " )";

    private static final String SQL_CREATE_MERCHANTS_TO_CURRENCIES_TABLE =
            "CREATE TABLE " + Tables.CurrenciesMerchants.TABLE_NAME + " (" +
                    Tables.CurrenciesMerchants._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Tables.CurrenciesMerchants.MERCHANT_ID + TEXT_TYPE + COMMA_SEP +
                    Tables.CurrenciesMerchants.CURRENCY_ID + INTEGER_TYPE +
                    " )";

    private static final String SQL_DELETE_MERCHANTS_TABLE = "DROP TABLE IF EXISTS " + Tables.Merchants.TABLE_NAME;
    private static final String SQL_DELETE_CURRENCIES_TABLE = "DROP TABLE IF EXISTS " + Tables.Currencies.TABLE_NAME;
    private static final String SQL_DELETE_MERCHANTS_TO_CURRENCIES_TABLE = "DROP TABLE IF EXISTS " + Tables.CurrenciesMerchants.TABLE_NAME;

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
