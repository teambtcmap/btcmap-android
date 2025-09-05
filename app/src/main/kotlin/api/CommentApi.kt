package api

import json.toJsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import settings.apiUrl
import settings.prefs
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object CommentApi {

    data class GetCommentsItem(
        val id: Long,
        val elementId: Long?,
        val comment: String?,
        val createdAt: String?,
        val updatedAt: String,
        val deletedAt: String?,
    )

    suspend fun getComments(
        updatedSince: ZonedDateTime?,
        limit: Long,
    ): List<GetCommentsItem> {
        val url = prefs.apiUrl.newBuilder().apply {
            addPathSegment("v4")
            addPathSegment("place-comments")
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
            res.body.byteStream().use { it.toGetCommentsItems() }
        }
    }

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
}