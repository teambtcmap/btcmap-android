package org.btcmap.api

import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class GetCommentsItem(
    val id: Long,
    val elementId: Long?,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

data class CommentQuoteResponse(
    val quoteSat: Long,
)

data class AddCommentResponse(
    val invoiceId: String,
    val invoice: String,
)

suspend fun Api.getComments(updatedSince: ZonedDateTime?, limit: Long): List<GetCommentsItem> {
    val url = url.newBuilder().addPathSegments("v4/place-comments").apply {
        addQueryParameter("limit", "$limit")
        addQueryParameter("include_deleted", "true")
        if (updatedSince != null) {
            addQueryParameter(
                "updated_since",
                updatedSince.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            )
        }
    }.build()

    return call(Request.Builder().url(url).build()) { it.toGetCommentsItems() }
}

suspend fun Api.getCommentQuote(): CommentQuoteResponse {
    val url = url.newBuilder().addPathSegments("v4/place-comments/quote").build()

    return call(Request.Builder().url(url).build()) { stream ->
        val body = stream.toJsonObject()

        CommentQuoteResponse(
            quoteSat = body.long("quote_sat"),
        )
    }
}

suspend fun Api.addComment(placeId: Long, comment: String): AddCommentResponse {
    val url = url.newBuilder().addPathSegments("v4/place-comments").build()

    val req = JsonObject().apply {
        addProperty("place_id", placeId.toString())
        addProperty("comment", comment)
    }

    return call(
        Request.Builder()
            .post(req.toString().toRequestBody("application/json".toMediaType()))
            .url(url)
            .build()
    ) { it.toAddCommentResponse() }
}

private fun InputStream.toGetCommentsItems(): List<GetCommentsItem> {
    return toJsonArray().map {
        GetCommentsItem(
            id = it.long("id"),
            elementId = it.longOrNull("place_id"),
            comment = it.nonBlankStringOrNull("text"),
            createdAt = it.nonBlankStringOrNull("created_at"),
            updatedAt = it.string("updated_at"),
            deletedAt = it.nonBlankStringOrNull("deleted_at"),
        )
    }
}

private fun InputStream.toAddCommentResponse(): AddCommentResponse {
    val body = toJsonObject()

    return AddCommentResponse(
        invoiceId = body.string("invoice_id"),
        invoice = body.string("invoice"),
    )
}
