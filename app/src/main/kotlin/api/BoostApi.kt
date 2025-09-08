package api

import json.toJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import settings.apiUrl
import settings.prefs

object BoostApi {

    data class QuoteResponse(
        val quote30dsat: Long,
        val quote90dsat: Long,
        val quote365dsat: Long,
    )

    suspend fun getQuote(): QuoteResponse {
        val url = prefs.apiUrl
            .newBuilder().apply {
                addPathSegment("v4")
                addPathSegment("place-boosts")
                addPathSegment("quote")
            }.build()

        val res = apiHttpClient().newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                QuoteResponse(
                    quote30dsat = body.getLong("quote_30d_sat"),
                    quote90dsat = body.getLong("quote_90d_sat"),
                    quote365dsat = body.getLong("quote_365d_sat"),
                )
            }
        }
    }

    data class PostResponse(
        val paymentRequest: String,
        val invoiceUuid: String,
    )

    suspend fun post(placeId: Long, days: Long): PostResponse {
        val url = prefs.apiUrl
            .newBuilder().apply {
                addPathSegment("v4")
                addPathSegment("place-boosts")
            }.build()

        val res = apiHttpClient().newCall(
            Request.Builder()
                .post("""{ "place_id": "$placeId", "days": $days }""".toRequestBody("application/json".toMediaType()))
                .url(url).build()
        ).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                PostResponse(
                    paymentRequest = body.getString("payment_request"),
                    invoiceUuid = body.getString("invoice_uuid"),
                )
            }
        }
    }
}