package reports

import api.Api

class ReportsRepo(
    private val api: Api,
    private val queries: ReportQueries,
) {

    suspend fun selectByAreaId(areaId: String) = queries.selectByAreaId(areaId)

    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()
            var count = 0L

            while (true) {
                val reports = api.getReports(queries.selectMaxUpdatedAt(), BATCH_SIZE)
                count += reports.size
                queries.insertOrReplace(reports.map { it.toReport() })

                if (reports.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                timeMillis = System.currentTimeMillis() - startMillis,
                createdOrUpdatedReports = count,
            )
        }
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedReports: Long,
    )

    companion object {
        private const val BATCH_SIZE = 5000L
    }
}