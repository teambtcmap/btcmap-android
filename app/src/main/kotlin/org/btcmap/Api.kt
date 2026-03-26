package org.btcmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import org.btcmap.json.toJsonArray
import org.btcmap.json.toJsonObject
import org.json.JSONObject
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class Invoice(
    val id: String,
    val status: String,
)

val Invoice.paid: Boolean
    get() = status == "paid"

data class GetAreasItem(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val icon: String?,
    val websiteUrl: String,
)

class Api(private val httpClient: OkHttpClient, private val url: HttpUrl) {

    data class PlaceBoostQuoteResponse(
        val quote30dsat: Long,
        val quote90dsat: Long,
        val quote365dsat: Long,
    )

    suspend fun getPlaceBoostQuote(): PlaceBoostQuoteResponse {
        val url = url.newBuilder().addPathSegments("v4/place-boosts/quote").build()
        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                PlaceBoostQuoteResponse(
                    quote30dsat = body.getLong("quote_30d_sat"),
                    quote90dsat = body.getLong("quote_90d_sat"),
                    quote365dsat = body.getLong("quote_365d_sat"),
                )
            }
        }
    }

    data class PlaceBoostResponse(
        val invoiceId: String,
        val invoice: String,
    )

    suspend fun boostPlace(placeId: Long, days: Long): PlaceBoostResponse {
        val url = url.newBuilder().addPathSegments("v4/place-boosts").build()

        val req = JSONObject().apply {
            put("place_id", placeId.toString())
            put("days", days)
        }

        val res = httpClient.newCall(
            Request.Builder().post(req.toString().toRequestBody("application/json".toMediaType()))
                .url(url).build()
        ).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { stream ->
                val body = stream.toJsonObject()

                PlaceBoostResponse(
                    invoiceId = body.getString("invoice_id"),
                    invoice = body.getString("invoice"),
                )
            }
        }
    }

    data class GetCommentsItem(
        val id: Long,
        val elementId: Long?,
        val comment: String?,
        val createdAt: String?,
        val updatedAt: String,
        val deletedAt: String?,
    )

    private fun InputStream.toGetCommentsItems(): List<GetCommentsItem> {
        return toJsonArray().map {
            GetCommentsItem(
                id = it.getLong("id"),
                elementId = it.optLong("place_id"),
                comment = it.optString("text").ifBlank { null },
                createdAt = it.optString("created_at").ifBlank { null },
                updatedAt = it.getString("updated_at"),
                deletedAt = it.optString("deleted_at").ifBlank { null },
            )
        }
    }

    suspend fun getComments(updatedSince: ZonedDateTime?, limit: Long): List<GetCommentsItem> {
        val url = url.newBuilder().addPathSegments("v4/place-comments").apply {
            addQueryParameter("limit", "$limit")
            if (updatedSince != null) {
                addQueryParameter(
                    "updated_since",
                    updatedSince.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                )
            }
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toGetCommentsItems() }
        }
    }

    data class CommentQuoteResponse(
        val quoteSat: Long,
    )

    suspend fun getCommentQuote(): CommentQuoteResponse {
        val url = url.newBuilder().addPathSegments("v4/place-comments/quote").build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                CommentQuoteResponse(
                    quoteSat = body.getLong("quote_sat"),
                )
            }
        }
    }

    data class AddCommentResponse(
        val invoiceId: String,
        val invoice: String,
    )

    private fun InputStream.toAddCommentResponse(): AddCommentResponse {
        return use { stream ->
            val body = stream.toJsonObject()

            AddCommentResponse(
                invoiceId = body.getString("invoice_id"),
                invoice = body.getString("invoice"),
            )
        }
    }

    suspend fun addComment(placeId: Long, comment: String): AddCommentResponse {
        val url = url.newBuilder().addPathSegments("v4/place-comments").build()

        val req = JSONObject().apply {
            put("place_id", placeId.toString())
            put("comment", comment)
        }

        val res = httpClient.newCall(
            Request.Builder()
                .post(req.toString().toRequestBody("application/json".toMediaType()))
                .url(url).build()
        ).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().toAddCommentResponse()
        }
    }

    data class GetEventsItem(
        val id: Long,
        val lat: Double,
        val lon: Double,
        val name: String,
        val website: HttpUrl,
        val startsAt: ZonedDateTime,
        val endsAt: ZonedDateTime?,
    )

    private fun InputStream.toGetEventsItems(): List<GetEventsItem> {
        return toJsonArray().map {
            GetEventsItem(
                id = it.getLong("id"),
                lat = it.getDouble("lat"),
                lon = it.getDouble("lon"),
                name = it.getString("name"),
                website = it.getString("website").toHttpUrl(),
                startsAt = ZonedDateTime.parse(it.getString("starts_at")),
                endsAt = if (it.isNull("ends_at")) null else ZonedDateTime.parse(
                    it.getString("ends_at")
                )
            )
        }
    }

    suspend fun getEvents(): List<GetEventsItem> {
        val url = url.newBuilder().addPathSegments("v4/events").build()
        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toGetEventsItems() }
        }
    }

    private fun InputStream.toAreas(): List<GetAreasItem> {
        return toJsonArray().map {
            GetAreasItem(
                id = it.getLong("id"),
                name = it.getString("name"),
                type = it.getString("type"),
                urlAlias = it.getString("url_alias"),
                icon = it.optString("icon").ifBlank { null },
                websiteUrl = it.getString("website_url"),
            )
        }
    }

    suspend fun getAreas(lat: Double, lon: Double): List<GetAreasItem> {
        val url = url.newBuilder().addPathSegments("v4/areas").apply {
            addQueryParameter("lat", lat.toString())
            addQueryParameter("lon", lon.toString())
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toAreas() }
        }
    }

    suspend fun getInvoice(id: String): Invoice {
        val url = url.newBuilder().addPathSegments("v4/invoices/$id").build()
        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                Invoice(
                    id = body.getString("id"),
                    status = body.getString("status"),
                )
            }
        }
    }

    data class GetPlacesItem(
        val id: Long,
        val lat: Double,
        val lon: Double,
        val icon: String,
        val name: String,
        val localizedName: JSONObject?,
        val updatedAt: String,
        val deletedAt: String?,
        val requiredAppUrl: String?,
        val boostedUntil: String?,
        val verifiedAt: String?,
        val address: String?,
        val openingHours: String?,
        val localizedOpeningHours: JSONObject?,
        val website: String?,
        val phone: String?,
        val email: String?,
        val twitter: String?,
        val facebook: String?,
        val instagram: String?,
        val line: String?,
        val bundled: Boolean,
        val comments: Long?,
        val telegram: HttpUrl?,
    )

    private fun InputStream.toGetPlacesItems(): List<GetPlacesItem> {
        return toJsonArray().map {
            GetPlacesItem(
                id = it.getLong("id"),
                lat = it.getDouble("lat"),
                lon = it.getDouble("lon"),
                icon = it.getString("icon"),
                name = it.getString("name"),
                localizedName = it.optJSONObject("localized_name"),
                updatedAt = it.getString("updated_at"),
                deletedAt = it.optString("deleted_at").ifBlank { null },
                requiredAppUrl = it.optString("required_app_url").ifBlank { null },
                boostedUntil = it.optString("boosted_until").ifBlank { null },
                verifiedAt = it.optString("verified_at").ifBlank { null },
                address = it.optString("address").ifBlank { null },
                openingHours = it.optString("opening_hours").ifBlank { null },
                localizedOpeningHours = it.optJSONObject("localized_opening_hours"),
                website = it.optString("website").ifBlank { null },
                phone = it.optString("phone").ifBlank { null },
                email = it.optString("email").ifBlank { null },
                twitter = it.optString("twitter").ifBlank { null },
                facebook = it.optString("facebook").ifBlank { null },
                instagram = it.optString("instagram").ifBlank { null },
                line = it.optString("line").ifBlank { null },
                bundled = false,
                comments = it.optLong("comments", 0),
                telegram = it.optString("telegram", "").let { telegramStr ->
                    if (telegramStr.isBlank()) null else telegramStr.toHttpUrl()
                },
            )
        }
    }

    suspend fun getPlaces(updatedSince: ZonedDateTime?, limit: Long): List<GetPlacesItem> {
        val fields = listOf(
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
        )

        val url = url.newBuilder().addPathSegments("v4/places").apply {
            addQueryParameter("fields", fields.joinToString(separator = ","))
            addQueryParameter("limit", "$limit")
            if (updatedSince != null) {
                addQueryParameter(
                    "updated_since",
                    updatedSince.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                )
            }
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toGetPlacesItems() }
        }
    }
}