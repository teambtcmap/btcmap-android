package element_comment

import json.toJsonArray
import java.io.InputStream

data class ElementCommentJson(
    val id: Long,
    val elementId: Long?,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun ElementCommentJson.toElementComment(): ElementComment {
    return ElementComment(
        id = id,
        elementId = elementId!!,
        comment = comment!!,
        createdAt = createdAt!!,
        updatedAt = updatedAt,
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