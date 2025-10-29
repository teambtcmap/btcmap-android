package search

data class SearchAdapterItem(
    val placeId: Long,
    val icon: String,
    val name: String,
    val distanceToUser: String?,
)
