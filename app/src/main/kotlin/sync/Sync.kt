package sync

import android.util.Log
import area.AreasRepo
import conf.ConfRepo
import reports.ReportsRepo
import element.ElementsRepo
import event.EventsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import user.UsersRepo
import java.time.ZoneOffset
import java.time.ZonedDateTime

class Sync(
    private val areasRepo: AreasRepo,
    private val confRepo: ConfRepo,
    private val elementsRepo: ElementsRepo,
    private val reportsRepo: ReportsRepo,
    private val usersRepo: UsersRepo,
    private val eventsRepo: EventsRepo,
) {

    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    suspend fun sync() {
        val startTime = System.currentTimeMillis()
        _active.update { true }
        Log.d(TAG, "Sync was requested")

        val lastSyncDateTime = confRepo.conf.value.lastSyncDate
        val minSyncIntervalExpiryDate = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(10)
        Log.d(TAG, "Last sync date: $lastSyncDateTime")
        Log.d(TAG, "Min sync interval expiry date: $minSyncIntervalExpiryDate")

        if (lastSyncDateTime != null && lastSyncDateTime.isAfter(minSyncIntervalExpiryDate)) {
            Log.d(TAG, "Cache is up to date, skipping sync")
            _active.update { false }
            return
        }

        runCatching {
            Log.d(TAG, "Fetching bundled elements")
            elementsRepo.fetchBundledElements()

            withContext(Dispatchers.Default) {
                listOf(
                    async { elementsRepo.sync() },
                    async { reportsRepo.sync() },
                    async {
                        areasRepo.sync().onSuccess {
                            Log.d(
                                TAG,
                                "Fetched ${it.createdOrUpdatedAreas} new or updated areas in ${it.timeMillis} ms"
                            )
                        }.onFailure {
                            Log.e(TAG, "Failed to fetch new or updated areas", it)
                        }
                    },
                    async {
                        usersRepo.sync().onSuccess {
                            Log.d(
                                TAG,
                                "Fetched ${it.createdOrUpdatedUsers} new or updated users in ${it.timeMillis} ms"
                            )
                        }.onFailure {
                            Log.e(TAG, "Failed to fetch new or updated users", it)
                        }
                    },
                    async {
                        eventsRepo.sync().onSuccess {
                            Log.d(
                                TAG,
                                "Fetched ${it.createdOrUpdatedEvents} new or updated events in ${it.timeMillis} ms"
                            )
                        }.onFailure {
                            Log.e(TAG, "Failed to fetch new or updated events", it)
                        }
                    },
                ).awaitAll()
            }
        }.onSuccess {
            Log.d(TAG, "Finished sync in ${System.currentTimeMillis() - startTime} ms")
            confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
            _active.update { false }
        }.onFailure {
            Log.e(TAG, "Sync failed", it)
            _active.update { false }
        }
    }

    companion object {
        private const val TAG = "sync"
    }
}