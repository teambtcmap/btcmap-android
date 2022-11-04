package users

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import app.cash.sqldelight.coroutines.mapToOneOrNull
import db.Database
import db.SelectUsersAsListItems
import db.User
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class UsersRepo(
    private val db: Database,
    private val httpClient: OkHttpClient,
    private val json: Json,
) {

    suspend fun selectUsersAsListItems(): List<SelectUsersAsListItems> {
        return db.userQueries
            .selectUsersAsListItems()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .first()
            .filter { it.changes > 0 }
    }

    suspend fun selectById(id: Long): User? {
        return db.userQueries
            .selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .firstOrNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val startMillis = System.currentTimeMillis()

                val maxUpdatedAt = db.userQueries
                    .selectMaxUpdatedAt()
                    .asFlow()
                    .mapToOneNotNull(Dispatchers.IO)
                    .firstOrNull()
                    ?.max

                val url = HttpUrl.Builder().apply {
                    scheme("https")
                    host("api.btcmap.org")
                    addPathSegment("v2")
                    addPathSegment("users")

                    if (!maxUpdatedAt.isNullOrBlank()) {
                        addQueryParameter("updated_since", maxUpdatedAt)
                    }
                }.build()

                val response = httpClient
                    .newCall(Request.Builder().url(url).build())
                    .await()

                if (!response.isSuccessful) {
                    throw Exception("Unexpected HTTP response code: ${response.code}")
                }

                response.body!!.byteStream().use { inputStream ->
                    val users = json.decodeToSequence(
                        stream = inputStream,
                        deserializer = UserJson.serializer(),
                    )

                    val createdOrUpdatedUsers = users
                        .chunked(1_000)
                        .map { db.insertOrReplace(it) }
                        .sum()

                    SyncReport(
                        timeMillis = System.currentTimeMillis() - startMillis,
                        createdOrUpdatedUsers = createdOrUpdatedUsers,
                    )
                }
            }
        }
    }

    private fun Database.insertOrReplace(users: List<UserJson>): Long {
        transaction {
            users.forEach {
                userQueries.insertOrReplace(
                    User(
                        id = it.id,
                        osm_json = it.osm_json.toString(),
                        tags = it.tags.toString(),
                        created_at = it.created_at,
                        updated_at = it.updated_at,
                        deleted_at = it.deleted_at,
                    )
                )
            }
        }

        return users.size.toLong()
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

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedUsers: Long,
    )
}