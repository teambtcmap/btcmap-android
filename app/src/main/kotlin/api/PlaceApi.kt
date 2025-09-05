package api

import json.toJsonArray
import json.toJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import settings.apiUrl
import settings.prefs
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object PlaceApi {

    data class GetPlacesItem(
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
        val comments: Long?,
    )

    suspend fun getPlaces(updatedSince: ZonedDateTime?, limit: Long): List<GetPlacesItem> {
        val url = prefs.apiUrl
            .newBuilder().apply {
                addPathSegment("v4")
                addPathSegment("places")
                addQueryParameter(
                    "fields",
                    "lat,lon,icon,name,updated_at,deleted_at,required_app_url,boosted_until,verified_at,address,opening_hours,website,phone,email,twitter,facebook,instagram,line,comments"
                )
                addQueryParameter("limit", "$limit")
                if (updatedSince != null) {
                    addQueryParameter(
                        "updated_since",
                        updatedSince.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    )
                }
            }.build()

        val res = apiHttpClient().newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toGetPlacesItems() }
        }
    }

    data class BoostQuote(
        val quote30dsat: Long,
        val quote90dsat: Long,
        val quote365dsat: Long,
    )

    suspend fun getBoostQuote(): BoostQuote {
        val url = prefs.apiUrl
            .newBuilder().apply {
                addPathSegment("v4")
                addPathSegment("places")
                addPathSegment("boost")
                addPathSegment("quote")
            }.build()

        val res = apiHttpClient().newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                BoostQuote(
                    quote30dsat = body.getLong("quote_30d_sat"),
                    quote90dsat = body.getLong("quote_90d_sat"),
                    quote365dsat = body.getLong("quote_365d_sat"),
                )
            }
        }
    }

    fun InputStream.toGetPlacesItems(): List<GetPlacesItem> {
        return toJsonArray().map {
            GetPlacesItem(
                id = it.getLong("id"),
                lat = it.getDouble("lat"),
                lon = it.getDouble("lon"),
                icon = it.getString("icon"),
                name = it.getString("name"),
                updatedAt = it.getString("updated_at"),
                deletedAt = it.optString("deleted_at").ifBlank { null },
                requiredAppUrl = it.optString("required_app_url").ifBlank { null },
                boostedUntil = it.optString("boosted_until").ifBlank { null },
                verifiedAt = it.optString("verified_at").ifBlank { null },
                address = it.optString("verified_at").ifBlank { null },
                openingHours = it.optString("opening_hours").ifBlank { null },
                website = it.optString("website").ifBlank { null },
                phone = it.optString("phone").ifBlank { null },
                email = it.optString("email").ifBlank { null },
                twitter = it.optString("twitter").ifBlank { null },
                facebook = it.optString("facebook").ifBlank { null },
                instagram = it.optString("instagram").ifBlank { null },
                line = it.optString("line").ifBlank { null },
                bundled = false,
                comments = it.optLong("comments", 0),
            )
        }
    }
}