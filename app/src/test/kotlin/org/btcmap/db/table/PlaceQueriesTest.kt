package org.btcmap.db.table

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test
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
            bundled = false,
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
    fun selectMerchants_excludesAtmAndExchange() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, icon = "restaurant")))
        db.place.insert(listOf(createPlace(id = 2L, icon = "local_atm")))
        db.place.insert(listOf(createPlace(id = 3L, icon = "currency_exchange")))
        db.place.insert(listOf(createPlace(id = 4L, icon = "coffee")))

        val results = db.place.selectMerchants()

        Assert.assertEquals(2, results.size)
        Assert.assertTrue(results.all { it.iconId != "local_atm" && it.iconId != "currency_exchange" })
    }

    @Test
    fun selectExchanges_includesOnlyAtmAndExchange() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L, icon = "restaurant")))
        db.place.insert(listOf(createPlace(id = 2L, icon = "local_atm")))
        db.place.insert(listOf(createPlace(id = 3L, icon = "currency_exchange")))

        val results = db.place.selectExchanges()

        Assert.assertEquals(2, results.size)
        Assert.assertTrue(results.all { it.iconId == "local_atm" || it.iconId == "currency_exchange" })
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
    fun selectCount_returnsCorrectCount() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L)))
        db.place.insert(listOf(createPlace(id = 2L)))
        db.place.insert(listOf(createPlace(id = 3L)))

        val result = db.place.selectCount()

        Assert.assertEquals(3L, result)
    }

    @Test
    fun deleteById_removesPlace() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L)))
        db.place.insert(listOf(createPlace(id = 2L)))

        Assert.assertEquals(2L, db.place.selectCount())

        db.place.deleteById(1L)

        Assert.assertEquals(1L, db.place.selectCount())
        Assert.assertNull(db.place.selectById(1L))
    }

    @Test
    fun deleteById_doesNothingWhenIdNotFound() {
        val db = createDatabase()
        db.place.insert(listOf(createPlace(id = 1L)))

        db.place.deleteById(999L)

        Assert.assertEquals(1L, db.place.selectCount())
    }

    @Test
    fun selectMerchants_withBoostedPlace() {
        val db = createDatabase()
        val boostedPlace = createPlace(id = 1L, icon = "restaurant")
        val normalPlace = createPlace(id = 2L, icon = "coffee")

        db.place.insert(listOf(boostedPlace))
        db.place.insert(listOf(normalPlace))

        val results = db.place.selectMerchants()

        Assert.assertEquals(2, results.size)
    }

    private fun createPlace(
        id: Long,
        name: String? = "Test Place",
        icon: String = "place",
        updatedAt: ZonedDateTime = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        lat: Double = 40.7128,
        lon: Double = -74.0060,
    ): Place {
        return Place(
            id = id,
            bundled = false,
            updatedAt = updatedAt,
            lat = lat,
            lon = lon,
            icon = icon,
            name = name,
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
        )
    }
}