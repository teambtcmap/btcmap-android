package conf

import java.time.ZonedDateTime

data class Conf(
    val lastSyncDate: ZonedDateTime?,
    val viewportNorthLat: Double,
    val viewportEastLon: Double,
    val viewportSouthLat: Double,
    val viewportWestLon: Double,
    val showTags: Boolean,
    val osmLogin: String,
    val osmPassword: String,
)
