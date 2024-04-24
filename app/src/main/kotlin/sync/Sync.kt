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
import log.LogRecordQueries
import org.json.JSONObject
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
    private val logRecordQueries: LogRecordQueries,
    private val syncNotificationController: SyncNotificationController,
) {

    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    suspend fun sync() {
        val startTime = System.currentTimeMillis()
        _active.update { true }
        Log.d(TAG, "Sync was requested")

        val lastSyncDateTime = confRepo.conf.value.lastSyncDate
        val initialSyncComplete = lastSyncDateTime != null
        val minSyncIntervalExpiryDate = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(10)
        Log.d(TAG, "Last sync date: $lastSyncDateTime")
        Log.d(TAG, "Min sync interval expiry date: $minSyncIntervalExpiryDate")

        if (lastSyncDateTime != null && lastSyncDateTime.isAfter(minSyncIntervalExpiryDate)) {
            Log.d(TAG, "Cache is up to date, skipping sync")
            _active.update { false }
            return
        }

        var eventsSyncReport: EventsRepo.SyncReport? = null

        runCatching {
            withContext(Dispatchers.IO) {
                logRecordQueries.insert(JSONObject(mapOf("message" to "started sync")))

                if (elementsRepo.selectCount() == 0L && elementsRepo.hasBundledElements()) {
                    elementsRepo.fetchBundledElements().getOrThrow()
                }
            }

            withContext(Dispatchers.IO) {
                listOf(
                    async { elementsRepo.sync().getOrThrow() },
                    async { reportsRepo.sync().getOrThrow() },
                    async { areasRepo.sync().getOrThrow() },
                    async { usersRepo.sync().getOrThrow() },
                    async { eventsSyncReport = eventsRepo.sync().getOrThrow() },
                ).awaitAll()
            }
        }.onSuccess {
            val syncTimeMs = System.currentTimeMillis() - startTime
            Log.d(TAG, "Finished sync in $syncTimeMs ms")
            confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
            if (initialSyncComplete) {
                syncNotificationController.showPostSyncNotifications(
                    syncTimeMs,
                    eventsSyncReport?.newEvents ?: emptyList(),
                )
            }
            _active.update { false }
        }.onFailure {
            Log.e(TAG, "Sync failed", it)
            syncNotificationController.showSyncFailedNotification(it)
            _active.update { false }
        }
    }

    companion object {
        private const val TAG = "sync"
    }
}