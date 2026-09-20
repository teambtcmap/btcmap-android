package org.btcmap.bundle

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.test.runTest
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.StringReader
import java.time.ZonedDateTime

class BundledEventsTest {
    private fun reader(json: String) = JsonReader(StringReader(json))

    private fun createDatabase(): Database = Database(BundledSQLiteDriver(), ":memory:")

    private fun event(id: Long, deletedAt: ZonedDateTime? = null) = Event(
        id = id,
        areaId = null,
        lat = 1.0,
        lon = 2.0,
        name = "Event $id",
        website = null,
        startsAt = ZonedDateTime.parse("2026-11-01T09:00:00Z"),
        endsAt = null,
        updatedAt = ZonedDateTime.parse("2026-01-02T00:00:00Z"),
        deletedAt = deletedAt,
    )

    /** A snapshot with [count] minimal but complete events, ids 1..count. */
    private fun snapshotJson(count: Int) = buildString {
        append('[')
        for (id in 1..count) {
            if (id > 1) append(',')
            append(
                """{"id":$id,"lat":1.0,"lon":2.0,"name":"Event $id",""" +
                    """"starts_at":"2026-11-01T09:00:00Z","updated_at":"2026-03-01T12:00:00Z"}""",
            )
        }
        append(']')
    }

    // --- parser -----------------------------------------------------------------

    @Test
    fun readBundledEvent_parsesSeededFields() {
        val json = """
            {
              "id": 7,
              "area_id": 42,
              "lat": 18.788,
              "lon": 99.0156,
              "name": "Bitcoin Half Marathon",
              "website": "https://www.bitcoinmarathon.org/",
              "starts_at": "2026-11-01T09:00:00+07:00",
              "ends_at": "2026-11-02T23:00:00+07:00",
              "updated_at": "2026-03-01T12:00:00Z",
              "unknown": "ignored"
            }
        """.trimIndent()

        val event = reader(json).readBundledEvent()

        Assert.assertEquals(7L, event.id)
        Assert.assertEquals(42L, event.areaId)
        Assert.assertEquals(18.788, event.lat, 0.0)
        Assert.assertEquals(99.0156, event.lon, 0.0)
        Assert.assertEquals("Bitcoin Half Marathon", event.name)
        Assert.assertEquals("https://www.bitcoinmarathon.org/", event.website.toString())
        Assert.assertEquals(ZonedDateTime.parse("2026-11-01T09:00:00+07:00"), event.startsAt)
        Assert.assertEquals(ZonedDateTime.parse("2026-11-02T23:00:00+07:00"), event.endsAt)
        Assert.assertEquals(ZonedDateTime.parse("2026-03-01T12:00:00Z"), event.updatedAt)
        Assert.assertNull(event.deletedAt)
    }

    @Test
    fun readBundledEvent_acceptsMissingOptionalFields() {
        val json = """
            {
              "id": 1,
              "lat": 1.0,
              "lon": 2.0,
              "name": "Meetup",
              "starts_at": "2026-11-01T09:00:00Z",
              "updated_at": "2026-03-01T12:00:00Z"
            }
        """.trimIndent()

        val event = reader(json).readBundledEvent()

        Assert.assertNull(event.areaId)
        Assert.assertNull(event.website)
        Assert.assertNull(event.endsAt)
    }

    @Test
    fun readBundledEvent_rejectsMissingRequiredFields() {
        val full = mapOf(
            "lat" to """"lat":1.0""",
            "lon" to """"lon":2.0""",
            "name" to """"name":"Meetup"""",
            "starts_at" to """"starts_at":"2026-11-01T09:00:00Z"""",
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
                reader(json).readBundledEvent()
                Assert.fail("expected missing '$field' to be rejected")
            } catch (e: IllegalArgumentException) {
                Assert.assertTrue(e.message.orEmpty().contains(field))
            }
        }
    }

    @Test
    fun readBundledEvent_rejectsUnparseableUpdatedAt() {
        val json = """
            {
              "id": 1,
              "lat": 1.0,
              "lon": 2.0,
              "name": "Meetup",
              "starts_at": "2026-11-01T09:00:00Z",
              "updated_at": "not-a-date"
            }
        """.trimIndent()

        try {
            reader(json).readBundledEvent()
            Assert.fail("expected an unparseable 'updated_at' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("updated_at"))
        }
    }

    @Test
    fun readBundledEvent_rejectsEmptyName() {
        val json = """
            {
              "id": 1,
              "lat": 1.0,
              "lon": 2.0,
              "name": "",
              "starts_at": "2026-11-01T09:00:00Z",
              "updated_at": "2026-03-01T12:00:00Z"
            }
        """.trimIndent()

        try {
            reader(json).readBundledEvent()
            Assert.fail("expected an empty 'name' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("name"))
        }
    }

    // --- seeding ----------------------------------------------------------------

    @Test
    fun importFrom_seedsEmptyDatabaseWithBundledRows() = runTest {
        val db = createDatabase()
        val json = """
            [
              {"id":1,"area_id":42,"lat":1.0,"lon":2.0,"name":"One","website":"https://example.com","starts_at":"2026-11-01T09:00:00Z","ends_at":"2026-11-02T09:00:00Z","updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"lat":3.0,"lon":4.0,"name":"Two","starts_at":"2026-12-01T09:00:00Z","updated_at":"2026-04-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledEvents.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(2L, result.eventsImported)
        Assert.assertFalse(result.duration.isNegative)
        Assert.assertEquals(2L, db.event.selectCount())

        val first = db.event.selectById(1L)
        Assert.assertNotNull(first)
        Assert.assertEquals(42L, first!!.areaId)
        Assert.assertEquals("One", first.name)
        Assert.assertEquals("https://example.com/", first.website.toString())
        Assert.assertEquals(ZonedDateTime.parse("2026-11-02T09:00:00Z"), first.endsAt)
        Assert.assertEquals(ZonedDateTime.parse("2026-03-01T12:00:00Z"), first.updatedAt)

        val second = db.event.selectById(2L)
        Assert.assertNotNull(second)
        Assert.assertNull(second!!.areaId)
        Assert.assertNull(second.endsAt)
    }

    @Test
    fun importFrom_skipsWhenDatabaseAlreadyHasEvents() = runTest {
        val db = createDatabase()
        db.event.insert(listOf(event(id = 99L)))

        var opened = false
        val result = BundledEvents.importFrom(db) {
            opened = true
            snapshotJson(1).byteInputStream()
        }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertFalse("snapshot must not be read once the seed is done", opened)
        Assert.assertEquals(1L, db.event.selectCount())
    }

    @Test
    fun importFrom_skipsWhenDatabaseHoldsOnlyTombstones() = runTest {
        val db = createDatabase()
        db.event.insert(
            listOf(event(id = 99L, deletedAt = ZonedDateTime.parse("2026-05-01T00:00:00Z"))),
        )

        val result = BundledEvents.importFrom(db) { snapshotJson(2).byteInputStream() }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals(1L, db.event.selectCount())
        Assert.assertTrue(db.event.selectAll().isEmpty())
    }

    @Test
    fun importFrom_isIdempotentAcrossRepeatedCalls() = runTest {
        val db = createDatabase()
        BundledEvents.importFrom(db) { snapshotJson(2).byteInputStream() }

        val second = BundledEvents.importFrom(db) { snapshotJson(5).byteInputStream() }

        Assert.assertEquals(0L, second.eventsImported)
        Assert.assertEquals(2L, db.event.selectCount())
    }

    @Test
    fun importFrom_emptySnapshotSeedsNothing() = runTest {
        val db = createDatabase()

        val result = BundledEvents.importFrom(db) { "[]".byteInputStream() }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals(0L, db.event.selectCount())
    }

    @Test
    fun importFrom_importsEveryRowAcrossBatchBoundaries() = runTest {
        val db = createDatabase()
        val count = BundledEvents.BATCH_SIZE + 1

        val result = BundledEvents.importFrom(db) { snapshotJson(count).byteInputStream() }

        Assert.assertEquals(count.toLong(), result.eventsImported)
        Assert.assertEquals(count.toLong(), db.event.selectCount())
        Assert.assertEquals(count, db.event.selectAll().size)
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

        BundledEvents.importFrom(db) { stream }

        Assert.assertTrue("the snapshot stream must be closed", closed)
    }

    // --- failure handling -------------------------------------------------------

    @Test
    fun importFrom_missingAssetIsNotAnError() = runTest {
        val db = createDatabase()

        val result = BundledEvents.importFrom(db) { throw FileNotFoundException("no asset") }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals(0L, db.event.selectCount())
    }

    @Test
    fun importFrom_malformedAssetRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        // The first entry is valid, the second is missing its required name.
        val json = """
            [
              {"id":1,"lat":1.0,"lon":2.0,"name":"One","starts_at":"2026-11-01T09:00:00Z","updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"lat":3.0,"lon":4.0,"starts_at":"2026-12-01T09:00:00Z","updated_at":"2026-03-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledEvents.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals("a partial seed must not survive a parse failure", 0L, db.event.selectCount())
    }

    @Test
    fun importFrom_invalidJsonRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        val json = """[{"id":1,"lat":1.0,"lon":2.0,"name":"One"},"""

        val result = BundledEvents.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals(0L, db.event.selectCount())
    }

    @Test
    fun importFrom_canBeRetriedAfterAFailedImport() = runTest {
        val db = createDatabase()
        BundledEvents.importFrom(db) {
            """[{"id":1,"lat":1.0,"lon":2.0}]""".byteInputStream()
        }
        Assert.assertEquals(0L, db.event.selectCount())

        val result = BundledEvents.importFrom(db) { snapshotJson(2).byteInputStream() }

        Assert.assertEquals(2L, result.eventsImported)
        Assert.assertEquals(2L, db.event.selectCount())
    }
}
