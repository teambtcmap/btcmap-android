package org.btcmap.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.btcmap.util.toJsonObject

data class User(
    val id: Long,
    val name: String,
    val roles: JsonArray,
    val savedPlaces: JsonArray,
    val savedAreas: JsonArray,
)

data class CreateTokenResponse(
    val token: String,
    val user: User,
)

suspend fun Api.createUser(name: String?, password: String): User {
    val url = url.newBuilder().addPathSegments("v4/users").build()

    val req = JsonObject().apply {
        addProperty("password", password)
        name?.takeIf { it.isNotBlank() }?.let { addProperty("name", it) }
    }

    return call(
        Request.Builder()
            .post(req.toString().toRequestBody("application/json".toMediaType()))
            .url(url)
            .build()
    ) { stream ->
        val json = stream.toJsonObject()

        User(
            id = json["id"].asLong,
            name = json["name"].asString,
            roles = json.getAsJsonArray("roles"),
            savedPlaces = JsonArray(),
            savedAreas = JsonArray(),
        )
    }
}

suspend fun Api.getUser(): User {
    val url = url.newBuilder().addPathSegments("v4/users/me").build()

    return call(Request.Builder().url(url).build()) { stream ->
        stream.toJsonObject().toUser()
    }
}

suspend fun Api.updateUsername(username: String): User {
    val url = url.newBuilder().addPathSegments("v4/users/me/username").build()

    val req = JsonObject().apply {
        addProperty("username", username)
    }

    return call(
        Request.Builder()
            .put(req.toString().toRequestBody("application/json".toMediaType()))
            .url(url)
            .build()
    ) { stream ->
        stream.toJsonObject().toUser()
    }
}

suspend fun Api.updatePassword(oldPassword: String, newPassword: String) {
    val url = url.newBuilder().addPathSegments("v4/users/me/password").build()

    val req = JsonObject().apply {
        addProperty("old_password", oldPassword)
        addProperty("new_password", newPassword)
    }

    call(
        Request.Builder()
            .put(req.toString().toRequestBody("application/json".toMediaType()))
            .url(url)
            .build()
    ) { }
}

suspend fun Api.signIn(
    username: String,
    password: String,
    label: String,
): CreateTokenResponse {
    val url = url.newBuilder().addPathSegments("v4/users/$username/tokens").build()

    val req = JsonObject().apply {
        addProperty("label", label)
    }

    return call(
        Request.Builder()
            .post(req.toString().toRequestBody("application/json".toMediaType()))
            .url(url)
            .header("Authorization", "Bearer $password")
            .build()
    ) { stream ->
        val body = stream.toJsonObject()

        CreateTokenResponse(
            token = body.get("token").asString,
            user = body.getAsJsonObject("user").toUser(),
        )
    }
}

private fun JsonObject.toUser(): User {
    return User(
        id = this["id"].asLong,
        name = this["name"].asString,
        roles = this.getAsJsonArray("roles"),
        savedPlaces = this.getAsJsonArray("saved_places"),
        savedAreas = this.getAsJsonArray("saved_areas"),
    )
}
