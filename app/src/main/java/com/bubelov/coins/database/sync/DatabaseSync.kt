package com.bubelov.coins.database.sync

import android.content.Context
import com.bubelov.coins.model.SyncLogEntry
import com.bubelov.coins.repository.ApiResult

import com.bubelov.coins.repository.place.PlacesRepository
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
        private val placeNotificationManager: PlaceNotificationManager,
        private val syncLogsRepository: SyncLogsRepository
) {
    private var futureSyncResult: Future<Any>? = null

    fun sync() {
        Timber.d("Starting sync")
        futureSyncResult?.cancel(true)

        futureSyncResult = doAsyncResult({
            Timber.e(it, "Couldn't sync database")
            scheduleNextSync()
        }, {
            Timber.d("Fetching new places")
            val placesResult = placesRepository.fetchNewPlaces()

            when (placesResult) {
                is ApiResult.Success -> {
                    Timber.d("Fetched ${placesResult.data.size} new places")
                    syncLogsRepository.addEntry(SyncLogEntry(System.currentTimeMillis(), placesResult.data.size))

                    placesResult.data.forEach {
                        placeNotificationManager.notifyUserIfNecessary(it)
                    }
                }
                is ApiResult.Error -> Timber.e(placesResult.e)
            }

            Timber.d("Sync completed")
            scheduleNextSync()
        })
    }

    private fun scheduleNextSync() {
        Timber.d("Scheduling next sync")

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