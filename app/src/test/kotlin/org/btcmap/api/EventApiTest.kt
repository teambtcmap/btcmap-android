package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class EventApiTest : ApiTestBase() {
    @Test
    fun getEvents_parsesDeltaList() = runTest {
        enqueueJson(DELTA_EVENTS)

        val events = api().getEvents(SINCE, 1000)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/events", request.url.encodedPath)
        Assert.assertEquals(
            "1970-01-01T00:00:00Z",
            request.url.queryParameter("updated_since"),
        )
        Assert.assertEquals("1000", request.url.queryParameter("limit"))
        Assert.assertEquals("true", request.url.queryParameter("include_deleted"))

        Assert.assertEquals(2, events.size)
        Assert.assertEquals(1L, events[0].id)
        Assert.assertEquals(1L, events[0].areaId)
        Assert.assertEquals(7.88, events[0].lat, 0.0)
        Assert.assertEquals(98.38, events[0].lon, 0.0)
        Assert.assertEquals("Phuket Bitcoin Meetup", events[0].name)
        Assert.assertEquals("https://meetup.example/1", events[0].website.toString())
        Assert.assertNotNull(events[0].endsAt)
        Assert.assertEquals("2024-01-01T10:00:00Z", events[0].updatedAt)
        Assert.assertNull(events[0].deletedAt)
        Assert.assertEquals("2024-01-02T10:00:00Z", events[1].updatedAt)
        Assert.assertEquals("2024-01-02T10:00:00Z", events[1].deletedAt)
    }

    @Test
    fun getEvents_keepsEventsWithMissingOrInvalidWebsite() = runTest {
        enqueueJson(
            """
            [
                {
                    "id": 1,
                    "lat": 7.88,
                    "lon": 98.38,
                    "name": "Broken",
                    "website": "not a url",
                    "starts_at": "2025-08-29T19:00:00+07:00",
                    "updated_at": "2024-01-01T10:00:00Z"
                },
                {
                    "id": 2,
                    "lat": 35.1,
                    "lon": 129.03,
                    "name": "None",
                    "website": null,
                    "starts_at": "2025-12-05T00:00:00+09:00",
                    "updated_at": "2024-01-01T10:00:00Z"
                },
                {
                    "id": 3,
                    "lat": 35.2,
                    "lon": 129.04,
                    "name": "Fine",
                    "website": "https://satsnfacts.xyz/",
                    "starts_at": "2025-12-05T00:00:00+09:00",
                    "updated_at": "2024-01-01T10:00:00Z"
                }
            ]
            """.trimIndent()
        )

        val events = api().getEvents(SINCE, 1000)

        Assert.assertEquals(3, events.size)
        Assert.assertNull(events[0].website)
        Assert.assertNull(events[1].website)
        Assert.assertEquals("https://satsnfacts.xyz/", events[2].website.toString())
    }

    @Test
    fun getEvents_missingUpdatedAtThrowsParseException() = runTest {
        enqueueJson(
            """
            [
                {"id":1,"lat":1.0,"lon":2.0,"name":"No cursor","starts_at":"2025-01-01T00:00:00Z"}
            ]
            """.trimIndent()
        )

        try {
            api().getEvents(SINCE, 1000)
            Assert.fail("Expected ApiParseException")
        } catch (e: ApiParseException) {
            Assert.assertEquals("Missing required JSON field 'updated_at'", e.message)
        }
    }

    @Test
    fun getEvent_parsesSingleEvent() = runTest {
        enqueueJson(EVENT)

        val event = api().getEvent(1)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/events/1", request.url.encodedPath)
        Assert.assertEquals(1L, event.id)
        Assert.assertEquals(1L, event.areaId)
        Assert.assertEquals(7.88, event.lat, 0.0)
        Assert.assertEquals(98.38, event.lon, 0.0)
        Assert.assertEquals("Phuket Bitcoin Meetup", event.name)
        Assert.assertEquals("https://meetup.example/1", event.website.toString())
        Assert.assertNotNull(event.endsAt)
    }

    @Test
    fun getEvents_invalidDateThrowsParseException() = runTest {
        enqueueJson(
            """
            [
                {"id":1,"lat":1.0,"lon":2.0,"name":"Bad","starts_at":"not-a-date","updated_at":"2024-01-01T10:00:00Z"}
            ]
            """.trimIndent()
        )

        try {
            api().getEvents(SINCE, 1000)
            Assert.fail("Expected ApiParseException")
        } catch (e: ApiParseException) {
            Assert.assertEquals("Field 'starts_at' is not a valid ISO 8601 datetime", e.message)
        }
    }

    private companion object {
        val SINCE: ZonedDateTime = ZonedDateTime.parse("1970-01-01T00:00:00Z")

        const val EVENT = """
            {
                "id": 1,
                "area_id": 1,
                "lat": 7.88,
                "lon": 98.38,
                "name": "Phuket Bitcoin Meetup",
                "website": "https://meetup.example/1",
                "starts_at": "2025-08-29T19:00:00+07:00",
                "ends_at": "2025-08-29T22:00:00+07:00"
            }
        """

        const val DELTA_EVENTS = """
            [
                {
                    "id": 1,
                    "area_id": 1,
                    "lat": 7.88,
                    "lon": 98.38,
                    "name": "Phuket Bitcoin Meetup",
                    "website": "https://meetup.example/1",
                    "starts_at": "2025-08-29T19:00:00+07:00",
                    "ends_at": "2025-08-29T22:00:00+07:00",
                    "updated_at": "2024-01-01T10:00:00Z"
                },
                {
                    "id": 2,
                    "area_id": null,
                    "lat": 35.1,
                    "lon": 129.03,
                    "name": "Sats N Facts",
                    "website": "https://satsnfacts.xyz/",
                    "starts_at": "2025-12-05T00:00:00+09:00",
                    "updated_at": "2024-01-02T10:00:00Z",
                    "deleted_at": "2024-01-02T10:00:00Z"
                }
            ]
        """
    }
}
