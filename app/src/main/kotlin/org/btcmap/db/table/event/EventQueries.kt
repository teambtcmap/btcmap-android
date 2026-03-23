package org.btcmap.db.table.event

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.table.event.EventProjectionFull.Companion.fromStatement

class EventQueries(private val conn: SQLiteConnection) {

    fun insert(rows: List<Event>) {
        val sql = """
            INSERT INTO ${EventSchema.NAME} (${Event.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
        """

        val stmt = conn.prepare(sql)

        stmt.use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindDouble(2, row.lat)
                stmt.bindDouble(3, row.lon)
                stmt.bindText(4, row.name)
                stmt.bindText(5, row.website.toString())
                stmt.bindText(6, row.startsAt.toString())
                if (row.endsAt == null) {
                    stmt.bindNull(7)
                } else {
                    stmt.bindText(7, row.endsAt.toString())
                }
                stmt.step()
            }
        }
    }

    fun selectAll(): List<Event> {
        val stmt = conn.prepare(
            """
                SELECT ${EventProjectionFull.columns}
                FROM ${EventSchema.NAME}
            """
        )

        stmt.use {
            val rows = mutableListOf<Event>()

            while (it.step()) {
                rows.add(fromStatement(it))
            }

            return rows
        }
    }

    fun selectById(id: Long): Event? {
        val stmt = conn.prepare(
            """
                SELECT ${EventProjectionFull.columns}
                FROM ${EventSchema.NAME}
                WHERE id = ?1
            """
        )
        stmt.bindLong(1, id)

        stmt.use {
            if (it.step()) {
                return fromStatement(it)
            }
            return null
        }
    }

    fun deleteAll() {
        conn.prepare("DELETE FROM ${EventSchema.NAME}").use { it.step() }
    }
}