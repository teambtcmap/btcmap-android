package sync

import android.util.Log
import area.AreasRepo
import area_element.AreaElementRepo
import conf.ConfRepo
import element.ElementsRepo
import element_comment.ElementCommentRepo
import event.EventsRepo
import kotlinx.coroutines.Deferred
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
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class Sync(
    private val areasRepo: AreasRepo,
    private val elementCommentRepo: ElementCommentRepo,
    private val elementsRepo: ElementsRepo,
    private val reportsRepo: ReportsRepo,
    private val usersRepo: UsersRepo,
    private val eventsRepo: EventsRepo,
    private val areaElementRepo: AreaElementRepo,
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

//                    if (areaElementRepo.selectCount() == 0L && areaElementRepo.hasBundledAreaElements()) {
//                        areaElementRepo.fetchBundledAreaElements()
//                    }

//                    if (elementCommentRepo.selectCount() == 0L && elementCommentRepo.hasBundledElements()) {
//                        elementCommentRepo.fetchBundledElements()
//                    }

                    if (elementsRepo.selectCount() == 0L && elementsRepo.hasBundledElements()) {
                        elementsRepo.fetchBundledElements()
                    }

//                    if (reportsRepo.selectCount() == 0L && reportsRepo.hasBundledReports()) {
//                        reportsRepo.fetchBundledReports()
//                    }

//                    if (eventsRepo.selectCount() == 0L && eventsRepo.hasBundledEvents()) {
//                        eventsRepo.fetchBundledEvents()
//                    }

//                    if (usersRepo.selectCount() == 0L && usersRepo.hasBundledUsers()) {
//                        usersRepo.fetchBundledUsers()
//                    }

//                    if (areasRepo.selectCount() == 0L && areasRepo.hasBundledAreas()) {
//                        areasRepo.fetchBundledAreas()
//                    }

//                    val syncJobs = mutableListOf<Deferred<Any>>()
//
//                    val areaElementsReport =
//                        async { areaElementRepo.sync() }.also { syncJobs += it }
//                    areaElementsReport.await()
//                    val elementCommentReport =
//                        async { elementCommentRepo.sync() }.also { syncJobs += it }
//                    elementCommentReport.await()
                    val elementsReport = elementsRepo.sync()
                    val elementCommentReport = elementCommentRepo.sync()
//                    elementsReport.await()
//                    val reportsReport = async { reportsRepo.sync() }.also { syncJobs += it }
//                    reportsReport.await()
//                    val eventsReport = async { eventsRepo.sync() }.also { syncJobs += it }
//                    eventsReport.await()
//                    val areasReport = async { areasRepo.sync() }.also { syncJobs += it }
//                    areasReport.await()
//                    val usersReport = async { usersRepo.sync() }.also { syncJobs += it }
//                    usersReport.await()
//
//                    syncJobs.awaitAll()

                    val fullReport = SyncReport(
                        startedAt = startedAt,
                        finishedAt = ZonedDateTime.now(ZoneOffset.UTC),
                        elementsReport = elementsReport,
                        reportsReport = ReportsRepo.SyncReport(
                            duration = Duration.ofSeconds(0),
                            newReports = 0,
                            updatedReports = 0,
                            deletedReports = 0,
                        ),
                        eventsReport = EventsRepo.SyncReport(
                            duration = Duration.ofSeconds(0),
                            newEvents = emptyList(),
                            updatedEvents = 0,
                            deletedEvents = 0,
                        ),
                        areasReport = AreasRepo.SyncReport(
                            duration = Duration.ofSeconds(0),
                            newAreas = 0,
                            updatedAreas = 0,
                            deletedAreas = 0,
                        ),
                        usersReport = UsersRepo.SyncReport(
                            duration = Duration.ofSeconds(0),
                            newUsers = 0,
                            updatedUsers = 0,
                            deletedUsers = 0,
                        ),
                        elementCommentReport = elementCommentReport,
                        areaElementsReport = AreaElementRepo.SyncReport(
                            duration = Duration.ofSeconds(0),
                            newAreaElements = 0,
                            updatedAreaElements = 0,
                            deletedAreaElements = 0,
                        ),
                    )
                    Log.d("sync-reports", fullReport.toString())
                    syncNotificationController.showPostSyncNotifications(
                        report = fullReport,
                        conf = conf.current,
                    )
                }
            }.onSuccess {
                conf.update { it.copy(lastSyncDate = now()) }
            }.onFailure {
                it.printStackTrace()
                syncNotificationController.showSyncFailedNotification(it)
            }
        }
    }
}

data class SyncReport(
    val startedAt: ZonedDateTime,
    val finishedAt: ZonedDateTime,
    val elementCommentReport: ElementCommentRepo.SyncReport,
    val elementsReport: ElementsRepo.SyncReport,
    val reportsReport: ReportsRepo.SyncReport,
    val eventsReport: EventsRepo.SyncReport,
    val areasReport: AreasRepo.SyncReport,
    val usersReport: UsersRepo.SyncReport,
    val areaElementsReport: AreaElementRepo.SyncReport,
)