package sync

import area.AreasRepo
import conf.ConfRepo
import element.ElementsRepo
import event.EventsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import reports.ReportsRepo
import time.now
import user.UsersRepo
import java.time.ZoneOffset
import java.time.ZonedDateTime

class Sync(
    private val areasRepo: AreasRepo,
    private val elementsRepo: ElementsRepo,
    private val reportsRepo: ReportsRepo,
    private val usersRepo: UsersRepo,
    private val eventsRepo: EventsRepo,
    private val conf: ConfRepo,
    private val syncNotificationController: SyncNotificationController,
) {
    private val mutex = Mutex()

    suspend fun sync(doNothingIfAlreadySyncing: Boolean) = withContext(Dispatchers.IO) {
        if (doNothingIfAlreadySyncing && mutex.isLocked) {
            return@withContext
        }

        mutex.withLock {
            runCatching {
                coroutineScope {
                    val startedAt = now()

                    if (elementsRepo.selectCount() == 0L && elementsRepo.hasBundledElements()) {
                        elementsRepo.fetchBundledElements()
                    }

                    if (reportsRepo.selectCount() == 0L && reportsRepo.hasBundledReports()) {
                        reportsRepo.fetchBundledReports()
                    }

                    if (eventsRepo.selectCount() == 0L && eventsRepo.hasBundledEvents()) {
                        eventsRepo.fetchBundledEvents()
                    }

                    if (usersRepo.selectCount() == 0L && usersRepo.hasBundledUsers()) {
                        usersRepo.fetchBundledUsers()
                    }

                    if (areasRepo.selectCount() == 0L && areasRepo.hasBundledAreas()) {
                        areasRepo.fetchBundledAreas()
                    }

                    val elementsReport = async { elementsRepo.sync() }
                    val reportsReport = async { reportsRepo.sync() }
                    val eventsReport = async { eventsRepo.sync() }
                    val areasReport = async { areasRepo.sync() }
                    val usersReport = async { usersRepo.sync() }

                    listOf(
                        elementsReport,
                        reportsReport,
                        areasReport,
                        usersReport,
                        eventsReport,
                    ).awaitAll()

                    val fullReport = SyncReport(
                        startedAt = startedAt,
                        finishedAt = ZonedDateTime.now(ZoneOffset.UTC),
                        elementsReport = elementsReport.await(),
                        reportsReport = reportsReport.await(),
                        eventsReport = eventsReport.await(),
                        areasReport = areasReport.await(),
                        usersReport = usersReport.await(),
                    )

                    syncNotificationController.showPostSyncNotifications(
                        report = fullReport,
                        conf = conf.current,
                    )
                }
            }.onSuccess {
                conf.update { it.copy(lastSyncDate = now()) }
            }.onFailure {
                syncNotificationController.showSyncFailedNotification(it)
            }
        }
    }
}

data class SyncReport(
    val startedAt: ZonedDateTime,
    val finishedAt: ZonedDateTime,
    val elementsReport: ElementsRepo.SyncReport,
    val reportsReport: ReportsRepo.SyncReport,
    val eventsReport: EventsRepo.SyncReport,
    val areasReport: AreasRepo.SyncReport,
    val usersReport: UsersRepo.SyncReport,
)