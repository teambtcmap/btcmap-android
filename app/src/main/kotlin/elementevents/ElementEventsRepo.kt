package elementevents

import http.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.util.regex.Pattern

@Single
class ElementEventsRepo {

    suspend fun getElementEvents(): List<ElementEvent> {
        val url = "https://api.btcmap.org/element_events"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrNull()
            ?: return emptyList()
        val events: JsonArray = Json.decodeFromString(response.body!!.string())
        return events.map { event ->
            val user2 = event.jsonObject["user_v2"]!!

            val userDescription = if (user2 is JsonObject) {
                user2.jsonObject["data"]?.jsonObject?.get("description")?.jsonPrimitive?.content
                    ?: ""
            } else {
                ""
            }

            var lnurl = ""

            val pattern = Pattern.compile("\\(lightning:[^)]*\\)", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(userDescription)
            val matchFound: Boolean = matcher.find()

            if (matchFound) {
                lnurl = matcher.group().trim('(', ')')
            }

            ElementEvent(
                date = event.jsonObject["date"]!!.jsonPrimitive.content,
                elementId = event.jsonObject["element_id"]!!.jsonPrimitive.content,
                elementLat = event.jsonObject["element_lat"]!!.jsonPrimitive.double,
                elementLon = event.jsonObject["element_lon"]!!.jsonPrimitive.double,
                elementName = event.jsonObject["element_name"]!!.jsonPrimitive.content,
                eventType = event.jsonObject["event_type"]!!.jsonPrimitive.content,
                user = event.jsonObject["user"]!!.jsonPrimitive.content,
                lnurl = lnurl
            )
        }
    }
}