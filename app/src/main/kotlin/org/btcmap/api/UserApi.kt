package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.Request
import org.btcmap.db.table.user.SavedItem
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

    val req = JsonObject().apply {
        addProperty("password", password)
        name?.takeIf { it.isNotBlank() }?.let { addProperty("name", it) }
    }

    return call(
        Request.Builder()
            .post(jsonBody(req))
            .url(url)
            .build()
    ) { stream ->
        stream.toJsonObject().toUser()
    }
}

suspend fun Api.getUser(): User {
    val url = buildUrl("v4", "users", "me")

    return call(Request.Builder().url(url).build()) { stream ->
        stream.toJsonObject().toUser()
    }
}

suspend fun Api.updateUsername(username: String): User {
    val url = buildUrl("v4", "users", "me", "username")

    val req = JsonObject().apply {
        addProperty("username", username)
    }

    return call(
        Request.Builder()
            .put(jsonBody(req))
            .url(url)
            .build()
    ) { stream ->
        stream.toJsonObject().toUser()
    }
}

suspend fun Api.updatePassword(oldPassword: String, newPassword: String) {
    val url = buildUrl("v4", "users", "me", "password")

    val req = JsonObject().apply {
        addProperty("old_password", oldPassword)
        addProperty("new_password", newPassword)
    }

    call(
        Request.Builder()
            .put(jsonBody(req))
            .url(url)
            .build()
    ) { }
}

suspend fun Api.signIn(
    username: String,
    password: String,
    label: String,
): CreateTokenResponse {
    val url = buildUrl("v4", "users", username, "tokens")

    val req = JsonObject().apply {
        addProperty("label", label)
    }

    return call(
        request = Request.Builder()
            .post(jsonBody(req))
            .url(url)
            .header("Authorization", "Bearer $password")
            .build(),
        clearSessionOnUnauthorized = false,
    ) { stream ->
        val body = stream.toJsonObject()

        CreateTokenResponse(
            token = body.string("token"),
            user = body.obj("user").toUser(),
        )
    }
}

private fun JsonObject.toUser(): User {
    return User(
        id = long("id"),
        name = string("name"),
        roles = arrayOrNull("roles")?.map { it.asString } ?: emptyList(),
        savedPlaces = arrayOrNull("saved_places")?.map { it.asJsonObject.toSavedItem() } ?: emptyList(),
        savedAreas = arrayOrNull("saved_areas")?.map { it.asJsonObject.toSavedItem() } ?: emptyList(),
    )
}

private fun JsonObject.toSavedItem(): SavedItem {
    return SavedItem(
        id = long("id"),
        name = string("name"),
    )
}
