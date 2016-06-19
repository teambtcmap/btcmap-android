package com.bubelov.coins.service.sync.merchants;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.bubelov.coins.Constants;
import com.bubelov.coins.EventBus;
import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.event.DatabaseSyncFailedEvent;
import com.bubelov.coins.event.DatabaseSyncStartedEvent;
import com.bubelov.coins.event.DatabaseSyncedEvent;
import com.bubelov.coins.model.Currency;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.service.CoinsIntentService;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

import timber.log.Timber;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:27
 */

public class DatabaseSyncService extends CoinsIntentService {
    private static final int MAX_MERCHANTS_PER_REQUEST = 2500;

    public static void startIfNeverSynced(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (!preferences.contains(PreferenceKeys.DATABASE_SYNC_DATE)) {
            start(context);
        }
    }

    public static void start(Context context) {
        context.startService(new Intent(context, DatabaseSyncService.class));
    }

    public DatabaseSyncService() {
        super(DatabaseSyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("New intent");

        if (intent == null) {
            return;
        }

        Timber.d("Posting sync started event");
        EventBus.getInstance().post(new DatabaseSyncStartedEvent());

        try {
            long merchantsBeforeSync = Merchant.getCount();
            Timber.d("Merchants before sync: %s", merchantsBeforeSync);
            long time = System.currentTimeMillis();
            sync();
            Timber.d("Sync time: %s", System.currentTimeMillis() - time);
            Timber.d("Posting sync finished event");
            EventBus.getInstance().post(new DatabaseSyncedEvent());
        } catch (Exception e) {
            Timber.e(e, "Couldn't sync database");
            EventBus.getInstance().post(new DatabaseSyncFailedEvent());
        }
    }

    private void sync() throws Exception {
        Timber.d("Requesting currencies");
        long time = System.currentTimeMillis();
        List<Currency> currencies = getApi().getCurrencies().execute().body();
        Timber.d("%s currencies loaded. Time: %s", currencies.size(), System.currentTimeMillis() - time);

        Timber.d("Inserting currencies");
        time = System.currentTimeMillis();
        Currency.insert(currencies);
        Timber.d("Inserted. Time: %s", System.currentTimeMillis() - time);

        SQLiteDatabase db = Injector.INSTANCE.getAppComponent().database();
        UserNotificationController notificationManager = new UserNotificationController(getApplicationContext());
        boolean initialSync = Merchant.getCount() == 0;

        while (true) {
            DateTime lastUpdate = getLatestMerchantUpdateDate(db);
            List<Merchant> merchants = getApi().getMerchants(lastUpdate.toString(Constants.DATE_FORMAT), MAX_MERCHANTS_PER_REQUEST).execute().body();

            Timber.d("Inserting %s merchants", merchants.size());
            time = System.currentTimeMillis();
            Merchant.insert(merchants);
            Timber.d("Inserted. Time: %s", System.currentTimeMillis() - time);

            for (Merchant merchant : merchants) {
                if (!initialSync && notificationManager.shouldNotifyUser(merchant)) {
                    notificationManager.notifyUser(merchant.getId(), merchant.getName());
                }
            }

            if (merchants.size() < MAX_MERCHANTS_PER_REQUEST) {
                break;
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putLong(PreferenceKeys.DATABASE_SYNC_DATE, System.currentTimeMillis())
                .apply();
    }

    private DateTime getLatestMerchantUpdateDate(SQLiteDatabase db) {
        Cursor lastUpdateCursor = db.query(DbContract.Merchants.TABLE_NAME,
                new String[]{DbContract.Merchants._UPDATED_AT},
                null,
                null,
                null,
                null,
                DbContract.Merchants._UPDATED_AT + " DESC",
                "1");

        long lastUpdateMillis = 0;

        if (lastUpdateCursor.moveToNext()) {
            lastUpdateMillis = lastUpdateCursor.isNull(0) ? 0 : lastUpdateCursor.getLong(0);
        }

        lastUpdateCursor.close();
        return new DateTime(DateTimeZone.UTC).withMillis(lastUpdateMillis);
    }
}