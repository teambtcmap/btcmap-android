package org.btcmap.place

import org.btcmap.db.table.place.Place
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant

class PlaceExtTest {

    private val now = Instant.parse("2026-06-01T00:00:00Z")

    @Test
    fun isBoosted_trueForFutureBoost() {
        val place = place(boostedUntil = Instant.parse("2026-06-02T00:00:00Z"))

        Assert.assertTrue(place.isBoosted(now))
    }

    @Test
    fun isBoosted_falseForExpiredBoost() {
        val place = place(boostedUntil = Instant.parse("2026-05-31T23:59:59Z"))

        Assert.assertFalse(place.isBoosted(now))
    }

    @Test
    fun isBoosted_falseWhenBoostIsNull() {
        Assert.assertFalse(place(boostedUntil = null).isBoosted(now))
    }

    @Test
    fun btcmapUrl_buildsFromThePlaceId() {
        Assert.assertEquals(
            "https://btcmap.org/merchant/123",
            place(boostedUntil = null).copy(id = 123).btcmapUrl(),
        )
    }

    @Test
    fun osmMapUrl_centresOnThePlace() {
        val place = place(boostedUntil = null).copy(lat = 52.2333742, lon = 21.0711489)

        Assert.assertEquals(
            "https://www.openstreetmap.org/?mlat=52.2333742&mlon=21.0711489" +
                "#map=17/52.2333742/21.0711489",
            place.osmMapUrl(),
        )
    }

    @Test
    fun osmUrl_buildsFromOsmId() {
        val place = place(boostedUntil = null).copy(osmId = "node:123")

        Assert.assertEquals("https://www.openstreetmap.org/node/123", place.osmUrl())
    }

    @Test
    fun osmUrl_keepsWayAndRelationTypes() {
        Assert.assertEquals(
            "https://www.openstreetmap.org/way/9",
            place(boostedUntil = null).copy(osmId = "way:9").osmUrl(),
        )
        Assert.assertEquals(
            "https://www.openstreetmap.org/relation/42",
            place(boostedUntil = null).copy(osmId = "relation:42").osmUrl(),
        )
    }

    @Test
    fun osmUrl_nullWhenOsmIdMissingOrMalformed() {
        Assert.assertNull(place(boostedUntil = null).osmUrl())
        Assert.assertNull(place(boostedUntil = null).copy(osmId = "node").osmUrl())
        Assert.assertNull(place(boostedUntil = null).copy(osmId = "node:").osmUrl())
        Assert.assertNull(place(boostedUntil = null).copy(osmId = ":123").osmUrl())
    }

    @Test
    fun osmEditUrl_buildsFromOsmId() {
        val place = place(boostedUntil = null).copy(osmId = "node:123")

        Assert.assertEquals(
            "https://www.openstreetmap.org/edit?node=123",
            place.osmEditUrl(),
        )
    }

    @Test
    fun osmEditUrl_keepsWayAndRelationTypes() {
        Assert.assertEquals(
            "https://www.openstreetmap.org/edit?way=9",
            place(boostedUntil = null).copy(osmId = "way:9").osmEditUrl(),
        )
        Assert.assertEquals(
            "https://www.openstreetmap.org/edit?relation=42",
            place(boostedUntil = null).copy(osmId = "relation:42").osmEditUrl(),
        )
    }

    @Test
    fun osmEditUrl_nullWhenOsmIdMissingOrMalformed() {
        Assert.assertNull(place(boostedUntil = null).osmEditUrl())
        Assert.assertNull(place(boostedUntil = null).copy(osmId = "node").osmEditUrl())
        Assert.assertNull(place(boostedUntil = null).copy(osmId = "node:").osmEditUrl())
        Assert.assertNull(place(boostedUntil = null).copy(osmId = ":123").osmEditUrl())
    }

    private fun place(boostedUntil: Instant?): Place {
        return Place(
            id = 1,
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            lat = 0.0,
            lon = 0.0,
            icon = "store",
            name = "Test",
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
            boostedUntil = boostedUntil,
            comments = null,
            telegram = null,
            osmId = null,
        )
    }
}
