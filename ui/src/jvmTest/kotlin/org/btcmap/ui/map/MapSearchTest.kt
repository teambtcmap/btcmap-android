package org.btcmap.ui.map

import kotlinx.coroutines.runBlocking
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.util.toUrl
import org.btcmap.map.EVENT_ICON
import org.btcmap.search.SearchAdapterItem

/**
 * The offline search the map runs as the user types, ported from the app's
 * instrumented search test when the Views search went with the map swap. It
 * reads only the local cache, so no server takes part.
 */
class MapSearchTest {

    private val referenceLat = 48.8566
    private val referenceLon = 2.3522

    @Test
    fun matchingEvent_isReturned() = runBlocking<Unit> {
        val db = database()
        db.event.insert(
            listOf(event(id = 1L, name = "Bitcoin Meetup Paris", lat = 48.8570, lon = 2.3530)),
        )

        val item = search(db, "bitcoin").single()

        assertTrue(item is SearchAdapterItem.Event)
        item as SearchAdapterItem.Event
        assertEquals(1L, item.eventId)
        assertEquals("Bitcoin Meetup Paris", item.name)
        assertEquals(EVENT_ICON, item.icon)
        assertNotNull(item.distanceToUser)
    }

    @Test
    fun placesAndEvents_sortByDistance() = runBlocking<Unit> {
        // The event is next to the reference point, the place is far away.
        val db = database()
        db.place.insert(listOf(place(id = 1L, name = "Bitcoin Cafe Far", lat = 48.90, lon = 2.3522)))
        db.event.insert(
            listOf(event(id = 2L, name = "Bitcoin Meetup Near", lat = 48.8567, lon = 2.3522)),
        )

        val names = search(db, "bitcoin").map { it.name }

        assertEquals(listOf("Bitcoin Meetup Near", "Bitcoin Cafe Far"), names)
    }

    @Test
    fun eventWebsite_isNotSearched() = runBlocking<Unit> {
        // The place matches on name and proves the search ran; the event only
        // matches on its website, so it must be absent.
        val db = database()
        db.place.insert(listOf(place(id = 1L, name = "Bitcoin Cafe", lat = 48.8566, lon = 2.3522)))
        db.event.insert(
            listOf(
                event(
                    id = 2L,
                    name = "London BTC",
                    lat = 51.5074,
                    lon = -0.1278,
                    website = "https://bitcoin.example.com",
                ),
            ),
        )

        val results = search(db, "bitcoin")

        assertTrue(results.any { it is SearchAdapterItem.Place })
        assertTrue(results.none { it is SearchAdapterItem.Event })
    }

    @Test
    fun matchingArea_isReturnedWithItsBbox() = runBlocking<Unit> {
        val db = database()
        db.area.insert(listOf(area(id = 1L, name = "Paris Bitcoin")))

        val item = search(db, "paris").single()

        assertTrue(item is SearchAdapterItem.Area)
        item as SearchAdapterItem.Area
        assertEquals(1L, item.areaId)
        assertEquals("Paris Bitcoin", item.name)
        assertEquals(listOf(2.22, 48.81, 2.47, 48.91), item.bbox)
        assertEquals("https://static.example/icon.png", item.iconUrl)
        assertEquals("https://static.example/icon.png", item.headerImageUrl)
        assertNotNull(item.distanceToUser)
    }

    @Test
    fun exactNameMatch_ranksAboveSubstring() = runBlocking<Unit> {
        // The exact match is farther away; ranking must still float it first.
        val db = database()
        db.place.insert(
            listOf(
                place(id = 1L, name = "Paris", lat = 48.90, lon = 2.3522),
                place(id = 2L, name = "Paris Cafe", lat = 48.8567, lon = 2.3522),
            ),
        )

        val names = search(db, "paris").map { it.name }

        assertEquals(listOf("Paris", "Paris Cafe"), names)
    }

    private fun search(db: Database, query: String) = search(
        db = db,
        query = query,
        referenceLat = referenceLat,
        referenceLon = referenceLon,
        formatDistance = { it.toInt().toString() },
    )

    private fun database(): Database = runBlocking {
        Database(BundledSQLiteDriver(), Files.createTempFile("map-search", ".db").toString()).apply { connect() }
    }

    private fun place(id: Long, name: String, lat: Double, lon: Double) = Place(
        id = id,
        updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
        lat = lat,
        lon = lon,
        icon = "local_cafe",
        name = name,
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
    )

    private fun event(
        id: Long,
        name: String,
        lat: Double,
        lon: Double,
        website: String? = null,
    ) = Event(
        id = id,
        lat = lat,
        lon = lon,
        name = name,
        website = website?.toUrl(),
        startsAt = Instant.parse("2999-01-01T18:00:00Z"),
        endsAt = null,
    )

    private fun area(id: Long, name: String) = Area(
        id = id,
        name = name,
        type = "community",
        urlAlias = "grand-paris",
        icon = "https://static.example/icon.png",
        iconWide = null,
        websiteUrl = "https://btcmap.org/community/grand-paris",
        description = null,
        bboxWest = 2.22,
        bboxSouth = 48.81,
        bboxEast = 2.47,
        bboxNorth = 48.91,
        geoJson = null,
    )
}
