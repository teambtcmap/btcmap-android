package org.btcmap.db.table.area

import androidx.sqlite.SQLiteStatement
import org.btcmap.db.getDoubleOrNull
import org.btcmap.db.getTextOrNull
import org.btcmap.db.getZonedDateTime
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

typealias Area = FullProjection

data class FullProjection(
    val id: Long,
    val name: String,
    val type: String,
    val urlAlias: String,
    val icon: String?,
    val iconWide: String?,
    val websiteUrl: String,
    val description: String?,
    // `[west, south, east, north]`, all null when the area has no bbox of its
    // own (the server omits a whole-world default).
    val bboxWest: Double?,
    val bboxSouth: Double?,
    val bboxEast: Double?,
    val bboxNorth: Double?,
    // Full GeoJSON geometry as returned by the server, or null when the area
    // has none. Stored verbatim; large polygons dominate the table size.
    val geoJson: String?,
    // Defaults to the epoch for callers that build an area for display only.
    // Sync writes the server's value; selectMaxUpdatedAt reads it back as the
    // delta cursor, and an epoch value simply means "sync from the beginning".
    val updatedAt: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC),
    val deletedAt: ZonedDateTime? = null,
) {
    companion object {
        const val COLUMNS =
            "$ID, $NAME, $TYPE, $URL_ALIAS, $ICON, $ICON_WIDE, $WEBSITE_URL, $DESCRIPTION, " +
                "$BBOX_WEST, $BBOX_SOUTH, $BBOX_EAST, $BBOX_NORTH, $GEO_JSON, $UPDATED_AT, $DELETED_AT"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                name = stmt.getText(1),
                type = stmt.getText(2),
                urlAlias = stmt.getText(3),
                icon = stmt.getTextOrNull(4),
                iconWide = stmt.getTextOrNull(5),
                websiteUrl = stmt.getText(6),
                description = stmt.getTextOrNull(7),
                bboxWest = stmt.getDoubleOrNull(8),
                bboxSouth = stmt.getDoubleOrNull(9),
                bboxEast = stmt.getDoubleOrNull(10),
                bboxNorth = stmt.getDoubleOrNull(11),
                geoJson = stmt.getTextOrNull(12),
                updatedAt = stmt.getZonedDateTime(13),
                deletedAt = stmt.getZonedDateTimeOrNull(14),
            )
        }
    }
}
