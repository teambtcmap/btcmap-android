package org.btcmap.bundle

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.test.runTest
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.StringReader
import java.time.ZonedDateTime

class BundledAreasTest {
    private fun reader(json: String) = JsonReader(StringReader(json))

    private fun createDatabase(): Database = Database(BundledSQLiteDriver(), ":memory:")

    private fun area(id: Long, deletedAt: ZonedDateTime? = null) = Area(
        id = id,
        name = "Area $id",
        type = "community",
        urlAlias = "area-$id",
        icon = null,
        iconWide = null,
        websiteUrl = "https://example.com/$id",
        description = null,
        bboxWest = null,
        bboxSouth = null,
        bboxEast = null,
        bboxNorth = null,
        geoJson = null,
        updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
        deletedAt = deletedAt,
    )

    /** A snapshot with [count] minimal but complete areas, ids 1..count. */
    private fun snapshotJson(count: Int) = buildString {
        append('[')
        for (id in 1..count) {
            if (id > 1) append(',')
            append(
                """{"id":$id,"name":"Area $id","type":"community","url_alias":"area-$id",""" +
                    """"website_url":"https://example.com/$id","updated_at":"2026-03-01T12:00:00Z"}""",
            )
        }
        append(']')
    }

    // --- parser -----------------------------------------------------------------

    @Test
    fun readBundledArea_parsesSeededFields() {
        val json = """
            {
              "id": 42,
              "name": "Grand Paris",
              "type": "community",
              "url_alias": "grand-paris",
              "icon": "https://static.btcmap.org/images/communities/grand-paris.jpg",
              "icon_wide": "https://static.btcmap.org/images/communities/grand-paris-wide.jpg",
              "website_url": "https://btcmap.org/community/grand-paris",
              "description": "A community",
              "bbox": [2.22, 48.81, 2.47, 48.91],
              "geo_json": {"type": "Polygon", "coordinates": [[[0.0, 0.0], [1.0, 0.0], [1.0, 1.0], [0.0, 0.0]]]},
              "updated_at": "2026-03-01T12:00:00Z",
              "unknown": "ignored"
            }
        """.trimIndent()

        val area = reader(json).readBundledArea()

        Assert.assertEquals(42L, area.id)
        Assert.assertEquals("Grand Paris", area.name)
        Assert.assertEquals("community", area.type)
        Assert.assertEquals("grand-paris", area.urlAlias)
        Assert.assertEquals(
            "https://static.btcmap.org/images/communities/grand-paris.jpg",
            area.icon,
        )
        Assert.assertEquals(
            "https://static.btcmap.org/images/communities/grand-paris-wide.jpg",
            area.iconWide,
        )
        Assert.assertEquals("https://btcmap.org/community/grand-paris", area.websiteUrl)
        Assert.assertEquals("A community", area.description)
        Assert.assertEquals(2.22, area.bboxWest!!, 0.0)
        Assert.assertEquals(48.81, area.bboxSouth!!, 0.0)
        Assert.assertEquals(2.47, area.bboxEast!!, 0.0)
        Assert.assertEquals(48.91, area.bboxNorth!!, 0.0)
        Assert.assertEquals(
            JsonParser.parseString(
                """{"type":"Polygon","coordinates":[[[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,0.0]]]}""",
            ),
            JsonParser.parseString(area.geoJson),
        )
        Assert.assertEquals(ZonedDateTime.parse("2026-03-01T12:00:00Z"), area.updatedAt)
        Assert.assertNull(area.deletedAt)
    }

    @Test
    fun readBundledArea_acceptsMissingOptionalFields() {
        val json = """
            {
              "id": 1,
              "name": "Community",
              "type": "community",
              "url_alias": "community",
              "website_url": "https://btcmap.org/community/community",
              "updated_at": "2026-03-01T12:00:00Z"
            }
        """.trimIndent()

        val area = reader(json).readBundledArea()

        Assert.assertNull(area.icon)
        Assert.assertNull(area.iconWide)
        Assert.assertNull(area.description)
        Assert.assertNull(area.bboxWest)
        Assert.assertNull(area.bboxSouth)
        Assert.assertNull(area.bboxEast)
        Assert.assertNull(area.bboxNorth)
        Assert.assertNull(area.geoJson)
    }

    @Test
    fun readBundledArea_treatsMalformedBboxAsAbsent() {
        val json = """
            {
              "id": 1,
              "name": "Community",
              "type": "community",
              "url_alias": "community",
              "website_url": "https://btcmap.org/community/community",
              "updated_at": "2026-03-01T12:00:00Z",
              "bbox": [1.0, 2.0, 3.0]
            }
        """.trimIndent()

        val area = reader(json).readBundledArea()

        Assert.assertNull(area.bboxWest)
        Assert.assertNull(area.bboxNorth)
    }

    @Test
    fun readBundledArea_rejectsMissingRequiredFields() {
        val full = mapOf(
            "name" to """"name":"Community"""",
            "type" to """"type":"community"""",
            "url_alias" to """"url_alias":"community"""",
            "website_url" to """"website_url":"https://btcmap.org/community/community"""",
            "updated_at" to """"updated_at":"2026-03-01T12:00:00Z"""",
        )
        val cases = buildMap {
            put("id", full.values.joinToString(separator = ",", prefix = "{", postfix = "}"))
            full.forEach { (omitted, _) ->
                put(
                    omitted,
                    full.filterKeys { it != omitted }
                        .values.joinToString(separator = ",", prefix = "{\"id\":1,", postfix = "}"),
                )
            }
        }

        cases.forEach { (field, json) ->
            try {
                reader(json).readBundledArea()
                Assert.fail("expected missing '$field' to be rejected")
            } catch (e: IllegalArgumentException) {
                Assert.assertTrue(e.message.orEmpty().contains(field))
            }
        }
    }

    @Test
    fun readBundledArea_rejectsUnparseableUpdatedAt() {
        val json = """
            {
              "id": 1,
              "name": "Community",
              "type": "community",
              "url_alias": "community",
              "website_url": "https://btcmap.org/community/community",
              "updated_at": "not-a-date"
            }
        """.trimIndent()

        try {
            reader(json).readBundledArea()
            Assert.fail("expected an unparseable 'updated_at' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("updated_at"))
        }
    }

    @Test
    fun readBundledArea_degradesNonObjectGeoJsonToNull() {
        val json = """
            {
              "id": 1,
              "name": "Community",
              "type": "community",
              "url_alias": "community",
              "website_url": "https://btcmap.org/community/community",
              "updated_at": "2026-03-01T12:00:00Z",
              "geo_json": "Polygon"
            }
        """.trimIndent()

        Assert.assertNull(reader(json).readBundledArea().geoJson)
    }

    // --- seeding ----------------------------------------------------------------

    @Test
    fun importFrom_seedsEmptyDatabaseWithBundledRows() = runTest {
        val db = createDatabase()
        val json = """
            [
              {"id":1,"name":"One","type":"community","url_alias":"one","website_url":"https://example.com/1","icon":"https://example.com/1.jpg","bbox":[1.0,2.0,3.0,4.0],"geo_json":{"type":"Polygon","coordinates":[]},"updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"name":"Two","type":"country","url_alias":"two","website_url":"https://example.com/2","description":"","updated_at":"2026-04-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledAreas.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(2L, result.areasImported)
        Assert.assertFalse(result.duration.isNegative)
        Assert.assertEquals(2L, db.area.selectCount())

        val first = db.area.selectById(1L)
        Assert.assertNotNull(first)
        Assert.assertEquals("One", first!!.name)
        Assert.assertEquals("community", first.type)
        Assert.assertEquals("one", first.urlAlias)
        Assert.assertEquals("https://example.com/1.jpg", first.icon)
        Assert.assertEquals(1.0, first.bboxWest!!, 0.0)
        Assert.assertEquals(4.0, first.bboxNorth!!, 0.0)
        Assert.assertNotNull(first.geoJson)
        Assert.assertEquals(ZonedDateTime.parse("2026-03-01T12:00:00Z"), first.updatedAt)

        val second = db.area.selectById(2L)
        Assert.assertNotNull(second)
        Assert.assertEquals("country", second!!.type)
        Assert.assertNull(second.bboxWest)
        Assert.assertNull(second.geoJson)
        Assert.assertEquals(ZonedDateTime.parse("2026-04-01T12:00:00Z"), second.updatedAt)
    }

    @Test
    fun importFrom_skipsWhenDatabaseAlreadyHasAreas() = runTest {
        val db = createDatabase()
        db.area.insert(listOf(area(id = 99L)))

        var opened = false
        val result = BundledAreas.importFrom(db) {
            opened = true
            snapshotJson(1).byteInputStream()
        }

        Assert.assertEquals(0L, result.areasImported)
        Assert.assertFalse("snapshot must not be read once the seed is done", opened)
        Assert.assertEquals(1L, db.area.selectCount())
        Assert.assertEquals(99L, db.area.selectById(99L)!!.id)
    }

    @Test
    fun importFrom_skipsWhenDatabaseHoldsOnlyTombstones() = runTest {
        val db = createDatabase()
        db.area.insert(
            listOf(area(id = 99L, deletedAt = ZonedDateTime.parse("2026-05-01T00:00:00Z"))),
        )

        val result = BundledAreas.importFrom(db) { snapshotJson(2).byteInputStream() }

        Assert.assertEquals(0L, result.areasImported)
        Assert.assertEquals(1L, db.area.selectCount(includeDeleted = true))
        Assert.assertEquals(0L, db.area.selectCount())
        Assert.assertTrue(db.area.selectAll().isEmpty())
        Assert.assertNull(db.area.selectById(99L))
    }

    @Test
    fun importFrom_isIdempotentAcrossRepeatedCalls() = runTest {
        val db = createDatabase()
        BundledAreas.importFrom(db) { snapshotJson(2).byteInputStream() }

        val second = BundledAreas.importFrom(db) { snapshotJson(5).byteInputStream() }

        Assert.assertEquals(0L, second.areasImported)
        Assert.assertEquals(2L, db.area.selectCount())
        Assert.assertNull(db.area.selectById(3L))
    }

    @Test
    fun importFrom_emptySnapshotSeedsNothing() = runTest {
        val db = createDatabase()

        val result = BundledAreas.importFrom(db) { "[]".byteInputStream() }

        Assert.assertEquals(0L, result.areasImported)
        Assert.assertEquals(0L, db.area.selectCount())
    }

    @Test
    fun importFrom_importsEveryRowAcrossBatchBoundaries() = runTest {
        val db = createDatabase()
        val count = BundledAreas.BATCH_SIZE + 1

        val result = BundledAreas.importFrom(db) { snapshotJson(count).byteInputStream() }

        Assert.assertEquals(count.toLong(), result.areasImported)
        Assert.assertEquals(count.toLong(), db.area.selectCount())
        Assert.assertNotNull(db.area.selectById(1L))
        Assert.assertNotNull(db.area.selectById(BundledAreas.BATCH_SIZE.toLong()))
        Assert.assertNotNull(db.area.selectById(count.toLong()))
    }

    @Test
    fun importFrom_closesTheStream() = runTest {
        val db = createDatabase()
        var closed = false
        val stream = object : ByteArrayInputStream(snapshotJson(1).toByteArray()) {
            override fun close() {
                closed = true
                super.close()
            }
        }

        BundledAreas.importFrom(db) { stream }

        Assert.assertTrue("the snapshot stream must be closed", closed)
    }

    // --- failure handling -------------------------------------------------------

    @Test
    fun importFrom_missingAssetIsNotAnError() = runTest {
        val db = createDatabase()

        val result = BundledAreas.importFrom(db) { throw FileNotFoundException("no asset") }

        Assert.assertEquals(0L, result.areasImported)
        Assert.assertEquals(0L, db.area.selectCount())
    }

    @Test
    fun importFrom_malformedAssetRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        // The first entry is valid, the second is missing its required name.
        val json = """
            [
              {"id":1,"name":"One","type":"community","url_alias":"one","website_url":"https://example.com/1","updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"type":"country","url_alias":"two","website_url":"https://example.com/2","updated_at":"2026-03-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledAreas.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(0L, result.areasImported)
        Assert.assertEquals("a partial seed must not survive a parse failure", 0L, db.area.selectCount())
    }

    @Test
    fun importFrom_invalidJsonRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        val json = """[{"id":1,"name":"One","type":"community"},"""

        val result = BundledAreas.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(0L, result.areasImported)
        Assert.assertEquals(0L, db.area.selectCount())
    }

    @Test
    fun importFrom_canBeRetriedAfterAFailedImport() = runTest {
        val db = createDatabase()
        BundledAreas.importFrom(db) {
            """[{"id":1,"name":"One","type":"community"}]""".byteInputStream()
        }
        Assert.assertEquals(0L, db.area.selectCount())

        val result = BundledAreas.importFrom(db) { snapshotJson(2).byteInputStream() }

        Assert.assertEquals(2L, result.areasImported)
        Assert.assertEquals(2L, db.area.selectCount())
    }
}
