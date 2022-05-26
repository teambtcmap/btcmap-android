package db

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceQueriesTest {

    @Test
    fun insert() {
        val db = testDb()

        val place = Place(
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

        db.placeQueries.insert(place)
        assertEquals(place, db.placeQueries.selectById(place.id).executeAsOne())
    }
}