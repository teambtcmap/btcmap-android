package com.bubelov.coins.service.sync.merchants;

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
import android.util.Log;

import com.bubelov.coins.Constants;
import com.bubelov.coins.event.DatabaseSyncFailedEvent;
import com.bubelov.coins.event.DatabaseUpToDateEvent;
import com.bubelov.coins.event.DatabaseSyncingEvent;
import com.bubelov.coins.event.NewMerchantsLoadedEvent;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.service.CoinsIntentService;
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

public class MerchantsSyncService extends CoinsIntentService {
    private static final String TAG = MerchantsSyncService.class.getSimpleName();

    private static final String FORCE_SYNC_EXTRA = "force_sync";

    private static final String KEY_LAST_SYNC_MILLIS = "last_sync_millis";

    private static final long SYNC_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

    private static final int MAX_MERCHANTS_PER_REQUEST = 500;

    private SharedPreferences preferences;

    private AlarmManager alarmManager;
    private PendingIntent syncIntent;

    private volatile boolean syncing;

    public static Intent makeIntent(Context context, boolean forceSync) {
        Intent intent = new Intent(context, MerchantsSyncService.class);
        intent.putExtra(FORCE_SYNC_EXTRA, forceSync);
        return intent;
    }

    public MerchantsSyncService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(TAG, MODE_PRIVATE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        syncIntent = PendingIntent.getService(this, 0, MerchantsSyncService.makeIntent(this, true), PendingIntent.FLAG_CANCEL_CURRENT);
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
                new String[]{"count(*) AS count"},
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
        UserNotificationController notificationManager = new UserNotificationController(getApplicationContext());
        boolean initialized = isInitialized();

        while (true) {
            String lastUpdateQuery = String.format("select max(m.%s) from %s as m join %s as cm on m.%s = cm.%s join %s c on c.%s = cm.%s where c.code = ?",
                    Database.Merchants._UPDATED_AT,
                    Database.Merchants.TABLE_NAME,
                    Database.CurrenciesMerchants.TABLE_NAME,
                    Database.Merchants._ID,
                    Database.CurrenciesMerchants.MERCHANT_ID,
                    Database.Currencies.TABLE_NAME,
                    Database.Currencies._ID,
                    Database.CurrenciesMerchants.CURRENCY_ID);

            Cursor lastUpdateCursor = db.rawQuery(lastUpdateQuery, new String[] { currencyCode });
            lastUpdateCursor.moveToNext();
            long lastUpdateMillis = lastUpdateCursor.isNull(0) ? 0 : lastUpdateCursor.getLong(0);
            lastUpdateCursor.close();
            DateTime lastUpdateDateTime = new DateTime(DateTimeZone.UTC).withMillis(lastUpdateMillis);

            List<Merchant> merchants = getApi().getMerchants(currencyCode, lastUpdateDateTime.toString(Constants.DATE_FORMAT), MAX_MERCHANTS_PER_REQUEST);
            Log.d(TAG, String.format("Downloaded %s merchants accepting %s", merchants.size(), currencyCode));

            ContentValues[] merchantsValues = new ContentValues[merchants.size()];
            ContentValues[] currenciesMerchantsValues = new ContentValues[merchants.size()];

            for (int i = 0; i < merchants.size(); i++) {
                Merchant merchant = merchants.get(i);

                if (initialized && notificationManager.shouldNotifyUser(merchant)) {
                    notificationManager.notifyUser(merchant.getId(), merchant.getName());
                }

                ContentValues merchantValues = new ContentValues();
                merchantValues.put(Database.Merchants._ID, merchant.getId());
                merchantValues.put(Database.Merchants._CREATED_AT, merchant.getCreatedAt().getMillis());
                merchantValues.put(Database.Merchants._UPDATED_AT, merchant.getUpdatedAt().getMillis());
                merchantValues.put(Database.Merchants.LATITUDE, merchant.getLatitude());
                merchantValues.put(Database.Merchants.LONGITUDE, merchant.getLongitude());
                merchantValues.put(Database.Merchants.NAME, merchant.getName());
                merchantValues.put(Database.Merchants.DESCRIPTION, merchant.getDescription());
                merchantValues.put(Database.Merchants.PHONE, merchant.getPhone());
                merchantValues.put(Database.Merchants.WEBSITE, merchant.getWebsite());
                merchantValues.put(Database.Merchants.AMENITY, merchant.getAmenity());
                merchantValues.put(Database.Merchants.OPENING_HOURS, merchant.getOpeningHours());
                merchantValues.put(Database.Merchants.ADDRESS, merchant.getAddress());

                merchantsValues[i] = merchantValues;

                ContentValues currencyMerchantValues = new ContentValues();
                currencyMerchantValues.put(Database.CurrenciesMerchants.CURRENCY_ID, currencyId);
                currencyMerchantValues.put(Database.CurrenciesMerchants.MERCHANT_ID, merchant.getId());

                currenciesMerchantsValues[i] = currencyMerchantValues;
            }

            getContentResolver().bulkInsert(Database.Merchants.CONTENT_URI, merchantsValues);
            getContentResolver().bulkInsert(Database.CurrenciesMerchants.CONTENT_URI, currenciesMerchantsValues);

            if (merchants.size() > 0) {
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