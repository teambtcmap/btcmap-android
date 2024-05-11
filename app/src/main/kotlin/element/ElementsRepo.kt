package element

import android.app.Application
import androidx.sqlite.db.transaction
import api.Api
import db.elementsUpdatedAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ElementsRepo(
    private val api: Api,
    private val app: Application,
    private val queries: ElementQueries,
) {

    suspend fun selectById(id: Long) = withContext(Dispatchers.IO) { queries.selectById(id) }

    suspend fun selectBySearchString(searchString: String): List<Element> {
        return withContext(Dispatchers.IO) { queries.selectBySearchString(searchString) }
    }

    suspend fun selectByCategory(category: String): List<Element> {
        return withContext(Dispatchers.IO) { queries.selectByCategory(category) }
    }

    suspend fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ) = withContext(Dispatchers.IO) {
        queries.selectByBoundingBox(
            minLat,
            maxLat,
            minLon,
            maxLon,
        )
    }

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
            withContext(Dispatchers.IO) {
                queries.selectWithoutClustering(
                    minLat = box.latSouth,
                    maxLat = box.latNorth,
                    minLon = box.lonWest,
                    maxLon = box.lonEast,
                    excludedCategories,
                )
            }
        } else {
            return withContext(Dispatchers.IO) {
                val clusters = queries.selectClusters(step, excludedCategories)
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

    suspend fun fetchBundledElements() {
        withContext(Dispatchers.IO) {
            app.assets.open("elements.json").use { bundledElements ->
                val elements = bundledElements
                    .toElementsJson()
                    .filter { it.deletedAt == null }
                    .map { it.toElement() }

                queries.db.writableDatabase.transaction {
                    elements.forEach { queries.insertOrReplace(it) }
                }
            }
        }
    }

    suspend fun sync(): SyncReport {
        val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
        var newItems = 0L
        var updatedItems = 0L
        var deletedItems = 0L
        var maxKnownUpdatedAt = withContext(Dispatchers.IO) { queries.selectMaxUpdatedAt() }

        while (true) {
            val delta = api.getElements(maxKnownUpdatedAt, BATCH_SIZE)

            if (delta.isEmpty()) {
                break
            } else {
                maxKnownUpdatedAt = ZonedDateTime.parse(delta.maxBy { it.updatedAt }.updatedAt)
            }

            withContext(Dispatchers.IO) {
                queries.db.writableDatabase.transaction {
                    delta.forEach {
                        val cached = queries.selectById(it.id)

                        if (it.deletedAt == null) {
                            if (cached == null) {
                                newItems++
                            } else {
                                updatedItems++
                            }

                            queries.insertOrReplace(it.toElement())
                        } else {
                            if (cached == null) {
                                // Already evicted from cache, nothing to do here
                            } else {
                                queries.deleteById(it.id)
                                deletedItems++
                            }
                        }
                    }
                }
            }

            elementsUpdatedAt.update { LocalDateTime.now() }

            if (delta.size < BATCH_SIZE) {
                break
            }
        }

        return SyncReport(
            duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
            newElements = newItems,
            updatedElements = updatedItems,
            deletedElements = deletedItems,
        )
    }

    data class SyncReport(
        val duration: Duration,
        val newElements: Long,
        val updatedElements: Long,
        val deletedElements: Long,
    )

    companion object {
        private const val BATCH_SIZE = 1000L
    }
}