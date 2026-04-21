package org.btcmap.db.table.event

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindLongOrNull
import org.btcmap.db.bindTextOrNull
import org.btcmap.db.bindZonedDateTimeOrNull

class EventQueries(private val conn: SQLiteConnection) {
    fun insert(rows: List<Event>) {
        val sql = """
            INSERT INTO $TABLE (${FullProjection.COLUMNS}) 
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9);
        """

        conn.prepare(sql).use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindLongOrNull(2, row.areaId)
                stmt.bindDouble(3, row.lat)
                stmt.bindDouble(4, row.lon)
                stmt.bindText(5, row.name)
                stmt.bindText(6, row.website.toString())
                stmt.bindText(7, row.startsAt.toString())
                stmt.bindZonedDateTimeOrNull(8, row.endsAt)
                stmt.bindTextOrNull(9, row.cronSchedule)
                stmt.step()
                stmt.reset()
            }
        }
    }

    fun selectAll(): List<Event> {
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE;
            """
        ).use {
            val rows = mutableListOf<Event>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectById(id: Long): Event? {
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE id = ?1;
            """
        ).use {
            it.bindLong(1, id)
            if (it.step()) {
                return FullProjection.fromStatement(it)
            }
            return null
        }
    }

    fun selectByAreaId(areaId: Long): List<Event> {
        conn.prepare(
            """
                SELECT ${FullProjection.COLUMNS}
                FROM $TABLE
                WHERE area_id = ?1;
            """
        ).use {
            it.bindLong(1, areaId)
            val rows = mutableListOf<Event>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun deleteAll() {
        conn.prepare("DELETE FROM $TABLE;").use { it.step() }
    }
}
