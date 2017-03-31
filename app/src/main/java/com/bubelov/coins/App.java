package com.bubelov.coins;

import android.app.Application;
import android.preference.PreferenceManager;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.service.DatabaseSyncService;

import timber.log.Timber;

/**
 * @author Igor Bubelov
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        Injector.INSTANCE.initMainComponent(this);
        DatabaseSyncService.startIfNeverSynced(this);
    }
}