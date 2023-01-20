package elements

data class AreaElement(
    val id: String,
    val lat: Double,
    val lon: Double,
    val icon: String,
    val osmTags: OsmTags,
)