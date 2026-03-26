package org.btcmap.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import org.btcmap.json.toJsonObject
import org.json.JSONObject

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
        val url = url.newBuilder().addPathSegments("place-boosts").build()

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
}