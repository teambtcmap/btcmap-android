package org.btcmap.api

import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class GetPlacesItemExtTest {
    @Test
    fun toPlace_mapsApiItemToProjection() {
        val item = GetPlacesItem(
            id = 42,
            lat = 1.5,
            lon = 2.5,
            icon = "cafe",
            name = "Cafe",
            localizedName = JsonParser.parseString("""{"de":"Kaffee"}""").asJsonObject,
            updatedAt = "2026-01-02T03:04:05Z",
            deletedAt = "2026-01-03T00:00:00Z",
            requiredAppUrl = "https://app.example",
            boostedUntil = "2026-02-01T00:00:00Z",
            verifiedAt = "2026-01-15",
            address = "1 Main St",
            openingHours = "Mo-Fr 09:00-17:00",
            localizedOpeningHours = JsonParser.parseString("""{"de":"Mo-Fr"}""").asJsonObject,
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

        Assert.assertFalse(place.bundled)
        Assert.assertEquals("2026-01-02T03:04:05Z", place.updatedAt.toString())
        Assert.assertEquals("Kaffee", place.localizedName!!.get("de").asString)
        Assert.assertEquals(ZonedDateTime.parse("2026-01-15T00:00:00Z"), place.verifiedAt)
        Assert.assertEquals(ZonedDateTime.parse("2026-02-01T00:00:00Z"), place.boostedUntil)
        Assert.assertEquals("https://cafe.example".toHttpUrl(), place.website)
        Assert.assertEquals("https://x.com/cafe".toHttpUrl(), place.twitter)
        Assert.assertEquals("https://fb.com/cafe".toHttpUrl(), place.facebook)
        Assert.assertEquals("https://ig.com/cafe".toHttpUrl(), place.instagram)
        Assert.assertEquals("https://line.me/cafe".toHttpUrl(), place.line)
        Assert.assertEquals("https://app.example".toHttpUrl(), place.requiredAppUrl)
        Assert.assertEquals("https://t.me/cafe".toHttpUrl(), place.telegram)
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
            localizedOpeningHours = null,
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

        Assert.assertEquals(ZonedDateTime.parse("2026-01-15T00:00:00Z"), item.toPlace().verifiedAt)
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
            localizedOpeningHours = null,
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
