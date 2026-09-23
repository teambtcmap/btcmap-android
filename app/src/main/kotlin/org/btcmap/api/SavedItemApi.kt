package org.btcmap.api

import com.google.gson.JsonPrimitive
import okhttp3.Request
import org.btcmap.util.toJsonLongArray

/**
 * Shared implementation of the `/saved` sub-resource that places and areas both
 * expose. [resource] is the REST collection name ("places" or "areas"); the
 * endpoints are identical apart from it.
 */
internal suspend fun Api.saveItem(resource: String, id: Long): List<Long> {
    val url = buildUrl("v4", resource, "saved")
    val body = jsonBody(JsonPrimitive(id))

    return call(Request.Builder().post(body).url(url).build()) { it.toJsonLongArray() }
}

internal suspend fun Api.removeSavedItem(resource: String, id: Long): List<Long> {
    val url = buildUrl("v4", resource, "saved", "$id")

    return call(Request.Builder().delete().url(url).build()) { it.toJsonLongArray() }
}
