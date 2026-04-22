package org.btcmap.db.table.user

import androidx.sqlite.SQLiteStatement
import com.google.gson.JsonArray
import org.btcmap.db.getJsonArray

typealias User = FullProjection

data class FullProjection(
    val id: Long,
    val name: String,
    val roles: JsonArray,
    val savedPlaces: JsonArray,
    val savedAreas: JsonArray,
) {
    companion object {
        const val COLUMNS = "$ID, $NAME, $ROLES, $SAVED_PLACES, $SAVED_AREAS"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                name = stmt.getText(1),
                roles = stmt.getJsonArray(2),
                savedPlaces = stmt.getJsonArray(3),
                savedAreas = stmt.getJsonArray(4),
            )
        }
    }
}