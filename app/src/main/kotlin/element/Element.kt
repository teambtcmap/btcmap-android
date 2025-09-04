package element

import api.GetPlacesItem
import bundle.BundledPlace
import db.table.place.Place
import json.toJsonArray
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate
import java.time.ZonedDateTime

data class Element(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String,
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
    val bundled: Boolean,
    val comments: Long,
)

private fun GetPlacesItem.toPlace(): Place {
    return Place(
        id = id,
        lat = lat,
        lon = lon,
        icon = icon,
        name = name,
        updatedAt = ZonedDateTime.parse(updatedAt),
        requiredAppUrl = requiredAppUrl?.toHttpUrl(),
        boostedUntil = if (boostedUntil == null) null else ZonedDateTime.parse(boostedUntil),
        verifiedAt = if (verifiedAt == null) null else ZonedDateTime.parse(verifiedAt + "T00:00:00Z"),
        address = address,
        openingHours = openingHours,
        website = website?.toHttpUrl(),
        phone = phone,
        email = email,
        twitter = twitter?.toHttpUrl(),
        facebook = facebook?.toHttpUrl(),
        instagram = instagram?.toHttpUrl(),
        line = line?.toHttpUrl(),
        bundled = false,
        comments = comments,
    )
}

private fun JSONObject.getZonedDateTime(name: String): ZonedDateTime {
    return ZonedDateTime.parse(getString(name))
}

private fun JSONObject.optZonedDateTime(name: String): ZonedDateTime? {
    val str = optString(name)
    return if (str.isBlank()) {
        null
    } else {
        ZonedDateTime.parse(str)
    }
}

private fun JSONObject.optHttpUrl(name: String): HttpUrl? {
    val str = optString(name)
    return if (str.isBlank()) {
        null
    } else {
        return str.toHttpUrl()
    }
}

private fun JSONObject.optLocalDate(name: String): LocalDate? {
    val str = optString(name)
    return if (str.isBlank()) {
        null
    } else {
        return LocalDate.parse(str)
    }
}