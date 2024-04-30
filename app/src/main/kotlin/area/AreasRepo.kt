package area

import android.content.Context
import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AreasRepo(
    private val api: Api,
    private val queries: AreaQueries,
    private val context: Context,
) {

    suspend fun selectById(id: String) = queries.selectById(id)

    suspend fun selectByType(type: String) = queries.selectByType(type)

    suspend fun selectMeetups() = queries.selectMeetups()

    suspend fun selectCount() = queries.selectCount()

    suspend fun hasBundledAreas(): Boolean {
        return withContext(Dispatchers.IO) {
            context.resources.assets.list("")!!.contains("areas.json")
        }
    }

    suspend fun fetchBundledAreas() {
        withContext(Dispatchers.IO) {
            context.assets.open("areas.json").use { bundledAreas ->
                val areas = bundledAreas.toAreasJson().map { it.toArea() }
                queries.insertOrReplace(areas)
            }
        }
    }

    suspend fun sync(): SyncReport {
        val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
        var newAreas = 0L
        var updatedAreas = 0L
        val maxUpdatedAtBeforeSync = queries.selectMaxUpdatedAt()

        while (true) {
            val areas = api.getAreas(queries.selectMaxUpdatedAt(), BATCH_SIZE).map { it.toArea() }

            areas.forEach {
                if (maxUpdatedAtBeforeSync == null
                    || it.createdAt.isAfter(maxUpdatedAtBeforeSync)
                ) {
                    newAreas += 1
                } else {
                    updatedAreas += 1
                }
            }

            queries.insertOrReplace(areas)

            if (areas.size < BATCH_SIZE) {
                break
            }
        }

        return SyncReport(
            duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
            newAreas = newAreas,
            updatedAreas = updatedAreas,
        )
    }

    data class SyncReport(
        val duration: Duration,
        val newAreas: Long,
        val updatedAreas: Long,
    )

    companion object {
        private const val BATCH_SIZE = 100L
    }
}