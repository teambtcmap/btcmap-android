package com.bubelov.coins;

import android.app.Application;
import android.preference.PreferenceManager;

import com.bubelov.coins.dagger.Injector;
import com.bubelov.coins.database.sync.DatabaseSyncService;
import com.bubelov.coins.util.ReleaseTree;
import com.facebook.stetho.Stetho;

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
            Stetho.initializeWithDefaults(this);
        } else {
            Timber.plant(new ReleaseTree());
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        Injector.INSTANCE.initMainComponent(this);

        DatabaseSyncService.Companion.start(this);
    }
}