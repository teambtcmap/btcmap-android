package reports

import db.getDate
import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import java.time.ZonedDateTime

class ReportQueries(val db: SQLiteOpenHelper) {

    fun insertOrReplace(report: Report) {
        db.writableDatabase.execSQL(
            """
            INSERT OR REPLACE
            INTO report(
                id,
                area_id,
                date,
                tags,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?)
            """,
            arrayOf(
                report.id,
                report.areaId,
                report.date,
                report.tags,
                report.updatedAt,
            ),
        )
    }

    fun selectById(id: Long): Report? {
        val cursor = db.readableDatabase.query(
            """
                SELECT
                    id,
                    area_id,
                    date,
                    tags,
                    updated_at
                FROM report
                WHERE id = ?
                ORDER BY date
                """,
            arrayOf(id),
        )

        if (!cursor.moveToNext()) {
            return null
        }

        return Report(
            id = cursor.getLong(0),
            areaId = cursor.getLong(1),
            date = cursor.getDate(2),
            tags = cursor.getJsonObject(3),
            updatedAt = cursor.getZonedDateTime(4)!!,
        )
    }

    fun selectByAreaId(areaId: Long): List<Report> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                id,
                area_id,
                date,
                tags,
                updated_at
            FROM report
            WHERE area_id = ?
            ORDER BY date
            """,
            arrayOf(areaId),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    Report(
                        id = cursor.getLong(0),
                        areaId = cursor.getLong(1),
                        date = cursor.getDate(2),
                        tags = cursor.getJsonObject(3),
                        updatedAt = cursor.getZonedDateTime(4)!!,
                    )
                )
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        val cursor = db.readableDatabase.query("SELECT max(updated_at) FROM report")

        if (!cursor.moveToNext()) {
            return null
        }

        return cursor.getZonedDateTime(0)
    }

    fun selectCount(): Long {
        val cursor = db.readableDatabase.query("SELECT count(*) FROM report")
        cursor.moveToNext()
        return cursor.getLong(0)
    }

    fun deleteById(id: Long) {
        db.readableDatabase.query(
            "DELETE FROM report WHERE id = ?",
            arrayOf(id),
        )
    }
}