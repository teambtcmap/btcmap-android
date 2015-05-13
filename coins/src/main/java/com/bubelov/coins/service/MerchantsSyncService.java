package com.bubelov.coins.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.util.Log;

import com.bubelov.coins.Constants;
import com.bubelov.coins.event.MerchantsSyncFinishedEvent;
import com.bubelov.coins.event.NewMerchantsLoadedEvent;
import com.bubelov.coins.manager.MerchantSyncManager;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.receiver.SyncMerchantsWakefulReceiver;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:27
 */

public class MerchantsSyncService extends CoinsIntentService {
    private static final String TAG = MerchantsSyncService.class.getName();

    private static final int MAX_MERCHANTS_PER_REQUEST = 250;

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
        Cursor currenciesCursor = getContentResolver().query(Database.Currencies.CONTENT_URI,
                new String[]{Database.Currencies._ID, Database.Currencies.CODE},
                null,
                null,
                null);

        Log.d(TAG, currenciesCursor.getCount() + " currencies found in DB");

        if (currenciesCursor.getCount() == 0) {
            currenciesCursor.close();
            Log.d(TAG, "Loading currencies from server");

            List<Currency> currencies = getApi().getCurrencies();
            Log.d(TAG, String.format("Downloaded %s currencies", currencies.size()));

            saveCurrencies(currencies);

            for (Currency currency : currencies) {
                try {
                    syncMerchants(currency.getId(), currency.getCode());
                } catch (Exception exception) {
                    Log.e(TAG, String.format("Couldn't sync merchants for currency %s", currency.getCode()), exception);
                }
            }
        } else {
            while (currenciesCursor.moveToNext()) {
                Long id = currenciesCursor.getLong(currenciesCursor.getColumnIndex(Database.Currencies._ID));
                String code = currenciesCursor.getString(currenciesCursor.getColumnIndex(Database.Currencies.CODE));
                syncMerchants(id, code);
            }

            currenciesCursor.close();
        }

        getBus().post(new MerchantsSyncFinishedEvent());
    }

    private void syncMerchants(long currencyId, String currencyCode) {
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        ContentValues values = new ContentValues();
        UserNotificationManager notificationManager = new UserNotificationManager(getApplicationContext());

        while (true) {
            long lastUpdateMillis;

            Cursor cursor = db.rawQuery("select max(m._updated_at) from merchants as m join currencies_merchants as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where c.code = ?",
                    new String[] { currencyCode });

            if (!cursor.moveToNext()) {
                Log.e(TAG, "Couldn't get last update timestamp");
                cursor.close();
                break;
            }

            lastUpdateMillis = cursor.isNull(0) ? 0 : cursor.getLong(0);
            cursor.close();

            List<Merchant> merchants = getApi().getMerchants(currencyCode, new SimpleDateFormat(Constants.DATE_FORMAT).format(lastUpdateMillis), MAX_MERCHANTS_PER_REQUEST);
            Log.d(TAG, String.format("Downloaded %s merchants accepting %s", merchants.size(), currencyCode));

            for (Merchant merchant : merchants) {
                notificationManager.onMerchantDownload(merchant);

                values.put(Database.Merchants._ID, merchant.getId());
                values.put(Database.Merchants._CREATED_AT, merchant.getCreatedAt().getTime());
                values.put(Database.Merchants._UPDATED_AT, merchant.getUpdatedAt().getTime());
                values.put(Database.Merchants.LATITUDE, merchant.getLatitude());
                values.put(Database.Merchants.LONGITUDE, merchant.getLongitude());
                values.put(Database.Merchants.NAME, merchant.getName());
                values.put(Database.Merchants.DESCRIPTION, merchant.getDescription());
                values.put(Database.Merchants.PHONE, merchant.getPhone());
                values.put(Database.Merchants.WEBSITE, merchant.getWebsite());
                values.put(Database.Merchants.AMENITY, merchant.getAmenity());

                if (db.insertWithOnConflict(Database.Merchants.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) == -1) {
                    Log.d(TAG, "Couldn't insert merchant");
                }

                values.clear();

                values.put(Database.CurrenciesMerchants.MERCHANT_ID, merchant.getId());
                values.put(Database.CurrenciesMerchants.CURRENCY_ID, currencyId);

                db.insertWithOnConflict(Database.CurrenciesMerchants.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                values.clear();
            }

            if (merchants.size() > 0) {
                getApp().getMerchantsCache().invalidate();
                getBus().post(new NewMerchantsLoadedEvent());
            }

            if (merchants.size() < MAX_MERCHANTS_PER_REQUEST) {
                break;
            }
        }
    }

    private void saveCurrencies(Collection<Currency> currencies) throws RemoteException, OperationApplicationException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        for (Currency currency : currencies) {
            operations.add(ContentProviderOperation
                    .newInsert(Database.Currencies.CONTENT_URI)
                    .withValue(Database.Currencies._ID, currency.getId())
                    .withValue(Database.Currencies._CREATED_AT, currency.getCreatedAt().getTime())
                    .withValue(Database.Currencies._UPDATED_AT, currency.getUpdatedAt().getTime())
                    .withValue(Database.Currencies.NAME, currency.getName())
                    .withValue(Database.Currencies.CODE, currency.getCode())
                    .build());
        }

        getContentResolver().applyBatch(Database.AUTHORITY, operations);
    }
}