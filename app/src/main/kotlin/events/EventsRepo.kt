package events

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import db.*
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class EventsRepo(
    private val db: Database,
) {

    suspend fun selectAllNotDeletedAsListItems(limit: Long): List<SelectAllNotDeletedEventsAsListItems> {
        return db.eventQueries.selectAllNotDeletedEventsAsListItems(limit).asFlow().mapToList(Dispatchers.IO)
            .first()
    }

    suspend fun selectEventsByUserIdAsListItems(userId: Long): List<SelectEventsByUserIdAsListItems> {
        return db.eventQueries.selectEventsByUserIdAsListItems(userId).asFlow()
            .mapToList(Dispatchers.IO).first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        val startMillis = System.currentTimeMillis()

        val maxUpdatedAt =
            db.eventQueries.selectMaxUpdatedAt().asFlow().mapToOneNotNull(Dispatchers.IO)
                .firstOrNull()?.max

        val url = if (maxUpdatedAt == null) {
            "https://api.btcmap.org/v2/events"
        } else {
            "https://api.btcmap.org/v2/events?updated_since=$maxUpdatedAt"
        }.toHttpUrl()

        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrElse { return Result.failure(it) }
        val json = Json { ignoreUnknownKeys = true }

        val events = runCatching {
            json.decodeFromStream(
                ListSerializer(EventJson.serializer()),
                response.body!!.byteStream(),
            )
        }.getOrElse { return Result.failure(it) }

        db.transaction {
            events.forEach {
                db.eventQueries.insert(
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

        return Result.success(
            SyncReport(
                timeMillis = System.currentTimeMillis() - startMillis,
                createdOrUpdatedElements = events.size.toLong(),
            )
        )
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