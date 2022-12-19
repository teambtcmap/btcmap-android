package events

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

class EventsRepo(
    private val queries: EventQueries,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {

    suspend fun selectAll(limit: Long) = queries.selectAll(limit)

    suspend fun selectByUserIdAsListItems(userId: Long) = queries.selectByUserId(userId)

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()

            val maxUpdatedAt = queries.selectMaxUpdatedAt()

            val url = HttpUrl.Builder().apply {
                scheme("https")
                host("api.btcmap.org")
                addPathSegment("v2")
                addPathSegment("events")

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
                var count = 0L

                withContext(Dispatchers.IO) {
                    json.decodeToSequence(
                        stream = responseBody,
                        deserializer = EventJson.serializer(),
                    ).chunked(1_000).forEach { chunk ->
                        queries.insertOrReplace(chunk.map { it.toEvent() })
                        count += chunk.size
                    }
                }

                SyncReport(
                    timeMillis = System.currentTimeMillis() - startMillis,
                    createdOrUpdatedElements = count,
                )
            }
        }
    }

    @Serializable
    private data class EventJson(
        val id: Long,
        val type: String,
        val element_id: String,
        val user_id: Long,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    private fun EventJson.toEvent(): Event {
        return Event(
            id = id,
            type = type,
            elementId = element_id,
            userId = user_id,
            createdAt = ZonedDateTime.parse(created_at),
            updatedAt = ZonedDateTime.parse(updated_at),
            deletedAt = if (deleted_at.isNotEmpty()) ZonedDateTime.parse(deleted_at) else null,
        )
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedElements: Long,
    )
}