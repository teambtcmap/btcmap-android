package com.bubelov.coins.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bubelov.coins.manager.MerchantSyncManager;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.receiver.SyncMerchantsWakefulReceiver;
import com.bubelov.coins.server.ServerException;
import com.bubelov.coins.database.Tables;
import com.bubelov.coins.model.Merchant;

import java.util.List;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:27
 */

public class MerchantsSyncService extends CoinsIntentService {
    private static final String TAG = MerchantsSyncService.class.getName();

    public static final String SYNC_COMPLETED_ACTION = TAG + ".SYNC_COMPLETED";

    private boolean active;

    public static Intent makeIntent(Context context) {
        return new Intent(context, MerchantsSyncService.class);
    }

    public MerchantsSyncService() {
        super(TAG);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (!active) {
            active = true;
            super.onStart(intent, startId);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            syncMerchants();
        } catch (ServerException exception) {
            Log.e(TAG, "Couldn't synchronize merchants", exception);
        } finally {
            active = false;
            new MerchantSyncManager(getApplicationContext()).setLastSyncMillis(System.currentTimeMillis());
            SyncMerchantsWakefulReceiver.completeWakefulIntent(intent);
            Intent syncCompletedIntent = new Intent(SYNC_COMPLETED_ACTION);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(syncCompletedIntent);
        }
    }

    private void syncMerchants() throws ServerException {
        SQLiteDatabase db = getDatabaseHelper().getReadableDatabase();

        Cursor currenciesCursor = db.query(Tables.Currencies.TABLE_NAME,
                new String[] { Tables.Currencies._ID, Tables.Currencies.NAME, Tables.Currencies.CODE },
                null,
                null,
                null,
                null,
                null);

        Log.d(TAG, currenciesCursor.getCount() + " currencies found in DB");

        if (currenciesCursor.getCount() == 0) {
            currenciesCursor.close();
            Log.d(TAG, "Loading currencies from server");

            List<Currency> currencies = getApi().getCurrencies();
            Log.d(TAG, String.format("Downloaded %s currencies", currencies.size()));

            for (Currency currency : currencies) {
                saveCurrency(currency);
                syncMerchants(currency);
            }
        } else {
            while (currenciesCursor.moveToNext()) {
                Currency currency = new Currency();
                currency.setId(currenciesCursor.getLong(0));
                currency.setName(currenciesCursor.getString(1));
                currency.setCode(currenciesCursor.getString(2));
                syncMerchants(currency);
            }

            currenciesCursor.close();
        }
    }

    private void syncMerchants(final Currency currency) throws ServerException {
        List<Merchant> merchants = getApi().getMerchants(currency.getCode());
        Log.d(TAG, String.format("Downloaded %s merchants accepting %s", merchants.size(), currency.getName()));

        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        ContentValues values = new ContentValues();

        UserNotificationManager notificationManager = new UserNotificationManager(getApplicationContext());

        for (Merchant merchant : merchants) {
            notificationManager.onMerchantDownload(merchant);

            values.put(Tables.Merchants._ID, merchant.getId());
            values.put(Tables.Merchants.LATITUDE, merchant.getLatitude());
            values.put(Tables.Merchants.LONGITUDE, merchant.getLongitude());
            values.put(Tables.Merchants.NAME, merchant.getName());
            values.put(Tables.Merchants.DESCRIPTION, merchant.getDescription());
            //values.put(Tables.Merchants.PHONE, merchant.getPhone());
            //values.put(Tables.Merchants.WEBSITE, merchant.getWebsite());

            if (db.insertWithOnConflict(Tables.Merchants.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) == -1) {
                Log.d(TAG, "Couldn't insert merchant");
            }

            values.clear();

            values.put(Tables.MerchantsToCurrencies.MERCHANT_ID, merchant.getId());
            values.put(Tables.MerchantsToCurrencies.CURRENCY_ID, currency.getId());

            db.insertWithOnConflict(Tables.MerchantsToCurrencies.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            values.clear();
        }
    }

    private boolean saveCurrency(Currency currency) {
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Tables.Currencies._ID, currency.getId());
        values.put(Tables.Currencies.NAME, currency.getName());
        values.put(Tables.Currencies.CODE, currency.getCode());

        return db.insertWithOnConflict(Tables.Currencies.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }
}