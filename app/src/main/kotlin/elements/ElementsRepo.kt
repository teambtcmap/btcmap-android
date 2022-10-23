package elements

import android.content.Context
import app.cash.sqldelight.coroutines.*
import db.*
import http.await
import icons.iconId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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

    suspend fun selectById(id: String): Element? {
        return db.elementQueries.selectById(id).asFlow().mapToOneOrNull(Dispatchers.IO).first()
    }

    suspend fun selectByBoundingBox(
        box: BoundingBox,
    ): List<View_element_map_pin> {
        val pins = if (box.lonEast > box.lonWest) {
            db.elementQueries.selectElementsAsMapPinsByBoundingBox(
                minLat = box.latSouth,
                maxLat = box.latNorth,
                minLon = box.lonWest,
                maxLon = box.lonEast,
            ).asFlow().mapToList(Dispatchers.IO).first()
        } else {
            val part1 = db.elementQueries.selectElementsAsMapPinsByBoundingBox(
                minLat = box.latSouth,
                maxLat = box.latNorth,
                minLon = box.lonWest,
                maxLon = 180.0,
            ).asFlow().mapToList(Dispatchers.IO).first()

            val part2 = db.elementQueries.selectElementsAsMapPinsByBoundingBox(
                minLat = box.latSouth,
                maxLat = box.latNorth,
                minLon = -180.0,
                maxLon = box.lonEast,
            ).asFlow().mapToList(Dispatchers.IO).first()

            part1 + part2
        }

        return pins
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

                val bundledElements = Json.decodeFromStream(
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
                                icon_id = it.osm_json["tags"]?.jsonObject?.iconId() ?: "",
                                osm_json = it.osm_json,
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
                        icon_id = it.osm_json["tags"]?.jsonObject?.iconId() ?: "",
                        osm_json = it.osm_json,
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
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedElements: Long,
    )
}