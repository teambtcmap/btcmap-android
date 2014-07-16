package com.bubelov.coins.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bubelov.coins.R;
import com.bubelov.coins.receiver.SyncMerchantsWakefulReceiver;

import java.text.SimpleDateFormat;

/**
 * Author: Igor Bubelov
 * Date: 10/07/14 21:38
 */

public class MerchantSyncManager {
    private static final String TAG = MerchantSyncManager.class.getName();

    private static final String SYNC_INTERVAL_KEY = "sync_interval";
    private static final String LAST_SYNC_MILLIS_KEY = "last_sync_millis";

    private static final long SYNC_INTERVAL_IN_MILLIS = 1000 * 60 * 60;

    private long syncInterval;
    private long lastSyncMillis;

    private Context context;
    private SharedPreferences preferences;

    private AlarmManager alarmManager;
    private PendingIntent syncIntent;

    public MerchantSyncManager(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        syncInterval = preferences.getLong(SYNC_INTERVAL_KEY, SYNC_INTERVAL_IN_MILLIS);
        lastSyncMillis = preferences.getLong(LAST_SYNC_MILLIS_KEY, 0);

        alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        syncIntent = PendingIntent.getBroadcast(context, 0, SyncMerchantsWakefulReceiver.makeIntent(context), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public void scheduleAlarm() {
        if (isEnabled()) {
            Log.d(TAG, "Scheduling an alarm for " + new SimpleDateFormat().format(lastSyncMillis + syncInterval));
            alarmManager.setInexactRepeating(AlarmManager.RTC, lastSyncMillis + syncInterval, syncInterval, syncIntent);
        } else {
            Log.d(TAG, "Alarm cancelled");
            alarmManager.cancel(syncIntent);
        }
    }

    public long getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(long syncInterval) {
        if (this.syncInterval == syncInterval) {
            return;
        }

        this.syncInterval = syncInterval;
        preferences.edit().putLong(SYNC_INTERVAL_KEY, syncInterval).commit();
        scheduleAlarm();
    }

    public long getLastSyncMillis() {
        return lastSyncMillis;
    }

    public void setLastSyncMillis(long lastSyncMillis) {
        if (this.lastSyncMillis == lastSyncMillis) {
            return;
        }

        this.lastSyncMillis = lastSyncMillis;
        preferences.edit().putLong(LAST_SYNC_MILLIS_KEY, lastSyncMillis).commit();
    }

    private boolean isEnabled() {
        return preferences.getBoolean(context.getString(R.string.pref_sync_merchants_key), true);
    }
}
