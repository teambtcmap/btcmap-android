package org.btcmap.search

sealed class SearchAdapterItem {
    abstract val icon: String
    abstract val name: String
    abstract val distanceToUser: String?

    data class Place(
        val placeId: Long,
        override val icon: String,
        override val name: String,
        override val distanceToUser: String?,
    ) : SearchAdapterItem()

    data class Area(
        val areaId: Long,
        val bbox: List<Double>?,
        val iconUrl: String?,
        override val icon: String,
        override val name: String,
        override val distanceToUser: String?,
    ) : SearchAdapterItem()
}
