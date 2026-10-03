package org.btcmap.db.table.preference

import androidx.sqlite.SQLiteConnection

class PreferenceQueries(private val conn: SQLiteConnection) {

    suspend fun selectAll(): Map<String, String> {
        val rows = LinkedHashMap<String, String>()
        conn.prepare(
            """
            SELECT $KEY, $VALUE
            FROM $TABLE;
            """
        ).use { stmt ->
            while (stmt.step()) {
                rows[stmt.getText(0)] = stmt.getText(1)
            }
        }
        return rows
    }

    suspend fun select(key: String): String? {
        return conn.prepare(
            """
            SELECT $VALUE
            FROM $TABLE
            WHERE $KEY = ?1;
            """
        ).use { stmt ->
            stmt.bindText(1, key)
            if (stmt.step()) stmt.getText(0) else null
        }
    }

    suspend fun upsert(key: String, value: String) {
        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE ($KEY, $VALUE)
            VALUES (?1, ?2);
            """
        ).use { stmt ->
            stmt.bindText(1, key)
            stmt.bindText(2, value)
            stmt.step()
        }
    }

    suspend fun delete(key: String) {
        conn.prepare(
            """
            DELETE FROM $TABLE
            WHERE $KEY = ?1;
            """
        ).use { stmt ->
            stmt.bindText(1, key)
            stmt.step()
        }
    }

    suspend fun deleteAll() {
        conn.prepare("DELETE FROM $TABLE;").use { it.step() }
    }
}
