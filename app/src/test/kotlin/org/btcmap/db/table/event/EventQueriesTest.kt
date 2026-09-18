package org.btcmap.db.table.event

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class EventQueriesTest {
    private fun createDatabase(): Database {
        return Database(BundledSQLiteDriver(), ":memory:")
    }

    @Test
    fun insert_and_selectAll() {
        val db = createDatabase()
        val event = Event(
            id = 1L,
            areaId = null,
            lat = 40.7128,
            lon = -74.0060,
            name = "Bitcoin Meetup",
            website = "https://example.com".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-06-01T18:00:00Z"),
            endsAt = ZonedDateTime.parse("2024-06-01T20:00:00Z"),
        )

        db.event.insert(listOf(event))
        val results = db.event.selectAll()

        Assert.assertEquals(1, results.size)
        Assert.assertEquals(1L, results[0].id)
        Assert.assertEquals("Bitcoin Meetup", results[0].name)
        Assert.assertEquals(40.7128, results[0].lat, 0.0001)
        Assert.assertEquals(-74.0060, results[0].lon, 0.0001)
    }

    @Test
    fun insert_and_selectById() {
        val db = createDatabase()
        val event = Event(
            id = 42L,
            areaId = null,
            lat = 51.5074,
            lon = -0.1278,
            name = "London BTC",
            website = "https://london.btc".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-07-01T19:00:00Z"),
            endsAt = null,
        )

        db.event.insert(listOf(event))
        val result = db.event.selectById(42L)

        Assert.assertNotNull(result)
        Assert.assertEquals(42L, result!!.id)
        Assert.assertEquals("London BTC", result.name)
        Assert.assertNull(result.endsAt)
    }

    @Test
    fun insert_and_selectEventWithoutWebsite() {
        val db = createDatabase()
        val event = Event(
            id = 1L,
            areaId = null,
            lat = 40.7128,
            lon = -74.0060,
            name = "No Website",
            website = null,
            startsAt = ZonedDateTime.parse("2024-06-01T18:00:00Z"),
            endsAt = null,
        )

        db.event.insert(listOf(event))

        Assert.assertNull(db.event.selectById(1L)!!.website)
    }

    @Test
    fun selectById_returnsNullWhenNotFound() {
        val db = createDatabase()

        val result = db.event.selectById(999L)

        Assert.assertNull(result)
    }

    @Test
    fun selectAll_returnsEmptyListWhenEmpty() {
        val db = createDatabase()

        val results = db.event.selectAll()

        Assert.assertTrue(results.isEmpty())
    }

    @Test
    fun insert_and_selectByBounds() {
        val db = createDatabase()
        val event1 = Event(
            id = 1L,
            areaId = null,
            lat = 40.7128,
            lon = -74.0060,
            name = "NYC Event",
            website = "https://example.com".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-06-01T18:00:00Z"),
            endsAt = null,
        )
        val event2 = Event(
            id = 2L,
            areaId = null,
            lat = 51.5074,
            lon = -0.1278,
            name = "London Event",
            website = "https://example.com".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-07-01T18:00:00Z"),
            endsAt = null,
        )
        val event3 = Event(
            id = 3L,
            areaId = null,
            lat = 34.0522,
            lon = -118.2437,
            name = "LA Event",
            website = "https://example.com".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-08-01T18:00:00Z"),
            endsAt = null,
        )

        db.event.insert(listOf(event1, event2, event3))

        val results = db.event.selectByBounds(
            minLat = 40.0,
            maxLat = 52.0,
            minLon = -75.0,
            maxLon = 0.0,
        )

        Assert.assertEquals(2, results.size)
        Assert.assertEquals("NYC Event", results[0].name)
        Assert.assertEquals("London Event", results[1].name)
    }

    @Test
    fun selectByBounds_returnsEmptyListWhenNoMatch() {
        val db = createDatabase()
        val event = Event(
            id = 1L,
            areaId = null,
            lat = 40.7128,
            lon = -74.0060,
            name = "NYC Event",
            website = "https://example.com".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-06-01T18:00:00Z"),
            endsAt = null,
        )

        db.event.insert(listOf(event))

        val results = db.event.selectByBounds(
            minLat = 50.0,
            maxLat = 60.0,
            minLon = -80.0,
            maxLon = -70.0,
        )

        Assert.assertTrue(results.isEmpty())
    }

    @Test
    fun deleteAll_removesAllEvents() {
        val db = createDatabase()
        val event1 = Event(
            id = 1L,
            areaId = null,
            lat = 40.7128,
            lon = -74.0060,
            name = "Event 1",
            website = "https://example.com".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-06-01T18:00:00Z"),
            endsAt = null,
        )
        val event2 = Event(
            id = 2L,
            areaId = null,
            lat = 51.5074,
            lon = -0.1278,
            name = "Event 2",
            website = "https://example.com".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-07-01T18:00:00Z"),
            endsAt = null,
        )

        db.event.insert(listOf(event1, event2))
        Assert.assertEquals(2, db.event.selectAll().size)

        db.event.deleteAll()

        Assert.assertTrue(db.event.selectAll().isEmpty())
    }

    @Test
    fun selectBySearchString_matchesNameSubstring() {
        val db = createDatabase()
        insertEvent(db, id = 1L, name = "Bitcoin Meetup")
        insertEvent(db, id = 2L, name = "Ethereum Meetup")

        val results = db.event.selectBySearchString("bitcoin")

        Assert.assertEquals(1, results.size)
        Assert.assertEquals("Bitcoin Meetup", results[0].name)
    }

    @Test
    fun selectBySearchString_isCaseInsensitive() {
        val db = createDatabase()
        insertEvent(db, id = 1L, name = "Bitcoin Meetup")

        Assert.assertEquals(1, db.event.selectBySearchString("bitcoin").size)
        Assert.assertEquals(1, db.event.selectBySearchString("BITCOIN").size)
        Assert.assertEquals(1, db.event.selectBySearchString("BiTcOiN").size)
    }

    @Test
    fun selectBySearchString_returnsEmptyListWhenNoMatch() {
        val db = createDatabase()
        insertEvent(db, id = 1L, name = "Bitcoin Meetup")

        Assert.assertTrue(db.event.selectBySearchString("lightning").isEmpty())
    }

    @Test
    fun selectBySearchString_doesNotMatchWebsite() {
        val db = createDatabase()
        insertEvent(db, id = 1L, name = "London BTC", website = "https://bitcoin.example.com")

        Assert.assertTrue(db.event.selectBySearchString("bitcoin").isEmpty())
    }

    @Test
    fun selectBySearchString_returnsAllMatchesWithoutLimit() {
        val db = createDatabase()
        insertEvent(db, id = 1L, name = "Bitcoin Meetup Berlin")
        insertEvent(db, id = 2L, name = "Bitcoin Meetup Lisbon")
        insertEvent(db, id = 3L, name = "Bitcoin Meetup Paris")
        insertEvent(db, id = 4L, name = "Ethereum Conference")

        val results = db.event.selectBySearchString("meetup")

        Assert.assertEquals(3, results.size)
    }

    private fun insertEvent(
        db: Database,
        id: Long,
        name: String,
        website: String? = null,
    ) {
        db.event.insert(
            listOf(
                Event(
                    id = id,
                    areaId = null,
                    lat = 40.7128,
                    lon = -74.0060,
                    name = name,
                    website = website?.toHttpUrl(),
                    startsAt = ZonedDateTime.parse("2024-06-01T18:00:00Z"),
                    endsAt = null,
                )
            )
        )
    }
}