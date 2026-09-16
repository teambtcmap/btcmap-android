package org.btcmap.api

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val placeFields = listOf(
    "lat",
    "lon",
    "icon",
    "name",
    "localized_name",
    "updated_at",
    "deleted_at",
    "required_app_url",
    "boosted_until",
    "verified_at",
    "address",
    "opening_hours",
    "localized_opening_hours",
    "website",
    "phone",
    "email",
    "twitter",
    "facebook",
    "instagram",
    "line",
    "comments",
    "telegram",
    "osm_id",
)

data class GetPlacesItem(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String,
    val localizedName: JsonObject?,
    val updatedAt: String,
    val deletedAt: String?,
    val requiredAppUrl: String?,
    val boostedUntil: String?,
    val verifiedAt: String?,
    val address: String?,
    val openingHours: String?,
    val localizedOpeningHours: JsonObject?,
    val website: String?,
    val phone: String?,
    val email: String?,
    val twitter: String?,
    val facebook: String?,
    val instagram: String?,
    val line: String?,
    val comments: Long?,
    val telegram: String?,
    val osmId: String?,
)

data class PlaceCoordinates(
    val lat: Double,
    val lon: Double,
)

suspend fun Api.getPlaces(updatedSince: ZonedDateTime?, limit: Long): List<GetPlacesItem> {
    val url = url.newBuilder().addPathSegments("v4/places").apply {
        addQueryParameter("fields", placeFields.joinToString(separator = ","))
        addQueryParameter("limit", "$limit")
        addQueryParameter("include_deleted", "true")
        if (updatedSince != null) {
            addQueryParameter(
                "updated_since",
                updatedSince.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            )
        }
    }.build()

    return call(Request.Builder().url(url).build()) { it.toGetPlacesItems() }
}

suspend fun Api.getPlaceCoordinates(id: Long): PlaceCoordinates {
    val url = url.newBuilder().addPathSegments("v4/places/$id").apply {
        addQueryParameter("fields", "lat,lon")
    }.build()

    return call(Request.Builder().url(url).build()) { stream ->
        val body = stream.toJsonObject()
        PlaceCoordinates(
            lat = body.get("lat").asDouble,
            lon = body.get("lon").asDouble,
        )
    }
}

suspend fun Api.savePlace(id: Long): List<Long> {
    val url = url.newBuilder().addPathSegments("v4/places/saved").build()
    val body = JsonPrimitive(id).toString().toRequestBody("application/json".toMediaType())

    return call(Request.Builder().post(body).url(url).build()) { it.toJsonLongArray() }
}

suspend fun Api.removeSavedPlace(id: Long): List<Long> {
    val url = url.newBuilder().addPathSegments("v4/places/saved/$id").build()

    return call(Request.Builder().delete().url(url).build()) { it.toJsonLongArray() }
}

private fun InputStream.toGetPlacesItems(): List<GetPlacesItem> {
    return toJsonArray().map { it.toGetPlacesItem() }
}

private fun JsonObject.toGetPlacesItem(): GetPlacesItem {
    return GetPlacesItem(
        id = get("id").asLong,
        lat = get("lat").asDouble,
        lon = get("lon").asDouble,
        icon = get("icon").asString,
        name = get("name").asString,
        localizedName = if (!has("localized_name") || get("localized_name").isJsonNull) null else get(
            "localized_name"
        )
            .getAsJsonObject(),
        updatedAt = get("updated_at").asString,
        deletedAt = if (!has("deleted_at") || get("deleted_at").isJsonNull) null else get(
            "deleted_at"
        )
            .asString.ifBlank { null },
        requiredAppUrl = if (!has("required_app_url") || get("required_app_url").isJsonNull) null else get(
            "required_app_url"
        )
            .asString.ifBlank { null },
        boostedUntil = if (!has("boosted_until") || get("boosted_until").isJsonNull) null else get(
            "boosted_until"
        )
            .asString.ifBlank { null },
        verifiedAt = if (!has("verified_at") || get("verified_at").isJsonNull) null else get(
            "verified_at"
        )
            .asString.ifBlank { null },
        address = if (!has("address") || get("address").isJsonNull) null else get("address")
            .asString.ifBlank { null },
        openingHours = if (!has("opening_hours") || get("opening_hours").isJsonNull) null else get(
            "opening_hours"
        )
            .asString.ifBlank { null },
        localizedOpeningHours = if (!has("localized_opening_hours") || get("localized_opening_hours").isJsonNull) null else get(
            "localized_opening_hours"
        ).getAsJsonObject(),
        website = if (!has("website") || get("website").isJsonNull) null else get("website")
            .asString.ifBlank { null },
        phone = if (!has("phone") || get("phone").isJsonNull) null else get("phone").asString
            .ifBlank { null },
        email = if (!has("email") || get("email").isJsonNull) null else get("email").asString
            .ifBlank { null },
        twitter = if (!has("twitter") || get("twitter").isJsonNull) null else get("twitter")
            .asString.ifBlank { null },
        facebook = if (!has("facebook") || get("facebook").isJsonNull) null else get(
            "facebook"
        )
            .asString.ifBlank { null },
        instagram = if (!has("instagram") || get("instagram").isJsonNull) null else get(
            "instagram"
        )
            .asString.ifBlank { null },
        line = if (!has("line") || get("line").isJsonNull) null else get("line").asString
            .ifBlank { null },
        comments = if (!has("comments") || get("comments").isJsonNull) null else get(
            "comments"
        )
            .asLong,
        telegram = if (!has("telegram") || get("telegram").isJsonNull) null else get(
            "telegram"
        )
            .asString.ifBlank { null },
        osmId = if (!has("osm_id") || get("osm_id").isJsonNull) null else get("osm_id")
            .asString.ifBlank { null },
    )
}
