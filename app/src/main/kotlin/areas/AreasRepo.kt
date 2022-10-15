package areas

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import app.cash.sqldelight.coroutines.mapToOneOrNull
import db.Area
import db.Database
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
class AreasRepo(
    private val db: Database,
) {

    suspend fun selectAllNotDeleted(): List<Area> {
        return db.areaQueries.selectAllNotDeleted().asFlow().mapToList(Dispatchers.IO).first()
    }

    suspend fun selectById(id: String): Area? {
        return db.areaQueries.selectById(id).asFlow().mapToOneOrNull(Dispatchers.IO).first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        val startMillis = System.currentTimeMillis()

        val maxUpdatedAt =
            db.areaQueries.selectMaxUpdatedAt().asFlow().mapToOneNotNull(Dispatchers.IO)
                .firstOrNull()?.max

        val url = if (maxUpdatedAt == null) {
            "https://api.btcmap.org/v2/areas"
        } else {
            "https://api.btcmap.org/v2/areas?updated_since=$maxUpdatedAt"
        }.toHttpUrl()

        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrElse { return Result.failure(it) }
        val json = Json { ignoreUnknownKeys = true }

        val areas = runCatching {
            json.decodeFromStream(
                ListSerializer(AreaJson.serializer()),
                response.body!!.byteStream(),
            )
        }.getOrElse { return Result.failure(it) }

        db.transaction {
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
                createdOrUpdatedAreas = areas.size.toLong(),
            )
        )
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
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedAreas: Long,
    )
}