package com.bubelov.coins.database.sync

import android.content.Context
import com.bubelov.coins.model.SyncLogEntry

import com.bubelov.coins.repository.currency.CurrenciesRepository
import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.placecategory.PlaceCategoriesRepository
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.bubelov.coins.util.PlaceNotificationManager
import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.PeriodicTask
import com.google.android.gms.gcm.Task
import org.jetbrains.anko.doAsyncResult

import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

import timber.log.Timber
import java.util.concurrent.Future

/**
 * @author Igor Bubelov
 */

@Singleton
class DatabaseSync @Inject
internal constructor(
        private val context: Context,
        private val placesRepository: PlacesRepository,
        private val placeCategoriesRepository: PlaceCategoriesRepository,
        private val currenciesRepository: CurrenciesRepository,
        private val placeNotificationManager: PlaceNotificationManager,
        private val syncLogsRepository: SyncLogsRepository
) {
    private var futureResult: Future<Any>? = null

    fun sync() {
        futureResult?.cancel(true)

        futureResult = doAsyncResult({ Timber.e(it, "Couldn't sync database") }, {
            val newPlaces = placesRepository.fetchNewPlaces()

            newPlaces.forEach {
                placeNotificationManager.notifyUserIfNecessary(it)
            }

            placeCategoriesRepository.reloadFromApi()
            currenciesRepository.reloadFromApi()

            syncLogsRepository.addEntry(SyncLogEntry(System.currentTimeMillis(), newPlaces.size))
        })

        scheduleNextSync()
    }

    private fun scheduleNextSync() {
        PeriodicTask.Builder().apply {
            setService(DatabaseGcmSyncService::class.java)
            setTag(DatabaseGcmSyncService.TAG)
            setPeriod(TimeUnit.DAYS.toSeconds(1))
            setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
            setPersisted(true)
        }.build().schedule()
    }

    private fun PeriodicTask.schedule() {
        GcmNetworkManager.getInstance(context).schedule(this)
    }
}