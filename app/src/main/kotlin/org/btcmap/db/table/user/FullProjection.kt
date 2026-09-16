package org.btcmap.db.table.user

import androidx.sqlite.SQLiteStatement

typealias User = FullProjection

data class FullProjection(
    val id: Long,
    val name: String,
    val roles: List<String>,
    val savedPlaces: List<SavedItem>,
    val savedAreas: List<SavedItem>,
) {
    companion object {
        const val COLUMNS = "$ID, $NAME, $ROLES, $SAVED_PLACES, $SAVED_AREAS"

        fun fromStatement(stmt: SQLiteStatement): FullProjection {
            return FullProjection(
                id = stmt.getLong(0),
                name = stmt.getText(1),
                roles = UserJson.rolesFromJson(stmt.getText(2)),
                savedPlaces = UserJson.savedItemsFromJson(stmt.getText(3)),
                savedAreas = UserJson.savedItemsFromJson(stmt.getText(4)),
            )
        }
    }
}
