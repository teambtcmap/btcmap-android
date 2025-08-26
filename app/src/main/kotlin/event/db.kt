package event

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.ZonedDateTime
import kotlin.collections.forEach

const val TABLE_NAME = "event"

enum class Columns(val sqlName: String) {
    Id("id"),
    Lat("lat"),
    Lon("lon"),
    Name("name"),
    Website("website"),
    StartsAt("starts_at"),
    EndsAt("ends_at");

    override fun toString(): String {
        return sqlName
    }
}

val SCHEMA = """
    CREATE TABLE $TABLE_NAME (
        ${Columns.Id} INTEGER PRIMARY KEY,
        ${Columns.Lat} REAL NOT NULL,
        ${Columns.Lon} REAL NOT NULL,
        ${Columns.Name} TEXT NOT NULL,
        ${Columns.Website} TEXT NOT NULL,
        ${Columns.StartsAt} TEXT NOT NULL,
        ${Columns.EndsAt} TEXT
    )
"""

fun deleteAllAndInsertBatch(events: List<Event>, conn: SQLiteConnection) {
    conn.execSQL("BEGIN TRANSACTION")
    try {
        deleteAll(conn)
        insertBatch(events, conn)
        conn.execSQL("END TRANSACTION")
    } catch (_: Throwable) {
        conn.execSQL("ROLLBACK TRANSACTION")
    }
}

fun insertBatch(events: List<Event>, conn: SQLiteConnection) {
    val sql = """
            INSERT
            INTO $TABLE_NAME (${Columns.entries.joinToString()}) 
            VALUES (${(1..Columns.entries.size).joinToString(", ") { "?$it" }})
        """
    val stmt = conn.prepare(sql)

    events.forEach { event ->
        stmt.reset()
        stmt.clearBindings()

        stmt.bindLong(1, event.id)
        stmt.bindDouble(2, event.lat)
        stmt.bindDouble(3, event.lon)
        stmt.bindText(4, event.name)
        stmt.bindText(5, event.website.toString())
        stmt.bindText(6, event.starts_at.toString())
        if (event.ends_at == null) {
            stmt.bindNull(7)
        } else {
            stmt.bindText(7, event.ends_at.toString())
        }

        stmt.step()
    }
}

fun selectAll(conn: SQLiteConnection): List<Event> {
    return conn.prepare(
        """
                SELECT ${Columns.entries.joinToString()}
                FROM $TABLE_NAME
                """
    ).use {
        buildList {
            while (it.step()) {
                add(it.toEvent())
            }
        }
    }
}

fun deleteAll(conn: SQLiteConnection) {
    conn.execSQL("DELETE FROM $TABLE_NAME")
}

private fun SQLiteStatement.toEvent(): Event {
    return Event(
        id = getLong(0),
        lat = getDouble(1),
        lon = getDouble(2),
        name = getText(3),
        website = getText(4).toHttpUrl(),
        starts_at = ZonedDateTime.parse(getText(5)),
        ends_at = if (isNull(6)) null else ZonedDateTime.parse(getText(6)),
    )
}