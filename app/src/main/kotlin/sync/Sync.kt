package sync

import android.util.Log
import area.AreasRepo
import conf.ConfRepo
import reports.ReportsRepo
import element.ElementsRepo
import event.Event
import event.EventsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import user.UsersRepo
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class Sync(
    private val areasRepo: AreasRepo,
    private val confRepo: ConfRepo,
    private val elementsRepo: ElementsRepo,
    private val reportsRepo: ReportsRepo,
    private val usersRepo: UsersRepo,
    private val eventsRepo: EventsRepo,
    private val syncNotificationController: SyncNotificationController,
) {

    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    suspend fun sync() {
        _active.update { true }
        val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
        val lastSyncFinishedAt = confRepo.conf.value.lastSyncDate

        if (lastSyncFinishedAt != null
            && Duration.between(lastSyncFinishedAt, startedAt).toMinutes() < 1
        ) {
            _active.update { false }
            return
        }

        var syncReport: SyncReport? = null

        runCatching {
            withContext(Dispatchers.IO) {
                if (elementsRepo.selectCount() == 0L && elementsRepo.hasBundledElements()) {
                    elementsRepo.fetchBundledElements().getOrThrow()
                }
            }

            withContext(Dispatchers.IO) {
                val elementsReport = async { elementsRepo.sync().getOrThrow() }
                val reportsReport = async { reportsRepo.sync().getOrThrow() }
                val areasReport = async { areasRepo.sync().getOrThrow() }
                val usersReport = async { usersRepo.sync().getOrThrow() }
                val eventsReport = async { eventsRepo.sync().getOrThrow() }

                listOf(
                    elementsReport,
                    reportsReport,
                    areasReport,
                    usersReport,
                    eventsReport,
                ).awaitAll()

                syncReport = SyncReport(
                    startedAt = startedAt,
                    finishedAt = ZonedDateTime.now(ZoneOffset.UTC),
                    newElements = elementsReport.await().newElements,
                    updatedElements = elementsReport.await().updatedElements,
                    newEvents = eventsReport.await().newEvents,
                    updatedEvents = eventsReport.await().updatedEvents,
                )
            }
        }.onSuccess {
            syncNotificationController.showPostSyncNotifications(
                report = syncReport!!,
                conf = confRepo.conf.value,
            )
            confRepo.update { it.copy(lastSyncDate = ZonedDateTime.now(ZoneOffset.UTC)) }
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

data class SyncReport(
    val startedAt: ZonedDateTime,
    val finishedAt: ZonedDateTime,
    val newElements: Long,
    val updatedElements: Long,
    val newEvents: List<Event>,
    val updatedEvents: List<Event>,
)