package com.bubelov.coins.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.bubelov.coins.PreferenceKeys;
import com.bubelov.coins.dagger.Injector;

/**
 * @author Igor Bubelov
 */

public class DatabaseSyncService extends IntentService {
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
        if (intent == null) {
            return;
        }

        Injector.INSTANCE.getAndroidComponent().databaseSync().run();
    }
}