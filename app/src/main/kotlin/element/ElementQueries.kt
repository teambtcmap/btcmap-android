package element

import androidx.core.database.getStringOrNull
import db.getJsonArray
import db.getJsonObject
import db.getZonedDateTime
import io.requery.android.database.sqlite.SQLiteOpenHelper
import java.time.ZonedDateTime

class ElementQueries(val db: SQLiteOpenHelper) {

    fun insertOrReplace(element: Element) {
        db.writableDatabase.execSQL(
            """
            INSERT OR REPLACE
            INTO element (
                id,
                overpass_data,
                tags,
                updated_at,
                ext_lat,
                ext_lon
            ) VALUES (?, ?, ?, ?, ?, ?)
            """,
            arrayOf(
                element.id,
                element.overpassData,
                element.tags,
                element.updatedAt,
                element.lat,
                element.lon,
            ),
        )
    }

    fun selectById(id: Long): Element? {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                id,
                overpass_data,
                tags,
                updated_at,
                ext_lat,
                ext_lon
            FROM element
            WHERE id = ?
            """,
            arrayOf(id),
        )

        if (!cursor.moveToNext()) {
            return null
        }

        return Element(
            id = cursor.getLong(0),
            overpassData = cursor.getJsonObject(1),
            tags = cursor.getJsonObject(2),
            updatedAt = cursor.getString(3)!!,
            lat = cursor.getDouble(4),
            lon = cursor.getDouble(5),
        )
    }

    fun selectBySearchString(searchString: String): List<Element> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                id,
                overpass_data,
                tags,
                updated_at,
                ext_lat,
                ext_lon
            FROM element
            WHERE UPPER(overpass_data) LIKE '%' || UPPER(?) || '%'
            """,
            arrayOf(searchString),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    Element(
                        id = cursor.getLong(0),
                        overpassData = cursor.getJsonObject(1),
                        tags = cursor.getJsonObject(2),
                        updatedAt = cursor.getString(3)!!,
                        lat = cursor.getDouble(4),
                        lon = cursor.getDouble(5),
                    )
                )
            }
        }
    }

    fun selectByOsmTagValue(tagName: String, tagValue: String): List<Element> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                id,
                overpass_data,
                tags,
                updated_at,
                ext_lat,
                ext_lon            
            FROM element
            WHERE json_extract(overpass_data, '$.tags.$tagName') = ?
            """,
            arrayOf(tagValue),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    Element(
                        id = cursor.getLong(0),
                        overpassData = cursor.getJsonObject(1),
                        tags = cursor.getJsonObject(2),
                        updatedAt = cursor.getString(3)!!,
                        lat = cursor.getDouble(4),
                        lon = cursor.getDouble(5),
                    )
                )
            }
        }
    }

    fun selectByBtcMapTagValue(tagName: String, tagValue: String): List<Element> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                id,
                overpass_data,
                tags,
                updated_at,
                ext_lat,
                ext_lon            
            FROM element
            WHERE json_extract(tags, '$.$tagName') = ?
            """,
            arrayOf(tagValue),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    Element(
                        id = cursor.getLong(0),
                        overpassData = cursor.getJsonObject(1),
                        tags = cursor.getJsonObject(2),
                        updatedAt = cursor.getString(3)!!,
                        lat = cursor.getDouble(4),
                        lon = cursor.getDouble(5),
                    )
                )
            }
        }
    }

    fun selectWithoutClustering(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        excludedCategories: List<String>,
    ): List<ElementsCluster> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                id,
                ext_lat,
                ext_lon,
                json_extract(tags, '$.icon:android') AS icon_id,
                json_extract(tags, '$.boost:expires') AS boost_expires,
                json_extract(overpass_data, '$.tags.payment:lightning:requires_companion_app') AS requires_companion_app
            FROM element
            WHERE
                json_extract(tags, '$.category') NOT IN (${excludedCategories.joinToString { "'$it'" }})
                AND ext_lat > ?
                AND ext_lat < ?
                AND ext_lon > ?
                AND ext_lon < ?
            ORDER BY ext_lat DESC
            """,
            arrayOf(
                minLat,
                maxLat,
                minLon,
                maxLon,
            ),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    ElementsCluster(
                        count = 1,
                        id = cursor.getLong(0),
                        lat = cursor.getDouble(1),
                        lon = cursor.getDouble(2),
                        iconId = cursor.getString(3),
                        boostExpires = cursor.getZonedDateTime(4),
                        requiresCompanionApp = (cursor.getStringOrNull(5) ?: "no") == "yes"
                    )
                )
            }
        }
    }

    fun selectClusters(
        step: Double,
        excludedCategories: List<String>,
    ): List<ElementsCluster> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                count(*),
                e.id,
                avg(e.ext_lat) AS lat,
                avg(e.ext_lon) AS lon,
                json_extract(e.tags, '$.icon:android') AS icon_id,
                json_extract(e.tags, '$.boost:expires') AS boost_expires,
                json_extract(e.overpass_data, '$.tags.payment:lightning:requires_companion_app') AS requires_companion_app
            FROM element e
            WHERE json_extract(e.tags, '$.category') NOT IN (${excludedCategories.joinToString { "'$it'" }})
            GROUP BY round(ext_lat / ?) * ?, round(ext_lon / ?) * ?
            ORDER BY e.ext_lat DESC
            """,
            arrayOf(step, step, step, step),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    ElementsCluster(
                        count = cursor.getLong(0),
                        id = cursor.getLong(1),
                        lat = cursor.getDouble(2),
                        lon = cursor.getDouble(3),
                        iconId = cursor.getString(4),
                        boostExpires = cursor.getZonedDateTime(5),
                        requiresCompanionApp = (cursor.getStringOrNull(6) ?: "no") == "yes"
                    )
                )
            }
        }
    }

    fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<AreaElement> {
        val cursor = db.readableDatabase.query(
            """
            SELECT
                id,
                ext_lat,
                ext_lon,
                json_extract(tags, '$.icon:android') AS icon_id,
                json_extract(overpass_data, '$.tags') AS osm_tags,
                json_extract(tags, '$.issues') AS issues,
                json_extract(overpass_data, '$.type') AS osm_type,
                json_extract(overpass_data, '$.id') AS osm_id
            FROM element
            WHERE ext_lat > ? AND ext_lat < ? AND ext_lon > ? AND ext_lon < ?
            """,
            arrayOf(
                minLat,
                maxLat,
                minLon,
                maxLon,
            ),
        )

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    AreaElement(
                        id = cursor.getLong(0),
                        lat = cursor.getDouble(1),
                        lon = cursor.getDouble(2),
                        icon = cursor.getString(3),
                        osmTags = cursor.getJsonObject(4),
                        issues = cursor.getJsonArray(5),
                        osmType = cursor.getString(6),
                        osmId = cursor.getLong(7),
                    )
                )
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        val cursor = db.readableDatabase.query("SELECT max(updated_at) FROM element")

        if (!cursor.moveToNext()) {
            return null
        }

        return cursor.getZonedDateTime(0)
    }

    fun selectCount(): Long {
        val cursor = db.readableDatabase.query("SELECT count(*) FROM element")
        cursor.moveToNext()
        return cursor.getLong(0)
    }

    fun deleteById(id: Long) {
        db.readableDatabase.query(
            "DELETE FROM element WHERE id = ?",
            arrayOf(id),
        )
    }
}