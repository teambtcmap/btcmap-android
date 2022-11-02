package users

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import app.cash.sqldelight.coroutines.mapToOneOrNull
import db.Database
import db.SelectAllUsersAsListItems
import db.User
import http.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.annotation.Single

@Single
class UsersRepo(
    private val db: Database,
) {

    suspend fun selectAllUsersAsListItems(): List<SelectAllUsersAsListItems> {
        return db.userQueries.selectAllUsersAsListItems().asFlow().mapToList(Dispatchers.IO).first()
            .filter { it.changes > 0 }
    }

    suspend fun selectById(id: Long): User? {
        return db.userQueries.selectById(id).asFlow().mapToOneOrNull(Dispatchers.IO).firstOrNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync(): Result<SyncReport> {
        val startMillis = System.currentTimeMillis()

        val maxUpdatedAt =
            db.userQueries.selectMaxUpdatedAt().asFlow().mapToOneNotNull(Dispatchers.IO)
                .firstOrNull()?.max

        val url = if (maxUpdatedAt == null) {
            "https://api.btcmap.org/v2/users"
        } else {
            "https://api.btcmap.org/v2/users?updated_since=$maxUpdatedAt"
        }.toHttpUrl()

        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrElse { return Result.failure(it) }
        val json = Json { ignoreUnknownKeys = true }

        val users = runCatching {
            withContext(Dispatchers.IO) {
                json.decodeFromStream(
                    ListSerializer(UserJson.serializer()),
                    response.body!!.byteStream(),
                )
            }
        }.getOrElse { return Result.failure(it) }

        withContext(Dispatchers.IO) {
            db.transaction {
                users.forEach {
                    db.userQueries.insertOrReplace(
                        User(
                            id = it.id,
                            osm_json = it.osm_json.toString(),
                            created_at = it.created_at,
                            updated_at = it.updated_at,
                            deleted_at = it.deleted_at ?: "",
                        )
                    )
                }
            }
        }
        
        return Result.success(
            SyncReport(
                timeMillis = System.currentTimeMillis() - startMillis,
                createdOrUpdatedUsers = users.size.toLong(),
            )
        )
    }

    @Serializable
    private data class UserJson(
        val id: Long,
        val osm_json: JsonObject,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String?,
    )

    data class SyncReport(
        val timeMillis: Long,
        val createdOrUpdatedUsers: Long,
    )
}