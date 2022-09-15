package db

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ElementQueriesTest {

    @Test
    fun insert() {
        testDb().elementQueries.apply {
            val row = testElement()
            insert(row)
            assertEquals(row, selectAll().executeAsOne())
        }
    }

    @Test
    fun selectAll() {
        testDb().elementQueries.apply {
            val rows = (0..Random.nextInt(6)).map { testElement() }
            rows.forEach { insert(it) }
            assertEquals(rows, selectAll().executeAsList())
        }
    }

    @Test
    fun selectByTypeAndId() {
        testDb().elementQueries.apply {
            val rows = (0..Random.nextInt(6)).map { testElement() }
            rows.forEach { insert(it) }
            val randomRow = rows.random()
            assertEquals(randomRow, selectByTypeAndId(randomRow.type, randomRow.id).executeAsOne())
        }
    }

    @Test
    fun selectBySearchString() {
        testDb().elementQueries.apply {
            val row1 = testElement().copy(tags = JsonObject(mapOf(Pair("amenity", JsonPrimitive("cafe")))))
            val row2 = testElement().copy(tags = JsonObject(mapOf(Pair("amenity", JsonPrimitive("bar")))))
            insert(row1)
            insert(row2)

            val result = selectBySearchString("cafe").executeAsOne()
            assertEquals(row1, result)
        }
    }

    @Test
    fun selectByBoundingBox() {
        testDb().elementQueries.apply {
            val rows = buildList { repeat(100) { add(testElement()) } }
            rows.forEach { insert(it) }
            val london = GeoPoint(51.509865, -0.118092)
            val phuket = GeoPoint(7.878978, 98.398392)
            val boundingBox = BoundingBox.fromGeoPoints(listOf(london, phuket))

            val resultRows = selectByBoundingBox(
                minLat = min(boundingBox.latNorth, boundingBox.latSouth),
                maxLat = max(boundingBox.latNorth, boundingBox.latSouth),
                minLon = min(boundingBox.lonEast, boundingBox.lonWest),
                maxLon = max(boundingBox.lonEast, boundingBox.lonWest),
            ).executeAsList()

            rows.forEach {
                assert(!boundingBox.contains(it.lat, it.lon) || resultRows.contains(it))
            }
        }
    }

    private fun testElement(): Element {
        return Element(
            type = arrayOf("node", "way", "relation").random(),
            id = Random.nextLong(),
            lat = Random.nextDouble(-90.0, 90.0),
            lon = Random.nextDouble(-180.0, 180.0),
            timestamp = ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(Random.nextLong(60 * 24 * 30)).toString(),
            boundsMinLat = null,
            boundsMinLon = null,
            boundsMaxLat = null,
            boundsMaxLon = null,
            tags = JsonObject(emptyMap()),
        )
    }
}