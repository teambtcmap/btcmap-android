package org.btcmap.db.table.area

import kotlinx.coroutines.runBlocking
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.json.parseJsonObject
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant

class AreaQueriesTest {
    private fun createDatabase(): Database = runBlocking {
        org.btcmap.db.testDatabase().apply { connect() }
    }

    private fun area(
        id: Long,
        name: String = "Grand Paris",
        updatedAt: String = "2024-01-01T00:00:00Z",
        deletedAt: Instant? = null,
        geoJson: String? = null,
    ): Area {
        return Area(
            id = id,
            name = name,
            type = "community",
            urlAlias = "grand-paris",
            icon = "https://static.example/icon.png",
            iconWide = "https://static.example/wide.png",
            websiteUrl = "https://btcmap.org/community/grand-paris",
            description = "Greater Paris",
            bboxWest = 2.22,
            bboxSouth = 48.81,
            bboxEast = 2.47,
            bboxNorth = 48.91,
            geoJson = geoJson,
            updatedAt = Instant.parse(updatedAt),
            deletedAt = deletedAt,
        )
    }

    @Test
    fun insert_and_selectById() = runBlocking<Unit> {
        val db = createDatabase()

        db.area.insert(listOf(area(7L)))

        val result = db.area.selectById(7L)!!
        Assert.assertEquals("Grand Paris", result.name)
        Assert.assertEquals("community", result.type)
        Assert.assertEquals("grand-paris", result.urlAlias)
        Assert.assertEquals("https://static.example/icon.png", result.icon)
        Assert.assertEquals("Greater Paris", result.description)
        Assert.assertEquals(2.22, result.bboxWest!!, 0.0001)
        Assert.assertEquals(48.91, result.bboxNorth!!, 0.0001)
    }

    @Test
    fun insert_and_selectLocalizedMaps() = runBlocking<Unit> {
        val db = createDatabase()
        val nameTranslations =
            parseJsonObject("""{"en":"Grand Paris","ru":"Большой Париж"}""")
        val descriptionTranslations =
            parseJsonObject("""{"en":"Greater Paris"}""")

        db.area.insert(
            listOf(
                area(7L).copy(
                    localizedName = nameTranslations,
                    localizedDescription = descriptionTranslations,
                )
            )
        )

        val result = db.area.selectById(7L)!!
        Assert.assertEquals(nameTranslations, result.localizedName)
        Assert.assertEquals(descriptionTranslations, result.localizedDescription)
    }

    @Test
    fun insert_and_selectGeoJson() = runBlocking<Unit> {
        val db = createDatabase()
        val polygon = """{"type":"Feature","properties":{},"geometry":{"type":"Polygon","coordinates":[[[2.22,48.81],[2.47,48.81],[2.47,48.91],[2.22,48.81]]]}}"""

        db.area.insert(listOf(area(7L, geoJson = polygon)))

        val result = db.area.selectById(7L)!!
        Assert.assertEquals(polygon, result.geoJson)
    }

    @Test
    fun insert_handlesNullOptionalFields() = runBlocking<Unit> {
        val db = createDatabase()
        val row = area(7L).copy(
            icon = null,
            iconWide = null,
            description = null,
            bboxWest = null,
            bboxSouth = null,
            bboxEast = null,
            bboxNorth = null,
            geoJson = null,
        )

        db.area.insert(listOf(row))

        val result = db.area.selectById(7L)!!
        Assert.assertNull(result.icon)
        Assert.assertNull(result.description)
        Assert.assertNull(result.bboxWest)
        Assert.assertNull(result.geoJson)
        Assert.assertNull(result.localizedName)
        Assert.assertNull(result.localizedDescription)
    }

    @Test
    fun insert_replacesExistingRow() = runBlocking<Unit> {
        val db = createDatabase()

        db.area.insert(listOf(area(7L, name = "Old")))
        db.area.insert(listOf(area(7L, name = "New")))

        Assert.assertEquals(1L, db.area.selectCount())
        Assert.assertEquals("New", db.area.selectById(7L)!!.name)
    }

    @Test
    fun selectById_hidesTombstones() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(listOf(area(7L, deletedAt = Instant.parse("2024-02-01T00:00:00Z"))))

        Assert.assertNull(db.area.selectById(7L))
        Assert.assertTrue(db.area.selectAll().isEmpty())
        Assert.assertEquals(0L, db.area.selectCount())
        // The row is still stored, so the delta cursor still points at it, and a
        // caller that needs the total can still count it.
        Assert.assertEquals(1L, db.area.selectCount(includeDeleted = true))
        Assert.assertEquals(
            Instant.parse("2024-01-01T00:00:00Z"),
            db.area.selectMaxUpdatedAt(),
        )
    }

    @Test
    fun selectBySearchString_matchesNameSubstring() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(listOf(area(1L, name = "Grand Paris"), area(2L, name = "Berlin")))

        val results = db.area.selectBySearchString("paris")

        Assert.assertEquals(1, results.size)
        Assert.assertEquals("Grand Paris", results[0].name)
    }

    @Test
    fun selectBySearchString_isCaseInsensitive() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(listOf(area(1L, name = "Grand Paris")))

        Assert.assertEquals(1, db.area.selectBySearchString("paris").size)
        Assert.assertEquals(1, db.area.selectBySearchString("PARIS").size)
        Assert.assertEquals(1, db.area.selectBySearchString("PaRiS").size)
    }

    @Test
    fun selectBySearchString_returnsEmptyWhenNoMatch() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(listOf(area(1L, name = "Grand Paris")))

        Assert.assertTrue(db.area.selectBySearchString("berlin").isEmpty())
    }

    @Test
    fun selectBySearchString_hidesTombstones() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(
            listOf(area(1L, name = "Grand Paris", deletedAt = Instant.parse("2024-02-01T00:00:00Z")))
        )

        Assert.assertTrue(db.area.selectBySearchString("paris").isEmpty())
    }

    @Test
    fun selectBySearchString_matchesLocalizedName() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(
            listOf(
                area(1L, name = "Grand Paris").copy(
                    localizedName = parseJsonObject("""{"ru":"Большой Париж"}""")
                )
            )
        )

        val results = db.area.selectBySearchString("Большой")

        Assert.assertEquals(1, results.size)
        Assert.assertEquals("Grand Paris", results[0].name)
    }

    @Test
    fun selectBySearchString_doesNotMatchAnUnrelatedLocalizedMap() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(
            listOf(
                area(1L, name = "Grand Paris").copy(
                    localizedName = parseJsonObject("""{"ru":"Большой Париж"}""")
                )
            )
        )

        Assert.assertTrue(db.area.selectBySearchString("Berlin").isEmpty())
    }

    @Test
    fun selectByBbox_returnsOnlyOverlappingVisibleAreas() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(
            listOf(
                area(1L, name = "Paris"),
                area(2L, name = "Berlin").copy(
                    bboxWest = 13.0,
                    bboxSouth = 52.3,
                    bboxEast = 13.8,
                    bboxNorth = 52.7,
                ),
                area(3L, name = "Gone", deletedAt = Instant.parse("2024-02-01T00:00:00Z")),
            ),
        )

        val results = db.area.selectByBbox(west = 2.3, south = 48.85, east = 2.4, north = 48.88)

        Assert.assertEquals(listOf("Paris"), results.map { it.name })
    }

    @Test
    fun selectByBbox_skipsRowsWithoutBbox() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(
            listOf(
                area(1L, name = "No bbox").copy(
                    bboxWest = null,
                    bboxSouth = null,
                    bboxEast = null,
                    bboxNorth = null,
                ),
            ),
        )

        Assert.assertTrue(
            db.area.selectByBbox(west = -1.0, south = -1.0, east = 1.0, north = 1.0).isEmpty()
        )
    }

    @Test
    fun selectMaxUpdatedAt_returnsLatestEvenWithTombstones() = runBlocking<Unit> {
        val db = createDatabase()
        db.area.insert(
            listOf(
                area(1L, updatedAt = "2024-01-01T00:00:00Z"),
                area(2L, updatedAt = "2024-03-01T00:00:00Z"),
                area(3L, updatedAt = "2024-02-01T00:00:00Z"),
            ),
        )

        Assert.assertEquals(
            Instant.parse("2024-03-01T00:00:00Z"),
            db.area.selectMaxUpdatedAt(),
        )
    }

    @Test
    fun selectMaxUpdatedAt_isNullWhenEmpty() = runBlocking<Unit> {
        val db = createDatabase()

        Assert.assertNull(db.area.selectMaxUpdatedAt())
    }
}
