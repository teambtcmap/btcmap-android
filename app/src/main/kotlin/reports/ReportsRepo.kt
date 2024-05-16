package reports

import android.content.Context
import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ReportsRepo(
    private val api: Api,
    private val queries: ReportQueries,
    private val context: Context,
) {

    suspend fun selectByAreaId(areaId: Long): List<Report> {
        return withContext(Dispatchers.IO) {
            queries.selectByAreaId(areaId)
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) {
            queries.selectCount()
        }
    }

    suspend fun hasBundledReports(): Boolean {
        return withContext(Dispatchers.IO) {
            context.resources.assets.list("")!!.contains("reports.json")
        }
    }

    suspend fun fetchBundledReports() {
        withContext(Dispatchers.IO) {
            context.assets.open("reports.json").use { bundledReports ->
                queries.insertOrReplace(
                    bundledReports
                        .toReportsJson()
                        .filter { it.deletedAt == null }
                        .map { it.toReport() }
                )
            }
        }
    }

    suspend fun sync(): SyncReport {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var newItems = 0L
            var updatedItems = 0L
            var deletedItems = 0L
            var maxKnownUpdatedAt = queries.selectMaxUpdatedAt()

            while (true) {
                val delta = api.getReports(maxKnownUpdatedAt, BATCH_SIZE)

                if (delta.isEmpty()) {
                    break
                } else {
                    maxKnownUpdatedAt = ZonedDateTime.parse(delta.maxBy { it.updatedAt }.updatedAt)
                }

                delta.forEach {
                    val cached = queries.selectById(it.id)

                    if (it.deletedAt == null) {
                        if (cached == null) {
                            newItems++
                        } else {
                            updatedItems++
                        }

                        queries.insertOrReplace(it.toReport())
                    } else {
                        if (cached == null) {
                            // Already evicted from cache, nothing to do here
                        } else {
                            queries.deleteById(it.id)
                            deletedItems++
                        }
                    }
                }

                if (delta.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                newReports = newItems,
                updatedReports = updatedItems,
                deletedReports = deletedItems,
            )
        }
    }

    data class SyncReport(
        val duration: Duration,
        val newReports: Long,
        val updatedReports: Long,
        val deletedReports: Long,
    )

    companion object {
        private const val BATCH_SIZE = 5000L
    }
}