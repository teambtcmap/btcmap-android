package reports

import androidx.sqlite.db.transaction
import db.getDate
import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class ReportQueries(private val db: SQLiteOpenHelper) {

    suspend fun insertOrReplace(reports: List<Report>) {
        withContext(Dispatchers.IO) {
            db.writableDatabase.transaction {
                reports.forEach {
                    execSQL(
                        """
                        INSERT OR REPLACE
                        INTO report(
                            area_id,
                            date,
                            tags,
                            created_at,
                            updated_at,
                            deleted_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?);
                        """,
                        arrayOf(
                            it.areaId,
                            it.date,
                            it.tags,
                            it.createdAt,
                            it.updatedAt,
                            it.deletedAt ?: "",
                        ),
                    )
                }
            }
        }
    }

    suspend fun selectByAreaId(areaId: String): List<Report> {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT
                    area_id,
                    date,
                    tags,
                    created_at,
                    updated_at,
                    deleted_at
                FROM report
                WHERE area_id = ?
                ORDER BY date;
                """,
                arrayOf(areaId),
            )

            buildList {
                while (cursor.moveToNext()) {
                    this += Report(
                        areaId = cursor.getString(0),
                        date = cursor.getDate(1),
                        tags = cursor.getJsonObject(2),
                        createdAt = cursor.getZonedDateTime(3)!!,
                        updatedAt = cursor.getZonedDateTime(4)!!,
                        deletedAt = cursor.getZonedDateTime(5),
                    )
                }
            }
        }
    }

    suspend fun selectMaxUpdatedAt(): ZonedDateTime? {
        return withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.query(
                """
                SELECT max(updated_at)
                FROM report;
                """
            )

            if (!cursor.moveToNext()) {
                return@withContext null
            }

            cursor.getZonedDateTime(0)
        }

    }
}