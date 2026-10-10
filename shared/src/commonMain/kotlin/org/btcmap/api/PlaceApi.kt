package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import kotlin.time.Instant

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
    val name: String?,
    val localizedName: JsonObject?,
    val updatedAt: String,
    val deletedAt: String?,
    val requiredAppUrl: String?,
    val boostedUntil: String?,
    val verifiedAt: String?,
    val address: String?,
    val openingHours: String?,
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

suspend fun Api.getPlaces(updatedSince: Instant?, limit: Long): List<GetPlacesItem> {
    val url = buildUrl("v4", "places") {
        parameters.append("fields", placeFields.joinToString(separator = ","))
        parameters.append("limit", "$limit")
        parameters.append("include_deleted", "true")
        addUpdatedSince(updatedSince)
    }

    return call(HttpMethod.Get, url) { it.toGetPlacesItems() }
}

suspend fun Api.getPlaceCoordinates(id: Long): PlaceCoordinates {
    val url = buildUrl("v4", "places", "$id") {
        parameters.append("fields", "lat,lon")
    }

    return call(HttpMethod.Get, url) { body ->
        val parsed = body.toJsonObject()
        PlaceCoordinates(
            lat = parsed.double("lat"),
            lon = parsed.double("lon"),
        )
    }
}

/**
 * Looks up a place's OSM id, including for deleted places. Delete entries in
 * the activity feed can name a place whose tombstone never reached the device
 * (the bundled snapshot carries only live places, and a tombstone older than
 * the snapshot's cursor is never in the delta), so the id has to be read from
 * the server. Returns null when the place is unknown or has no OSM id.
 */
suspend fun Api.getPlaceOsmId(id: Long): String? {
    val url = buildUrl("v4", "places", "$id") {
        parameters.append("fields", "osm_id")
    }

    return call(HttpMethod.Get, url) { body ->
        body.toJsonObject().nonBlankStringOrNull("osm_id")
    }
}

suspend fun Api.savePlace(id: Long): List<Long> = saveItem("places", id)

suspend fun Api.removeSavedPlace(id: Long): List<Long> = removeSavedItem("places", id)

private fun String.toGetPlacesItems(): List<GetPlacesItem> {
    return toJsonArray().map { it.toGetPlacesItem() }
}

internal fun JsonObject.toGetPlacesItem(): GetPlacesItem {
    return GetPlacesItem(
        id = long("id"),
        lat = double("lat"),
        lon = double("lon"),
        icon = string("icon"),
        name = nonBlankStringOrNull("name"),
        localizedName = objectOrNull("localized_name"),
        updatedAt = string("updated_at"),
        deletedAt = nonBlankStringOrNull("deleted_at"),
        requiredAppUrl = nonBlankStringOrNull("required_app_url"),
        boostedUntil = nonBlankStringOrNull("boosted_until"),
        verifiedAt = nonBlankStringOrNull("verified_at"),
        address = nonBlankStringOrNull("address"),
        openingHours = nonBlankStringOrNull("opening_hours"),
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
