package users

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import db.Database
import db.User
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
class UsersRepo(
    private val db: Database,
) {

    suspend fun selectAll(): List<User> {
        return db.userQueries.selectAll().asFlow().mapToList().first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sync() {
        val url = "https://api.btcmap.org/v2/users"

        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = request.await()

        val users = Json.decodeFromStream(
            ListSerializer(UserJson.serializer()),
            response.body!!.byteStream(),
        )

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

    @Serializable
    private data class UserJson(
        val id: Long,
        val osm_json: JsonObject,
        val created_at: String,
        val updated_at: String,
        val deleted_at: String?,
    )
}