package org.btcmap.db.table.event

import org.btcmap.db.table.area.AreaGeometry
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class EventExtTest {

    @Test
    fun isWithin_pointInsidePolygon_isTrue() {
        val geometry = AreaGeometry.parse(POLYGON)

        Assert.assertTrue(event(lat = 48.86, lon = 2.35).isWithin(geometry))
    }

    @Test
    fun isWithin_pointOutsidePolygon_isFalse() {
        val geometry = AreaGeometry.parse(POLYGON)

        Assert.assertFalse(event(lat = 48.0, lon = 2.35).isWithin(geometry))
        Assert.assertFalse(event(lat = 48.86, lon = 10.0).isWithin(geometry))
    }

    @Test
    fun isWithin_nullGeoJson_isFalse() {
        Assert.assertFalse(event(lat = 0.0, lon = 0.0).isWithin(AreaGeometry.parse(null)))
    }

    private fun event(lat: Double, lon: Double): Event {
        return Event(
            id = 1,
            areaId = null,
            lat = lat,
            lon = lon,
            name = "Event",
            website = null,
            startsAt = ZonedDateTime.parse("2026-01-01T00:00:00Z"),
            endsAt = null,
        )
    }

    private companion object {
        const val POLYGON =
            """{"type":"Polygon","coordinates":[[[2.22,48.81],[2.47,48.81],[2.47,48.91],[2.22,48.91],[2.22,48.81]]]}"""
    }
}
