package com.bubelov.coins

import android.app.Application
import android.preference.PreferenceManager

import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.database.sync.DatabaseSyncService
import com.bubelov.coins.util.ReleaseTree
import com.facebook.stetho.Stetho

import timber.log.Timber

/**
 * @author Igor Bubelov
 */

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Stetho.initializeWithDefaults(this)
        } else {
            Timber.plant(ReleaseTree())
        }

        Injector.init(this)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true)
        DatabaseSyncService.start(this)
    }
}