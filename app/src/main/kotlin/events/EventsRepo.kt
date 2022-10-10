package events

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import db.Database
import db.Event
import http.await
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

    suspend fun selectAll(): List<Event> {
        return db.eventQueries.selectAll().asFlow().mapToList().first()
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