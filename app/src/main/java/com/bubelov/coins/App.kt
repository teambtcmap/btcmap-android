package com.bubelov.coins

import android.app.Activity
import android.app.Application
import android.app.Service
import android.preference.PreferenceManager

import com.bubelov.coins.dagger.Injector
import com.bubelov.coins.database.sync.DatabaseSync
import com.bubelov.coins.util.ReleaseTree

import javax.inject.Inject

import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import timber.log.Timber

/**
 * @author Igor Bubelov
 */

class App : Application(), HasActivityInjector, HasServiceInjector {
    @Inject internal lateinit var activityInjector: DispatchingAndroidInjector<Activity>

    @Inject internal lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    @Inject lateinit var databaseSync: DatabaseSync

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        Injector.init(this)
        Injector.appComponent.inject(this)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true)
        databaseSync.start()
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return activityInjector
    }

    override fun serviceInjector(): AndroidInjector<Service> {
        return serviceInjector
    }
}