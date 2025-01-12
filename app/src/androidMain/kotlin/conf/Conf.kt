package conf

import org.maplibre.android.geometry.LatLngBounds
import java.time.ZonedDateTime

data class Conf(
    val lastSyncDate: ZonedDateTime?,
    val viewportNorthLat: Double,
    val viewportEastLon: Double,
    val viewportSouthLat: Double,
    val viewportWestLon: Double,
    val showAtms: Boolean,
    val showSyncSummary: Boolean,
    val notifyOfNewElementsNearby: Boolean,
)

fun Conf.mapViewport(): LatLngBounds {
    return LatLngBounds.from(
        latNorth = viewportNorthLat,
        lonEast = viewportEastLon,
        latSouth = viewportSouthLat,
        lonWest = viewportWestLon,
    )
}