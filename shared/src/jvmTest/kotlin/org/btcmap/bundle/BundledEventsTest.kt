package org.btcmap.bundle

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okio.Buffer
import okio.ForwardingSource
import kotlinx.coroutines.test.runTest
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant

class BundledEventsTest {

    private fun createDatabase(): Database = org.btcmap.db.testDatabase()

    private fun event(id: Long, deletedAt: Instant? = null) = Event(
        id = id,
        lat = 1.0,
        lon = 2.0,
        name = "Event $id",
        website = null,
        startsAt = Instant.parse("2026-11-01T09:00:00Z"),
        endsAt = null,
        updatedAt = Instant.parse("2026-01-02T00:00:00Z"),
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

        val event = parseBundledEvent(json)

        Assert.assertEquals(7L, event.id)
        Assert.assertEquals(18.788, event.lat, 0.0)
        Assert.assertEquals(99.0156, event.lon, 0.0)
        Assert.assertEquals("Bitcoin Half Marathon", event.name)
        Assert.assertEquals("https://www.bitcoinmarathon.org/", event.website.toString())
        Assert.assertEquals(Instant.parse("2026-11-01T09:00:00+07:00"), event.startsAt)
        Assert.assertEquals(Instant.parse("2026-11-02T23:00:00+07:00"), event.endsAt)
        Assert.assertEquals(Instant.parse("2026-03-01T12:00:00Z"), event.updatedAt)
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

        val event = parseBundledEvent(json)

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
                parseBundledEvent(json)
                Assert.fail("expected missing '$field' to be rejected")
            } catch (e: Exception) {
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
            parseBundledEvent(json)
            Assert.fail("expected an unparseable 'updated_at' to be rejected")
        } catch (_: Exception) {
            // The shared API mapper's date parser reports the raw value, not the
            // field name; the point is that a malformed cursor fails the seed.
        }
    }

    @Test
    fun readBundledEvent_keepsEmptyName() {
        // The seed now follows the API's field rules, which do not reject an
        // empty name; this documents the drop of the old seed-only check.
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

        Assert.assertEquals("", parseBundledEvent(json).name)
    }

    // --- seeding ----------------------------------------------------------------

    @Test
    fun import_seedsEmptyDatabaseWithBundledRows() = runTest {
        val db = createDatabase()
        val json = """
            [
              {"id":1,"lat":1.0,"lon":2.0,"name":"One","website":"https://example.com","starts_at":"2026-11-01T09:00:00Z","ends_at":"2026-11-02T09:00:00Z","updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"lat":3.0,"lon":4.0,"name":"Two","starts_at":"2026-12-01T09:00:00Z","updated_at":"2026-04-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledEvents.import(db) { json.asSource() }

        Assert.assertEquals(2L, result.eventsImported)
        Assert.assertFalse(result.duration.isNegative())
        Assert.assertEquals(2L, db.event.selectCount())

        val first = db.event.selectById(1L)
        Assert.assertNotNull(first)
        Assert.assertEquals("One", first!!.name)
        Assert.assertEquals("https://example.com", first.website.toString())
        Assert.assertEquals(Instant.parse("2026-11-02T09:00:00Z"), first.endsAt)
        Assert.assertEquals(Instant.parse("2026-03-01T12:00:00Z"), first.updatedAt)

        val second = db.event.selectById(2L)
        Assert.assertNotNull(second)
        Assert.assertNull(second!!.endsAt)
    }

    @Test
    fun import_skipsWhenDatabaseAlreadyHasEvents() = runTest {
        val db = createDatabase()
        db.event.insert(listOf(event(id = 99L)))

        var opened = false
        val result = BundledEvents.import(db) {
            opened = true
            snapshotJson(1).asSource()
        }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertFalse("snapshot must not be read once the seed is done", opened)
        Assert.assertEquals(1L, db.event.selectCount())
    }

    @Test
    fun import_skipsWhenDatabaseHoldsOnlyTombstones() = runTest {
        val db = createDatabase()
        db.event.insert(
            listOf(event(id = 99L, deletedAt = Instant.parse("2026-05-01T00:00:00Z"))),
        )

        val result = BundledEvents.import(db) { snapshotJson(2).asSource() }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals(1L, db.event.selectCount(includeDeleted = true))
        Assert.assertTrue(db.event.selectAll().isEmpty())
    }

    @Test
    fun import_isIdempotentAcrossRepeatedCalls() = runTest {
        val db = createDatabase()
        BundledEvents.import(db) { snapshotJson(2).asSource() }

        val second = BundledEvents.import(db) { snapshotJson(5).asSource() }

        Assert.assertEquals(0L, second.eventsImported)
        Assert.assertEquals(2L, db.event.selectCount())
    }

    @Test
    fun import_emptySnapshotSeedsNothing() = runTest {
        val db = createDatabase()

        val result = BundledEvents.import(db) { "[]".asSource() }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals(0L, db.event.selectCount())
    }

    @Test
    fun import_importsEveryRowAcrossBatchBoundaries() = runTest {
        val db = createDatabase()
        val count = BundledEvents.BATCH_SIZE + 1

        val result = BundledEvents.import(db) { snapshotJson(count).asSource() }

        Assert.assertEquals(count.toLong(), result.eventsImported)
        Assert.assertEquals(count.toLong(), db.event.selectCount())
        Assert.assertEquals(count, db.event.selectAll().size)
    }

    @Test
    fun import_closesTheStream() = runTest {
        val db = createDatabase()
        var closed = false
        val source = Buffer().writeUtf8(snapshotJson(1))
        val closing = object : ForwardingSource(source) {
            override fun close() {
                closed = true
                super.close()
            }
        }

        BundledEvents.import(db) { closing }

        Assert.assertTrue("the snapshot stream must be closed", closed)
    }

    // --- failure handling -------------------------------------------------------

    @Test
    fun import_missingAssetIsNotAnError() = runTest {
        val db = createDatabase()

        val result = BundledEvents.import(db) { null }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals(0L, db.event.selectCount())
    }

    @Test
    fun import_malformedAssetRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        // The first entry is valid, the second is missing its required name.
        val json = """
            [
              {"id":1,"lat":1.0,"lon":2.0,"name":"One","starts_at":"2026-11-01T09:00:00Z","updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"lat":3.0,"lon":4.0,"starts_at":"2026-12-01T09:00:00Z","updated_at":"2026-03-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledEvents.import(db) { json.asSource() }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals("a partial seed must not survive a parse failure", 0L, db.event.selectCount())
    }

    @Test
    fun import_invalidJsonRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        val json = """[{"id":1,"lat":1.0,"lon":2.0,"name":"One"},"""

        val result = BundledEvents.import(db) { json.asSource() }

        Assert.assertEquals(0L, result.eventsImported)
        Assert.assertEquals(0L, db.event.selectCount())
    }

    @Test
    fun import_canBeRetriedAfterAFailedImport() = runTest {
        val db = createDatabase()
        BundledEvents.import(db) {
            """[{"id":1,"lat":1.0,"lon":2.0}]""".asSource()
        }
        Assert.assertEquals(0L, db.event.selectCount())

        val result = BundledEvents.import(db) { snapshotJson(2).asSource() }

        Assert.assertEquals(2L, result.eventsImported)
        Assert.assertEquals(2L, db.event.selectCount())
    }
}
