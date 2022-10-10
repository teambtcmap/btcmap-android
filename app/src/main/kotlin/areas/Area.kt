package areas

import kotlinx.serialization.Serializable

@Serializable
data class Area(
    val id: String,
    val name: String,
    val min_lon: Double,
    val min_lat: Double,
    val max_lon : Double,
    val max_lat: Double,
    val area_type: String,
    val elements: Long,
    val up_to_date_elements: Long,
)