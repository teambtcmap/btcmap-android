package org.btcmap.map

import org.btcmap.db.table.place.Place
import org.maplibre.android.geometry.LatLngBounds
import java.time.ZonedDateTime

object ParisPlaces {
    const val CENTER_LAT = 48.8566
    const val CENTER_LON = 2.3522

    private const val SPACING = 0.0015

    val places: List<Place> = buildList {
        var id = 1L
        for (row in 0 until 2) {
            for (col in -2..2) {
                val lat = CENTER_LAT + row * SPACING
                val lon = CENTER_LON + col * SPACING
                add(place(id = id, name = "Paris Merchant $id", lat = lat, lon = lon))
                id++
            }
        }
    }

    val target: Place
        get() = places.first { it.lat == CENTER_LAT && it.lon == CENTER_LON }

    val bounds: LatLngBounds = LatLngBounds.from(
        latNorth = CENTER_LAT + 0.0025,
        lonEast = CENTER_LON + 0.004,
        latSouth = CENTER_LAT - 0.001,
        lonWest = CENTER_LON - 0.004,
    )

    private fun place(id: Long, name: String, lat: Double, lon: Double): Place {
        return Place(
            id = id,
            updatedAt = ZonedDateTime.parse("2000-01-01T00:00:00Z"),
            lat = lat,
            lon = lon,
            icon = "local_cafe",
            name = name,
            localizedName = null,
            verifiedAt = null,
            address = null,
            openingHours = null,
            localizedOpeningHours = null,
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
    }
}
