package element

import android.app.Application
import android.util.Log
import api.Api
import kotlinx.coroutines.*
import log.LogRecordQueries
import org.json.JSONObject
import org.osmdroid.util.BoundingBox

class ElementsRepo(
    private val api: Api,
    private val app: Application,
    private val queries: ElementQueries,
    private val logRecordQueries: LogRecordQueries,
) {

    suspend fun selectById(id: Long) = queries.selectById(id)

    suspend fun selectBySearchString(searchString: String): List<Element> {
        return queries.selectBySearchString(searchString)
    }

    suspend fun selectByCategory(category: String): List<Element> {
        return queries.selectByCategory(category)
    }

    suspend fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ) = queries.selectByBoundingBox(
        minLat,
        maxLat,
        minLon,
        maxLon,
    )

    suspend fun selectByBoundingBox(
        zoom: Double?,
        box: BoundingBox,
        excludedCategories: List<String>,
    ): List<ElementsCluster> {
        if (zoom == null) {
            return emptyList()
        }

        var step = 1.0

        if (zoom >= 1.0) {
            step = 35.0
        }

        if (zoom >= 2.0) {
            step = 25.0
        }

        if (zoom >= 3.0) {
            step = 17.0
        }

        if (zoom >= 4.0) {
            step = 13.0
        }

        if (zoom >= 5.0) {
            step = 6.5
        }

        if (zoom > 6.0) {
            step = 3.0
        }

        if (zoom > 7.0) {
            step = 1.5
        }

        if (zoom > 8.0) {
            step = 0.8
        }

        if (zoom > 9.0) {
            step = 0.4
        }

        if (zoom > 10.0) {
            step = 0.2
        }

        if (zoom > 11.0) {
            step = 0.1
        }

        if (zoom > 12.0) {
            step = 0.04
        }

        if (zoom > 13.0) {
            step = 0.02
        }

        if (zoom > 14.0) {
            step = 0.005
        }

        if (zoom > 15.0) {
            step = 0.003
        }

        if (zoom > 16.0) {
            step = 0.002
        }

        if (zoom > 17.0) {
            step = 0.001
        }

        return if (zoom > 18) {
            queries.selectWithoutClustering(
                minLat = box.latSouth,
                maxLat = box.latNorth,
                minLon = box.lonWest,
                maxLon = box.lonEast,
                excludedCategories,
            )
        } else {
            val clusters = queries.selectClusters(step, excludedCategories)

            return withContext(Dispatchers.IO) {
                clusters.filter { box.contains(it.lat, it.lon) }
            }
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) { queries.selectCount() }
    }

    suspend fun hasBundledElements(): Boolean {
        return withContext(Dispatchers.IO) {
            app.resources.assets.list("")!!.contains("elements.json")
        }
    }

    suspend fun fetchBundledElements(): Result<Unit> {
        return runCatching {
            val startMs = System.currentTimeMillis()

            app.assets.open("elements.json").use { bundledElements ->
                withContext(Dispatchers.IO) {
                    val elements = bundledElements.toElementsJson().map { it.toElement() }
                    queries.insertOrReplace(elements)

                    logRecordQueries.insert(
                        JSONObject(
                            mapOf(
                                "message" to "fetched bundled elements",
                                "count" to elements.size.toLong(),
                                "time_ms" to System.currentTimeMillis() - startMs,
                            )
                        )
                    )
                }
            }
        }
    }

    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()
            var count = 0L

            while (true) {
                Log.d("sync", "selectMaxUpdatedAt() = ${queries.selectMaxUpdatedAt()}")
                val elements = api.getElements(queries.selectMaxUpdatedAt(), BATCH_SIZE.toLong())
                count += elements.size
                queries.insertOrReplace(elements.map { it.toElement() })

                if (elements.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                timeMillis = System.currentTimeMillis() - startMillis,
                createdOrUpdatedElements = count,
            )
        }
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedElements: Long,
    )

    companion object {
        private const val BATCH_SIZE = 1000
    }
}