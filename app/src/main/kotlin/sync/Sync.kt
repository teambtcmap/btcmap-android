package sync

import android.util.Log
import element.ElementsRepo
import element_comment.ElementCommentRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import time.now
import java.time.ZoneOffset
import java.time.ZonedDateTime

class Sync(
    private val elementsRepo: ElementsRepo,
    private val elementCommentRepo: ElementCommentRepo,
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

                    val elementsReport = elementsRepo.sync()
                    val elementCommentReport = elementCommentRepo.sync()

                    val fullReport = SyncReport(
                        startedAt = startedAt,
                        finishedAt = ZonedDateTime.now(ZoneOffset.UTC),
                        elementsReport = elementsReport,
                        elementCommentReport = elementCommentReport,
                    )
                    Log.d("sync-reports", fullReport.toString())
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}

data class SyncReport(
    val startedAt: ZonedDateTime,
    val finishedAt: ZonedDateTime,
    val elementsReport: ElementsRepo.SyncReport,
    val elementCommentReport: ElementCommentRepo.SyncReport,
)