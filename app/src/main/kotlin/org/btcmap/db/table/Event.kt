package org.btcmap.db.table

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.ZonedDateTime

object EventSchema {
    const val TABLE_NAME = "event"

    override fun toString(): String {
        return """
            CREATE TABLE $TABLE_NAME (
                ${Columns.Id} INTEGER PRIMARY KEY NOT NULL,
                ${Columns.Lat} REAL NOT NULL,
                ${Columns.Lon} REAL NOT NULL,
                ${Columns.Name} TEXT NOT NULL,
                ${Columns.Website} TEXT NOT NULL,
                ${Columns.StartsAt} TEXT NOT NULL,
                ${Columns.EndsAt} TEXT;
            )
        """
    }

    enum class Columns(val sqlName: String) {
        Id("id"),
        Lat("lat"),
        Lon("lon"),
        Name("name"),
        Website("website"),
        StartsAt("starts_at"),
        EndsAt("ends_at");

        override fun toString() = sqlName
    }
}

typealias Event = EventProjectionFull

data class EventProjectionFull(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: HttpUrl,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime?,
) {
    companion object {
        val columns: String
            get() {
                return EventSchema.Columns.entries.joinToString(",") { it.sqlName }
            }

        fun fromStatement(stmt: SQLiteStatement): EventProjectionFull {
            return EventProjectionFull(
                id = stmt.getLong(0),
                lat = stmt.getDouble(1),
                lon = stmt.getDouble(2),
                name = stmt.getText(3),
                website = stmt.getText(4).toHttpUrl(),
                startsAt = ZonedDateTime.parse(stmt.getText(5)),
                endsAt = if (stmt.isNull(6)) null else ZonedDateTime.parse(stmt.getText(6)),
            )
        }
    }
}

class EventQueries(private val conn: SQLiteConnection) {

    fun insert(rows: List<Event>) {
        val sql = """
            INSERT INTO ${EventSchema.TABLE_NAME} (${Event.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
        """

        val stmt = conn.prepare(sql)

        stmt.use {
            rows.forEach { row ->
                it.bindLong(1, row.id)
                it.bindDouble(2, row.lat)
                it.bindDouble(3, row.lon)
                it.bindText(4, row.name)
                it.bindText(5, row.website.toString())
                it.bindText(6, row.startsAt.toString())
                if (row.endsAt == null) {
                    it.bindNull(7)
                } else {
                    it.bindText(7, row.endsAt.toString())
                }
                it.step()
            }
        }
    }

    fun selectAll(): List<Event> {
        val stmt = conn.prepare(
            """
                SELECT ${EventProjectionFull.columns}
                FROM ${EventSchema.TABLE_NAME}
            """
        )

        stmt.use {
            val rows = mutableListOf<Event>()

            while (it.step()) {
                rows.add(EventProjectionFull.fromStatement(it))
            }

            return rows
        }
    }

    fun selectById(id: Long): Event? {
        val stmt = conn.prepare(
            """
                SELECT ${EventProjectionFull.columns}
                FROM ${EventSchema.TABLE_NAME}
                WHERE id = ?1
            """
        )
        stmt.bindLong(1, id)

        stmt.use {
            if (it.step()) {
                return EventProjectionFull.fromStatement(it)
            }
            return null
        }
    }

    fun deleteAll() {
        conn.prepare("DELETE FROM ${EventSchema.TABLE_NAME}").use { it.step() }
    }
}