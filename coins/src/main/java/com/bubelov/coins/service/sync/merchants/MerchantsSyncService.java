package com.bubelov.coins.service.sync.merchants;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bubelov.coins.App;
import com.bubelov.coins.Constants;
import com.bubelov.coins.dao.CurrencyDAO;
import com.bubelov.coins.dao.MerchantDAO;
import com.bubelov.coins.database.Database;
import com.bubelov.coins.event.DatabaseSyncFailedEvent;
import com.bubelov.coins.event.MerchantsSyncFinishedEvent;
import com.bubelov.coins.event.DatabaseSyncStartedEvent;
import com.bubelov.coins.database.DbContract;
import com.bubelov.coins.model.Merchant;
import com.bubelov.coins.service.CoinsIntentService;
import com.bubelov.coins.util.Utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author: Igor Bubelov
 * Date: 07/07/14 22:27
 */

public class MerchantsSyncService extends CoinsIntentService {
    private static final String TAG = MerchantsSyncService.class.getSimpleName();

    private static final String FORCE_SYNC_EXTRA = "force_sync";

    private static final String KEY_SYNCING = "syncing";

    private static final String KEY_LAST_SYNC_MILLIS = "last_sync_millis";

    private static final long SYNC_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

    private static final int MAX_MERCHANTS_PER_REQUEST = 500;

    private SharedPreferences preferences;

    private AlarmManager alarmManager;
    private PendingIntent syncIntent;

    public static Intent makeIntent(Context context, boolean forceSync) {
        Intent intent = new Intent(context, MerchantsSyncService.class);
        intent.putExtra(FORCE_SYNC_EXTRA, forceSync);
        return intent;
    }

    public static boolean isSyncing() {
        SharedPreferences preferences = App.getInstance().getSharedPreferences(TAG, MODE_PRIVATE);
        return preferences.getBoolean(KEY_SYNCING, false);
    }

    public MerchantsSyncService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(TAG, MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_SYNCING, false).apply();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        syncIntent = PendingIntent.getService(this, 0, MerchantsSyncService.makeIntent(this, true), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            scheduleNextSync();
            return;
        }

        boolean forceSync = intent.getBooleanExtra(FORCE_SYNC_EXTRA, false);

        if (!isTimeForSync() && !forceSync) {
            scheduleNextSync();
            return;
        }

        if (!Utils.isOnline(this)) {
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5), syncIntent);
            return;
        }

        getBus().post(new DatabaseSyncStartedEvent());

        try {
            preferences.edit().putBoolean(KEY_SYNCING, true).apply();
            int merchantsBeforeSync = getMerchantsCount();
            sync();
            preferences.edit().putLong(KEY_LAST_SYNC_MILLIS, System.currentTimeMillis()).apply();
            getBus().post(new MerchantsSyncFinishedEvent(getMerchantsCount() != merchantsBeforeSync));
        } catch (Exception exception) {
            getBus().post(new DatabaseSyncFailedEvent());
        } finally {
            preferences.edit().putBoolean(KEY_SYNCING, false).apply();
            scheduleNextSync();
        }
    }

    private int getMerchantsCount() {
        Cursor countCursor = getContentResolver().query(DbContract.Merchants.CONTENT_URI,
                new String[]{"count(*) AS count"},
                null,
                null,
                null);

        countCursor.moveToFirst();
        int count = countCursor.getInt(0);
        countCursor.close();

        return count;
    }

    private void scheduleNextSync() {
        long lastSyncMillis = preferences.getLong(KEY_LAST_SYNC_MILLIS, 0);
        alarmManager.set(AlarmManager.RTC, lastSyncMillis + SYNC_INTERVAL_MILLIS, syncIntent);
    }

    private boolean isTimeForSync() {
        return System.currentTimeMillis() - preferences.getLong(KEY_LAST_SYNC_MILLIS, 0) > SYNC_INTERVAL_MILLIS;
    }

    private void sync() throws Exception {
        CurrencyDAO.insert(this, getApi().getCurrencies().execute().body());

        SQLiteDatabase db = Database.get();
        UserNotificationController notificationManager = new UserNotificationController(getApplicationContext());
        boolean initialized = isInitialized();

        while (true) {
            DateTime lastUpdate = getLatestMerchantUpdateDate(db);
            List<Merchant> merchants = getApi().getMerchants(lastUpdate.toString(Constants.DATE_FORMAT), MAX_MERCHANTS_PER_REQUEST).execute().body();

            MerchantDAO.insertMerchants(this, merchants);

            for (Merchant merchant : merchants) {
                if (initialized && notificationManager.shouldNotifyUser(merchant)) {
                    notificationManager.notifyUser(merchant.getId(), merchant.getName());
                }
            }

            if (merchants.size() < MAX_MERCHANTS_PER_REQUEST) {
                break;
            }
        }
    }

    private boolean isInitialized() {
        return preferences.contains(KEY_LAST_SYNC_MILLIS);
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