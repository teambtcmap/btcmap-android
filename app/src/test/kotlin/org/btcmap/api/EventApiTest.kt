package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class EventApiTest : ApiTestBase() {
    @Test
    fun getEvents_parsesList() = runTest {
        enqueueJson(EVENTS)

        val events = api().getEvents()

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/events", request.url.encodedPath)

        Assert.assertEquals(2, events.size)
        Assert.assertEquals(1L, events[0].id)
        Assert.assertEquals(1L, events[0].areaId)
        Assert.assertEquals(7.88, events[0].lat, 0.0)
        Assert.assertEquals(98.38, events[0].lon, 0.0)
        Assert.assertEquals("Phuket Bitcoin Meetup", events[0].name)
        Assert.assertEquals("https://meetup.example/1", events[0].website.toString())
        Assert.assertNotNull(events[0].endsAt)
        Assert.assertNull(events[1].areaId)
        Assert.assertNull(events[1].endsAt)
    }

    @Test
    fun getAreaEvents_usesIdOrAlias() = runTest {
        enqueueJson(EVENTS)

        val events = api().getAreaEvents("grand-paris")

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/areas/grand-paris/events", request.url.encodedPath)
        Assert.assertEquals(2, events.size)
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

    private companion object {
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

        const val EVENTS = """
            [
                {
                    "id": 1,
                    "area_id": 1,
                    "lat": 7.88,
                    "lon": 98.38,
                    "name": "Phuket Bitcoin Meetup",
                    "website": "https://meetup.example/1",
                    "starts_at": "2025-08-29T19:00:00+07:00",
                    "ends_at": "2025-08-29T22:00:00+07:00"
                },
                {
                    "id": 2,
                    "area_id": null,
                    "lat": 35.1,
                    "lon": 129.03,
                    "name": "Sats N Facts",
                    "website": "https://satsnfacts.xyz/",
                    "starts_at": "2025-12-05T00:00:00+09:00",
                    "ends_at": null
                }
            ]
        """
    }
}
