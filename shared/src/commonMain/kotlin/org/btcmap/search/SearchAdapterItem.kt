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
        val boosted: Boolean,
    ) : SearchAdapterItem()

    data class Area(
        val areaId: Long,
        val bbox: List<Double>?,
        val iconUrl: String?,
        val headerImageUrl: String?,
        override val icon: String,
        override val name: String,
        override val distanceToUser: String?,
    ) : SearchAdapterItem()

    data class Event(
        val eventId: Long,
        override val icon: String,
        override val name: String,
        override val distanceToUser: String?,
    ) : SearchAdapterItem()

    /**
     * A latitude/longitude pair typed into the search field, offered so the map
     * can move there directly. Unlike the other results it has no cached row
     * behind it.
     */
    data class Coordinate(
        val lat: Double,
        val lon: Double,
        override val icon: String,
        override val name: String,
        override val distanceToUser: String?,
    ) : SearchAdapterItem()

    /**
     * A named place found by the OpenStreetMap Nominatim search service, shown
     * as its own group under the local results. Like a [Coordinate] it has no
     * cached row behind it, so tapping it only moves the map.
     */
    data class Nominatim(
        val lat: Double,
        val lon: Double,
        override val icon: String,
        override val name: String,
        override val distanceToUser: String?,
    ) : SearchAdapterItem()
}
