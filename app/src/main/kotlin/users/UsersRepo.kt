package users

import db.*
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single
import java.time.ZonedDateTime

@Single
class UsersRepo(
    private val userQueries: UserQueries,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {

    suspend fun selectAll() = userQueries.selectAll()

    suspend fun selectById(id: Long) = userQueries.selectById(id)

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            val startMillis = System.currentTimeMillis()

            val maxUpdatedAt = userQueries.selectMaxUpdatedAt()

            val url = HttpUrl.Builder().apply {
                scheme("https")
                host("api.btcmap.org")
                addPathSegment("v2")
                addPathSegment("users")

                if (maxUpdatedAt != null) {
                    addQueryParameter("updated_since", maxUpdatedAt.toString())
                }
            }.build()

            val response = httpClient
                .newCall(Request.Builder().url(url).build())
                .await()

            if (!response.isSuccessful) {
                throw Exception("Unexpected HTTP response code: ${response.code}")
            }

            response.body!!.byteStream().use { inputStream ->
                withContext(Dispatchers.IO) {
                    var count = 0L

                    json.decodeToSequence(
                        stream = inputStream,
                        deserializer = UserJson.serializer(),
                    ).chunked(1_000).forEach { chunk ->
                        userQueries.insertOrReplace(chunk.map { it.toUser() })
                        count += chunk.size
                    }

                    SyncReport(
                        timeMillis = System.currentTimeMillis() - startMillis,
                        createdOrUpdatedUsers = count,
                    )
                }
            }
        }
    }

    @Serializable
    private data class UserJson(
        val id: Long,
        val osm_json: JsonObject,
        val tags: JsonObject,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String,
    )

    private fun UserJson.toUser(): User {
        return User(
            id = id,
            osmJson = osm_json,
            tags = tags,
            createdAt = ZonedDateTime.parse(created_at),
            updatedAt = ZonedDateTime.parse(updated_at),
            deletedAt = if (deleted_at.isNotBlank()) ZonedDateTime.parse(deleted_at) else null,
        )
    }

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedUsers: Long,
    )
}