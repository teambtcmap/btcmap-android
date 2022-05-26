package db

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ElementQueriesTest {

    @Test
    fun insert() {
        val db = testDb()

        val element = Element(
            type = "",
            id = "",
            lat = 0.0,
            lon = 0.0,
            timestamp = "",
            boundsMinLat = null,
            boundsMinLon = null,
            boundsMaxLat = null,
            boundsMaxLon = null,
            tags = JsonObject(emptyMap()),
        )

        db.elementQueries.insert(element)
        assertEquals(element, db.elementQueries.selectById(element.id).executeAsOne())
    }
}