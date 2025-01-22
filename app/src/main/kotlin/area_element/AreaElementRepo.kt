package area_element

import android.app.Application
import android.util.Log
import api.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AreaElementRepo(
    private val api: Api,
    private val app: Application,
    private val queries: AreaElementQueries,
) {

    suspend fun selectByAreaId(areaId: Long): List<AreaElement> {
        return withContext(Dispatchers.IO) {
            queries.selectByAreaId(areaId)
        }
    }

    suspend fun selectByElementId(elementId: Long): List<AreaElement> {
        return withContext(Dispatchers.IO) {
            queries.selectByElementId(elementId)
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) {
            queries.selectCount()
        }
    }

    suspend fun hasBundledAreaElements(): Boolean {
        return withContext(Dispatchers.IO) {
            val res = app.resources.assets.list("")!!.contains("area-elements.json")
            Log.d("AreaElementRepo", "hasBundledAreaElements = $res")
            res
        }
    }

    suspend fun fetchBundledAreaElements() {
        withContext(Dispatchers.IO) {
            app.assets.open("area-elements.json").use { bundledAreaElements ->
                val areaElements = bundledAreaElements.toAreaElementsJson()
                Log.d("AreaElementRepo", "Loaded ${areaElements.size} bundled area elements")
                queries.insertOrReplace(areaElements
                    .filter { it.deletedAt == null }
                    .map { it.toAreaElement() })
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
                val delta = api.getAreaElements(maxKnownUpdatedAt, BATCH_SIZE)

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

                        queries.insertOrReplace(listOf(it.toAreaElement()))
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
                newAreaElements = newItems,
                updatedAreaElements = updatedItems,
                deletedAreaElements = deletedItems,
            )
        }
    }

    data class SyncReport(
        val duration: Duration,
        val newAreaElements: Long,
        val updatedAreaElements: Long,
        val deletedAreaElements: Long,
    )

    companion object {
        private const val BATCH_SIZE = 10_000L
    }
}