package elements

import android.content.Context
import app.cash.sqldelight.coroutines.*
import db.*
import http.await
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import org.osmdroid.util.BoundingBox

@Single
class ElementsRepo(
    private val context: Context,
    private val db: Database,
    private val json: Json,
    private val httpClient: OkHttpClient,
) {

    private val clustersCache = mutableMapOf<Double, List<SelectElementClusters>>()

    init {
        GlobalScope.launch {
            db.elementQueries
                .selectCount()
                .asFlow()
                .mapToOne(Dispatchers.IO)
                .collect { clustersCache.clear() }
        }
    }

    suspend fun selectById(id: String): Element? {
        return db.elementQueries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .first()
    }

    suspend fun selectByBoundingBox(
        zoom: Double?,
        box: BoundingBox,
    ): List<SelectElementClusters> {
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
            val pins = db.elementQueries.selectElementsAsPinsByBoundingBox(
                minLat = box.latSouth,
                maxLat = box.latNorth,
                minLon = box.lonWest,
                maxLon = box.lonEast,
            )
                .asFlow()
                .mapToList(Dispatchers.IO)
                .first()

            withContext(Dispatchers.IO) {
                pins.map {
                    SelectElementClusters(
                        count = 1,
                        id = it.id,
                        lat = it.lat,
                        lon = it.lon,
                        icon_id = it.icon_id,
                    )
                }
            }
        } else {
            val clusters = clustersCache.getOrPut(step) {
                db.elementQueries
                    .selectElementClusters(step = step)
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .first()
            }

            return withContext(Dispatchers.IO) {
                clusters.filter { box.contains(it.lat!!, it.lon!!) }
            }
        }
    }

    suspend fun selectElementIdIconAndTags(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<SelectElementIdIconAndTags> {
        return db.elementQueries.selectElementIdIconAndTags(
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon,
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun fetchBundledElements(): Result<SyncReport> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val startMillis = System.currentTimeMillis()

                val rows = db.elementQueries
                    .selectCount()
                    .asFlow()
                    .mapToOne(Dispatchers.IO)
                    .first()

                if (rows > 0) {
                    return@withContext SyncReport(
                        timeMillis = System.currentTimeMillis() - startMillis,
                        createdOrUpdatedElements = 0,
                    )
                }

                context.assets.open("elements.json").use { inputStream ->
                    val elements = json.decodeToSequence(
                        stream = inputStream,
                        deserializer = ElementJson.serializer(),
                    )

                    val createdOrUpdatedElements = elements
                        .chunked(1_000)
                        .map { db.insertOrReplace(it) }
                        .sum()

                    SyncReport(
                        timeMillis = System.currentTimeMillis() - startMillis,
                        createdOrUpdatedElements = createdOrUpdatedElements,
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

                val maxUpdatedAt = db.elementQueries
                    .selectMaxUpdatedAt()
                    .asFlow()
                    .mapToOneNotNull(Dispatchers.IO)
                    .firstOrNull()
                    ?.max

                val url = HttpUrl.Builder().apply {
                    scheme("https")
                    host("api.btcmap.org")
                    addPathSegment("v2")
                    addPathSegment("elements")

                    if (!maxUpdatedAt.isNullOrBlank()) {
                        addQueryParameter("updated_since", maxUpdatedAt)
                    }
                }.build()

                val response = httpClient
                    .newCall(Request.Builder().url(url).build())
                    .await()

                if (!response.isSuccessful) {
                    throw Exception("Unexpected HTTP response code: ${response.code}")
                }

                response.body!!.byteStream().use { inputStream ->
                    val elements = json.decodeToSequence(
                        stream = inputStream,
                        deserializer = ElementJson.serializer(),
                    )

                    val createdOrUpdatedElements = elements
                        .chunked(1_000)
                        .map { db.insertOrReplace(it) }
                        .sum()

                    Result.success(
                        SyncReport(
                            timeMillis = System.currentTimeMillis() - startMillis,
                            createdOrUpdatedElements = createdOrUpdatedElements,
                        )
                    )
                }
            }
        }
    }

    private fun Database.insertOrReplace(elements: List<ElementJson>): Long {
        val elementsWithLatLon = elements.map { Pair(it, it.getLatLon()) }

        transaction {
            elementsWithLatLon.forEach {
                val latLon = it.second

                elementQueries.insertOrReplace(
                    Element(
                        id = it.first.id,
                        lat = latLon.first,
                        lon = latLon.second,
                        osm_json = it.first.osm_json,
                        tags = it.first.tags,
                        created_at = it.first.created_at,
                        updated_at = it.first.updated_at,
                        deleted_at = it.first.deleted_at,
                    )
                )
            }
        }

        return elementsWithLatLon.size.toLong()
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

    @Serializable
    private data class ElementJson(
        val id: String,
        val osm_json: JsonObject,
        val tags: JsonObject,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedElements: Long,
    )
}