package areas

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import app.cash.sqldelight.coroutines.mapToOneOrNull
import db.Area
import db.Database
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class AreasRepo(
    private val db: Database,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {

    suspend fun selectAllNotDeleted(): List<Area> {
        return db.areaQueries
            .selectAllNotDeleted()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
    }

    suspend fun selectById(id: String): Area? {
        return db
            .areaQueries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val startMillis = System.currentTimeMillis()

                val maxUpdatedAt =
                    db.areaQueries
                        .selectMaxUpdatedAt()
                        .asFlow()
                        .mapToOneNotNull(Dispatchers.IO)
                        .firstOrNull()
                        ?.max

                val url = HttpUrl.Builder().apply {
                    scheme("https")
                    host("api.btcmap.org")
                    addPathSegment("v2")
                    addPathSegment("areas")

                    if (!maxUpdatedAt.isNullOrBlank()) {
                        addQueryParameter("updated_since", maxUpdatedAt)
                    }
                }.build()

                val request = httpClient.newCall(Request.Builder().url(url).build())
                val response = request.await()

                if (!response.isSuccessful) {
                    throw Exception("Unexpected HTTP response code: ${response.code}")
                }

                response.body!!.byteStream().use { inputStream ->
                    val areas = json.decodeToSequence(
                        stream = inputStream,
                        deserializer = AreaJson.serializer(),
                    )

                    val createdOrUpdatedAreas = areas
                        .chunked(1_000)
                        .map { db.insertOrReplace(it) }
                        .sum()

                    SyncReport(
                        timeMillis = System.currentTimeMillis() - startMillis,
                        createdOrUpdatedAreas = createdOrUpdatedAreas,
                    )
                }
            }
        }
    }

    private fun Database.insertOrReplace(areas: List<AreaJson>): Long {
        val validAreas = areas.filter { it.valid() }

        transaction {
            validAreas.forEach {
                areaQueries.insertOrReplace(
                    Area(
                        id = it.id,
                        tags = it.tags,
                        created_at = it.created_at,
                        updated_at = it.updated_at,
                        deleted_at = it.deleted_at,
                    )
                )
            }
        }

        return validAreas.size.toLong()
    }

    @Serializable
    private data class AreaJson(
        val id: String,
        val tags: JsonObject,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    private fun AreaJson.valid(): Boolean {
        return (tags["name"]?.jsonPrimitive?.content ?: "").isNotBlank()
                && tags.containsKey("box:north") && tags["box:north"]!!.jsonPrimitive.doubleOrNull != null
                && tags.containsKey("box:east") && tags["box:east"]!!.jsonPrimitive.doubleOrNull != null
                && tags.containsKey("box:south") && tags["box:south"]!!.jsonPrimitive.doubleOrNull != null
                && tags.containsKey("box:west") && tags["box:west"]!!.jsonPrimitive.doubleOrNull != null
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedAreas: Long,
    )
}