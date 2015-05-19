package com.bubelov.coins.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.util.Log;

import com.bubelov.coins.Constants;
import com.bubelov.coins.event.DatabaseSyncFailedEvent;
import com.bubelov.coins.event.DatabaseUpToDateEvent;
import com.bubelov.coins.event.DatabaseSyncingEvent;
import com.bubelov.coins.event.NewMerchantsLoadedEvent;
import com.bubelov.coins.manager.UserNotificationManager;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.util.Utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

    private static final String KEY_LAST_SYNC_MILLIS = "last_sync_millis";

    private static final long SYNC_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

    private static final int MAX_MERCHANTS_PER_REQUEST = 250;

    private SharedPreferences preferences;

    private AlarmManager alarmManager;
    private PendingIntent syncIntent;

    private volatile boolean syncing;

    public static Intent makeIntent(Context context, boolean forceSync) {
        Intent intent = new Intent(context, DatabaseSyncService.class);
        intent.putExtra(FORCE_SYNC_EXTRA, forceSync);
        return intent;
    }

    public DatabaseSyncService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(TAG, MODE_PRIVATE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        syncIntent = PendingIntent.getService(this, 0, DatabaseSyncService.makeIntent(this, true), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        scheduleNextSync();

        if (syncing) {
            getBus().post(new DatabaseSyncingEvent());
            return;
        }

        boolean forceSync = intent.getBooleanExtra(FORCE_SYNC_EXTRA, false);

        if (forceSync || isTimeForSync()) {
            if (Utils.isOnline(this)) {
                super.onStart(intent, startId);
            } else {
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5), syncIntent);
            }
        } else {
            getBus().post(new DatabaseUpToDateEvent());
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        syncing = true;
        getBus().post(new DatabaseSyncingEvent());

        try {
            sync();
            preferences.edit().putLong(KEY_LAST_SYNC_MILLIS, System.currentTimeMillis()).apply();
            getBus().post(new DatabaseUpToDateEvent());
        } catch (Exception exception) {
            getBus().post(new DatabaseSyncFailedEvent());
            Log.e(TAG, "Couldn't synchronize database", exception);
        } finally {
            syncing = false;
            scheduleNextSync();
        }
    }

    private void scheduleNextSync() {
        if (syncing) {
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + SYNC_INTERVAL_MILLIS, syncIntent);
        } else {
            long lastSyncMillis = preferences.getLong(KEY_LAST_SYNC_MILLIS, 0);
            alarmManager.set(AlarmManager.RTC, lastSyncMillis + SYNC_INTERVAL_MILLIS, syncIntent);
        }
    }

    private boolean isTimeForSync() {
        return System.currentTimeMillis() - preferences.getLong(KEY_LAST_SYNC_MILLIS, 0) > SYNC_INTERVAL_MILLIS;
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

        if (count == 0) {
            saveCurrencies(getApi().getCurrencies());
        }
    }

    private void syncMerchants(long currencyId, String currencyCode) throws RemoteException, OperationApplicationException {
        SQLiteDatabase db = getDatabaseHelper().getWritableDatabase();
        UserNotificationManager notificationManager = new UserNotificationManager(getApplicationContext());
        boolean initialized = isInitialized();

        while (true) {
            Cursor lastUpdateCursor = db.rawQuery("select max(m._updated_at) from merchants as m join currencies_merchants as mc on m._id = mc.merchant_id join currencies c on c._id = mc.currency_id where c.code = ?",
                    new String[] { currencyCode });

            lastUpdateCursor.moveToNext();
            long lastUpdateMillis = lastUpdateCursor.isNull(0) ? 0 : lastUpdateCursor.getLong(0);
            lastUpdateCursor.close();
            DateTime lastUpdateDateTime = new DateTime(DateTimeZone.UTC).withMillis(lastUpdateMillis);

            List<Merchant> merchants = getApi().getMerchants(currencyCode, lastUpdateDateTime.toString(Constants.DATE_FORMAT), MAX_MERCHANTS_PER_REQUEST);
            Log.d(TAG, String.format("Downloaded %s merchants accepting %s", merchants.size(), currencyCode));

            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            for (Merchant merchant : merchants) {
                if (initialized && notificationManager.shouldNotifyUser(merchant)) {
                    notificationManager.notifyUser(merchant.getId(), merchant.getName());
                }

                operations.add(ContentProviderOperation
                        .newInsert(Database.Merchants.CONTENT_URI)
                        .withValue(Database.Merchants._ID, merchant.getId())
                        .withValue(Database.Merchants._CREATED_AT, merchant.getCreatedAt().getMillis())
                        .withValue(Database.Merchants._UPDATED_AT, merchant.getUpdatedAt().getMillis())
                        .withValue(Database.Merchants.LATITUDE, merchant.getLatitude())
                        .withValue(Database.Merchants.LONGITUDE, merchant.getLongitude())
                        .withValue(Database.Merchants.NAME, merchant.getName())
                        .withValue(Database.Merchants.DESCRIPTION, merchant.getDescription())
                        .withValue(Database.Merchants.PHONE, merchant.getPhone())
                        .withValue(Database.Merchants.WEBSITE, merchant.getWebsite())
                        .withValue(Database.Merchants.AMENITY, merchant.getAmenity())
                        .withValue(Database.Merchants.OPENING_HOURS, merchant.getOpeningHours())
                        .withValue(Database.Merchants.ADDRESS, merchant.getAddress())
                        .build());

                operations.add(ContentProviderOperation
                        .newInsert(ContentUris.withAppendedId(Database.Merchants.CONTENT_URI, merchant.getId()).buildUpon().appendPath("currencies").build())
                        .withValue(Database.Currencies._ID, currencyId)
                        .build());
            }

            getContentResolver().applyBatch(Database.AUTHORITY, operations);

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
                    .withValue(Database.Currencies._CREATED_AT, currency.getCreatedAt().getMillis())
                    .withValue(Database.Currencies._UPDATED_AT, currency.getUpdatedAt().getMillis())
                    .withValue(Database.Currencies.NAME, currency.getName())
                    .withValue(Database.Currencies.CODE, currency.getCode())
                    .build());
        }

        getContentResolver().applyBatch(Database.AUTHORITY, operations);
    }

    private boolean isInitialized() {
        return preferences.contains(KEY_LAST_SYNC_MILLIS);
    }
}