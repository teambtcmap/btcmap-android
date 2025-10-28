package api

import http.httpClient
import json.toJsonArray
import json.toJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import org.json.JSONObject
import settings.apiUrlV4
import settings.prefs
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object CommentApi {
    private const val ENDPOINT = "place-comments"

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

    suspend fun getComments(
        updatedSince: ZonedDateTime?,
        limit: Long,
    ): List<GetCommentsItem> {
        val url = prefs.apiUrlV4(ENDPOINT).newBuilder().apply {
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

    data class QuoteResponse(
        val quoteSat: Long,
    )

    // https://github.com/teambtcmap/btcmap-api/blob/master/docs/rest/v4/place-comments.md#get-a-comment-quote
    suspend fun getQuote(): QuoteResponse {
        val url = prefs.apiUrlV4(ENDPOINT, "quote")

        val res = httpClient.newCall(Request.Builder().url(url).build()).executeAsync()

        if (!res.isSuccessful) {
            throw Exception("Unexpected HTTP response code: ${res.code}")
        }

        return withContext(Dispatchers.IO) {
            res.body.byteStream().use {
                val body = it.toJsonObject()

                QuoteResponse(
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

    // https://github.com/teambtcmap/btcmap-api/blob/master/docs/rest/v4/place-comments.md#order-comment
    suspend fun addComment(placeId: Long, comment: String): AddCommentResponse {
        val url = prefs.apiUrlV4(ENDPOINT)

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
}