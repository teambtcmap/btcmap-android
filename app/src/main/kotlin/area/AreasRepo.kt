package area

import api.Api

class AreasRepo(
    private val api: Api,
    private val queries: AreaQueries,
) {

    suspend fun selectById(id: String) = queries.selectById(id)

    suspend fun selectByType(type: String) = queries.selectByType(type)

    suspend fun selectMeetups() = queries.selectMeetups()

    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()
            var count = 0L

            while (true) {
                val areas = api.getAreas(queries.selectMaxUpdatedAt(), BATCH_SIZE)
                count += areas.size
                queries.insertOrReplace(areas.map { it.toArea() })

                if (areas.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                timeMillis = System.currentTimeMillis() - startMillis,
                createdOrUpdatedAreas = count,
            )
        }
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedAreas: Long,
    )

    companion object {
        private const val BATCH_SIZE = 100L
    }
}