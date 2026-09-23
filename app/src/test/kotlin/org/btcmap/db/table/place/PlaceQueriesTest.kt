package org.btcmap.db.table.place

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class PlaceQueriesTest {

    private fun createDatabase(): Database {
        return Database(BundledSQLiteDriver(), ":memory:")
    }

    @Test
    fun insert_and_selectById() {
        val db = createDatabase()
        val place = createPlace(id = 1L, name = "Coffee Shop", icon = "coffee")

        db.place.insert(listOf(place))
        val result = db.place.selectById(1L)

        Assert.assertNotNull(result)
        Assert.assertEquals(1L, result!!.id)
        Assert.assertEquals("Coffee Shop", result.name)
        Assert.assertEquals("coffee", result.icon)
    }

    @Test
    fun insert_and_selectById_withAllNullableFields() {
        val db = createDatabase()
        val place = Place(
            id = 1L,
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            lat = 40.7128,
            lon = -74.0060,
            icon = "restaurant",
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

        db.place.insert(listOf(place))
        val result = db.place.selectById(1L)

        Assert.assertNotNull(result)
        Assert.assertNull(result!!.name)
        Assert.assertNull(result.localizedName)
        Assert.assertNull(result.website)
        Assert.assertNull(result.phone)
    }

    @Test
    fun selectById_returnsNullWhenNotFound() {
        val db = createDatabase()

        val result = db.place.selectById(999L)

        Assert.assertNull(result)
    }

    @Test
    fun selectByOsmId_returnsMatchingPlace() {
        val db = createDatabase()
        db.place.insert(
            listOf(
                createPlace(id = 1L, name = "Coffee Shop").copy(osmId = "node:1"),
                createPlace(id = 2L, name = "Bar").copy(osmId = "way:2"),
            )
        )

        val result = db.place.selectByOsmId("way:2")

        Assert.assertNotNull(result)
        Assert.assertEquals(2L, result!!.id)
        Assert.assertEquals("Bar", result.name)
    }

    @Test
    fun selectByOsmId_returnsNullWhenNotFound() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L).copy(osmId = "node:1")))

        Assert.assertNull(db.place.selectByOsmId("node:999"))
    }

    @Test
    fun selectByOsmIds_returnsMatchingPlacesKeyedByOsmId() {
        val db = createDatabase()
        db.place.insert(
            listOf(
                createPlace(id = 1L, name = "Coffee Shop").copy(osmId = "node:1"),
                createPlace(id = 2L, name = "Bar").copy(osmId = "way:2"),
                createPlace(id = 3L, name = "Shop").copy(osmId = "node:3"),
            )
        )

        val result = db.place.selectByOsmIds(listOf("node:1", "node:3", "node:999"))

        Assert.assertEquals(setOf("node:1", "node:3"), result.keys)
        Assert.assertEquals(1L, result.getValue("node:1").id)
        Assert.assertEquals(3L, result.getValue("node:3").id)
    }

    @Test
    fun selectByOsmIds_skipsDeletedPlacesAndEmptyInput() {
        val db = createDatabase()
        val deletedAt = ZonedDateTime.parse("2024-01-02T10:00:00Z")
        db.place.insert(
            listOf(
                createPlace(id = 1L, icon = "coffee", updatedAt = deletedAt)
                    .copy(osmId = "node:1", deletedAt = deletedAt),
                createPlace(id = 2L, icon = "bar").copy(osmId = "way:2"),
            )
        )

        Assert.assertEquals(
            setOf("way:2"),
            db.place.selectByOsmIds(listOf("node:1", "way:2")).keys,
        )
        Assert.assertTrue(db.place.selectByOsmIds(emptyList()).isEmpty())
    }

    @Test
    fun insert_orReplace_updatesExistingPlace() {
        val db = createDatabase()
        val place1 = createPlace(id = 1L, name = "Original Name", icon = "coffee")
        val place2 = createPlace(id = 1L, name = "Updated Name", icon = "restaurant")

        db.place.insert(listOf(place1))
        db.place.insert(listOf(place2))

        val result = db.place.selectById(1L)

        Assert.assertEquals("Updated Name", result!!.name)
        Assert.assertEquals("restaurant", result.icon)
    }

    @Test
    fun selectBySearchString_findsByName() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, name = "Starbucks", icon = "coffee")))
        db.place.insert(listOf(createPlace(id = 2L, name = "McDonald's", icon = "fast_food")))
        db.place.insert(listOf(createPlace(id = 3L, name = "Coffee Bean", icon = "coffee")))

        val results = db.place.selectBySearchString("coffee")

        Assert.assertEquals(1, results.size)
    }

    @Test
    fun selectBySearchString_caseInsensitive() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, name = "STARBUCKS", icon = "coffee")))

        val results = db.place.selectBySearchString("starbucks")

        Assert.assertEquals(1, results.size)
    }

    @Test
    fun selectBySearchString_returnsEmptyWhenNoMatch() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, name = "Coffee Shop", icon = "coffee")))

        val results = db.place.selectBySearchString("pizza")

        Assert.assertTrue(results.isEmpty())
    }

    @Test
    fun selectBySearchString_treatsWildcardsLiterally() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, name = "50% Off Cafe")))
        db.place.insert(listOf(createPlace(id = 2L, name = "500 Satoshis Cafe")))

        // Without an ESCAPE clause "%" is a wildcard, so "50%" would also match
        // "500 Satoshis Cafe".
        val results = db.place.selectBySearchString("50%")

        Assert.assertEquals(1, results.size)
        Assert.assertEquals("50% Off Cafe", results[0].name)
    }

    @Test
    fun selectByOsmIds_chunksLargeInput() {
        val db = createDatabase()
        // Larger than the per-statement bound-variable cap, so the ids span
        // more than one query and the results have to be merged.
        val count = 1_000
        db.place.insert(
            (1..count).map { id -> createPlace(id = id.toLong()).copy(osmId = "node:$id") },
        )

        val result = db.place.selectByOsmIds((1..count).map { "node:$it" })

        Assert.assertEquals(count, result.size)
        Assert.assertEquals(1L, result.getValue("node:1").id)
        Assert.assertEquals(count.toLong(), result.getValue("node:$count").id)
    }

    @Test
    fun selectByBounds_returnsOnlyPlacesInsideTheBox() {
        val db = createDatabase()
        db.place.insert(
            listOf(
                createPlace(id = 1L, lat = 10.0, lon = 10.0),
                createPlace(id = 2L, lat = 20.0, lon = 20.0),
                createPlace(id = 3L, lat = 30.0, lon = 30.0),
            )
        )

        val results = db.place.selectByBounds(
            minLat = 9.0,
            maxLat = 21.0,
            minLon = 9.0,
            maxLon = 21.0,
        )

        Assert.assertEquals(setOf(1L, 2L), results.map { it.id }.toSet())
    }

    @Test
    fun selectByBounds_excludesDeletedPlaces() {
        val db = createDatabase()
        val deletedAt = ZonedDateTime.parse("2024-01-02T10:00:00Z")
        db.place.insert(
            listOf(
                createPlace(id = 1L, lat = 10.0, lon = 10.0),
                createPlace(id = 2L, lat = 11.0, lon = 11.0).copy(deletedAt = deletedAt),
            )
        )

        val results = db.place.selectByBounds(
            minLat = 9.0,
            maxLat = 12.0,
            minLon = 9.0,
            maxLon = 12.0,
        )

        Assert.assertEquals(listOf(1L), results.map { it.id })
    }

    @Test
    fun selectByBounds_withBoost_returnsOnlyPlacesCarryingABoost() {
        val db = createDatabase()
        db.place.insert(
            listOf(
                createPlace(id = 1L, lat = 10.0, lon = 10.0),
                createPlace(id = 2L, lat = 11.0, lon = 11.0)
                    .copy(boostedUntil = ZonedDateTime.parse("2999-01-01T00:00:00Z")),
            )
        )

        val results = db.place.selectByBounds(
            minLat = 9.0,
            maxLat = 12.0,
            minLon = 9.0,
            maxLon = 12.0,
            withBoost = true,
        )

        Assert.assertEquals(listOf(2L), results.map { it.id })
    }

    @Test
    fun selectExchanges_includesOnlyAtmAndExchange() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, icon = "restaurant")))
        db.place.insert(listOf(createPlace(id = 2L, icon = "local_atm")))
        db.place.insert(listOf(createPlace(id = 3L, icon = "currency_exchange")))

        val results = db.place.selectExchanges()

        Assert.assertEquals(2, results.size)
        Assert.assertTrue(results.all { it.icon == "local_atm" || it.icon == "currency_exchange" })
    }

    @Test
    fun selectMerchantsByBounds_returnsOnlyMerchantsWithinBounds() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, icon = "restaurant", lat = 40.7128, lon = -74.0060)))
        db.place.insert(listOf(createPlace(id = 2L, icon = "coffee", lat = 51.5074, lon = -0.1278)))
        db.place.insert(listOf(createPlace(id = 3L, icon = "local_atm", lat = 40.7128, lon = -74.0060)))
        db.place.insert(listOf(createPlace(id = 4L, icon = "currency_exchange", lat = 51.5074, lon = -0.1278)))

        val results = db.place.selectMerchantsByBounds(
            minLat = 40.0,
            maxLat = 52.0,
            minLon = -75.0,
            maxLon = 0.0,
        )

        Assert.assertEquals(2, results.size)
        Assert.assertTrue(results.all { it.icon != "local_atm" && it.icon != "currency_exchange" })
        Assert.assertTrue(results.any { it.icon == "restaurant" })
        Assert.assertTrue(results.any { it.icon == "coffee" })
    }

    @Test
    fun selectMerchantsByBounds_returnsEmptyWhenNoMatch() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, icon = "restaurant", lat = 40.7128, lon = -74.0060)))

        val results = db.place.selectMerchantsByBounds(
            minLat = 50.0,
            maxLat = 60.0,
            minLon = -80.0,
            maxLon = -70.0,
        )

        Assert.assertTrue(results.isEmpty())
    }

    @Test
    fun selectExchangesByBounds_returnsOnlyExchangesWithinBounds() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, icon = "restaurant", lat = 40.7128, lon = -74.0060)))
        db.place.insert(listOf(createPlace(id = 2L, icon = "local_atm", lat = 40.7128, lon = -74.0060)))
        db.place.insert(listOf(createPlace(id = 3L, icon = "currency_exchange", lat = 51.5074, lon = -0.1278)))

        val results = db.place.selectExchangesByBounds(
            minLat = 40.0,
            maxLat = 52.0,
            minLon = -75.0,
            maxLon = 0.0,
        )

        Assert.assertEquals(2, results.size)
        Assert.assertTrue(results.all { it.icon == "local_atm" || it.icon == "currency_exchange" })
    }

    @Test
    fun selectExchangesByBounds_returnsEmptyWhenNoMatch() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, icon = "local_atm", lat = 40.7128, lon = -74.0060)))

        val results = db.place.selectExchangesByBounds(
            minLat = 50.0,
            maxLat = 60.0,
            minLon = -80.0,
            maxLon = -70.0,
        )

        Assert.assertEquals("Results size", 0, results.size)
    }

    @Test
    fun selectMaxUpdatedAt_returnsNullWhenEmpty() {
        val db = createDatabase()

        val result = db.place.selectMaxUpdatedAt()

        Assert.assertNull(result)
    }

    @Test
    fun selectMaxUpdatedAt_returnsMaxDate() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"))))
        db.place.insert(listOf(createPlace(id = 2L, updatedAt = ZonedDateTime.parse("2024-01-03T10:00:00Z"))))
        db.place.insert(listOf(createPlace(id = 3L, updatedAt = ZonedDateTime.parse("2024-01-02T10:00:00Z"))))

        val result = db.place.selectMaxUpdatedAt()

        Assert.assertEquals(ZonedDateTime.parse("2024-01-03T10:00:00Z"), result)
    }

    @Test
    fun selectMaxUpdatedAt_comparesByInstantNotByText() {
        val db = createDatabase()
        // ZonedDateTime.toString() drops a zero fraction, so the earlier
        // "2024-01-01T10:00Z" sorts after "2024-01-01T10:00:00.500Z" as text.
        db.place.insert(listOf(createPlace(id = 1L, updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"))))
        db.place.insert(listOf(createPlace(id = 2L, updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00.500Z"))))

        val result = db.place.selectMaxUpdatedAt()

        Assert.assertEquals(ZonedDateTime.parse("2024-01-01T10:00:00.500Z"), result)
    }

    @Test
    fun selectCount_returnsCorrectCount() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L)))
        db.place.insert(listOf(createPlace(id = 2L)))
        db.place.insert(listOf(createPlace(id = 3L)))

        val result = db.place.selectCount()

        Assert.assertEquals(3L, result)
    }

    @Test
    fun selectCount_excludesTombstonesUnlessAsked() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L)))
        db.place.insert(
            listOf(
                createPlace(id = 2L)
                    .copy(deletedAt = ZonedDateTime.parse("2024-02-01T00:00:00Z")),
            ),
        )

        Assert.assertEquals(1L, db.place.selectCount())
        Assert.assertEquals(2L, db.place.selectCount(includeDeleted = true))
    }

    @Test
    fun selectMerchantsByBounds_withMinVerifiedAt_comparesByInstantNotByText() {
        val db = createDatabase()
        // "10:00:00Z" sorts after "10:00:00.500Z" as text but is earlier by
        // instant, so a text comparison would wrongly include the first place.
        db.place.insert(listOf(createPlace(
            id = 1L,
            icon = "restaurant",
            verifiedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )))
        db.place.insert(listOf(createPlace(
            id = 2L,
            icon = "coffee",
            verifiedAt = ZonedDateTime.parse("2024-01-01T10:00:01Z"),
        )))

        val results = db.place.selectMerchantsByBounds(
            minLat = 40.0,
            maxLat = 41.0,
            minLon = -75.0,
            maxLon = -73.0,
            minVerifiedAt = ZonedDateTime.parse("2024-01-01T10:00:00.500Z"),
        )

        Assert.assertEquals(listOf(2L), results.map { it.id })
    }

    @Test
    fun selectMerchantsByBounds_withMinVerifiedAt_returnsOnlyRecentMerchants() {
        val db = createDatabase()
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        db.place.insert(listOf(createPlace(
            id = 1L,
            icon = "restaurant",
            verifiedAt = now.minusYears(2)
        )))
        db.place.insert(listOf(createPlace(
            id = 2L,
            icon = "coffee",
            verifiedAt = now.minusYears(4)
        )))
        db.place.insert(listOf(createPlace(
            id = 3L,
            icon = "bank",
            verifiedAt = now.minusYears(1)
        )))

        val results = db.place.selectMerchantsByBounds(
            minLat = 40.0,
            maxLat = 41.0,
            minLon = -75.0,
            maxLon = -73.0,
            minVerifiedAt = now.minusYears(3)
        )

        Assert.assertEquals(2, results.size)
        Assert.assertTrue(results.any { it.id == 1L })
        Assert.assertTrue(results.any { it.id == 3L })
        Assert.assertFalse(results.any { it.id == 2L })
    }

    @Test
    fun selectMerchantsByBounds_withMinVerifiedAtNull_returnsAllMerchants() {
        val db = createDatabase()
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        db.place.insert(listOf(createPlace(
            id = 1L,
            icon = "restaurant",
            verifiedAt = now.minusYears(5)
        )))
        db.place.insert(listOf(createPlace(
            id = 2L,
            icon = "coffee",
            verifiedAt = now.minusYears(1)
        )))

        val results = db.place.selectMerchantsByBounds(
            minLat = 40.0,
            maxLat = 41.0,
            minLon = -75.0,
            maxLon = -73.0,
            minVerifiedAt = null
        )

        Assert.assertEquals(2, results.size)
    }

    @Test
    fun selectMerchantsByBounds_withMinVerifiedAt_excludesUnverified() {
        val db = createDatabase()
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        db.place.insert(listOf(createPlace(
            id = 1L,
            icon = "restaurant",
            verifiedAt = now.minusYears(1)
        )))
        db.place.insert(listOf(createPlace(
            id = 2L,
            icon = "coffee",
            verifiedAt = null
        )))
        db.place.insert(listOf(createPlace(
            id = 3L,
            icon = "bank",
            verifiedAt = now.minusYears(2)
        )))

        val results = db.place.selectMerchantsByBounds(
            minLat = 40.0,
            maxLat = 41.0,
            minLon = -75.0,
            maxLon = -73.0,
            minVerifiedAt = now.minusYears(1).minusDays(1)
        )

        Assert.assertEquals(1, results.size)
        Assert.assertTrue(results.any { it.id == 1L })
        Assert.assertFalse(results.any { it.id == 2L })
        Assert.assertFalse(results.any { it.id == 3L })
    }

    @Test
    fun selectMerchantsByBounds_withMinVerifiedAt_returnsEmptyWhenAllExcluded() {
        val db = createDatabase()
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        db.place.insert(listOf(createPlace(
            id = 1L,
            icon = "restaurant",
            verifiedAt = now.minusYears(5)
        )))
        db.place.insert(listOf(createPlace(
            id = 2L,
            icon = "coffee",
            verifiedAt = now.minusYears(4)
        )))

        val results = db.place.selectMerchantsByBounds(
            minLat = 40.0,
            maxLat = 41.0,
            minLon = -75.0,
            maxLon = -73.0,
            minVerifiedAt = now.minusYears(1)
        )

        Assert.assertEquals(0, results.size)
    }

    @Test
    fun insert_keepsTombstoneButHidesItFromReads() {
        val db = createDatabase()
        val deletedAt = ZonedDateTime.parse("2024-01-02T10:00:00Z")
        db.place.insert(listOf(createPlace(id = 1L, icon = "coffee")))
        db.place.insert(
            listOf(
                createPlace(id = 1L, icon = "coffee", updatedAt = deletedAt)
                    .copy(deletedAt = deletedAt)
            )
        )

        Assert.assertNull(db.place.selectById(1L))
        Assert.assertNull(db.place.selectByOsmId("node:1"))
        Assert.assertEquals(0L, db.place.selectCount())
        Assert.assertTrue(db.place.selectBySearchString("Test").isEmpty())
        Assert.assertTrue(
            db.place.selectMerchantsByBounds(40.0, 41.0, -75.0, -73.0).isEmpty()
        )
        Assert.assertTrue(db.place.selectExchanges().isEmpty())

        // The tombstone must still advance the delta cursor and stay on disk.
        Assert.assertEquals(deletedAt, db.place.selectMaxUpdatedAt())
        Assert.assertEquals(1L, physicalRowCount(db, "place"))
    }

    private fun physicalRowCount(db: Database, table: String): Long {
        db.conn.prepare("SELECT count(*) FROM $table;").use {
            it.step()
            return it.getLong(0)
        }
    }

    private fun createPlace(
        id: Long,
        name: String? = "Test Place",
        icon: String = "place",
        updatedAt: ZonedDateTime = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        lat: Double = 40.7128,
        lon: Double = -74.0060,
        verifiedAt: ZonedDateTime? = null,
    ): Place {
        return Place(
            id = id,
            updatedAt = updatedAt,
            lat = lat,
            lon = lon,
            icon = icon,
            name = name,
            localizedName = null,
            verifiedAt = verifiedAt,
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
    }
}