package element

import android.app.Application
import api.Api
import db.elementsUpdatedAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.pow

class ElementsRepo(
    private val api: Api,
    private val app: Application,
    private val queries: ElementQueries,
) {

    suspend fun selectById(id: Long): Element? {
        return withContext(Dispatchers.IO) {
            queries.selectById(id)
        }
    }

    suspend fun selectBySearchString(searchString: String): List<Element> {
        return withContext(Dispatchers.IO) {
            queries.selectBySearchString(searchString)
        }
    }

    suspend fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<Element> {
        return withContext(Dispatchers.IO) {
            queries.selectByBoundingBox(
                minLat,
                maxLat,
                minLon,
                maxLon,
            )
        }
    }

    suspend fun selectByBoundingBox(
        zoom: Double?,
        bounds: LatLngBounds,
    ): List<ElementsCluster> {
        if (zoom == null) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            if (zoom > 18) {
                withContext(Dispatchers.IO) {
                    queries.selectWithoutClustering(
                        minLat = bounds.latitudeSouth,
                        maxLat = bounds.latitudeNorth,
                        minLon = bounds.longitudeWest,
                        maxLon = bounds.longitudeEast,
                    )
                }
            } else {
                val step = 50.0 / 2.0.pow(zoom)
                withContext(Dispatchers.IO) {
                    val clusters = queries.selectClusters(
                        step / 2,
                        step,
                    )
                    clusters.filter { bounds.contains(LatLng(it.lat, it.lon)) }
                }
            }
        }
    }

    suspend fun selectCount(): Long {
        return withContext(Dispatchers.IO) {
            queries.selectCount()
        }
    }

    suspend fun hasBundledElements(): Boolean {
        return withContext(Dispatchers.IO) {
            app.resources.assets.list("")!!.contains("elements.json")
        }
    }

    suspend fun fetchBundledElements() {
        withContext(Dispatchers.IO) {
            app.assets.open("elements.json").use { bundledElements ->
                queries.insertOrReplace(bundledElements.toBundledElements().map { it.toElement() })
            }
        }
        elementsUpdatedAt.update { LocalDateTime.now() }
    }

    suspend fun sync(): SyncReport {
        return withContext(Dispatchers.IO) {
            val startedAt = ZonedDateTime.now(ZoneOffset.UTC)
            var newItems = 0L
            var updatedItems = 0L
            var deletedItems = 0L
            var maxKnownUpdatedAt = queries.selectMaxUpdatedAt()

            while (true) {
                val delta = api.getElements(maxKnownUpdatedAt, BATCH_SIZE)

                if (delta.isEmpty()) {
                    break
                } else {
                    maxKnownUpdatedAt = delta.maxBy { it.updatedAt }.updatedAt
                }

                delta.forEach {
                    val cached = queries.selectById(it.id)

                    if (it.deletedAt == null) {
                        if (cached == null) {
                            newItems++
                        } else {
                            updatedItems++
                        }

                        queries.insertOrReplace(listOf(it))
                    } else {
                        if (cached == null) {
                            // Already evicted from cache, nothing to do here
                        } else {
                            queries.deleteById(it.id)
                            deletedItems++
                        }
                    }
                }

                elementsUpdatedAt.update { LocalDateTime.now() }

                if (delta.size < BATCH_SIZE) {
                    break
                }
            }

            SyncReport(
                duration = Duration.between(startedAt, ZonedDateTime.now(ZoneOffset.UTC)),
                newElements = newItems,
                updatedElements = updatedItems,
                deletedElements = deletedItems,
            )
        }
    }

    data class SyncReport(
        val duration: Duration,
        val newElements: Long,
        val updatedElements: Long,
        val deletedElements: Long,
    )

    companion object {
        private const val BATCH_SIZE = 10_000L
    }
}