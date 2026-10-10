package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject

data class User(
    val id: Long,
    val name: String,
    val roles: List<String>,
    val savedPlaces: List<SavedItem>,
    val savedAreas: List<SavedItem>,
)

/** One user returned by the admin/root user search. */
data class UserSearchResult(
    val id: Long,
    val name: String,
    val roles: List<String>,
    val createdAt: String,
    val geofence: List<Long>,
    val npub: String?,
)

data class CreateTokenResponse(
    val token: String,
    val user: User,
)

suspend fun Api.createUser(name: String?, password: String): User {
    val url = buildUrl("v4", "users")

    val req = buildJsonObject {
        put("password", password)
        name?.takeIf { it.isNotBlank() }?.let { put("name", it) }
    }

    return call(HttpMethod.Post, url, withoutAuth = true, body = req) { body ->
        body.toJsonObject().toUser()
    }
}

suspend fun Api.getUser(): User {
    val url = buildUrl("v4", "users", "me")

    return call(HttpMethod.Get, url) { body -> body.toJsonObject().toUser() }
}

/**
 * Searches users by a case-insensitive substring of their name, ordered by
 * name. Restricted to `admin` and `root` users; the server rejects anyone else
 * with 403. An empty [query] lists users up to [limit]; soft-deleted users are
 * excluded and `%`/`_` in [query] are matched literally.
 */
suspend fun Api.searchUsers(query: String, limit: Int = 100): List<UserSearchResult> {
    val url = buildUrl("v4", "users") {
        parameters.append("query", query)
        parameters.append("limit", "$limit")
    }

    return call(HttpMethod.Get, url) { body ->
        body.toJsonArray().map { it.toUserSearchResult() }
    }
}

/**
 * Partially updates a user (`PATCH /v4/users/{id}`): a replacement role set
 * and/or geofence, leaving an omitted field untouched. Restricted to admins and
 * roots; the server enforces the exact policy and rejects anything else with
 * 403. Returns the updated user.
 */
suspend fun Api.updateUser(
    userId: Long,
    roles: List<String>? = null,
    geofence: List<Long>? = null,
): UserSearchResult {
    val url = buildUrl("v4", "users", "$userId")

    val req = buildJsonObject {
        roles?.let { list ->
            put("roles", buildJsonArray { list.forEach { add(JsonPrimitive(it)) } })
        }
        geofence?.let { ids ->
            put("geofence", buildJsonArray { ids.forEach { add(JsonPrimitive(it)) } })
        }
    }

    return call(HttpMethod.Patch, url, body = req) { body ->
        body.toJsonObject().toUserSearchResult()
    }
}

suspend fun Api.updateUsername(username: String): User {
    val url = buildUrl("v4", "users", "me", "username")

    val req = buildJsonObject {
        put("username", username)
    }

    return call(HttpMethod.Put, url, body = req) { body -> body.toJsonObject().toUser() }
}

suspend fun Api.updatePassword(oldPassword: String, newPassword: String) {
    val url = buildUrl("v4", "users", "me", "password")

    val req = buildJsonObject {
        put("old_password", oldPassword)
        put("new_password", newPassword)
    }

    call(HttpMethod.Put, url, body = req) { }
}

suspend fun Api.signIn(
    username: String,
    password: String,
    label: String,
): CreateTokenResponse {
    val url = buildUrl("v4", "users", username, "tokens")

    val req = buildJsonObject {
        put("label", label)
    }

    return call(
        method = HttpMethod.Post,
        url = url,
        authorization = "Bearer $password",
        clearSessionOnUnauthorized = false,
        body = req,
    ) { body ->
        val parsed = body.toJsonObject()

        val token = parsed.string("token")
        if (token.isBlank()) {
            throw ApiParseException("Sign-in response is missing a token")
        }

        CreateTokenResponse(
            token = token,
            user = parsed.obj("user").toUser(),
        )
    }
}

/**
 * Revokes the given session token server-side (`POST /v4/auth/signout`). The
 * token is passed explicitly because the caller clears the stored token before
 * this best-effort call runs. A revoked token is rejected with 401 on a repeat
 * call, so the session is not cleared again from this request.
 */
suspend fun Api.signOut(token: String) {
    val url = buildUrl("v4", "auth", "signout")

    call(
        method = HttpMethod.Post,
        url = url,
        authorization = "Bearer $token",
        clearSessionOnUnauthorized = false,
        body = buildJsonObject { },
    ) { }
}

private fun JsonObject.toUser(): User {
    return User(
        id = long("id"),
        name = string("name"),
        roles = arrayOrNull("roles")?.map { it.jsonPrimitive.content } ?: emptyList(),
        savedPlaces = arrayOrNull("saved_places")?.map { it.jsonObject.toSavedItem() } ?: emptyList(),
        savedAreas = arrayOrNull("saved_areas")?.map { it.jsonObject.toSavedItem() } ?: emptyList(),
    )
}

private fun JsonObject.toSavedItem(): SavedItem {
    return SavedItem(
        id = long("id"),
        name = string("name"),
    )
}

private fun JsonObject.toUserSearchResult(): UserSearchResult {
    return UserSearchResult(
        id = long("id"),
        name = string("name"),
        roles = arrayOrNull("roles")?.map { it.jsonPrimitive.content } ?: emptyList(),
        createdAt = string("created_at"),
        geofence = arrayOrNull("geofence")?.map { it.jsonPrimitive.long } ?: emptyList(),
        npub = stringOrNull("npub"),
    )
}
