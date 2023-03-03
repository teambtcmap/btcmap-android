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
                val events = api.getReports(queries.selectMaxUpdatedAt(), BATCH_SIZE)
                count += events.size
                queries.insertOrReplace(events.map { it.toReport() })

                if (events.size < BATCH_SIZE) {
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