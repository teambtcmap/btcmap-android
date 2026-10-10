package org.btcmap.ui.map

import kotlinx.coroutines.runBlocking
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.nio.file.Files
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.btcmap.db.Database
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.util.toUrl
import org.btcmap.map.EVENT_ICON
import org.btcmap.search.NominatimPlace
import org.btcmap.search.NominatimViewbox
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
    fun cityArea_getsCityIcon() = runBlocking<Unit> {
        val db = database()
        db.area.insert(listOf(area(id = 1L, name = "Paris", type = "city")))

        val item = search(db, "paris").single()

        assertTrue(item is SearchAdapterItem.Area)
        assertEquals("location_city", item.icon)
    }

    @Test
    fun nonCityArea_keepsTheGlobeIcon() = runBlocking<Unit> {
        val db = database()
        db.area.insert(listOf(area(id = 1L, name = "Grand Paris", type = "community")))

        val item = search(db, "paris").single()

        assertTrue(item is SearchAdapterItem.Area)
        assertEquals("public", item.icon)
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

    @Test
    fun shortCjkQuery_isSearchable() {
        // Two ideographs are a whole word, unlike two Latin letters.
        assertTrue(isSearchableQuery("肉饼"))
        assertTrue(isSearchableQuery("北京"))
        assertTrue(isSearchableQuery("  肉饼  "))
        assertTrue(isSearchableQuery("ラーメン"))
        assertTrue(isSearchableQuery("김밥"))
    }

    @Test
    fun shortLatinQuery_isNotSearchable() {
        assertFalse(isSearchableQuery("ab"))
        assertFalse(isSearchableQuery("a"))
        assertFalse(isSearchableQuery("  "))
        assertFalse(isSearchableQuery(""))
        assertTrue(isSearchableQuery("abc"))
    }

    @Test
    fun singleCjkCharacter_isNotSearchable() {
        assertFalse(isSearchableQuery("肉"))
    }

    @Test
    fun coordinateQuery_parsesToASingleResult() {
        val item = coordinateSearchItem("39.936212,116.437600")

        assertNotNull(item)
        assertEquals(39.936212, item.lat)
        assertEquals(116.4376, item.lon)
        assertEquals("place", item.icon)
        assertEquals("39.936212, 116.4376", item.name)
    }

    @Test
    fun urlEncodedCoordinateQuery_isAccepted() {
        // A comma copied from a map link arrives percent-encoded.
        val item = coordinateSearchItem("39.936212%2C116.437600")

        assertNotNull(item)
        assertEquals(39.936212, item.lat)
        assertEquals(116.4376, item.lon)
    }

    @Test
    fun coordinateQuery_maySeparateNumbersWithSpace() {
        assertEquals(SearchCoordinates(39.94, 116.44), parseCoordinates("39.94 116.44"))
    }

    @Test
    fun outOfRangeCoordinate_isNotACoordinate() {
        assertNull(parseCoordinates("91,0"))
        assertNull(parseCoordinates("0,181"))
    }

    @Test
    fun name_isNotACoordinate() {
        assertNull(parseCoordinates("bitcoin cafe"))
        assertNull(parseCoordinates("Holiday Inn Express"))
    }

    @Test
    fun nominatimResults_ordersNearestFirst() = runBlocking<Unit> {
        val box = NominatimViewbox(1.0, 2.0, 3.0, 4.0)
        val calls = mutableListOf<Pair<NominatimViewbox?, Boolean>>()
        val results = nominatimResults(
            search = { viewbox, bounded ->
                calls += viewbox to bounded
                listOf(
                    NominatimPlace(48.90, 2.3522, "Far"),
                    NominatimPlace(48.8567, 2.3522, "Near"),
                )
            },
            referenceLat = 48.8566,
            referenceLon = 2.3522,
            formatDistance = { "$it m" },
            viewbox = box,
        )

        // A non-empty bounded result is used as-is, and sorted nearest first.
        assertEquals(1, calls.size)
        assertEquals(box, calls[0].first)
        assertTrue(calls[0].second)
        assertEquals(listOf("Near", "Far"), results.map { it.name })
    }

    @Test
    fun nominatimResults_fallsBackToAGlobalSearchWhenTheBoundedOneFindsNothing() = runBlocking<Unit> {
        val box = NominatimViewbox(1.0, 2.0, 3.0, 4.0)
        val boundedValues = mutableListOf<Boolean>()
        val results = nominatimResults(
            search = { _, bounded ->
                boundedValues += bounded
                if (bounded) emptyList<NominatimPlace>() else listOf(NominatimPlace(39.94, 116.37, "孔乙己酒家"))
            },
            referenceLat = 39.9042,
            referenceLon = 116.4074,
            formatDistance = { "$it m" },
            viewbox = box,
        )

        assertEquals(listOf(true, false), boundedValues)
        assertEquals(listOf("孔乙己酒家"), results.map { it.name })
    }

    @Test
    fun nominatimResults_swallowsFailures() = runBlocking<Unit> {
        val results = nominatimResults(
            search = { _, _ -> throw IllegalStateException("boom") },
            referenceLat = 0.0,
            referenceLon = 0.0,
            formatDistance = { it.toString() },
            viewbox = null,
        )

        assertTrue(results.isEmpty())
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

    private fun area(id: Long, name: String, type: String = "community") = Area(
        id = id,
        name = name,
        type = type,
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
