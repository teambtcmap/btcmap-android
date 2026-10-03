package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.btcmap.util.toJsonArray
import org.btcmap.util.toJsonObject
import kotlin.time.Instant

data class GetCommentsItem(
    val id: Long,
    val placeId: Long,
    val comment: String,
    val createdAt: String,
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

suspend fun Api.getComments(updatedSince: Instant?, limit: Long): List<GetCommentsItem> {
    val url = buildUrl("v4", "place-comments") {
        parameters.append("limit", "$limit")
        parameters.append("include_deleted", "true")
        addUpdatedSince(updatedSince)
    }

    return call(HttpMethod.Get, url, withoutAuth = true) { it.toGetCommentsItems() }
}

suspend fun Api.getCommentQuote(): CommentQuoteResponse {
    val url = buildUrl("v4", "place-comments", "quote")

    return call(HttpMethod.Get, url, withoutAuth = true) { body ->
        val parsed = body.toJsonObject()

        CommentQuoteResponse(
            quoteSat = parsed.long("quote_sat"),
        )
    }
}

suspend fun Api.addComment(placeId: Long, comment: String): AddCommentResponse {
    val url = buildUrl("v4", "place-comments")

    val req = buildJsonObject {
        put("place_id", placeId.toString())
        put("comment", comment)
    }

    return call(HttpMethod.Post, url, withoutAuth = true, body = req) { it.toAddCommentResponse() }
}

private fun String.toGetCommentsItems(): List<GetCommentsItem> {
    return toJsonArray().map {
        GetCommentsItem(
            id = it.long("id"),
            placeId = it.long("place_id"),
            comment = it.string("text"),
            createdAt = it.string("created_at"),
            updatedAt = it.string("updated_at"),
            deletedAt = it.nonBlankStringOrNull("deleted_at"),
        )
    }
}

private fun String.toAddCommentResponse(): AddCommentResponse {
    val body = toJsonObject()

    return AddCommentResponse(
        invoiceId = body.string("invoice_id"),
        invoice = body.string("invoice"),
    )
}
