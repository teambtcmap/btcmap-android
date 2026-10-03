package org.btcmap.bundle

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okio.Buffer
import okio.ForwardingSource
import org.btcmap.json.parseJsonObject
import kotlinx.coroutines.test.runTest
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class BundledPlacesTest {

    private fun createDatabase(): Database = Database(BundledSQLiteDriver(), ":memory:")

    private fun place(id: Long, deletedAt: ZonedDateTime? = null) = Place(
        id = id,
        updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
        lat = 0.0,
        lon = 0.0,
        icon = "store",
        name = null,
        localizedName = null,
        verifiedAt = null,
        address = null,
        openingHours = null,
        phone = null,
        website = null,
        email = null,
        twitter = null,
        facebook = null,
        instagram = null,
        line = null,
        requiredAppUrl = null,
        boostedUntil = null,
        comments = null,
        telegram = null,
        osmId = null,
        deletedAt = deletedAt,
    )

    /** A snapshot with [count] minimal but complete places, ids 1..count. */
    private fun snapshotJson(count: Int) = buildString {
        append('[')
        for (id in 1..count) {
            if (id > 1) append(',')
            append("""{"id":$id,"lat":1.0,"lon":2.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}""")
        }
        append(']')
    }

    // --- parser -----------------------------------------------------------------

    @Test
    fun readBundledPlace_parsesSeededFields() {
        val json = """
            {
              "id": 42,
              "lat": 1.5,
              "lon": 2.5,
              "icon": "local_cafe",
              "name": "Cafe",
              "comments": 7,
              "boosted_until": "2026-02-01T00:00:00Z",
              "updated_at": "2026-03-01T12:00:00Z",
              "unknown": "ignored"
            }
        """.trimIndent()

        val place = parseBundledPlace(json)

        Assert.assertEquals(42L, place.id)
        Assert.assertEquals(1.5, place.lat, 0.0)
        Assert.assertEquals(2.5, place.lon, 0.0)
        Assert.assertEquals("local_cafe", place.icon)
        Assert.assertEquals("Cafe", place.name)
        Assert.assertEquals(7L, place.comments)
        Assert.assertEquals(ZonedDateTime.parse("2026-02-01T00:00:00Z"), place.boostedUntil)
        Assert.assertEquals(ZonedDateTime.parse("2026-03-01T12:00:00Z"), place.updatedAt)
    }

    @Test
    fun readBundledPlace_parsesEverySyncedField() {
        val json = """
            {
              "id": 7,
              "lat": 1.0,
              "lon": 2.0,
              "icon": "store",
              "name": "Shop",
              "localized_name": {"en": "Shop", "de": "Laden"},
              "updated_at": "2026-03-01T12:00:00Z",
              "verified_at": "2026-01-15",
              "address": "1 Main St",
              "opening_hours": "Mo-Fr 08:00-18:00",
              "phone": "+1234567890",
              "website": "https://example.com",
              "email": "a@example.com",
              "twitter": "https://x.com/example",
              "facebook": "https://facebook.com/example",
              "instagram": "https://instagram.com/example",
              "line": "https://line.me/example",
              "required_app_url": "https://example.com/app",
              "boosted_until": "2026-02-01T00:00:00Z",
              "comments": 3,
              "telegram": "https://t.me/example",
              "osm_id": "node:1"
            }
        """.trimIndent()

        val place = parseBundledPlace(json)

        Assert.assertEquals(parseJsonObject("""{"en":"Shop","de":"Laden"}"""), place.localizedName)
        Assert.assertEquals(
            ZonedDateTime.parse("2026-01-15T00:00:00Z"),
            place.verifiedAt,
        )
        Assert.assertEquals("1 Main St", place.address)
        Assert.assertEquals("Mo-Fr 08:00-18:00", place.openingHours)
        Assert.assertEquals("+1234567890", place.phone)
        Assert.assertEquals("https://example.com/", place.website.toString())
        Assert.assertEquals("a@example.com", place.email)
        Assert.assertEquals("https://x.com/example", place.twitter.toString())
        Assert.assertEquals("https://facebook.com/example", place.facebook.toString())
        Assert.assertEquals("https://instagram.com/example", place.instagram.toString())
        Assert.assertEquals("https://line.me/example", place.line.toString())
        Assert.assertEquals("https://example.com/app", place.requiredAppUrl.toString())
        Assert.assertEquals("https://t.me/example", place.telegram.toString())
        Assert.assertEquals("node:1", place.osmId)
        Assert.assertNull(place.deletedAt)
    }

    @Test
    fun readBundledPlace_acceptsExplicitNulls() {
        val json = """
            {
              "id": 1,
              "lat": 0.0,
              "lon": 0.0,
              "icon": "store",
              "name": null,
              "localized_name": null,
              "updated_at": "2026-03-01T12:00:00Z",
              "verified_at": null,
              "address": null,
              "opening_hours": null,
              "phone": null,
              "website": null,
              "email": null,
              "twitter": null,
              "facebook": null,
              "instagram": null,
              "line": null,
              "required_app_url": null,
              "boosted_until": null,
              "comments": null,
              "telegram": null,
              "osm_id": null
            }
        """.trimIndent()

        val place = parseBundledPlace(json)

        Assert.assertNull(place.name)
        Assert.assertNull(place.localizedName)
        Assert.assertNull(place.verifiedAt)
        Assert.assertNull(place.address)
        Assert.assertNull(place.website)
        Assert.assertNull(place.comments)
        Assert.assertNull(place.boostedUntil)
    }

    @Test
    fun readBundledPlace_acceptsMissingOptionalFields() {
        val json =
            """{"id":1,"lat":0.0,"lon":0.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}"""

        val place = parseBundledPlace(json)

        Assert.assertNull(place.name)
        Assert.assertNull(place.comments)
        Assert.assertNull(place.boostedUntil)
        Assert.assertEquals(ZonedDateTime.parse("2026-03-01T12:00:00Z"), place.updatedAt)
    }

    @Test
    fun readBundledPlace_degradedOptionalValuesBecomeNull() {
        val json = """
            {
              "id": 1,
              "lat": 0.0,
              "lon": 0.0,
              "icon": "store",
              "updated_at": "2026-03-01T12:00:00Z",
              "verified_at": "not-a-date",
              "boosted_until": "not-a-date",
              "localized_name": "not-an-object",
              "website": "not a url"
            }
        """.trimIndent()

        val place = parseBundledPlace(json)

        Assert.assertNull(place.verifiedAt)
        Assert.assertNull(place.boostedUntil)
        Assert.assertNull(place.localizedName)
        Assert.assertNull(place.website)
    }

    @Test
    fun readBundledPlace_rejectsMissingRequiredFields() {
        val cases = mapOf(
            "id" to """{"lat":0.0,"lon":0.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}""",
            "lat" to """{"id":1,"lon":0.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}""",
            "lon" to """{"id":1,"lat":0.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}""",
            "icon" to """{"id":1,"lat":0.0,"lon":0.0,"updated_at":"2026-03-01T12:00:00Z"}""",
            "updated_at" to """{"id":1,"lat":0.0,"lon":0.0,"icon":"store"}""",
        )

        cases.forEach { (field, json) ->
            try {
                parseBundledPlace(json)
                Assert.fail("expected missing '$field' to be rejected")
            } catch (e: IllegalArgumentException) {
                Assert.assertTrue(e.message.orEmpty().contains(field))
            }
        }
    }

    @Test
    fun readBundledPlace_rejectsUnparseableUpdatedAt() {
        val json = """{"id":1,"lat":0.0,"lon":0.0,"icon":"store","updated_at":"not-a-date"}"""

        try {
            parseBundledPlace(json)
            Assert.fail("expected an unparseable 'updated_at' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("updated_at"))
        }
    }

    @Test
    fun readBundledPlace_missingIdMessageNeverNamesANullId() {
        try {
            parseBundledPlace("""{"lat":0.0,"lon":0.0,"icon":"store"}""")
            Assert.fail("expected missing 'id' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertFalse(e.message.orEmpty().contains("null"))
        }
    }

    @Test
    fun readBundledPlace_missingRequiredFieldAfterIdNamesThePlace() {
        try {
            parseBundledPlace("""{"id":7,"lon":0.0,"icon":"store"}""")
            Assert.fail("expected missing 'lat' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("7"))
        }
    }

    @Test
    fun readBundledPlace_rejectsCoordinatesOutOfRange() {
        val cases = listOf(
            """{"id":1,"lat":90.1,"lon":0.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}""",
            """{"id":1,"lat":-90.1,"lon":0.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}""",
            """{"id":1,"lat":0.0,"lon":180.1,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}""",
            """{"id":1,"lat":0.0,"lon":-180.1,"icon":"store","updated_at":"2026-03-01T12:00:00Z"}""",
        )

        cases.forEach { json ->
            try {
                parseBundledPlace(json)
                Assert.fail("expected out-of-range coordinates in $json to be rejected")
            } catch (e: IllegalArgumentException) {
                Assert.assertTrue(e.message.orEmpty().contains("outside"))
            }
        }
    }

    @Test
    fun readBundledPlace_rejectsEmptyIcon() {
        try {
            parseBundledPlace("""{"id":1,"lat":0.0,"lon":0.0,"icon":"","updated_at":"2026-03-01T12:00:00Z"}""")
            Assert.fail("expected an empty 'icon' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("icon"))
        }
    }

    @Test
    fun readBundledPlace_treatsUnparseableBoostedUntilAsMissing() {
        val json = """{"id":1,"lat":0.0,"lon":0.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z","boosted_until":"not-a-date"}"""

        val place = parseBundledPlace(json)

        Assert.assertNull(place.boostedUntil)
        Assert.assertNotNull(place.id)
    }

    // --- seeding ----------------------------------------------------------------

    @Test
    fun import_seedsEmptyDatabaseWithBundledRows() = runTest {
        val db = createDatabase()
        val json = """
            [
              {"id":1,"lat":1.0,"lon":2.0,"icon":"store","name":"One","comments":3,"boosted_until":"2026-02-01T00:00:00Z","updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"lat":3.0,"lon":4.0,"icon":"cafe","name":null,"comments":null,"boosted_until":null,"updated_at":"2026-04-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledPlaces.import(db) { json.asSource() }

        Assert.assertEquals(2L, result.placesImported)
        Assert.assertFalse(result.duration.isNegative())
        Assert.assertEquals(2L, db.place.selectCount())

        val first = db.place.selectById(1L)
        Assert.assertNotNull(first)
        Assert.assertEquals(1L, first!!.id)
        Assert.assertEquals(1.0, first.lat, 0.0)
        Assert.assertEquals(2.0, first.lon, 0.0)
        Assert.assertEquals("store", first.icon)
        Assert.assertEquals("One", first.name)
        Assert.assertEquals(3L, first.comments)
        Assert.assertEquals(ZonedDateTime.parse("2026-02-01T00:00:00Z"), first.boostedUntil)
        Assert.assertEquals(ZonedDateTime.parse("2026-03-01T12:00:00Z"), first.updatedAt)
        val second = db.place.selectById(2L)
        Assert.assertNotNull(second)
        Assert.assertNull(second!!.name)
        Assert.assertNull(second.comments)
        Assert.assertNull(second.boostedUntil)
        Assert.assertEquals(ZonedDateTime.parse("2026-04-01T12:00:00Z"), second.updatedAt)
    }

    @Test
    fun import_skipsWhenDatabaseAlreadyHasPlaces() = runTest {
        val db = createDatabase()
        db.place.insert(listOf(place(id = 99L)))

        var opened = false
        val result = BundledPlaces.import(db) {
            opened = true
            snapshotJson(1).asSource()
        }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertFalse("snapshot must not be read once the seed is done", opened)
        Assert.assertEquals(1L, db.place.selectCount())
        Assert.assertEquals(99L, db.place.selectById(99L)!!.id)
    }

    @Test
    fun import_skipsWhenDatabaseHoldsOnlyTombstones() = runTest {
        val db = createDatabase()
        db.place.insert(
            listOf(place(id = 99L, deletedAt = ZonedDateTime.parse("2026-05-01T00:00:00Z"))),
        )

        val result = BundledPlaces.import(db) { snapshotJson(2).asSource() }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals(1L, db.place.selectCount(includeDeleted = true))
        Assert.assertEquals(0L, db.place.selectCount())
        Assert.assertNull(db.place.selectById(99L))
    }

    @Test
    fun import_isIdempotentAcrossRepeatedCalls() = runTest {
        val db = createDatabase()
        BundledPlaces.import(db) { snapshotJson(2).asSource() }

        val second = BundledPlaces.import(db) { snapshotJson(5).asSource() }

        Assert.assertEquals(0L, second.placesImported)
        Assert.assertEquals(2L, db.place.selectCount())
        Assert.assertNull(db.place.selectById(3L))
    }

    @Test
    fun import_reportsProgressAfterEachCommittedBatch() = runTest {
        val db = createDatabase()
        val count = BundledPlaces.BATCH_SIZE * 2 + 3
        val totals = mutableListOf<Long>()

        BundledPlaces.import(db, onBatch = { totals += it }) {
            snapshotJson(count).asSource()
        }

        Assert.assertEquals(
            listOf(
                BundledPlaces.BATCH_SIZE.toLong(),
                (BundledPlaces.BATCH_SIZE * 2).toLong(),
                count.toLong(),
            ),
            totals,
        )
    }

    @Test
    fun import_restartsAnInterruptedSeedInsteadOfTrustingItsRows() = runTest {
        val db = createDatabase()
        // A previous import committed one batch and was killed before it could
        // mark itself complete.
        db.place.insert(listOf(place(id = 99L)))
        db.preference.upsert(BundledPlaces.SEED_STATE_KEY, BundledPlaces.SEED_IN_PROGRESS)

        val result = BundledPlaces.import(db) { snapshotJson(2).asSource() }

        Assert.assertEquals(2L, result.placesImported)
        Assert.assertEquals(2L, db.place.selectCount())
        Assert.assertNull("the interrupted prefix must not survive", db.place.selectById(99L))
    }

    @Test
    fun import_emptySnapshotSeedsNothing() = runTest {
        val db = createDatabase()

        val result = BundledPlaces.import(db) { "[]".asSource() }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals(0L, db.place.selectCount())
    }

    @Test
    fun import_importsEveryRowAcrossBatchBoundaries() = runTest {
        val db = createDatabase()
        val count = BundledPlaces.BATCH_SIZE + 1

        val result = BundledPlaces.import(db) { snapshotJson(count).asSource() }

        Assert.assertEquals(count.toLong(), result.placesImported)
        Assert.assertEquals(count.toLong(), db.place.selectCount())
        Assert.assertNotNull(db.place.selectById(1L))
        Assert.assertNotNull(db.place.selectById(BundledPlaces.BATCH_SIZE.toLong()))
        Assert.assertNotNull(db.place.selectById(count.toLong()))
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

        BundledPlaces.import(db) { closing }

        Assert.assertTrue("the snapshot stream must be closed", closed)
    }

    // --- failure handling -------------------------------------------------------

    @Test
    fun import_missingAssetIsNotAnError() = runTest {
        val db = createDatabase()

        val result = BundledPlaces.import(db) { null }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals(0L, db.place.selectCount())
    }

    @Test
    fun import_malformedAssetRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        // The first entry is valid, the second is missing its required icon.
        val json = """[{"id":1,"lat":1.0,"lon":2.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z"},{"id":2,"lat":3.0,"lon":4.0}]"""

        val result = BundledPlaces.import(db) { json.asSource() }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals("a partial seed must not survive a parse failure", 0L, db.place.selectCount())
    }

    @Test
    fun import_invalidJsonRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        val json = """[{"id":1,"lat":1.0,"lon":2.0,"icon":"store"},"""

        val result = BundledPlaces.import(db) { json.asSource() }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals(0L, db.place.selectCount())
    }

    @Test
    fun import_canBeRetriedAfterAFailedImport() = runTest {
        val db = createDatabase()
        BundledPlaces.import(db) {
            """[{"id":1,"lat":1.0,"lon":2.0}]""".asSource()
        }
        Assert.assertEquals(0L, db.place.selectCount())

        val result = BundledPlaces.import(db) { snapshotJson(2).asSource() }

        Assert.assertEquals(2L, result.placesImported)
        Assert.assertEquals(2L, db.place.selectCount())
    }

    @Test
    fun import_badBoostedUntilDoesNotDiscardTheSeed() = runTest {
        val db = createDatabase()
        val json = """[{"id":1,"lat":1.0,"lon":2.0,"icon":"store","updated_at":"2026-03-01T12:00:00Z","boosted_until":"not-a-date"}]"""

        val result = BundledPlaces.import(db) { json.asSource() }

        Assert.assertEquals(1L, result.placesImported)
        Assert.assertEquals(1L, db.place.selectCount())
        Assert.assertNull(db.place.selectById(1L)!!.boostedUntil)
    }
}
