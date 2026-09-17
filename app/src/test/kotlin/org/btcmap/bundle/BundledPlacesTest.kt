package org.btcmap.bundle

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.test.runTest
import org.btcmap.db.Database
import org.btcmap.db.table.place.Place
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.StringReader
import java.time.ZonedDateTime

class BundledPlacesTest {
    private fun reader(json: String) = JsonReader(StringReader(json))

    private fun createDatabase(): Database = Database(BundledSQLiteDriver(), ":memory:")

    private fun place(id: Long) = Place(
        id = id,
        bundled = false,
        updatedAt = ZonedDateTime.parse("2024-01-01T00:00:00Z"),
        lat = 0.0,
        lon = 0.0,
        icon = "store",
        name = null,
        localizedName = null,
        verifiedAt = null,
        address = null,
        openingHours = null,
        localizedOpeningHours = null,
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
    )

    /** A snapshot with [count] minimal but complete places, ids 1..count. */
    private fun snapshotJson(count: Int) = buildString {
        append('[')
        for (id in 1..count) {
            if (id > 1) append(',')
            append("""{"id":$id,"lat":1.0,"lon":2.0,"icon":"store"}""")
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
              "unknown": "ignored"
            }
        """.trimIndent()

        val place = reader(json).readBundledPlace()

        Assert.assertEquals(42L, place.id)
        Assert.assertEquals(1.5, place.lat, 0.0)
        Assert.assertEquals(2.5, place.lon, 0.0)
        Assert.assertEquals("local_cafe", place.icon)
        Assert.assertEquals("Cafe", place.name)
        Assert.assertEquals(7L, place.comments)
        Assert.assertEquals(ZonedDateTime.parse("2026-02-01T00:00:00Z"), place.boostedUntil)
        Assert.assertTrue(place.bundled)
        Assert.assertEquals(ZonedDateTime.parse("2000-01-01T00:00:00Z"), place.updatedAt)
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
              "comments": null,
              "boosted_until": null
            }
        """.trimIndent()

        val place = reader(json).readBundledPlace()

        Assert.assertNull(place.name)
        Assert.assertNull(place.comments)
        Assert.assertNull(place.boostedUntil)
    }

    @Test
    fun readBundledPlace_acceptsMissingOptionalFields() {
        val json = """{"id":1,"lat":0.0,"lon":0.0,"icon":"store"}"""

        val place = reader(json).readBundledPlace()

        Assert.assertNull(place.name)
        Assert.assertNull(place.comments)
        Assert.assertNull(place.boostedUntil)
    }

    @Test
    fun readBundledPlace_rejectsMissingRequiredFields() {
        val cases = mapOf(
            "id" to """{"lat":0.0,"lon":0.0,"icon":"store"}""",
            "lat" to """{"id":1,"lon":0.0,"icon":"store"}""",
            "lon" to """{"id":1,"lat":0.0,"icon":"store"}""",
            "icon" to """{"id":1,"lat":0.0,"lon":0.0}""",
        )

        cases.forEach { (field, json) ->
            try {
                reader(json).readBundledPlace()
                Assert.fail("expected missing '$field' to be rejected")
            } catch (e: IllegalArgumentException) {
                Assert.assertTrue(e.message.orEmpty().contains(field))
            }
        }
    }

    @Test
    fun readBundledPlace_missingIdMessageNeverNamesANullId() {
        try {
            reader("""{"lat":0.0,"lon":0.0,"icon":"store"}""").readBundledPlace()
            Assert.fail("expected missing 'id' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertFalse(e.message.orEmpty().contains("null"))
        }
    }

    @Test
    fun readBundledPlace_missingRequiredFieldAfterIdNamesThePlace() {
        try {
            reader("""{"id":7,"lon":0.0,"icon":"store"}""").readBundledPlace()
            Assert.fail("expected missing 'lat' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("7"))
        }
    }

    @Test
    fun readBundledPlace_rejectsCoordinatesOutOfRange() {
        val cases = listOf(
            """{"id":1,"lat":90.1,"lon":0.0,"icon":"store"}""",
            """{"id":1,"lat":-90.1,"lon":0.0,"icon":"store"}""",
            """{"id":1,"lat":0.0,"lon":180.1,"icon":"store"}""",
            """{"id":1,"lat":0.0,"lon":-180.1,"icon":"store"}""",
        )

        cases.forEach { json ->
            try {
                reader(json).readBundledPlace()
                Assert.fail("expected out-of-range coordinates in $json to be rejected")
            } catch (e: IllegalArgumentException) {
                Assert.assertTrue(e.message.orEmpty().contains("outside"))
            }
        }
    }

    @Test
    fun readBundledPlace_rejectsEmptyIcon() {
        try {
            reader("""{"id":1,"lat":0.0,"lon":0.0,"icon":""}""").readBundledPlace()
            Assert.fail("expected an empty 'icon' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("icon"))
        }
    }

    @Test
    fun readBundledPlace_treatsUnparseableBoostedUntilAsMissing() {
        val json = """{"id":1,"lat":0.0,"lon":0.0,"icon":"store","boosted_until":"not-a-date"}"""

        val place = reader(json).readBundledPlace()

        Assert.assertNull(place.boostedUntil)
        Assert.assertNotNull(place.id)
    }

    // --- seeding ----------------------------------------------------------------

    @Test
    fun importFrom_seedsEmptyDatabaseWithBundledRows() = runTest {
        val db = createDatabase()
        val json = """
            [
              {"id":1,"lat":1.0,"lon":2.0,"icon":"store","name":"One","comments":3,"boosted_until":"2026-02-01T00:00:00Z"},
              {"id":2,"lat":3.0,"lon":4.0,"icon":"cafe","name":null,"comments":null,"boosted_until":null}
            ]
        """.trimIndent()

        val result = BundledPlaces.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(2L, result.placesImported)
        Assert.assertFalse(result.duration.isNegative)
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
        Assert.assertTrue(first.bundled)
        Assert.assertEquals(ZonedDateTime.parse("2000-01-01T00:00:00Z"), first.updatedAt)

        val second = db.place.selectById(2L)
        Assert.assertNotNull(second)
        Assert.assertNull(second!!.name)
        Assert.assertNull(second.comments)
        Assert.assertNull(second.boostedUntil)
        Assert.assertTrue(second.bundled)
    }

    @Test
    fun importFrom_skipsWhenDatabaseAlreadyHasPlaces() = runTest {
        val db = createDatabase()
        db.place.insert(listOf(place(id = 99L)))

        var opened = false
        val result = BundledPlaces.importFrom(db) {
            opened = true
            snapshotJson(1).byteInputStream()
        }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertFalse("snapshot must not be read once the seed is done", opened)
        Assert.assertEquals(1L, db.place.selectCount())
        Assert.assertEquals(99L, db.place.selectById(99L)!!.id)
    }

    @Test
    fun importFrom_isIdempotentAcrossRepeatedCalls() = runTest {
        val db = createDatabase()
        BundledPlaces.importFrom(db) { snapshotJson(2).byteInputStream() }

        val second = BundledPlaces.importFrom(db) { snapshotJson(5).byteInputStream() }

        Assert.assertEquals(0L, second.placesImported)
        Assert.assertEquals(2L, db.place.selectCount())
        Assert.assertNull(db.place.selectById(3L))
    }

    @Test
    fun importFrom_emptySnapshotSeedsNothing() = runTest {
        val db = createDatabase()

        val result = BundledPlaces.importFrom(db) { "[]".byteInputStream() }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals(0L, db.place.selectCount())
    }

    @Test
    fun importFrom_importsEveryRowAcrossBatchBoundaries() = runTest {
        val db = createDatabase()
        val count = BundledPlaces.BATCH_SIZE + 1

        val result = BundledPlaces.importFrom(db) { snapshotJson(count).byteInputStream() }

        Assert.assertEquals(count.toLong(), result.placesImported)
        Assert.assertEquals(count.toLong(), db.place.selectCount())
        Assert.assertNotNull(db.place.selectById(1L))
        Assert.assertNotNull(db.place.selectById(BundledPlaces.BATCH_SIZE.toLong()))
        Assert.assertNotNull(db.place.selectById(count.toLong()))
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

        BundledPlaces.importFrom(db) { stream }

        Assert.assertTrue("the snapshot stream must be closed", closed)
    }

    // --- failure handling -------------------------------------------------------

    @Test
    fun importFrom_missingAssetIsNotAnError() = runTest {
        val db = createDatabase()

        val result = BundledPlaces.importFrom(db) { throw FileNotFoundException("no asset") }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals(0L, db.place.selectCount())
    }

    @Test
    fun importFrom_malformedAssetRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        // The first entry is valid, the second is missing its required icon.
        val json = """[{"id":1,"lat":1.0,"lon":2.0,"icon":"store"},{"id":2,"lat":3.0,"lon":4.0}]"""

        val result = BundledPlaces.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals("a partial seed must not survive a parse failure", 0L, db.place.selectCount())
    }

    @Test
    fun importFrom_invalidJsonRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        val json = """[{"id":1,"lat":1.0,"lon":2.0,"icon":"store"},"""

        val result = BundledPlaces.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(0L, result.placesImported)
        Assert.assertEquals(0L, db.place.selectCount())
    }

    @Test
    fun importFrom_canBeRetriedAfterAFailedImport() = runTest {
        val db = createDatabase()
        BundledPlaces.importFrom(db) {
            """[{"id":1,"lat":1.0,"lon":2.0}]""".byteInputStream()
        }
        Assert.assertEquals(0L, db.place.selectCount())

        val result = BundledPlaces.importFrom(db) { snapshotJson(2).byteInputStream() }

        Assert.assertEquals(2L, result.placesImported)
        Assert.assertEquals(2L, db.place.selectCount())
    }

    @Test
    fun importFrom_badBoostedUntilDoesNotDiscardTheSeed() = runTest {
        val db = createDatabase()
        val json = """[{"id":1,"lat":1.0,"lon":2.0,"icon":"store","boosted_until":"not-a-date"}]"""

        val result = BundledPlaces.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(1L, result.placesImported)
        Assert.assertEquals(1L, db.place.selectCount())
        Assert.assertNull(db.place.selectById(1L)!!.boostedUntil)
    }
}
