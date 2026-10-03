package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonPrimitive
import org.btcmap.util.toJsonLongArray

/**
 * Shared implementation of the `/saved` sub-resource that places and areas both
 * expose. [resource] is the REST collection name ("places" or "areas"); the
 * endpoints are identical apart from it.
 */
internal suspend fun Api.saveItem(resource: String, id: Long): List<Long> {
    val url = buildUrl("v4", resource, "saved")

    return call(HttpMethod.Post, url, body = JsonPrimitive(id)) { it.toJsonLongArray() }
}

internal suspend fun Api.removeSavedItem(resource: String, id: Long): List<Long> {
    val url = buildUrl("v4", resource, "saved", "$id")

    return call(HttpMethod.Delete, url) { it.toJsonLongArray() }
}
