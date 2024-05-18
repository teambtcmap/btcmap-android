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

    suspend fun selectById(id: Long): Area? {
        return withContext(Dispatchers.IO) {
            queries.selectById(id)
        }
    }

    suspend fun selectByType(type: String): List<Area> {
        return withContext(Dispatchers.IO) {
            queries.selectByType(type)
        }
    }

    suspend fun selectMeetups(): List<Meetup> {
        return withContext(Dispatchers.IO) {
            queries.selectMeetups()
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) {
            queries.selectCount()
        }
    }

    suspend fun hasBundledAreas(): Boolean {
        return withContext(Dispatchers.IO) {
            context.resources.assets.list("")!!.contains("areas.json")
        }
    }

    suspend fun fetchBundledAreas() {
        withContext(Dispatchers.IO) {
            context.assets.open("areas.json").use { bundledAreas ->
                queries.insertOrReplace(
                    bundledAreas
                        .toAreasJson()
                        .filter { it.deletedAt == null }
                        .map { it.toArea() }
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
                val delta = api.getAreas(maxKnownUpdatedAt, BATCH_SIZE)

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

                        queries.insertOrReplace(listOf(it.toArea()))
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
                newAreas = newItems,
                updatedAreas = updatedItems,
                deletedAreas = deletedItems,
            )
        }
    }

    data class SyncReport(
        val duration: Duration,
        val newAreas: Long,
        val updatedAreas: Long,
        val deletedAreas: Long,
    )

    companion object {
        private const val BATCH_SIZE = 100L
    }
}