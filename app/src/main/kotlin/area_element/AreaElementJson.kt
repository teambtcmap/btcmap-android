package area_element

import json.toJsonArray
import java.io.InputStream

data class AreaElementJson(
    val id: Long,
    val areaId: Long?,
    val elementId: Long?,
    val updatedAt: String,
    val deletedAt: String?,
)

fun AreaElementJson.toAreaElement(): AreaElement {
    return AreaElement(
        id = id,
        areaId = areaId!!,
        elementId = elementId!!,
        updatedAt = updatedAt,
    )
}

fun InputStream.toAreaElementsJson(): List<AreaElementJson> {
    return toJsonArray().map {
        AreaElementJson(
            id = it.getLong("id"),
            areaId = it.optLong("area_id"),
            elementId = it.optLong("element_id"),
            updatedAt = it.getString("updated_at"),
            deletedAt = it.optString("deleted_at").ifBlank { null },
        )
    }
}