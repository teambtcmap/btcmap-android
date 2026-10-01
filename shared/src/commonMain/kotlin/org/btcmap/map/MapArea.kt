package org.btcmap.map

/**
 * An area containing the current map centre, shown as a chip above the map.
 *
 * [headerImageUrl] is the wide image the area screen shows as its header, i.e.
 * the same URL [org.btcmap.area.AreaFragment] loads, so the chip can warm it
 * while the map is on screen.
 */
data class MapArea(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val upcomingEventsCount: Int,
    val headerImageUrl: String?,
)
