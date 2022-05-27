package db

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ElementQueriesTest {

    @Test
    fun insert() {
        val element = element()

        val db = testDb()
        db.elementQueries.insert(element)

        assertEquals(element, db.elementQueries.selectAll().executeAsOne())
    }

    @Test
    fun selectById() {
        val element = element()

        val db = testDb()
        db.elementQueries.insert(element)

        assertEquals(element, db.elementQueries.selectByTypeAndId(element.type, element.id).executeAsOne())
    }

    @Test
    fun selectBySearchString() {
        val element1 = element().copy(tags = JsonObject(mapOf(Pair("amenity", JsonPrimitive("cafe")))))
        val element2 = element().copy(tags = JsonObject(mapOf(Pair("amenity", JsonPrimitive("bar")))))

        val db = testDb()
        db.elementQueries.insert(element1)
        db.elementQueries.insert(element2)

        val searchResult = db.elementQueries.selectBySearchString("cafe")
        assertEquals(element1, searchResult.executeAsOne())
    }

    fun element(): Element {
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