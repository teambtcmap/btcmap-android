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
    fun insertOrReplace() {
        testDb().elementQueries.apply {
            val row = testElement()
            insertOrReplace(row)
            assertEquals(row, selectAll().executeAsOne())
        }
    }

    @Test
    fun selectAll() {
        testDb().elementQueries.apply {
            val rows = (0..Random.nextInt(6)).map { testElement() }
            rows.forEach { insertOrReplace(it) }
            assertEquals(rows, selectAll().executeAsList())
        }
    }

    @Test
    fun selectById() {
        testDb().elementQueries.apply {
            val rows = (0..Random.nextInt(6)).map { testElement() }
            rows.forEach { insertOrReplace(it) }
            val randomRow = rows.random()
            assertEquals(randomRow, selectById(randomRow.id).executeAsOne())
        }
    }

    @Test
    fun selectBySearchString() {
        testDb().elementQueries.apply {
            val row1 = testElement().copy(
                osm_json = JsonObject(
                    mapOf(
                        Pair(
                            "amenity",
                            JsonPrimitive("cafe")
                        )
                    )
                ).toString()
            )
            val row2 = testElement().copy(
                osm_json = JsonObject(
                    mapOf(
                        Pair(
                            "amenity",
                            JsonPrimitive("bar")
                        )
                    )
                ).toString()
            )
            insertOrReplace(row1)
            insertOrReplace(row2)

            val result = selectBySearchString("cafe").executeAsOne()
            assertEquals(row1, result)
        }
    }

    @Test
    fun selectByBoundingBox() {
        testDb().elementQueries.apply {
            val rows = buildList { repeat(100) { add(testElement()) } }
            rows.forEach { insertOrReplace(it) }
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
            id = "${arrayOf("node", "way", "relation").random()}:${Random.nextLong()}",
            lat = Random.nextDouble(-90.0, 90.0),
            lon = Random.nextDouble(-180.0, 180.0),
            osm_json = "{}",
            created_at = ZonedDateTime.now(ZoneOffset.UTC)
                .minusMinutes(Random.nextLong(60 * 24 * 30)).toString(),
            updated_at = ZonedDateTime.now(ZoneOffset.UTC)
                .minusMinutes(Random.nextLong(60 * 24 * 30)).toString(),
            deleted_at = "",
        )
    }
}