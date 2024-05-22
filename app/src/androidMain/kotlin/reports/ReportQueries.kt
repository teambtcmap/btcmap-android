package reports

import androidx.sqlite.use
import db.Database
import db.getDate
import db.getJsonObjectOld
import db.getZonedDateTime
import java.time.ZonedDateTime

class ReportQueries(private val db: Database) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE report (
                id INTEGER PRIMARY KEY,
                area_id INTEGER NOT NULL,
                date TEXT NOT NULL,
                tags TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """
    }

    fun insertOrReplace(reports: List<Report>) {
        db.transaction { conn ->
            reports.forEach { report ->
                conn.prepare(
                    """
                    INSERT OR REPLACE
                    INTO report(
                        id,
                        area_id,
                        date,
                        tags,
                        updated_at
                    )
                    VALUES (?1, ?2, ?3, ?4, ?5)
                    """
                ).use {
                    report.apply {
                        it.bindLong(1, id)
                        it.bindLong(2, areaId)
                        it.bindText(3, date.toString())
                        it.bindText(4, tags.toString())
                        it.bindText(5, updatedAt.toString())
                    }

                    it.step()
                }
            }
        }
    }

    fun selectById(id: Long): Report? {
        return db.withConn { conn ->
            conn.prepare(
                """
                SELECT
                    id,
                    area_id,
                    date,
                    tags,
                    updated_at
                FROM report
                WHERE id = ?1
                ORDER BY date
                """
            ).use {
                it.bindLong(1, id)

                if (it.step()) {
                    Report(
                        id = it.getLong(0),
                        areaId = it.getLong(1),
                        date = it.getDate(2),
                        tags = it.getJsonObjectOld(3),
                        updatedAt = it.getZonedDateTime(4),
                    )
                } else {
                    null
                }
            }
        }
    }

    fun selectByAreaId(areaId: Long): List<Report> {
        return db.withConn { conn ->
            conn.prepare(
                """
                SELECT
                    id,
                    area_id,
                    date,
                    tags,
                    updated_at
                FROM report
                WHERE area_id = ?1
                ORDER BY date
                """
            ).use {
                it.bindLong(1, areaId)

                buildList {
                    while (it.step()) {
                        add(
                            Report(
                                id = it.getLong(0),
                                areaId = it.getLong(1),
                                date = it.getDate(2),
                                tags = it.getJsonObjectOld(3),
                                updatedAt = it.getZonedDateTime(4),
                            )
                        )
                    }
                }
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        return db.withConn { conn ->
            conn.prepare("SELECT max(updated_at) FROM report").use {
                if (it.step()) {
                    it.getZonedDateTime(0)
                } else {
                    null
                }
            }
        }
    }

    fun selectCount(): Long {
        return db.withConn { conn ->
            conn.prepare("SELECT count(*) FROM report").use {
                it.step()
                it.getLong(0)
            }
        }
    }

    fun deleteById(id: Long) {
        db.withConn { conn ->
            conn.prepare("DELETE FROM report WHERE id = ?1").use {
                it.bindLong(1, id)
                it.step()
            }
        }
    }
}