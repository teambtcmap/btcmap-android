package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PlaceApiTest : ApiTestBase() {
    @Test
    fun getPlaces_sendsParametersAndParsesAllFields() = runTest {
        enqueueJson(FULL_PLACE_ARRAY)

        val updatedSince = ZonedDateTime.parse("2026-01-01T00:00:00Z")
        val places = api().getPlaces(updatedSince = updatedSince, limit = 10)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/places", request.url.encodedPath)
        Assert.assertEquals("10", request.url.queryParameter("limit"))
        Assert.assertEquals(
            updatedSince.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            request.url.queryParameter("updated_since"),
        )
        Assert.assertTrue(request.url.queryParameter("fields")!!.contains("deleted_at"))

        Assert.assertEquals(1, places.size)
        val place = places.single()
        Assert.assertEquals(42L, place.id)
        Assert.assertEquals(1.5, place.lat, 0.0)
        Assert.assertEquals(2.5, place.lon, 0.0)
        Assert.assertEquals("cafe", place.icon)
        Assert.assertEquals("Cafe", place.name)
        Assert.assertEquals("Kaffee", place.localizedName!!.get("de").asString)
        Assert.assertEquals("2026-01-02T03:04:05Z", place.updatedAt)
        Assert.assertEquals("https://app.example", place.requiredAppUrl)
        Assert.assertEquals("2026-02-01T00:00:00Z", place.boostedUntil)
        Assert.assertEquals("2026-01-15", place.verifiedAt)
        Assert.assertEquals("1 Main St", place.address)
        Assert.assertEquals("Mo-Fr 09:00-17:00", place.openingHours)
        Assert.assertEquals("Mo-Fr", place.localizedOpeningHours!!.get("de").asString)
        Assert.assertEquals("https://cafe.example", place.website)
        Assert.assertEquals("+123", place.phone)
        Assert.assertEquals("a@b.c", place.email)
        Assert.assertEquals("https://x.com/cafe", place.twitter)
        Assert.assertEquals("https://fb.com/cafe", place.facebook)
        Assert.assertEquals("https://ig.com/cafe", place.instagram)
        Assert.assertEquals("https://line.me/cafe", place.line)
        Assert.assertEquals(3L, place.comments)
        Assert.assertEquals("https://t.me/cafe", place.telegram)
    }

    @Test
    fun getPlaces_omitsUpdatedSinceWhenNullAndParsesNullFields() = runTest {
        enqueueJson(
            """
            [
                {
                    "id": 1,
                    "lat": 0.0,
                    "lon": 0.0,
                    "icon": "store",
                    "name": "Blank",
                    "localized_name": null,
                    "updated_at": "2026-01-02T03:04:05Z",
                    "deleted_at": null,
                    "required_app_url": null,
                    "boosted_until": null,
                    "verified_at": null,
                    "address": "  ",
                    "opening_hours": null,
                    "localized_opening_hours": null,
                    "website": null,
                    "phone": null,
                    "email": null,
                    "twitter": null,
                    "facebook": null,
                    "instagram": null,
                    "line": null,
                    "comments": null,
                    "telegram": null
                }
            ]
            """.trimIndent()
        )

        val places = api().getPlaces(updatedSince = null, limit = 5)

        val request = takeRequest()
        Assert.assertNull(request.url.queryParameter("updated_since"))

        val place = places.single()
        Assert.assertNull(place.localizedName)
        Assert.assertNull(place.deletedAt)
        Assert.assertNull(place.requiredAppUrl)
        Assert.assertNull(place.boostedUntil)
        Assert.assertNull(place.verifiedAt)
        Assert.assertNull(place.address)
        Assert.assertNull(place.openingHours)
        Assert.assertNull(place.localizedOpeningHours)
        Assert.assertNull(place.website)
        Assert.assertNull(place.phone)
        Assert.assertNull(place.email)
        Assert.assertNull(place.twitter)
        Assert.assertNull(place.facebook)
        Assert.assertNull(place.instagram)
        Assert.assertNull(place.line)
        Assert.assertNull(place.comments)
        Assert.assertNull(place.telegram)
    }

    @Test
    fun getPlaces_parsesDeletedPlace() = runTest {
        enqueueJson(
            """
            [
                {
                    "id": 9,
                    "lat": 0.0,
                    "lon": 0.0,
                    "icon": "store",
                    "name": "Gone",
                    "localized_name": null,
                    "updated_at": "2026-01-02T03:04:05Z",
                    "deleted_at": "2026-01-03T00:00:00Z",
                    "required_app_url": null,
                    "boosted_until": null,
                    "verified_at": null,
                    "address": null,
                    "opening_hours": null,
                    "localized_opening_hours": null,
                    "website": null,
                    "phone": null,
                    "email": null,
                    "twitter": null,
                    "facebook": null,
                    "instagram": null,
                    "line": null,
                    "comments": null,
                    "telegram": null
                }
            ]
            """.trimIndent()
        )

        val place = api().getPlaces(updatedSince = null, limit = 5).single()

        Assert.assertEquals("2026-01-03T00:00:00Z", place.deletedAt)
    }

    @Test
    fun getPlaceCoordinates_parsesLatLon() = runTest {
        enqueueJson("""{"id":5,"lat":47.55,"lon":8.72}""")

        val coordinates = api().getPlaceCoordinates(5)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/places/5", request.url.encodedPath)
        Assert.assertEquals("lat,lon", request.url.queryParameter("fields"))
        Assert.assertEquals(47.55, coordinates.lat, 0.0)
        Assert.assertEquals(8.72, coordinates.lon, 0.0)
    }

    @Test
    fun savePlace_postsId() = runTest {
        enqueueJson("")

        api().savePlace(123)

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/places/saved", request.url.encodedPath)
        Assert.assertEquals("123", request.jsonBody())
    }

    @Test
    fun removeSavedPlace_deletes() = runTest {
        enqueueJson("")

        api().removeSavedPlace(123)

        val request = takeRequest()
        Assert.assertEquals("DELETE", request.method)
        Assert.assertEquals("/v4/places/saved/123", request.url.encodedPath)
    }

    private companion object {
        const val FULL_PLACE_ARRAY = """
            [
                {
                    "id": 42,
                    "lat": 1.5,
                    "lon": 2.5,
                    "icon": "cafe",
                    "name": "Cafe",
                    "localized_name": {"de": "Kaffee"},
                    "updated_at": "2026-01-02T03:04:05Z",
                    "deleted_at": null,
                    "required_app_url": "https://app.example",
                    "boosted_until": "2026-02-01T00:00:00Z",
                    "verified_at": "2026-01-15",
                    "address": "1 Main St",
                    "opening_hours": "Mo-Fr 09:00-17:00",
                    "localized_opening_hours": {"de": "Mo-Fr"},
                    "website": "https://cafe.example",
                    "phone": "+123",
                    "email": "a@b.c",
                    "twitter": "https://x.com/cafe",
                    "facebook": "https://fb.com/cafe",
                    "instagram": "https://ig.com/cafe",
                    "line": "https://line.me/cafe",
                    "comments": 3,
                    "telegram": "https://t.me/cafe"
                }
            ]
        """
    }
}
