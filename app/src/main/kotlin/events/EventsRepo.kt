package events

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import db.*
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
class EventsRepo(
    private val db: Database,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {

    suspend fun selectAllNotDeletedAsListItems(limit: Long): List<SelectAllNotDeletedEventsAsListItems> {
        return db.eventQueries
            .selectAllNotDeletedEventsAsListItems(limit)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
    }

    suspend fun selectEventsByUserIdAsListItems(userId: Long): List<SelectEventsByUserIdAsListItems> {
        return db.eventQueries
            .selectEventsByUserIdAsListItems(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val startMillis = System.currentTimeMillis()

                val maxUpdatedAt = db.eventQueries
                    .selectMaxUpdatedAt()
                    .asFlow()
                    .mapToOneNotNull(Dispatchers.IO)
                    .firstOrNull()
                    ?.max

                val url = HttpUrl.Builder().apply {
                    scheme("https")
                    host("api.btcmap.org")
                    addPathSegment("v2")
                    addPathSegment("events")

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
                    val events = json.decodeToSequence(
                        stream = inputStream,
                        deserializer = EventJson.serializer(),
                    )

                    val createdOrUpdatedElements = events
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

    private fun Database.insertOrReplace(events: List<EventJson>): Long {
        transaction {
            events.forEach {
                eventQueries.insertOrReplace(
                    Event(
                        id = it.id,
                        type = it.type,
                        element_id = it.element_id,
                        user_id = it.user_id,
                        created_at = it.created_at,
                        updated_at = it.updated_at,
                        deleted_at = it.deleted_at,
                    )
                )
            }
        }

        return events.size.toLong()
    }

    @Serializable
    private data class EventJson(
        val id: Long,
        val date: String,
        val type: String,
        val element_id: String,
        val user_id: Long,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedElements: Long,
    )
}