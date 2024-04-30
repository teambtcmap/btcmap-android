package area

import android.content.Context
import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

        return SyncReport(
            timeMillis = System.currentTimeMillis() - startMillis,
            createdOrUpdatedAreas = count,
        )
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedAreas: Long,
    )

    companion object {
        private const val BATCH_SIZE = 100L
    }
}