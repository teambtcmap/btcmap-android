package areas

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import db.Area
import db.Database
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
class AreasRepo(
    private val db: Database,
) {

    suspend fun getAreas(): List<Area> {
        return db.areaQueries.selectAll().asFlow().mapToList().first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync() {
        val url = "https://api.btcmap.org/v2/areas"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = request.await()
        val json = Json { ignoreUnknownKeys = true }

        val areas = json.decodeFromStream(
            ListSerializer(AreaJson.serializer()),
            response.body!!.byteStream(),
        )

        db.transaction {
            db.areaQueries.deleteAll()

            areas.forEach {
                db.areaQueries.insertOrReplace(
                    Area(
                        id = it.id,
                        name = it.name,
                        type = it.type,
                        min_lon = it.min_lon,
                        min_lat = it.min_lat,
                        max_lon = it.max_lon,
                        max_lat = it.max_lat,
                    )
                )
            }
        }
    }

    @Serializable
    private data class AreaJson(
        val id: String,
        val name: String,
        val type: String,
        val min_lon: Double,
        val min_lat: Double,
        val max_lon: Double,
        val max_lat: Double,
    )
}