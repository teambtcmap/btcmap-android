package com.bubelov.coins.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.bubelov.coins.event.MerchantsSyncFinishedEvent;
import com.bubelov.coins.event.NewMerchantsLoadedEvent;
import com.bubelov.coins.manager.MerchantSyncManager;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.receiver.SyncMerchantsWakefulReceiver;
import com.bubelov.coins.server.ServerException;
import com.bubelov.coins.database.Tables;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.util.Utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:27
 */

public class MerchantsSyncService extends CoinsIntentService {
    private static final String TAG = MerchantsSyncService.class.getName();

    private boolean active;

    public static Intent makeIntent(Context context) {
        return new Intent(context, MerchantsSyncService.class);
    }

    public MerchantsSyncService() {
        super(TAG);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "Got new intent");

        if (!active) {
            if (Utils.isOnline(this)) {
                active = true;
                super.onStart(intent, startId);
            } else {
                Log.d(TAG, "Network is unavailable. Will try later");
                MerchantSyncManager syncManager = new MerchantSyncManager(getApplicationContext());
                syncManager.sheduleDelayed(TimeUnit.MINUTES.toMillis(15));
            }
        } else {
            Log.d(TAG, "Service already running");
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            syncMerchants();
        } catch (Exception exception) {
            Log.e(TAG, "Couldn't synchronize merchants", exception);
        } finally {
            active = false;
            new MerchantSyncManager(getApplicationContext()).setLastSyncMillis(System.currentTimeMillis());
            SyncMerchantsWakefulReceiver.completeWakefulIntent(intent);
        }
    }

    private void syncMerchants() throws Exception {
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
            }

            for (Currency currency : currencies) {
                try {
                    syncMerchants(currency);
                } catch (Exception exception) {
                    Log.e(TAG, String.format("Couldn't sync merchants for currency %s", currency.getCode()), exception);
                }
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

        getBus().post(new MerchantsSyncFinishedEvent());
    }

    private void syncMerchants(Currency currency) throws ServerException {
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        ContentValues values = new ContentValues();

        UserNotificationManager notificationManager = new UserNotificationManager(getApplicationContext());

        int page = 1;
        int perPage = 150;

        while (true) {
            List<Merchant> merchants = getApi().getMerchants(currency.getCode(), page, perPage);
            Log.d(TAG, String.format("Downloaded %s merchants accepting %s", merchants.size(), currency.getName()));

            for (Merchant merchant : merchants) {
                notificationManager.onMerchantDownload(merchant);

                values.put(Tables.Merchants._ID, merchant.getId());
                values.put(Tables.Merchants.LATITUDE, merchant.getLatitude());
                values.put(Tables.Merchants.LONGITUDE, merchant.getLongitude());
                values.put(Tables.Merchants.NAME, merchant.getName());
                values.put(Tables.Merchants.DESCRIPTION, merchant.getDescription());
                values.put(Tables.Merchants.PHONE, merchant.getPhone());
                values.put(Tables.Merchants.WEBSITE, merchant.getWebsite());
                values.put(Tables.Merchants.AMENITY, merchant.getAmenity());

                if (db.insertWithOnConflict(Tables.Merchants.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) == -1) {
                    Log.d(TAG, "Couldn't insert merchant");
                }

                values.clear();

                values.put(Tables.CurrenciesMerchants.MERCHANT_ID, merchant.getId());
                values.put(Tables.CurrenciesMerchants.CURRENCY_ID, currency.getId());

                db.insertWithOnConflict(Tables.CurrenciesMerchants.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                values.clear();
            }

            if (merchants.size() > 0) {
                getApp().getMerchantsCache().invalidate();
                getBus().post(new NewMerchantsLoadedEvent());
            }

            if (merchants.size() < perPage) {
                break;
            } else {
                page++;
            }
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