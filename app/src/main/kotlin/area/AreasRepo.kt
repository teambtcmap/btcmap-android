package area

import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val maxUpdatedAt = queries.selectMaxUpdatedAt()

            withContext(Dispatchers.IO) {
                var count = 0L

                api.getAreas(maxUpdatedAt).chunked(1_000).forEach { chunk ->
                    queries.insertOrReplace(chunk.filter { it.valid() }.map { it.toArea() })
                    count += chunk.size
                }

                SyncReport(
                    timeMillis = System.currentTimeMillis() - startMillis,
                    createdOrUpdatedAreas = count,
                )
            }
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
}