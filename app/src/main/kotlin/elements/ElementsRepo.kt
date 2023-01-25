package elements

import android.content.Context
import db.*
import http.await
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.osmdroid.util.BoundingBox
import java.time.ZonedDateTime

class ElementsRepo(
    private val context: Context,
    private val elementQueries: ElementQueries,
    private val json: Json,
    private val httpClient: OkHttpClient,
) {

    private val clustersCache = mutableMapOf<Double, List<ElementsCluster>>()

    suspend fun selectById(id: String) = elementQueries.selectById(id)

    suspend fun selectBySearchString(searchString: String): List<Element> {
        return elementQueries.selectBySearchString(searchString)
    }

    suspend fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ) = elementQueries.selectByBoundingBox(
        minLat,
        maxLat,
        minLon,
        maxLon,
    )

    suspend fun selectByBoundingBox(
        zoom: Double?,
        box: BoundingBox,
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
            elementQueries.selectWithoutClustering(
                minLat = box.latSouth,
                maxLat = box.latNorth,
                minLon = box.lonWest,
                maxLon = box.lonEast,
            )
        } else {
            val clusters = clustersCache.getOrPut(step) {
                elementQueries.selectClusters(step)
            }

            return withContext(Dispatchers.IO) {
                clusters.filter { box.contains(it.lat, it.lon) }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun fetchBundledElements(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()

            val rows = elementQueries.selectCount()

            if (rows > 0) {
                return@runCatching SyncReport(
                    timeMillis = System.currentTimeMillis() - startMillis,
                    createdOrUpdatedElements = 0,
                )
            }

            context.assets.open("elements.json").use { bundledElements ->
                withContext(Dispatchers.IO) {
                    var count = 0L

                    json.decodeToSequence(
                        stream = bundledElements,
                        deserializer = ElementJson.serializer(),
                    ).chunked(1_000).forEach { chunk ->
                        elementQueries.insertOrReplace(chunk.map { it.toElement() })
                        count += chunk.size
                        clustersCache.clear()
                    }

                    SyncReport(
                        timeMillis = System.currentTimeMillis() - startMillis,
                        createdOrUpdatedElements = count,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            return withContext(Dispatchers.IO) {
                val startMillis = System.currentTimeMillis()

                val maxUpdatedAt = elementQueries.selectMaxUpdatedAt()

                val url = HttpUrl.Builder().apply {
                    scheme("https")
                    host("api.btcmap.org")
                    addPathSegment("v2")
                    addPathSegment("elements")

                    if (maxUpdatedAt != null) {
                        addQueryParameter("updated_since", maxUpdatedAt.toString())
                    }
                }.build()

                val response = httpClient
                    .newCall(Request.Builder().url(url).build())
                    .await()

                if (!response.isSuccessful) {
                    throw Exception("Unexpected HTTP response code: ${response.code}")
                }

                response.body!!.byteStream().use { responseBody ->
                    withContext(Dispatchers.IO) {
                        var count = 0L

                        json.decodeToSequence(
                            stream = responseBody,
                            deserializer = ElementJson.serializer(),
                        ).chunked(1_000).forEach { chunk ->
                            elementQueries.insertOrReplace(chunk.map { it.toElement() })
                            count += chunk.size
                            clustersCache.clear()
                        }

                        Result.success(
                            SyncReport(
                                timeMillis = System.currentTimeMillis() - startMillis,
                                createdOrUpdatedElements = count,
                            )
                        )
                    }
                }
            }
        }
    }

    @Serializable
    private data class ElementJson(
        val id: String,
        val osm_json: JsonObject,
        val tags: JsonObject,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    private fun ElementJson.toElement(): Element {
        val latLon = getLatLon()

        return Element(
            id = id,
            lat = latLon.first,
            lon = latLon.second,
            osmJson = osm_json,
            tags = tags,
            createdAt = ZonedDateTime.parse(created_at),
            updatedAt = ZonedDateTime.parse(updated_at),
            deletedAt = if (deleted_at.isNotBlank()) ZonedDateTime.parse(deleted_at) else null,
        )
    }

    private fun ElementJson.getLatLon(): Pair<Double, Double> {
        val lat: Double
        val lon: Double

        if (osm_json["type"]!!.jsonPrimitive.content == "node") {
            lat = osm_json["lat"]!!.jsonPrimitive.double
            lon = osm_json["lon"]!!.jsonPrimitive.double
        } else {
            val bounds = osm_json["bounds"]!!.jsonObject

            val boundsMinLat = bounds["minlat"]!!.jsonPrimitive.double
            val boundsMinLon = bounds["minlon"]!!.jsonPrimitive.double
            val boundsMaxLat = bounds["maxlat"]!!.jsonPrimitive.double
            val boundsMaxLon = bounds["maxlon"]!!.jsonPrimitive.double

            lat = (boundsMinLat + boundsMaxLat) / 2.0
            lon = (boundsMinLon + boundsMaxLon) / 2.0
        }

        return Pair(lat, lon)
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedElements: Long,
    )
}