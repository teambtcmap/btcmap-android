package com.bubelov.coins.database.sync

import android.content.Context
import com.bubelov.coins.model.SyncLogEntry
import com.bubelov.coins.repository.Result

import com.bubelov.coins.repository.place.PlacesRepository
import com.bubelov.coins.repository.synclogs.SyncLogsRepository
import com.bubelov.coins.util.PlaceNotificationManager
import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.PeriodicTask
import com.google.android.gms.gcm.Task
import kotlinx.coroutines.experimental.launch

import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

import timber.log.Timber

/**
 * @author Igor Bubelov
 */

@Singleton
class DatabaseSync @Inject
internal constructor(
        private val context: Context,
        private val placesRepository: PlacesRepository,
        private val placeNotificationManager: PlaceNotificationManager,
        private val syncLogsRepository: SyncLogsRepository
) {
    fun start() = launch {
        try {
            Timber.d("Starting sync")
            val result = placesRepository.fetchNewPlaces()

            when (result) {
                is Result.Success -> {
                    Timber.d("Fetched ${result.data.size} new places")
                    syncLogsRepository.addEntry(SyncLogEntry(System.currentTimeMillis(), result.data.size))

                    result.data.forEach {
                        placeNotificationManager.notifyUserIfNecessary(it)
                    }

                    Timber.d("Sync completed")
                }
                is Result.Error -> throw result.e
            }
        } catch (e: Exception) {
            Timber.e(e, "Couldn't sync database")
        }

        scheduleNextSync()
    }

    private fun scheduleNextSync() {
        Timber.d("Scheduling next sync")

        PeriodicTask.Builder().apply {
            setService(DatabaseSyncService::class.java)
            setTag(DatabaseSyncService.TAG)
            setPeriod(TimeUnit.DAYS.toSeconds(1))
            setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
            setPersisted(true)
        }.build().schedule()
    }

    private fun PeriodicTask.schedule() {
        GcmNetworkManager.getInstance(context).schedule(this)
    }
}