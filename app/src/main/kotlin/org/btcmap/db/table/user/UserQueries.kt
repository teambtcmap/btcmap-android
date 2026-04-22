package org.btcmap.db.table.user

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindJsonArray

class UserQueries(private val conn: SQLiteConnection) {

    fun insert(row: User) {
        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE ($ID, $NAME, $ROLES, $SAVED_PLACES, $SAVED_AREAS)
            VALUES (?1, ?2, ?3, ?4, ?5);
            """
        ).use { stmt ->
            stmt.bindLong(1, row.id)
            stmt.bindText(2, row.name)
            stmt.bindJsonArray(3, row.roles)
            stmt.bindJsonArray(4, row.savedPlaces)
            stmt.bindJsonArray(5, row.savedAreas)
            stmt.step()
        }
    }

    fun select(): User? {
        return conn.prepare(
            """
            SELECT ${FullProjection.COLUMNS}
            FROM $TABLE;
            """
        ).use {
            if (it.step()) {
                FullProjection.fromStatement(it)
            } else {
                null
            }
        }
    }

    fun delete() {
        conn.prepare("DELETE FROM $TABLE;").use { it.step() }
    }
}