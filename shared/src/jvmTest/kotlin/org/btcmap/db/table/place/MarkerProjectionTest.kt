package org.btcmap.db.table.place

import io.ktor.http.Url
import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant

class MarkerProjectionTest {

    @Test
    fun placeToMarker_carriesMarkerFields() {
        val place = place(
            comments = 3,
            requiredAppUrl = "https://example.com/app".toUrl(),
            boostedUntil = Instant.parse("2024-02-01T00:00:00Z"),
        )

        val marker = place.toMarker()

        Assert.assertEquals(7L, marker.id)
        Assert.assertEquals(1.5, marker.lat, 0.0)
        Assert.assertEquals(2.5, marker.lon, 0.0)
        Assert.assertEquals("restaurant", marker.icon)
        Assert.assertEquals("https://example.com/app", marker.requiredAppUrl)
        Assert.assertEquals(3L, marker.comments)
        Assert.assertEquals(place.boostedUntil, marker.boostedUntil)
        Assert.assertEquals(place.verifiedAt, marker.verifiedAt)
    }

    @Test
    fun placeToMarker_defaultsMissingCommentsToZero() {
        Assert.assertEquals(0L, place(comments = null).toMarker().comments)
    }

    private fun place(
        comments: Long?,
        requiredAppUrl: Url? = null,
        boostedUntil: Instant? = null,
    ): Place {
        return Place(
            id = 7,
            updatedAt = Instant.parse("2024-01-01T00:00:00Z"),
            lat = 1.5,
            lon = 2.5,
            icon = "restaurant",
            name = "Cafe",
            localizedName = null,
            verifiedAt = Instant.parse("2024-01-01T00:00:00Z"),
            address = null,
            openingHours = null,
            phone = null,
            website = null,
            email = null,
            twitter = null,
            facebook = null,
            instagram = null,
            line = null,
            requiredAppUrl = requiredAppUrl,
            boostedUntil = boostedUntil,
            comments = comments,
            telegram = null,
            osmId = null,
        )
    }
}
