package api

import json.toJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import org.json.JSONObject
import java.io.InputStream

object BoostApi {

    private const val ENDPOINT = "place-boosts"

    data class QuoteResponse(
        val quote30dsat: Long,
        val quote90dsat: Long,
        val quote365dsat: Long,
    )

    suspend fun getQuote(): QuoteResponse {
        val url = apiUrl(ENDPOINT).addPathSegment("quote").build()

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

    data class BoostResponse(
        val invoiceId: String,
        val invoice: String,
    )

    private fun InputStream.toBoostResponse(): BoostResponse {
        return use { stream ->
            val body = stream.toJsonObject()

            BoostResponse(
                invoiceId = body.getString("invoice_id"),
                invoice = body.getString("invoice"),
            )
        }
    }

    suspend fun boost(placeId: Long, days: Long): BoostResponse {
        val url = apiUrl(ENDPOINT).build()

        val req = JSONObject().apply {
            put("place_id", placeId.toString())
            put("days", days)
        }

        val res = apiHttpClient().newCall(
            Request.Builder().post(req.toString().toRequestBody("application/json".toMediaType()))
                .url(url).build()
        ).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().toBoostResponse()
        }
    }
}