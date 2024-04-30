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

    suspend fun selectByAreaId(areaId: String) = queries.selectByAreaId(areaId)

    suspend fun selectCount() = queries.selectCount()

    suspend fun hasBundledReports(): Boolean {
        return withContext(Dispatchers.IO) {
            context.resources.assets.list("")!!.contains("reports.json")
        }
    }

    suspend fun fetchBundledReports() {
        withContext(Dispatchers.IO) {
            context.assets.open("reports.json").use { bundledReports ->
                val reports = bundledReports.toReportsJson().map { it.toReport() }
                queries.insertOrReplace(reports)
            }
        }
    }

    suspend fun sync(): SyncReport {
        val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
        val newReports = mutableListOf<Report>()
        val updatedReports = mutableListOf<Report>()
        val maxUpdatedAtBeforeSync = queries.selectMaxUpdatedAt()

        while (true) {
            val reports =
                api.getReports(queries.selectMaxUpdatedAt(), BATCH_SIZE).map { it.toReport() }

            reports.forEach {
                if (maxUpdatedAtBeforeSync == null
                    || it.createdAt.isAfter(maxUpdatedAtBeforeSync)
                ) {
                    newReports += it
                } else {
                    updatedReports += it
                }
            }

            queries.insertOrReplace(reports)

            if (reports.size < BATCH_SIZE) {
                break
            }
        }

        return SyncReport(
            duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
            newReports = newReports,
            updatedReports = updatedReports,
        )
    }

    data class SyncReport(
        val duration: Duration,
        val newReports: List<Report>,
        val updatedReports: List<Report>,
    )

    companion object {
        private const val BATCH_SIZE = 5000L
    }
}