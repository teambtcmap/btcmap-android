package area

import api.Api
import java.time.ZonedDateTime

class AreasRepo(
    private val api: Api,
    private val queries: AreaQueries,
) {

    suspend fun selectById(id: String) = queries.selectById(id)

    suspend fun selectByType(type: String) = queries.selectByType(type)

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

    private fun AreaJson.toArea(): Area {
        return Area(
            id = id,
            tags = tags,
            createdAt = ZonedDateTime.parse(created_at),
            updatedAt = ZonedDateTime.parse(updated_at),
            deletedAt = if (deleted_at.isNotEmpty()) ZonedDateTime.parse(deleted_at) else null,
        )
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedAreas: Long,
    )

    companion object {
        private const val BATCH_SIZE = 50L
    }
}