package org.btcmap.api

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
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

data class ActivityFeedItem(
    val type: String,
    val placeId: Long,
    val placeName: String,
    val osmUserId: Long?,
    val osmUserName: String?,
    val osmUserTip: String?,
    val image: String?,
    val date: String,
    val durationDays: Long?,
    val comment: String?,
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
                    quote30dsat = body.get("quote_30d_sat").asLong,
                    quote90dsat = body.get("quote_90d_sat").asLong,
                    quote365dsat = body.get("quote_365d_sat").asLong,
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

        val req = JsonObject().apply {
            addProperty("place_id", placeId.toString())
            addProperty("days", days)
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
                    invoiceId = body.get("invoice_id").asString,
                    invoice = body.get("invoice").asString,
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
                id = it.get("id").asLong,
                elementId = if (!it.has("place_id") || it.get("place_id").isJsonNull) null else it.get(
                    "place_id"
                ).asLong,
                comment = if (!it.has("text") || it.get("text").isJsonNull) null else it.get("text").asString.ifBlank { null },
                createdAt = if (!it.has("created_at") || it.get("created_at").isJsonNull) null else it.get(
                    "created_at"
                ).asString.ifBlank { null },
                updatedAt = it.get("updated_at").asString,
                deletedAt = if (!it.has("deleted_at") || it.get("deleted_at").isJsonNull) null else it.get(
                    "deleted_at"
                ).asString.ifBlank { null },
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
                    quoteSat = body.get("quote_sat").asLong,
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
                invoiceId = body.get("invoice_id").asString,
                invoice = body.get("invoice").asString,
            )
        }
    }

    suspend fun addComment(placeId: Long, comment: String): AddCommentResponse {
        val url = url.newBuilder().addPathSegments("v4/place-comments").build()

        val req = JsonObject().apply {
            addProperty("place_id", placeId.toString())
            addProperty("comment", comment)
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
                id = it.get("id").asLong,
                lat = it.get("lat").asDouble,
                lon = it.get("lon").asDouble,
                name = it.get("name").asString,
                website = it.get("website").asString.toHttpUrl(),
                startsAt = ZonedDateTime.parse(it.get("starts_at").asString),
                endsAt = if (!it.has("ends_at") || it.get("ends_at").isJsonNull) null else ZonedDateTime.parse(
                    it.get("ends_at").asString
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
                id = it.get("id").asLong,
                name = it.get("name").asString,
                type = it.get("type").asString,
                urlAlias = it.get("url_alias").asString,
                icon = if (!it.has("icon") || it.get("icon").isJsonNull) null else it.get("icon").asString
                    .ifBlank { null },
                websiteUrl = it.get("website_url").asString,
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

    suspend fun getArea(id: String): GetAreaItem {
        val url = url.newBuilder().addPathSegments("v4/areas/$id").build()
        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { stream ->
                val body = stream.toJsonObject()
                GetAreaItem(
                    id = body.get("id").asLong,
                    name = body.get("name").asString,
                    type = body.get("type").asString,
                    urlAlias = body.get("url_alias").asString,
                    icon = if (!body.has("icon") || body.get("icon").isJsonNull) null else body.get("icon").asString.ifBlank { null },
                    iconWide = if (!body.has("icon_wide") || body.get("icon_wide").isJsonNull) null else body.get("icon_wide").asString.ifBlank { null },
                    websiteUrl = body.get("website_url").asString,
                    description = if (!body.has("description") || body.get("description").isJsonNull) null else body.get("description").asString.ifBlank { null },
                )
            }
        }
    }

    private fun InputStream.toActivityFeedItems(): List<ActivityFeedItem> {
        return toJsonArray().map {
            ActivityFeedItem(
                type = it.get("type").asString,
                placeId = it.get("place_id").asLong,
                placeName = it.get("place_name").asString,
                osmUserId = if (!it.has("osm_user_id") || it.get("osm_user_id").isJsonNull) null else it.get("osm_user_id").asLong,
                osmUserName = if (!it.has("osm_user_name") || it.get("osm_user_name").isJsonNull) null else it.get("osm_user_name").asString.ifBlank { null },
                osmUserTip = if (!it.has("osm_user_tip") || it.get("osm_user_tip").isJsonNull) null else it.get("osm_user_tip").asString.ifBlank { null },
                image = if (!it.has("image") || it.get("image").isJsonNull) null else it.get("image").asString.ifBlank { null },
                date = it.get("date").asString,
                durationDays = if (!it.has("duration_days") || it.get("duration_days").isJsonNull) null else it.get("duration_days").asLong,
                comment = if (!it.has("comment") || it.get("comment").isJsonNull) null else it.get("comment").asString.ifBlank { null },
            )
        }
    }

    suspend fun getActivity(areaIds: List<String>): List<ActivityFeedItem> {
        val url = url.newBuilder().addPathSegments("v4/activity").apply {
            addQueryParameter("areas", areaIds.joinToString(","))
            addQueryParameter("days", "7")
        }.build()

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use { it.toActivityFeedItems() }
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
                    id = body.get("id").asString,
                    status = body.get("status").asString,
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
        val bundled: Boolean,
        val comments: Long?,
        val telegram: HttpUrl?,
    )

    private fun InputStream.toGetPlacesItems(): List<GetPlacesItem> {
        return toJsonArray().map {
            GetPlacesItem(
                id = it.get("id").asLong,
                lat = it.get("lat").asDouble,
                lon = it.get("lon").asDouble,
                icon = it.get("icon").asString,
                name = it.get("name").asString,
                localizedName = if (!it.has("localized_name") || it.get("localized_name").isJsonNull) null else it.get(
                    "localized_name"
                )
                    .getAsJsonObject(),
                updatedAt = it.get("updated_at").asString,
                deletedAt = if (!it.has("deleted_at") || it.get("deleted_at").isJsonNull) null else it.get(
                    "deleted_at"
                )
                    .asString.ifBlank { null },
                requiredAppUrl = if (!it.has("required_app_url") || it.get("required_app_url").isJsonNull) null else it.get(
                    "required_app_url"
                )
                    .asString.ifBlank { null },
                boostedUntil = if (!it.has("boosted_until") || it.get("boosted_until").isJsonNull) null else it.get(
                    "boosted_until"
                )
                    .asString.ifBlank { null },
                verifiedAt = if (!it.has("verified_at") || it.get("verified_at").isJsonNull) null else it.get(
                    "verified_at"
                )
                    .asString.ifBlank { null },
                address = if (!it.has("address") || it.get("address").isJsonNull) null else it.get("address")
                    .asString.ifBlank { null },
                openingHours = if (!it.has("opening_hours") || it.get("opening_hours").isJsonNull) null else it.get(
                    "opening_hours"
                )
                    .asString.ifBlank { null },
                localizedOpeningHours = if (!it.has("localized_opening_hours") || it.get("localized_opening_hours").isJsonNull) null else it.get(
                    "localized_opening_hours"
                ).getAsJsonObject(),
                website = if (!it.has("website") || it.get("website").isJsonNull) null else it.get("website")
                    .asString.ifBlank { null },
                phone = if (!it.has("phone") || it.get("phone").isJsonNull) null else it.get("phone").asString
                    .ifBlank { null },
                email = if (!it.has("email") || it.get("email").isJsonNull) null else it.get("email").asString
                    .ifBlank { null },
                twitter = if (!it.has("twitter") || it.get("twitter").isJsonNull) null else it.get("twitter")
                    .asString.ifBlank { null },
                facebook = if (!it.has("facebook") || it.get("facebook").isJsonNull) null else it.get(
                    "facebook"
                )
                    .asString.ifBlank { null },
                instagram = if (!it.has("instagram") || it.get("instagram").isJsonNull) null else it.get(
                    "instagram"
                )
                    .asString.ifBlank { null },
                line = if (!it.has("line") || it.get("line").isJsonNull) null else it.get("line").asString
                    .ifBlank { null },
                bundled = false,
                comments = if (!it.has("comments") || it.get("comments").isJsonNull) null else it.get(
                    "comments"
                )
                    .asLong,
                telegram = if (!it.has("telegram") || it.get("telegram").isJsonNull) null else it.get(
                    "telegram"
                )
                    .asString.toHttpUrl(),
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

    data class User(
        val id: Long,
        val name: String,
        val roles: JsonArray,
        val savedPlaces: JsonArray,
        val savedAreas: JsonArray,
    )

    fun JsonObject.toUser(): User {
        return User(
            id = this["id"].asLong,
            name = this["name"].asString,
            roles = this.getAsJsonArray("roles"),
            savedPlaces = this.getAsJsonArray("saved_places"),
            savedAreas = this.getAsJsonArray("saved_areas"),
        )
    }

    suspend fun createUser(password: String): User {
        val url = url.newBuilder().addPathSegments("v4/users").build()

        val req = JsonObject().apply {
            addProperty("password", password)
        }

        val res = httpClient.newCall(
            Request.Builder()
                .post(req.toString().toRequestBody("application/json".toMediaType()))
                .url(url)
                .build()
        ).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return res.body.byteStream().use {
            val json = it.toJsonObject()
            User(
                id = json["id"].asLong,
                name = json["name"].asString,
                roles = json.getAsJsonArray("roles"),
                savedPlaces = JsonArray(),
                savedAreas = JsonArray(),
            )
        }
    }

    suspend fun getUser(): User {
        val url = url.newBuilder().addPathSegments("v4/users/me").build()

        val res = httpClient.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return res.body.byteStream().use {
            it.toJsonObject().toUser()
        }
    }

    data class CreateTokenResponse(
        val token: String,
        val user: User,
    )

    suspend fun signIn(
        username: String,
        password: String,
        label: String
    ): CreateTokenResponse {
        val url = url.newBuilder().addPathSegments("v4/users/$username/tokens").build()

        val req = JsonObject().apply {
            addProperty("label", label)
        }

        val res = httpClient.newCall(
            Request.Builder()
                .post(req.toString().toRequestBody("application/json".toMediaType()))
                .url(url)
                .header("Authorization", "Bearer $password")
                .build()
        ).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return res.body.byteStream().use {
            val body = it.toJsonObject()
            CreateTokenResponse(
                token = body.get("token").asString,
                user = body.getAsJsonObject("user").toUser(),
            )
        }
    }

    suspend fun savePlace(id: Long) {
        val url = url.newBuilder().addPathSegments("v4/places/saved").build()
        val args = JsonPrimitive(id)
        val res = httpClient.newCall(
            Request.Builder()
                .post(args.toString().toRequestBody("application/json".toMediaType()))
                .url(url)
                .build()
        ).executeAsync()
        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }
    }

    suspend fun removeSavedPlace(id: Long) {
        val url = url.newBuilder().addPathSegments("v4/places/saved/$id").build()
        val res = httpClient.newCall(
            Request.Builder()
                .delete()
                .url(url)
                .build()
        ).executeAsync()
        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }
    }
}