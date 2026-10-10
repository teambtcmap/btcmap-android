package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import kotlin.time.Instant
import org.btcmap.platform.currentLanguage

data class GetAreaItem(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val icon: String?,
    val iconWide: String?,
    val websiteUrl: String,
    val description: String?,
)

/**
 * A row of the incremental `GET /v4/areas` change log. Unlike the single-area
 * endpoint, a delta row always carries [updatedAt] and may carry [deletedAt] as
 * a tombstone.
 */
data class GetAreasDeltaItem(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val icon: String?,
    val iconWide: String?,
    val websiteUrl: String,
    val description: String?,
    // ISO-8601 `YYYY-MM-DD`, or null when the area has never been verified.
    val verifiedAt: String?,
    // Per-language `name:<lang>` / `description:<lang>` maps keyed by a
    // two-letter code, merged into one object by the API. Null when the area
    // carries no translation for any language.
    val localizedName: JsonObject?,
    val localizedDescription: JsonObject?,
    val bboxWest: Double?,
    val bboxSouth: Double?,
    val bboxEast: Double?,
    val bboxNorth: Double?,
    // Raw GeoJSON object as serialized JSON text, or null when absent.
    val geoJson: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

private const val AREA_DELTA_FIELDS =
    "id,name,type,url_alias,icon,icon_wide,website_url,description,verified_at," +
        "localized_name,localized_description,bbox,geo_json,updated_at,deleted_at"

suspend fun Api.getAreas(updatedSince: Instant, limit: Long): List<GetAreasDeltaItem> {
    val url = buildUrl("v4", "areas") {
        parameters.append("fields", AREA_DELTA_FIELDS)
        // Always send updated_since so the server filters rather than returning
        // the full snapshot, and include_deleted so tombstones reach the cache.
        addUpdatedSince(updatedSince)
        parameters.append("limit", "$limit")
        parameters.append("include_deleted", "true")
    }

    return call(HttpMethod.Get, url, withoutAuth = true) { it.toGetAreasDeltaItems() }
}

suspend fun Api.getArea(
    id: String,
    lang: String = currentLanguage(),
): GetAreaItem {
    val url = buildUrl("v4", "areas", id) {
        if (lang.isNotBlank()) parameters.append("lang", lang)
    }

    return call(HttpMethod.Get, url, withoutAuth = true) { body ->
        val parsed = body.toJsonObject()
        GetAreaItem(
            id = parsed.long("id"),
            name = parsed.string("name"),
            type = parsed.string("type"),
            urlAlias = parsed.string("url_alias"),
            icon = parsed.nonBlankStringOrNull("icon"),
            iconWide = parsed.nonBlankStringOrNull("icon_wide"),
            websiteUrl = parsed.string("website_url"),
            description = parsed.nonBlankStringOrNull("description"),
        )
    }
}

/**
 * Sets an area's last verification date (`PATCH /v4/areas/{id}`), stored by the
 * server as the `verified:date` tag. [date] is a `YYYY-MM-DD` string. Only area
 * managers, admins and roots may call it; the server rejects anyone else with
 * `403`. Returns once the server has accepted the change.
 */
suspend fun Api.verifyArea(id: Long, date: String) = patchArea(id) {
    put("verified_at", date)
}

/** Renames an area (`PATCH /v4/areas/{id}`); [name] must not be blank. */
suspend fun Api.setAreaName(id: Long, name: String) = patchArea(id) {
    put("name", name)
}

/** Sets an area's description (`PATCH /v4/areas/{id}`); null clears it. */
suspend fun Api.setAreaDescription(id: Long, description: String?) = patchArea(id) {
    put("description", description)
}

/** Sends one partial update of the fields [fields] adds to the area `{id}`. */
private suspend fun Api.patchArea(id: Long, fields: JsonObjectBuilder.() -> Unit) {
    val url = buildUrl("v4", "areas", "$id")
    val req = buildJsonObject(fields)
    call(HttpMethod.Patch, url, body = req) { }
}

suspend fun Api.saveArea(id: Long): List<Long> = saveItem("areas", id)

suspend fun Api.removeSavedArea(id: Long): List<Long> = removeSavedItem("areas", id)

internal fun JsonObject.toGetAreasDeltaItem(): GetAreasDeltaItem {
    val bbox = doubleArrayOrNull("bbox")?.takeIf { it.size == 4 }

    return GetAreasDeltaItem(
        id = long("id"),
        name = string("name"),
        type = string("type"),
        urlAlias = string("url_alias"),
        icon = nonBlankStringOrNull("icon"),
        iconWide = nonBlankStringOrNull("icon_wide"),
        websiteUrl = string("website_url"),
        description = nonBlankStringOrNull("description"),
        verifiedAt = nonBlankStringOrNull("verified_at"),
        localizedName = objectOrNull("localized_name"),
        localizedDescription = objectOrNull("localized_description"),
        bboxWest = bbox?.get(0),
        bboxSouth = bbox?.get(1),
        bboxEast = bbox?.get(2),
        bboxNorth = bbox?.get(3),
        geoJson = objectOrNull("geo_json")?.toString(),
        updatedAt = string("updated_at"),
        deletedAt = nonBlankStringOrNull("deleted_at"),
    )
}

private fun String.toGetAreasDeltaItems(): List<GetAreasDeltaItem> {
    return toJsonArray().map { it.toGetAreasDeltaItem() }
}
