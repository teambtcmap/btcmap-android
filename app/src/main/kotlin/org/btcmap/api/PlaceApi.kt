package org.btcmap.api

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.Request
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
    val url = buildUrl("v4", "places") {
        addQueryParameter("fields", placeFields.joinToString(separator = ","))
        addQueryParameter("limit", "$limit")
        addQueryParameter("include_deleted", "true")
        if (updatedSince != null) {
            addQueryParameter(
                "updated_since",
                updatedSince.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            )
        }
    }

    return call(Request.Builder().url(url).build()) { it.toGetPlacesItems() }
}

suspend fun Api.getPlaceCoordinates(id: Long): PlaceCoordinates {
    val url = buildUrl("v4", "places", "$id") {
        addQueryParameter("fields", "lat,lon")
    }

    return call(Request.Builder().url(url).build()) { stream ->
        val body = stream.toJsonObject()
        PlaceCoordinates(
            lat = body.double("lat"),
            lon = body.double("lon"),
        )
    }
}

suspend fun Api.savePlace(id: Long): List<Long> {
    val url = buildUrl("v4", "places", "saved")
    val body = jsonBody(JsonPrimitive(id))

    return call(Request.Builder().post(body).url(url).build()) { it.toJsonLongArray() }
}

suspend fun Api.removeSavedPlace(id: Long): List<Long> {
    val url = buildUrl("v4", "places", "saved", "$id")

    return call(Request.Builder().delete().url(url).build()) { it.toJsonLongArray() }
}

private fun InputStream.toGetPlacesItems(): List<GetPlacesItem> {
    return toJsonArray().map { it.toGetPlacesItem() }
}

private fun JsonObject.toGetPlacesItem(): GetPlacesItem {
    return GetPlacesItem(
        id = long("id"),
        lat = double("lat"),
        lon = double("lon"),
        icon = string("icon"),
        name = string("name"),
        localizedName = objectOrNull("localized_name"),
        updatedAt = string("updated_at"),
        deletedAt = nonBlankStringOrNull("deleted_at"),
        requiredAppUrl = nonBlankStringOrNull("required_app_url"),
        boostedUntil = nonBlankStringOrNull("boosted_until"),
        verifiedAt = nonBlankStringOrNull("verified_at"),
        address = nonBlankStringOrNull("address"),
        openingHours = nonBlankStringOrNull("opening_hours"),
        localizedOpeningHours = objectOrNull("localized_opening_hours"),
        website = nonBlankStringOrNull("website"),
        phone = nonBlankStringOrNull("phone"),
        email = nonBlankStringOrNull("email"),
        twitter = nonBlankStringOrNull("twitter"),
        facebook = nonBlankStringOrNull("facebook"),
        instagram = nonBlankStringOrNull("instagram"),
        line = nonBlankStringOrNull("line"),
        comments = longOrNull("comments"),
        telegram = nonBlankStringOrNull("telegram"),
        osmId = nonBlankStringOrNull("osm_id"),
    )
}
