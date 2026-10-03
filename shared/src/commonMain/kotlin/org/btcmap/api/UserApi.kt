package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.btcmap.util.toJsonObject

data class User(
    val id: Long,
    val name: String,
    val roles: List<String>,
    val savedPlaces: List<SavedItem>,
    val savedAreas: List<SavedItem>,
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
