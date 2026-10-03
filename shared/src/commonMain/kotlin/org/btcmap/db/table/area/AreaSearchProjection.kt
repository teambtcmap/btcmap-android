package org.btcmap.db.table.area

import androidx.sqlite.SQLiteStatement
import kotlinx.serialization.json.JsonObject
import org.btcmap.db.getDoubleOrNull
import org.btcmap.db.getJsonObjectOrNull
import org.btcmap.db.getTextOrNull

typealias SearchArea = AreaSearchProjection

/**
 * The fields an area search needs to rank and render a result: the name to
 * match and show, the icon to render, and the bounding box used to measure
 * distance and to move the camera when the row is tapped. Reading only these
 * instead of [FullProjection] keeps a broad query from materializing the full
 * GeoJSON polygon of every matching area.
 */
data class AreaSearchProjection(
    val id: Long,
    val name: String,
    val localizedName: JsonObject?,
    val icon: String?,
    val iconWide: String?,
    val bboxWest: Double?,
    val bboxSouth: Double?,
    val bboxEast: Double?,
    val bboxNorth: Double?,
) {
    companion object {
        const val COLUMNS =
            "$ID, $NAME, $LOCALIZED_NAME, $ICON, $ICON_WIDE, " +
                "$BBOX_WEST, $BBOX_SOUTH, $BBOX_EAST, $BBOX_NORTH"

        fun fromStatement(stmt: SQLiteStatement): AreaSearchProjection {
            return AreaSearchProjection(
                id = stmt.getLong(0),
                name = stmt.getText(1),
                localizedName = stmt.getJsonObjectOrNull(2),
                icon = stmt.getTextOrNull(3),
                iconWide = stmt.getTextOrNull(4),
                bboxWest = stmt.getDoubleOrNull(5),
                bboxSouth = stmt.getDoubleOrNull(6),
                bboxEast = stmt.getDoubleOrNull(7),
                bboxNorth = stmt.getDoubleOrNull(8),
            )
        }
    }
}
