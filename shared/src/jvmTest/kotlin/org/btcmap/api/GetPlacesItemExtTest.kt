package org.btcmap.api

import kotlinx.serialization.json.jsonPrimitive
import org.btcmap.util.toUrl
import org.btcmap.json.parseJsonObject
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant

class GetPlacesItemExtTest {
    @Test
    fun toPlace_mapsApiItemToProjection() {
        val item = GetPlacesItem(
            id = 42,
            lat = 1.5,
            lon = 2.5,
            icon = "cafe",
            name = "Cafe",
            localizedName = parseJsonObject("""{"de":"Kaffee"}"""),
            updatedAt = "2026-01-02T03:04:05Z",
            deletedAt = "2026-01-03T00:00:00Z",
            requiredAppUrl = "https://app.example",
            boostedUntil = "2026-02-01T00:00:00Z",
            verifiedAt = "2026-01-15",
            address = "1 Main St",
            openingHours = "Mo-Fr 09:00-17:00",
            website = "https://cafe.example",
            phone = "+123",
            email = "a@b.c",
            twitter = "https://x.com/cafe",
            facebook = "https://fb.com/cafe",
            instagram = "https://ig.com/cafe",
            line = "https://line.me/cafe",
            comments = 3L,
            telegram = "https://t.me/cafe",
            osmId = "node:42",
        )

        val place = item.toPlace()

        Assert.assertEquals("2026-01-02T03:04:05Z", place.updatedAt.toString())
        Assert.assertEquals("Kaffee", place.localizedName!!.getValue("de").jsonPrimitive.content)
        Assert.assertEquals(Instant.parse("2026-01-15T00:00:00Z"), place.verifiedAt)
        Assert.assertEquals(Instant.parse("2026-02-01T00:00:00Z"), place.boostedUntil)
        Assert.assertEquals("https://cafe.example".toUrl(), place.website)
        Assert.assertEquals("https://x.com/cafe".toUrl(), place.twitter)
        Assert.assertEquals("https://fb.com/cafe".toUrl(), place.facebook)
        Assert.assertEquals("https://ig.com/cafe".toUrl(), place.instagram)
        Assert.assertEquals("https://line.me/cafe".toUrl(), place.line)
        Assert.assertEquals("https://app.example".toUrl(), place.requiredAppUrl)
        Assert.assertEquals("https://t.me/cafe".toUrl(), place.telegram)
        Assert.assertEquals(3L, place.comments)
        Assert.assertEquals("node:42", place.osmId)
    }

    @Test
    fun toPlace_acceptsFullRfc3339VerifiedAt() {
        val item = GetPlacesItem(
            id = 1,
            lat = 0.0,
            lon = 0.0,
            icon = "store",
            name = "Cafe",
            localizedName = null,
            updatedAt = "2026-01-02T03:04:05Z",
            deletedAt = null,
            requiredAppUrl = null,
            boostedUntil = null,
            verifiedAt = "2026-01-15T00:00:00Z",
            address = null,
            openingHours = null,
            website = null,
            phone = null,
            email = null,
            twitter = null,
            facebook = null,
            instagram = null,
            line = null,
            comments = null,
            telegram = null,
            osmId = null,
        )

        Assert.assertEquals(Instant.parse("2026-01-15T00:00:00Z"), item.toPlace().verifiedAt)
    }

    @Test
    fun toPlace_acceptsVerifiedAtWithOmittedZeroSeconds() {
        // The API's RFC 3339 form drops the seconds when they are zero, so a
        // verified place arrives as `...T00:00Z` rather than a full instant.
        val item = GetPlacesItem(
            id = 1,
            lat = 0.0,
            lon = 0.0,
            icon = "store",
            name = "Cafe",
            localizedName = null,
            updatedAt = "2026-01-02T03:04:05Z",
            deletedAt = null,
            requiredAppUrl = null,
            boostedUntil = null,
            verifiedAt = "2026-01-15T00:00Z",
            address = null,
            openingHours = null,
            website = null,
            phone = null,
            email = null,
            twitter = null,
            facebook = null,
            instagram = null,
            line = null,
            comments = null,
            telegram = null,
            osmId = null,
        )

        Assert.assertEquals(Instant.parse("2026-01-15T00:00:00Z"), item.toPlace().verifiedAt)
    }

    @Test
    fun toPlace_keepsOptionalFieldsNull() {
        val item = GetPlacesItem(
            id = 1,
            lat = 0.0,
            lon = 0.0,
            icon = "store",
            name = "Blank",
            localizedName = null,
            updatedAt = "2026-01-02T03:04:05Z",
            deletedAt = null,
            requiredAppUrl = null,
            boostedUntil = null,
            verifiedAt = null,
            address = null,
            openingHours = null,
            website = null,
            phone = null,
            email = null,
            twitter = null,
            facebook = null,
            instagram = null,
            line = null,
            comments = null,
            telegram = null,
            osmId = null,
        )

        val place = item.toPlace()

        Assert.assertNull(place.localizedName)
        Assert.assertNull(place.verifiedAt)
        Assert.assertNull(place.boostedUntil)
        Assert.assertNull(place.website)
        Assert.assertNull(place.twitter)
        Assert.assertNull(place.facebook)
        Assert.assertNull(place.instagram)
        Assert.assertNull(place.line)
        Assert.assertNull(place.requiredAppUrl)
        Assert.assertNull(place.telegram)
        Assert.assertNull(place.comments)
        Assert.assertNull(place.osmId)
    }
}
