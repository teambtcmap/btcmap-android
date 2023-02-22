package element

import android.app.Application
import api.Api
import db.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.osmdroid.util.BoundingBox

class ElementsRepo(
    private val api: Api,
    private val app: Application,
    private val queries: ElementQueries,
    private val json: Json,
) {

    suspend fun selectById(id: String) = queries.selectById(id)

    suspend fun selectBySearchString(searchString: String): List<Element> {
        return queries.selectBySearchString(searchString)
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

    suspend fun selectCategories() = queries.selectCategories()

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun fetchBundledElements(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()

            val rows = queries.selectCount()

            if (rows > 0) {
                return@runCatching SyncReport(
                    timeMillis = System.currentTimeMillis() - startMillis,
                    createdOrUpdatedElements = 0,
                )
            }

            app.assets.open("elements.json").use { bundledElements ->
                withContext(Dispatchers.IO) {
                    var count = 0L

                    json.decodeToSequence(
                        stream = bundledElements,
                        deserializer = ElementJson.serializer(),
                    ).chunked(BATCH_SIZE).forEach { chunk ->
                        queries.insertOrReplace(chunk.map { it.toElement() })
                        count += chunk.size
                    }

                    SyncReport(
                        timeMillis = System.currentTimeMillis() - startMillis,
                        createdOrUpdatedElements = count,
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