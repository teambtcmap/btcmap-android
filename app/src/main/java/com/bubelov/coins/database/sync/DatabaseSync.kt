package com.bubelov.coins.database.sync

import android.content.Context

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
internal constructor(private val context: Context, private val placesRepository: PlacesRepository, private val placeCategoriesRepository: PlaceCategoriesRepository, private val currenciesRepository: CurrenciesRepository, private val placeNotificationManager: PlaceNotificationManager) {
    @Volatile var isSyncing: Boolean = false
        private set

    private val callbacks = ArrayList<Callback>()

    internal fun sync() {
        isSyncing = true

        try {
            val cachedPlacesBeforeSync = placesRepository.cachedPlacesCount
            val newPlaces = placesRepository.fetchNewPlaces()

            if (cachedPlacesBeforeSync == 0L) {
                for (place in newPlaces) {
                    placeNotificationManager.notifyUserIfNecessary(place)
                }
            }

            placeCategoriesRepository.reloadFromApi()
            currenciesRepository.reloadFromApi()

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