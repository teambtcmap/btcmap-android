package org.btcmap.map

/**
 * An area containing the current map centre, shown as a chip above the map.
 */
data class MapArea(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val upcomingEventsCount: Int,
)
