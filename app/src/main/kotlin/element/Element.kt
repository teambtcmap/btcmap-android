package element

import android.util.Log
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
    return toJsonArray().mapNotNull { it.toElementOrNull() }
}

fun InputStream.toBundledElements(): List<BundledElement> {
    return toJsonArray().mapNotNull { it.toBundledElementOrNull() }
}

private fun JSONObject.toElementOrNull(): Element? {
    val id = try {
        getLong("id")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: id", t)
        return null
    }
    val lat = try {
        getDouble("lat")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: lat", t)
        return null
    }
    val lon = try {
        getDouble("lon")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: lon", t)
        return null
    }
    val icon = try {
        getString("icon")
    } catch (t: Throwable) {
        Log.e("Element:kt", "invalid field: icon", t)
        return null
    }
    val name = try {
        getString("name")
    } catch (t: Throwable) {
        Log.e("Element:kt", "invalid field: name", t)
        return null
    }
    val updatedAt = try {
        getZonedDateTime("updated_at")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: updated_at", t)
        return null
    }
    val deletedAt = try {
        optZonedDateTime("deleted_at")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: deleted_at", t)
        return null
    }
    val requiredAppUrl = try {
        optHttpUrl("required_app_url")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: required_app_url", t)
        return null
    }
    val boostedUntil = try {
        optZonedDateTime("boosted_until")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: boosted_until", t)
        return null
    }
    val verifiedAt = try {
        optLocalDate("verified_at")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: verified_at", t)
        return null
    }
    val address = optString("verified_at").ifBlank { null }
    val openingHours = optString("opening_hours").ifBlank { null }
    val website = try {
        optHttpUrl("website")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: website", t)
        return null
    }
    val phone = optString("phone").ifBlank { null }
    val email = optString("email").ifBlank { null }
    val twitter = try {
        optHttpUrl("twitter")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: twitter", t)
        return null
    }
    val facebook = try {
        optHttpUrl("facebook")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: facebook", t)
        return null
    }
    val instagram = try {
        optHttpUrl("instagram")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: instagram", t)
        return null
    }
    val line = try {
        optHttpUrl("line")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: line", t)
        return null
    }
    return Element(
        id = id,
        lat = lat,
        lon = lon,
        icon = icon,
        name = name,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        requiredAppUrl = requiredAppUrl,
        boostedUntil = boostedUntil,
        verifiedAt = verifiedAt,
        address = address,
        openingHours = openingHours,
        website = website,
        phone = phone,
        email = email,
        twitter = twitter,
        facebook = facebook,
        instagram = instagram,
        line = line,
    )
}

private fun JSONObject.toBundledElementOrNull(): BundledElement? {
    val id = try {
        getLong("id")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: id", t)
        return null
    }
    val lat = try {
        getDouble("lat")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: lat", t)
        return null
    }
    val lon = try {
        getDouble("lon")
    } catch (t: Throwable) {
        Log.e("Element.kt", "invalid field: lon", t)
        return null
    }
    val icon = try {
        getString("icon")
    } catch (t: Throwable) {
        Log.e("Element:kt", "invalid field: icon", t)
        return null
    }
    val name = try {
        getString("name")
    } catch (t: Throwable) {
        Log.e("Element:kt", "invalid field: name", t)
        return null
    }
    return BundledElement(
        id = id,
        lat = lat,
        lon = lon,
        icon = icon,
        name = name,
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