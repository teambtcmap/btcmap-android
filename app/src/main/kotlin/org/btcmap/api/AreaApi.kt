package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.Request
import org.btcmap.auth.withoutAuth
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import java.io.InputStream
import java.time.ZonedDateTime
import java.util.Locale

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
    "id,name,type,url_alias,icon,icon_wide,website_url,description,bbox,geo_json,updated_at,deleted_at"

suspend fun Api.getAreas(updatedSince: ZonedDateTime, limit: Long): List<GetAreasDeltaItem> {
    val url = buildUrl("v4", "areas") {
        addQueryParameter("fields", AREA_DELTA_FIELDS)
        // Always send updated_since so the server filters rather than returning
        // the full snapshot, and include_deleted so tombstones reach the cache.
        addUpdatedSince(updatedSince)
        addQueryParameter("limit", "$limit")
        addQueryParameter("include_deleted", "true")
    }

    return call(Request.Builder().withoutAuth().url(url).build()) { it.toGetAreasDeltaItems() }
}

suspend fun Api.getArea(
    id: String,
    lang: String = Locale.getDefault().language,
): GetAreaItem {
    val url = buildUrl("v4", "areas", id) {
        if (lang.isNotBlank()) addQueryParameter("lang", lang)
    }

    return call(Request.Builder().withoutAuth().url(url).build()) { stream ->
        val body = stream.toJsonObject()
        GetAreaItem(
            id = body.long("id"),
            name = body.string("name"),
            type = body.string("type"),
            urlAlias = body.string("url_alias"),
            icon = body.nonBlankStringOrNull("icon"),
            iconWide = body.nonBlankStringOrNull("icon_wide"),
            websiteUrl = body.string("website_url"),
            description = body.nonBlankStringOrNull("description"),
        )
    }
}

suspend fun Api.saveArea(id: Long): List<Long> = saveItem("areas", id)

suspend fun Api.removeSavedArea(id: Long): List<Long> = removeSavedItem("areas", id)

private fun JsonObject.toGetAreasDeltaItem(): GetAreasDeltaItem {
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
        bboxWest = bbox?.get(0),
        bboxSouth = bbox?.get(1),
        bboxEast = bbox?.get(2),
        bboxNorth = bbox?.get(3),
        geoJson = objectOrNull("geo_json")?.toString(),
        updatedAt = string("updated_at"),
        deletedAt = nonBlankStringOrNull("deleted_at"),
    )
}

private fun InputStream.toGetAreasDeltaItems(): List<GetAreasDeltaItem> {
    return toJsonArray().map { it.toGetAreasDeltaItem() }
}
