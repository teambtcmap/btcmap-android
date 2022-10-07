package areas

import kotlinx.serialization.Serializable

@Serializable
data class Area(
    val id: String,
    val name: String,
    val area_type: String,
    val elements: Long,
    val up_to_date_elements: Long,
)