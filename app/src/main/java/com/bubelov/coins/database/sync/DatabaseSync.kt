package com.bubelov.coins.database.sync

import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.WorkerThread

import com.bubelov.coins.PreferenceKeys
import com.bubelov.coins.repository.currency.CurrenciesRepository
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository
import com.bubelov.coins.util.PlaceNotificationManager
import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.PeriodicTask
import com.google.android.gms.gcm.Task

import java.util.ArrayList
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

import timber.log.Timber

/**
 * @author Igor Bubelov
 */

@Singleton
class DatabaseSync @Inject
internal constructor(private val context: Context, private val placesRepository: PlacesRepository, private val placeCategoriesRepository: PlaceCategoriesRepository, private val currenciesRepository: CurrenciesRepository, private val placeNotificationManager: PlaceNotificationManager, private val preferences: SharedPreferences) {
    @Volatile var isSyncing: Boolean = false
        private set

    private val callbacks = ArrayList<Callback>()

    @WorkerThread
    internal fun sync() {
        isSyncing = true

        try {
            val newPlaces = placesRepository.fetchNewPlaces()

            for (place in newPlaces) {
                placeNotificationManager.notifyUserIfNecessary(place)
            }

            if (!placeCategoriesRepository.reloadFromNetwork()) {
                throw IllegalStateException("Couldn't sync place categories")
            }

            if (!currenciesRepository.reloadFromNetwork()) {
                throw IllegalStateException("Couldn't sync place categories")
            }

            preferences.edit().putLong(PreferenceKeys.LAST_SYNC_DATE, System.currentTimeMillis()).apply()

            for (callback in callbacks) {
                callback.onDatabaseSyncFinished()
            }
        } catch (e: Exception) {
            Timber.e(e, "Couldn't sync database")

            for (callback in callbacks) {
                callback.onDatabaseSyncError()
            }
        } finally {
            isSyncing = false
            scheduleNextSync()
        }
    }

    val lastSyncDate: Long
        get() {
            if (preferences.contains(PreferenceKeys.LAST_SYNC_DATE)) {
                return preferences.getLong(PreferenceKeys.LAST_SYNC_DATE, 0)
            } else {
                return 0
            }
        }

    fun addCallback(callback: Callback) {
        callbacks.add(callback)
    }

    fun removeCallback(callback: Callback) {
        callbacks.remove(callback)
    }

    private fun scheduleNextSync() {
        GcmNetworkManager.getInstance(context).schedule(PeriodicTask.Builder()
                .setService(DatabaseGcmSyncService::class.java)
                .setTag(DatabaseGcmSyncService.TAG)
                .setPeriod(TimeUnit.DAYS.toSeconds(1))
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setPersisted(true)
                .build())
    }

    interface Callback {
        fun onDatabaseSyncFinished()

        fun onDatabaseSyncError()
    }
}