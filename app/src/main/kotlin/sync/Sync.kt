package sync

import android.util.Log
import areas.AreasRepo
import conf.ConfRepo
import reports.ReportsRepo
import elements.ElementsRepo
import events.EventsRepo
import org.koin.core.annotation.Single
import users.UsersRepo
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Single
class Sync(
    private val areasRepo: AreasRepo,
    private val confRepo: ConfRepo,
    private val elementsRepo: ElementsRepo,
    private val reportsRepo: ReportsRepo,
    private val usersRepo: UsersRepo,
    private val eventsRepo: EventsRepo,
) {

    suspend fun sync() {
        Log.d(TAG, "Sync was requested")

        val lastSyncDateTime = confRepo.conf.value.lastSyncDate
        val minSyncIntervalExpiryDate = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(10)
        Log.d(TAG, "Last sync date: $lastSyncDateTime")
        Log.d(TAG, "Min sync interval expiry date: $minSyncIntervalExpiryDate")

        if (lastSyncDateTime != null && lastSyncDateTime.isAfter(minSyncIntervalExpiryDate)) {
            Log.d(TAG, "Cache is up to date, skipping sync")
            return
        }

        Log.d(TAG, "Fetching bundled elements")

        elementsRepo.fetchBundledElements().onSuccess {
            Log.d(
                TAG,
                "Fetched ${it.createdOrUpdatedElements} bundled elements in ${it.timeMillis} ms"
            )
        }.onFailure {
            Log.e(TAG, "Failed to fetch bundled elements", it)
        }

        Log.d(TAG, "Syncing elements")

        elementsRepo.sync().onSuccess {
            Log.d(
                TAG,
                "Fetched ${it.createdOrUpdatedElements} new or updated elements in ${it.timeMillis} ms"
            )
        }.onFailure {
            Log.e(TAG, "Failed to fetch new or updated elements", it)
        }

        runCatching {
            Log.d(TAG, "Syncing reports")
            reportsRepo.sync()
        }.onSuccess {
            Log.d(TAG, "Synced reports")
        }.onFailure {
            Log.d(TAG, "Failed to sync reports")
        }

        Log.d(TAG, "Syncing areas")

        areasRepo.sync().onSuccess {
            Log.d(
                TAG,
                "Fetched ${it.createdOrUpdatedAreas} new or updated areas in ${it.timeMillis} ms"
            )
        }.onFailure {
            Log.e(TAG, "Failed to fetch new or updated areas", it)
        }

        Log.d(TAG, "Syncing users")

        usersRepo.sync().onSuccess {
            Log.d(
                TAG,
                "Fetched ${it.createdOrUpdatedUsers} new or updated users in ${it.timeMillis} ms"
            )
        }.onFailure {
            Log.e(TAG, "Failed to fetch new or updated users", it)
        }

        Log.d(TAG, "Syncing events")

        eventsRepo.sync().onSuccess {
            Log.d(
                TAG,
                "Fetched ${it.createdOrUpdatedElements} new or updated events in ${it.timeMillis} ms"
            )
        }.onFailure {
            Log.e(TAG, "Failed to fetch new or updated events", it)
        }

        confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
    }

    companion object {
        private const val TAG = "sync"
    }
}