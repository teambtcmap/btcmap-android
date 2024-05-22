package conf

import org.osmdroid.util.BoundingBox
import java.time.ZonedDateTime

data class Conf(
    val lastSyncDate: ZonedDateTime?,
    val viewportNorthLat: Double,
    val viewportEastLon: Double,
    val viewportSouthLat: Double,
    val viewportWestLon: Double,
    val showAtms: Boolean,
    val showOsmAttribution: Boolean,
    val showSyncSummary: Boolean,
    val showAllNewElements: Boolean,
)

fun Conf.mapViewport(): BoundingBox {
    return BoundingBox(
        viewportNorthLat,
        viewportEastLon,
        viewportSouthLat,
        viewportWestLon,
    )
}