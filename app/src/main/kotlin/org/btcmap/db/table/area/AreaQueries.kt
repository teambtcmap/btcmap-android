package org.btcmap.db.table.area

import androidx.sqlite.SQLiteConnection
import org.btcmap.db.bindDoubleOrNull
import org.btcmap.db.bindTextOrNull
import org.btcmap.db.bindZonedDateTime
import org.btcmap.db.bindZonedDateTimeOrNull
import org.btcmap.db.getZonedDateTimeOrNull
import java.time.ZonedDateTime

class AreaQueries(private val conn: SQLiteConnection) {
    fun insert(rows: List<Area>) {
        if (rows.isEmpty()) return

        // OR REPLACE, not a plain INSERT: the delta sync can return the same
        // area more than once (a soft delete bumps updated_at, and a page can
        // be re-read while widening the window), so a plain insert would abort
        // the whole sync transaction with a primary-key conflict.
        conn.prepare(
            """
            INSERT OR REPLACE INTO $TABLE ($ID, $NAME, $TYPE, $URL_ALIAS, $ICON, $ICON_WIDE, $WEBSITE_URL, $DESCRIPTION, $BBOX_WEST, $BBOX_SOUTH, $BBOX_EAST, $BBOX_NORTH, $GEO_JSON, $UPDATED_AT, $DELETED_AT)
            VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15);
            """
        ).use { stmt ->
            rows.forEach { row ->
                stmt.bindLong(1, row.id)
                stmt.bindText(2, row.name)
                stmt.bindText(3, row.type)
                stmt.bindText(4, row.urlAlias)
                stmt.bindTextOrNull(5, row.icon)
                stmt.bindTextOrNull(6, row.iconWide)
                stmt.bindText(7, row.websiteUrl)
                stmt.bindTextOrNull(8, row.description)
                stmt.bindDoubleOrNull(9, row.bboxWest)
                stmt.bindDoubleOrNull(10, row.bboxSouth)
                stmt.bindDoubleOrNull(11, row.bboxEast)
                stmt.bindDoubleOrNull(12, row.bboxNorth)
                stmt.bindTextOrNull(13, row.geoJson)
                stmt.bindZonedDateTime(14, row.updatedAt)
                stmt.bindZonedDateTimeOrNull(15, row.deletedAt)
                stmt.step()
                stmt.reset()
            }
        }
    }

    fun selectById(id: Long): Area? {
        conn.prepare(
            """
            SELECT ${FullProjection.COLUMNS}
            FROM $TABLE
            WHERE $ID = ?1 AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindLong(1, id)
            if (it.step()) {
                return FullProjection.fromStatement(it)
            }
            return null
        }
    }

    fun selectBySearchString(searchString: String): List<Area> {
        conn.prepare(
            """
            SELECT ${FullProjection.COLUMNS}
            FROM $TABLE
            WHERE UPPER($NAME) LIKE '%' || UPPER(?1) || '%'
                AND $DELETED_AT IS NULL;
            """
        ).use {
            it.bindText(1, searchString)
            val rows = mutableListOf<Area>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectAll(): List<Area> {
        conn.prepare(
            """
            SELECT ${FullProjection.COLUMNS}
            FROM $TABLE
            WHERE $DELETED_AT IS NULL;
            """
        ).use {
            val rows = mutableListOf<Area>()
            while (it.step()) {
                rows.add(FullProjection.fromStatement(it))
            }
            return rows
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        // See CommentQueries.selectMaxUpdatedAt: text ordering of
        // ZonedDateTime.toString() values is not chronological.
        conn.prepare(
            """
            SELECT $UPDATED_AT
            FROM $TABLE
            ORDER BY julianday($UPDATED_AT) DESC
            LIMIT 1;
            """
        ).use {
            if (!it.step()) {
                return null
            }
            return it.getZonedDateTimeOrNull(0)
        }
    }

    fun selectCount(): Long {
        conn.prepare("SELECT count(*) FROM $TABLE WHERE $DELETED_AT IS NULL;").use {
            it.step()
            return it.getLong(0)
        }
    }
}
