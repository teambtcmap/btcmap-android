package areas

import http.await
import kotlinx.serialization.builtins.ListSerializer
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
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromStream(
            ListSerializer(Area.serializer()),
            response.body!!.byteStream()
        )
    }
}