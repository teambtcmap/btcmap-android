package element

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.use
import db.getJsonArray
import db.getJsonObjectOld
import db.getText
import db.getZonedDateTime
import db.getZonedDateTimeOrNull
import db.transaction
import java.time.ZonedDateTime

class ElementQueries(private val conn: SQLiteConnection) {

    companion object {
        const val CREATE_TABLE = """
            CREATE TABLE element (
                id INTEGER NOT NULL PRIMARY KEY,
                overpass_data TEXT NOT NULL,
                tags TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                ext_lat REAL NOT NULL,
                ext_lon REAL NOT NULL
            );
            """
    }

    fun insertOrReplace(elements: List<Element>) {
        conn.transaction { conn ->
            elements.forEach { element ->
                conn.prepare(
                    """
                    INSERT OR REPLACE
                    INTO element (
                        id,
                        overpass_data,
                        tags,
                        updated_at,
                        ext_lat,
                        ext_lon
                    ) VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                    """
                ).use {
                    it.bindLong(1, element.id)
                    it.bindText(2, element.overpassData.toString())
                    it.bindText(3, element.tags.toString())
                    it.bindText(4, element.updatedAt)
                    it.bindDouble(5, element.lat)
                    it.bindDouble(6, element.lon)
                    it.step()
                }
            }
        }
    }

    fun selectById(id: Long): Element? {
        return conn.prepare(
            """
                SELECT
                    id,
                    overpass_data,
                    tags,
                    updated_at,
                    ext_lat,
                    ext_lon
                FROM element
                WHERE id = ?1
                """
        ).use {
            it.bindLong(1, id)

            if (it.step()) {
                Element(
                    id = it.getLong(0),
                    overpassData = it.getJsonObjectOld(1),
                    tags = it.getJsonObjectOld(2),
                    updatedAt = it.getText(3),
                    lat = it.getDouble(4),
                    lon = it.getDouble(5),
                )
            } else {
                null
            }
        }
    }

    fun selectBySearchString(searchString: String): List<Element> {
        return conn.prepare(
            """
                SELECT
                    id,
                    overpass_data,
                    tags,
                    updated_at,
                    ext_lat,
                    ext_lon
                FROM element
                WHERE UPPER(overpass_data) LIKE '%' || UPPER(?1) || '%'
                """
        ).use {
            it.bindText(1, searchString)

            buildList {
                while (it.step()) {
                    add(
                        Element(
                            id = it.getLong(0),
                            overpassData = it.getJsonObjectOld(1),
                            tags = it.getJsonObjectOld(2),
                            updatedAt = it.getText(3),
                            lat = it.getDouble(4),
                            lon = it.getDouble(5),
                        )
                    )
                }
            }
        }
    }

    fun selectByOsmTagValue(tagName: String, tagValue: String): List<Element> {
        return conn.prepare(
            """
                SELECT
                    id,
                    overpass_data,
                    tags,
                    updated_at,
                    ext_lat,
                    ext_lon            
                FROM element
                WHERE json_extract(overpass_data, '$.tags.$tagName') = ?1
                """
        ).use {
            it.bindText(1, tagValue)

            buildList {
                while (it.step()) {
                    add(
                        Element(
                            id = it.getLong(0),
                            overpassData = it.getJsonObjectOld(1),
                            tags = it.getJsonObjectOld(2),
                            updatedAt = it.getText(3),
                            lat = it.getDouble(4),
                            lon = it.getDouble(5),
                        )
                    )
                }
            }
        }
    }

    fun selectByBtcMapTagValue(tagName: String, tagValue: String): List<Element> {
        return conn.prepare(
            """
                SELECT
                    id,
                    overpass_data,
                    tags,
                    updated_at,
                    ext_lat,
                    ext_lon            
                FROM element
                WHERE json_extract(tags, '$.$tagName') = ?1
                """
        ).use {
            it.bindText(1, tagValue)

            buildList {
                while (it.step()) {
                    add(
                        Element(
                            id = it.getLong(0),
                            overpassData = it.getJsonObjectOld(1),
                            tags = it.getJsonObjectOld(2),
                            updatedAt = it.getText(3),
                            lat = it.getDouble(4),
                            lon = it.getDouble(5),
                        )
                    )
                }
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
        return conn.prepare(
            """
                SELECT
                    e.id,
                    e.ext_lat,
                    e.ext_lon,
                    json_extract(e.tags, '$.icon:android') AS icon_id,
                    json_extract(e.tags, '$.boost:expires') AS boost_expires,
                    json_extract(e.overpass_data, '$.tags.payment:lightning:requires_companion_app') AS requires_companion_app,
                    (SELECT count(*) from element_comment c WHERE c.element_id = e.id) AS comments
                FROM element e
                WHERE
                    json_extract(e.tags, '$.category') NOT IN (${excludedCategories.joinToString { "'$it'" }})
                    AND e.ext_lat > ?1
                    AND e.ext_lat < ?2
                    AND e.ext_lon > ?3
                    AND e.ext_lon < ?4
                ORDER BY e.ext_lat DESC
                """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)

            buildList {
                while (it.step()) {
                    add(
                        ElementsCluster(
                            count = 1,
                            id = it.getLong(0),
                            lat = it.getDouble(1),
                            lon = it.getDouble(2),
                            iconId = it.getText(3),
                            boostExpires = it.getZonedDateTimeOrNull(4),
                            requiresCompanionApp = it.getText(5, defaultValue = "no") == "yes",
                            comments = it.getLong(6),
                        )
                    )
                }
            }
        }
    }

    fun selectClusters(
        stepLat: Double,
        stepLon: Double,
        excludedCategories: List<String>,
    ): List<ElementsCluster> {
        return conn.prepare(
            """
                SELECT
                    count(*),
                    e.id,
                    avg(e.ext_lat) AS lat,
                    avg(e.ext_lon) AS lon,
                    json_extract(e.tags, '$.icon:android') AS icon_id,
                    json_extract(e.tags, '$.boost:expires') AS boost_expires,
                    json_extract(e.overpass_data, '$.tags.payment:lightning:requires_companion_app') AS requires_companion_app,
                    (SELECT count(*) from element_comment c WHERE c.element_id = e.id) AS comments
                FROM element e
                WHERE json_extract(e.tags, '$.category') NOT IN (${excludedCategories.joinToString { "'$it'" }})
                GROUP BY round(ext_lat / ?1) * ?1, round(ext_lon / ?2) * ?2
                ORDER BY e.ext_lat DESC
                """
        ).use {
            it.bindDouble(1, stepLat)
            it.bindDouble(2, stepLon)

            buildList {
                while (it.step()) {
                    add(
                        ElementsCluster(
                            count = it.getLong(0),
                            id = it.getLong(1),
                            lat = it.getDouble(2),
                            lon = it.getDouble(3),
                            iconId = it.getText(4),
                            boostExpires = it.getZonedDateTimeOrNull(5),
                            requiresCompanionApp = it.getText(6, defaultValue = "no") == "yes",
                            comments = it.getLong(7),
                        )
                    )
                }
            }
        }
    }

    fun selectByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ): List<AreaElement> {
        return conn.prepare(
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
                WHERE ext_lat > ?1 AND ext_lat < ?2 AND ext_lon > ?3 AND ext_lon < ?4
                """
        ).use {
            it.bindDouble(1, minLat)
            it.bindDouble(2, maxLat)
            it.bindDouble(3, minLon)
            it.bindDouble(4, maxLon)

            buildList {
                while (it.step()) {
                    add(
                        AreaElement(
                            id = it.getLong(0),
                            lat = it.getDouble(1),
                            lon = it.getDouble(2),
                            icon = it.getText(3),
                            osmTags = it.getJsonObjectOld(4),
                            issues = it.getJsonArray(5),
                            osmType = it.getText(6),
                            osmId = it.getLong(7),
                        )
                    )
                }
            }
        }
    }

    fun selectMaxUpdatedAt(): ZonedDateTime? {
        return conn.prepare("SELECT max(updated_at) FROM element").use {
            if (it.step()) {
                it.getZonedDateTimeOrNull(0)
            } else {
                null
            }
        }
    }

    fun selectCount(): Long {
        return conn.prepare("SELECT count(*) FROM element").use {
            it.step()
            it.getLong(0)
        }
    }

    fun deleteById(id: Long) {
        conn.prepare("DELETE FROM element WHERE id = ?1").use {
            it.bindLong(1, id)
            it.step()
        }
    }
}