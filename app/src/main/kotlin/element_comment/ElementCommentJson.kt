package element_comment

import db.table.comment.Comment
import json.toJsonArray
import java.io.InputStream
import java.time.ZonedDateTime

data class ElementCommentJson(
    val id: Long,
    val elementId: Long?,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun ElementCommentJson.toElementComment(): Comment {
    return Comment(
        id = id,
        placeId = elementId!!,
        comment = comment!!,
        createdAt = ZonedDateTime.parse(createdAt!!),
        updatedAt = ZonedDateTime.parse(updatedAt),
    )
}

fun InputStream.toElementCommentsJson(): List<ElementCommentJson> {
    return toJsonArray().map {
        ElementCommentJson(
            id = it.getLong("id"),
            elementId = it.optLong("place_id"),
            comment = it.optString("text").ifBlank { null },
            createdAt = it.optString("created_at").ifBlank { null },
            updatedAt = it.getString("updated_at"),
            deletedAt = it.optString("deleted_at").ifBlank { null },
        )
    }
}