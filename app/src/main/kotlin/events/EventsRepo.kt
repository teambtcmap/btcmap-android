package events

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import db.Database
import db.Event
import db.SelectAllAsListItems
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class EventsRepo(
    private val db: Database,
) {

    suspend fun selectByUserId(userId: Long): List<Event> {
        return db.eventQueries.selectByUserId(userId).asFlow().mapToList(Dispatchers.IO).first()
    }

    suspend fun selectAllAsListItems(): List<SelectAllAsListItems> {
        return db.eventQueries.selectAllAsListItems().asFlow().mapToList(Dispatchers.IO).first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync() {
        val url = "https://api.btcmap.org/v2/events"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = request.await()
        val json = Json { ignoreUnknownKeys = true }

        val events = json.decodeFromStream(
            ListSerializer(EventJson.serializer()),
            response.body!!.byteStream(),
        )

        db.transaction {
            db.eventQueries.deleteAll()

            events.forEach {
                db.eventQueries.insert(
                    Event(
                        date = it.date,
                        type = it.type,
                        element_id = it.element_id,
                        user_id = it.user_id,
                    )
                )
            }
        }
    }

    @Serializable
    private data class EventJson(
        val date: String,
        val type: String,
        val element_id: String,
        val user_id: Long,
    )
}