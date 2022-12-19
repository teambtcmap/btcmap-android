package areas

import db.*
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.ZonedDateTime

class AreasRepo(
    private val queries: AreaQueries,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {

    suspend fun selectAll() = queries.selectAll()

    suspend fun selectById(id: String) = queries.selectById(id)

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()

            val maxUpdatedAt = queries.selectMaxUpdatedAt()

            val url = HttpUrl.Builder().apply {
                scheme("https")
                host("api.btcmap.org")
                addPathSegment("v2")
                addPathSegment("areas")

                if (maxUpdatedAt != null) {
                    addQueryParameter("updated_since", maxUpdatedAt.toString())
                }
            }.build()

            val request = httpClient.newCall(Request.Builder().url(url).build())
            val response = request.await()

            if (!response.isSuccessful) {
                throw Exception("Unexpected HTTP response code: ${response.code}")
            }

            withContext(Dispatchers.IO) {
                response.body!!.byteStream().use { responseBody ->
                    withContext(Dispatchers.IO) {
                        var count = 0L

                        json.decodeToSequence(
                            stream = responseBody,
                            deserializer = AreaJson.serializer(),
                        ).chunked(1_000).forEach { chunk ->
                            queries.insertOrReplace(chunk.filter { it.valid() }.map { it.toArea() })
                            count += chunk.size
                        }

                        SyncReport(
                            timeMillis = System.currentTimeMillis() - startMillis,
                            createdOrUpdatedAreas = count,
                        )
                    }
                }
            }
        }
    }

    @Serializable
    private data class AreaJson(
        val id: String,
        val tags: JsonObject,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    private fun AreaJson.toArea(): Area {
        return Area(
            id = id,
            tags = tags,
            createdAt = ZonedDateTime.parse(created_at),
            updatedAt = ZonedDateTime.parse(updated_at),
            deletedAt = if (deleted_at.isNotEmpty()) ZonedDateTime.parse(deleted_at) else null,
        )
    }

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