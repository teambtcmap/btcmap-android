package element

import json.toJsonArray
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate
import java.time.ZonedDateTime

data class Element(
    // core fields, bundled with the app
    val id: Long,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String,
    val updatedAt: ZonedDateTime,
    val deletedAt: ZonedDateTime?,
    // secondary fields, fetched from backend
    val requiredAppUrl: HttpUrl?,
    val boostedUntil: ZonedDateTime?,
    val verifiedAt: LocalDate?,
    val address: String?,
    val openingHours: String?,
    val website: HttpUrl?,
    val phone: String?,
    val email: String?,
    val twitter: HttpUrl?,
    val facebook: HttpUrl?,
    val instagram: HttpUrl?,
    val line: HttpUrl?,
)

data class BundledElement(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val name: String,
)

fun BundledElement.toElement(): Element {
    return Element(
        id = this.id,
        lat = this.lat,
        lon = this.lon,
        icon = this.icon,
        name = this.name,
        updatedAt = ZonedDateTime.parse("2000-01-01T00:00:00Z"),
        deletedAt = null,
        requiredAppUrl = null,
        boostedUntil = null,
        verifiedAt = null,
        address = null,
        openingHours = null,
        website = null,
        phone = null,
        email = null,
        twitter = null,
        facebook = null,
        instagram = null,
        line = null,
    )
}

fun InputStream.toElements(): List<Element> {
    return toJsonArray().map { it.toElement() }
}

fun InputStream.toBundledElements(): List<BundledElement> {
    return toJsonArray().mapNotNull { it.toBundledElement() }
}

private fun JSONObject.toElement(): Element {
    return Element(
        id = getLong("id"),
        lat = getDouble("lat"),
        lon = getDouble("lon"),
        icon = getString("icon"),
        name = getString("name"),
        updatedAt = getZonedDateTime("updated_at"),
        deletedAt = optZonedDateTime("deleted_at"),
        requiredAppUrl = optHttpUrl("required_app_url"),
        boostedUntil = optZonedDateTime("boosted_until"),
        verifiedAt = optLocalDate("verified_at"),
        address = optString("verified_at").ifBlank { null },
        openingHours = optString("opening_hours").ifBlank { null },
        website = optHttpUrl("website"),
        phone = optString("phone").ifBlank { null },
        email = optString("email").ifBlank { null },
        twitter = optHttpUrl("twitter"),
        facebook = optHttpUrl("facebook"),
        instagram = optHttpUrl("instagram"),
        line = optHttpUrl("line"),
    )
}

private fun JSONObject.toBundledElement(): BundledElement? {
    return BundledElement(
        id = getLong("id"),
        lat = getDouble("lat"),
        lon = getDouble("lon"),
        icon = getString("icon"),
        name = getString("name"),
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