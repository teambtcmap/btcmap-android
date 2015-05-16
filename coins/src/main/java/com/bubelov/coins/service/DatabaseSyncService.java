package com.bubelov.coins.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bubelov.coins.Constants;
import com.bubelov.coins.R;
import com.bubelov.coins.event.MerchantsSyncFinishedEvent;
import com.bubelov.coins.event.NewMerchantsLoadedEvent;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.model.Currency;
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

public class DatabaseSyncService extends CoinsIntentService {
    private static final String TAG = DatabaseSyncService.class.getSimpleName();

    private static final String FORCE_SYNC_EXTRA = "force_sync";

    private static final String KEY_IS_SYNCING = "syncing";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_LAST_SYNC_MILLIS = "last_sync_millis";

    private static final long SYNC_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

    private static final int MAX_MERCHANTS_PER_REQUEST = 250;

    private SharedPreferences preferences;

    private AlarmManager alarmManager;
    private PendingIntent syncIntent;

    public static Intent makeIntent(Context context, boolean forceSync) {
        Intent intent = new Intent(context, DatabaseSyncService.class);
        intent.putExtra(FORCE_SYNC_EXTRA, forceSync);
        return intent;
    }

    public static boolean isSyncing(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(TAG, MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_SYNCING, false);
    }

    public DatabaseSyncService() {
        super(TAG);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "Got new intent");

        preferences = getSharedPreferences(TAG, MODE_PRIVATE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        syncIntent = PendingIntent.getService(this, 0, DatabaseSyncService.makeIntent(this, true), PendingIntent.FLAG_CANCEL_CURRENT);

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_sync_merchants_key), true)) {
            Log.d(TAG, "Database sync turned off");
        }

        if (isSyncing()) {
            Log.d(TAG, "Service already running");
            return;
        }

        long lastSyncMillis = preferences.getLong(KEY_LAST_SYNC_MILLIS, 0);
        alarmManager.set(AlarmManager.RTC, lastSyncMillis + SYNC_INTERVAL_MILLIS, syncIntent);

        if (intent.getBooleanExtra(FORCE_SYNC_EXTRA, false) || System.currentTimeMillis() - preferences.getLong(KEY_LAST_SYNC_MILLIS, 0) > SYNC_INTERVAL_MILLIS) {
            if (Utils.isOnline(this)) {
                setSyncing(true);
                super.onStart(intent, startId);
            } else {
                Log.d(TAG, "Network is unavailable. Will try later");
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5), syncIntent);
            }
        } else {
            Log.d(TAG, "Too early for sync");
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            sync();
            getSharedPreferences(TAG, MODE_PRIVATE).edit().putBoolean(KEY_INITIALIZED, true).apply();
        } catch (Exception exception) {
            Log.e(TAG, "Couldn't synchronize database", exception);
        } finally {
            setSyncing(false);
            preferences.edit().putLong(KEY_LAST_SYNC_MILLIS, System.currentTimeMillis()).apply();
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + SYNC_INTERVAL_MILLIS, syncIntent);
            getBus().post(new MerchantsSyncFinishedEvent());
        }
    }

    @Override
    public void onDestroy() {
        setSyncing(false);
        super.onDestroy();
    }

    private void sync() throws Exception {
        syncCurrenciesIfNecessary();

        Cursor currencies = getContentResolver().query(Database.Currencies.CONTENT_URI,
                new String[]{Database.Currencies._ID, Database.Currencies.CODE},
                null,
                null,
                null);

        while (currencies.moveToNext()) {
            Long id = currencies.getLong(currencies.getColumnIndex(Database.Currencies._ID));
            String code = currencies.getString(currencies.getColumnIndex(Database.Currencies.CODE));
            syncMerchants(id, code);
        }

        currencies.close();
    }

    private void syncCurrenciesIfNecessary() throws RemoteException, OperationApplicationException {
        Cursor countCursor = getContentResolver().query(Database.Currencies.CONTENT_URI,
                new String[]{"count(*) AS count "},
                null,
                null,
                null);

        countCursor.moveToFirst();
        int count = countCursor.getInt(0);
        countCursor.close();

        Log.d(TAG, count + " currencies found in DB");

        if (count == 0) {
            Log.d(TAG, "Loading currencies from server");
            List<Currency> currencies = getApi().getCurrencies();
            Log.d(TAG, String.format("Downloaded %s currencies", currencies.size()));
            saveCurrencies(currencies);
        }
    }

    private void syncMerchants(long currencyId, String currencyCode) {
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        ContentValues values = new ContentValues();
        UserNotificationManager notificationManager = new UserNotificationManager(getApplicationContext());
        boolean initialized = getSharedPreferences(TAG, MODE_PRIVATE).getBoolean(KEY_INITIALIZED, false);

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
                if (initialized && notificationManager.shouldNotifyUser(merchant)) {
                    notificationManager.notifyUser(merchant.getId(), merchant.getName());
                }

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

    private boolean isSyncing() {
        return isSyncing(this);
    }

    private void setSyncing(boolean syncing) {
        preferences.edit().putBoolean(KEY_IS_SYNCING, syncing).apply();
    }
}