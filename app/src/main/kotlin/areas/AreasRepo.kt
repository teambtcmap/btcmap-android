package areas

import http.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class AreasRepo {

    suspend fun getAreas(): List<Area> {
        val url = "https://api.btcmap.org/areas"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrNull()
            ?: return emptyList()
        val areas: JsonArray = Json.decodeFromString(response.body!!.string())
        return areas.map {
            Area(
                id = it.jsonObject["id"]!!.jsonPrimitive.content,
                name = it.jsonObject["name"]!!.jsonPrimitive.content,
                elements = it.jsonObject["elements"]!!.jsonPrimitive.long,
                upToDateElements = it.jsonObject["up_to_date_elements"]!!.jsonPrimitive.long,
            )
        }
    }
}