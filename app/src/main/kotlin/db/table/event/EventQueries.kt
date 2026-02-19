package db.table.event

import android.database.sqlite.SQLiteDatabase
import db.table.event.EventProjectionFull.Companion.fromCursor

object EventQueries {
    fun insert(
        rows: List<Event>,
        db: SQLiteDatabase,
    ) {
        val sql = """
            INSERT INTO ${EventSchema.NAME} (${Event.columns}) 
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
        """

        val stmt = db.compileStatement(sql)

        stmt.use {
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindDouble(2, row.lat)
                stmt.bindDouble(3, row.lon)
                stmt.bindString(4, row.name)
                stmt.bindString(5, row.website.toString())
                stmt.bindString(6, row.startsAt.toString())
                if (row.endsAt == null) {
                    stmt.bindNull(7)
                } else {
                    stmt.bindString(7, row.endsAt.toString())
                }
                stmt.executeInsert()
            }
        }
    }

    fun selectAll(db: SQLiteDatabase): List<Event> {
        val cursor = db.rawQuery(
            """
                SELECT ${EventProjectionFull.columns}
                FROM ${EventSchema.NAME}
            """,
            null
        )

        cursor.use {
            val rows = mutableListOf<EventProjectionFull>()

            while (cursor.moveToNext()) {
                rows.add(fromCursor(cursor))
            }

            return rows
        }
    }

    fun selectById(id: Long, db: SQLiteDatabase): Event? {
        val cursor = db.rawQuery(
            """
                SELECT ${EventProjectionFull.columns}
                FROM ${EventSchema.NAME}
                WHERE id = ?
            """,
            arrayOf(id.toString())
        )

        cursor.use {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor)
            }
            return null
        }
    }

    fun deleteAll(db: SQLiteDatabase): Int {
        return db.delete(
            EventSchema.NAME,
            null,
            null,
        )
    }
}