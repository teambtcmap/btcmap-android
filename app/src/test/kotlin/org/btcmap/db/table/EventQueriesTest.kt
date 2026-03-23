package org.btcmap.db.table

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
    fun deleteAll_removesAllEvents() {
        val db = createDatabase()
        val event1 = Event(
            id = 1L,
            lat = 40.7128,
            lon = -74.0060,
            name = "Event 1",
            website = "https://example.com".toHttpUrl(),
            startsAt = ZonedDateTime.parse("2024-06-01T18:00:00Z"),
            endsAt = null,
        )
        val event2 = Event(
            id = 2L,
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
}