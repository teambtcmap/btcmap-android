package elements

import android.content.Context
import app.cash.sqldelight.coroutines.*
import db.*
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import org.osmdroid.util.BoundingBox

@Single
class ElementsRepo(
    private val context: Context,
    private val db: Database,
) {

    init {
        GlobalScope.launch {
            db.elementQueries.selectCount().asFlow().mapToOne(Dispatchers.IO).collect {
                clustersCache.clear()
            }
        }
    }

    private val clustersCache = mutableMapOf<Double, List<SelectElementClusters>>()

    suspend fun selectById(id: String): Element? {
        return db.elementQueries.selectById(id).asFlow().mapToOneOrNull(Dispatchers.IO).first()
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
            db.elementQueries.selectElementsAsPinsByBoundingBox(
                minLat = box.latSouth,
                maxLat = box.latNorth,
                minLon = box.lonWest,
                maxLon = box.lonEast,
            ).asFlow().mapToList(Dispatchers.IO).first().map {
                SelectElementClusters(
                    count = 1,
                    id = it.id,
                    lat = it.lat,
                    lon = it.lon,
                    icon_id = it.icon_id,
                )
            }
        } else {
            clustersCache.getOrPut(step) {
                db.elementQueries.selectElementClusters(step = step).asFlow()
                    .mapToList(Dispatchers.IO).first()
            }.filter {
                box.contains(it.lat!!, it.lon!!)
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
        ).asFlow().mapToList(Dispatchers.IO).first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun fetchBundledElements(): Result<SyncReport> {
        val startMillis = System.currentTimeMillis()

        if (db.elementQueries.selectCount().asFlow().mapToOne(Dispatchers.IO).first() > 0) {
            return Result.success(
                SyncReport(
                    timeMillis = System.currentTimeMillis() - startMillis,
                    createdOrUpdatedElements = 0,
                )
            )
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val bundledElementsInputStream = context.assets.open("elements.json")
                val json = Json { ignoreUnknownKeys = true }

                val bundledElements = json.decodeFromStream(
                    ListSerializer(ElementJson.serializer()),
                    bundledElementsInputStream,
                )

                db.transaction {
                    bundledElements.forEach {
                        val latLon = it.getLatLon()

                        db.elementQueries.insertOrReplace(
                            Element(
                                id = it.id,
                                lat = latLon.first,
                                lon = latLon.second,
                                osm_json = it.osm_json,
                                tags = it.tags,
                                created_at = it.created_at,
                                updated_at = it.updated_at,
                                deleted_at = it.deleted_at,
                            )
                        )
                    }
                }

                SyncReport(
                    timeMillis = System.currentTimeMillis() - startMillis,
                    createdOrUpdatedElements = bundledElements.size.toLong(),
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        val startMillis = System.currentTimeMillis()

        val maxUpdatedAt =
            db.elementQueries.selectMaxUpdatedAt().asFlow().mapToOneNotNull(Dispatchers.IO)
                .firstOrNull()?.max

        val url = if (maxUpdatedAt == null) {
            "https://api.btcmap.org/v2/elements"
        } else {
            "https://api.btcmap.org/v2/elements?updated_since=$maxUpdatedAt"
        }.toHttpUrl()

        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrElse { return Result.failure(it) }
        val json = Json { ignoreUnknownKeys = true }

        val elements = runCatching {
            json.decodeFromStream(
                ListSerializer(ElementJson.serializer()),
                response.body!!.byteStream(),
            )
        }.getOrElse { return Result.failure(it) }

        db.transaction {
            elements.forEach {
                val latLon = it.getLatLon()

                db.elementQueries.insertOrReplace(
                    Element(
                        id = it.id,
                        lat = latLon.first,
                        lon = latLon.second,
                        osm_json = it.osm_json,
                        tags = it.tags,
                        created_at = it.created_at,
                        updated_at = it.updated_at,
                        deleted_at = it.deleted_at,
                    )
                )
            }
        }

        return Result.success(
            SyncReport(
                timeMillis = System.currentTimeMillis() - startMillis,
                createdOrUpdatedElements = elements.size.toLong(),
            )
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