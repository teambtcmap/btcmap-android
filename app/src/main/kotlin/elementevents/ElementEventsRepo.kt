package elementevents

import http.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class ElementEventsRepo {

    suspend fun getElementEvents(): List<ElementEvent> {
        val url = "https://api.btcmap.org/element_events"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrNull()
            ?: return emptyList()
        val areas: JsonArray = Json.decodeFromString(response.body!!.string())
        return areas.map {
            ElementEvent(
                date = it.jsonObject["date"]!!.jsonPrimitive.content,
                elementId = it.jsonObject["element_id"]!!.jsonPrimitive.content,
                elementLat = it.jsonObject["element_lat"]!!.jsonPrimitive.double,
                elementLon = it.jsonObject["element_lon"]!!.jsonPrimitive.double,
                elementName = it.jsonObject["element_name"]!!.jsonPrimitive.content,
                eventType = it.jsonObject["event_type"]!!.jsonPrimitive.content,
                user = it.jsonObject["user"]!!.jsonPrimitive.content,
            )
        }
    }
}