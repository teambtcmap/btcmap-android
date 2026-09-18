package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime
import java.util.Locale

class AreaApiTest : ApiTestBase() {
    @Test
    fun getAreas_sendsCoordinatesAndParsesEvents() = runTest {
        enqueueJson(AREAS_WITH_EVENTS)

        val areas = api().getAreas(lat = 48.8566, lon = 2.3522)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/areas", request.url.encodedPath)
        Assert.assertEquals("48.8566", request.url.queryParameter("lat"))
        Assert.assertEquals("2.3522", request.url.queryParameter("lon"))

        val area = areas.single()
        Assert.assertEquals(7L, area.id)
        Assert.assertEquals("Grand Paris", area.name)
        Assert.assertEquals("community", area.type)
        Assert.assertEquals("grand-paris", area.urlAlias)
        Assert.assertEquals("https://btcmap.org/community/grand-paris", area.websiteUrl)
        Assert.assertEquals(2, area.upcomingEventsCount)
        Assert.assertEquals(2, area.upcomingEvents.size)
        Assert.assertEquals(1L, area.upcomingEvents[0].id)
        Assert.assertEquals(7L, area.upcomingEvents[0].areaId)
        Assert.assertEquals("Meetup", area.upcomingEvents[0].name)
        Assert.assertNotNull(area.upcomingEvents[0].endsAt)
        Assert.assertNull(area.upcomingEvents[1].areaId)
        Assert.assertNull(area.upcomingEvents[1].endsAt)
    }

    @Test
    fun getAreas_withoutUpcomingEventsReturnsEmptyList() = runTest {
        enqueueJson(AREAS_WITHOUT_EVENTS)

        val area = api().getAreas(lat = 1.0, lon = 2.0).single()

        Assert.assertEquals(0, area.upcomingEventsCount)
        Assert.assertTrue(area.upcomingEvents.isEmpty())
    }

    @Test
    fun getArea_parsesOptionalFields() = runTest {
        enqueueJson(
            """
            {
                "id": 7,
                "name": "Grand Paris",
                "type": "community",
                "url_alias": "grand-paris",
                "icon": "https://static/icon.png",
                "icon_wide": "https://static/wide.png",
                "website_url": "https://btcmap.org/community/grand-paris",
                "description": "Greater Paris"
            }
            """.trimIndent()
        )

        val area = api().getArea("grand-paris")

        val request = takeRequest()
        Assert.assertEquals("/v4/areas/grand-paris", request.url.encodedPath)
        Assert.assertEquals(7L, area.id)
        Assert.assertEquals("https://static/icon.png", area.icon)
        Assert.assertEquals("https://static/wide.png", area.iconWide)
        Assert.assertEquals("Greater Paris", area.description)
    }

    @Test
    fun getArea_handlesNullAndBlankOptionalFields() = runTest {
        enqueueJson(
            """
            {
                "id": 7,
                "name": "Grand Paris",
                "type": "community",
                "url_alias": "grand-paris",
                "icon": null,
                "icon_wide": "  ",
                "website_url": "https://btcmap.org/community/grand-paris",
                "description": null
            }
            """.trimIndent()
        )

        val area = api().getArea("7")

        Assert.assertNull(area.icon)
        Assert.assertNull(area.iconWide)
        Assert.assertNull(area.description)
    }

    @Test
    fun getArea_sendsCurrentLocaleLanguage() = runTest {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.GERMAN)
        try {
            enqueueJson(
                """
                {
                    "id": 7,
                    "name": "Grand Paris",
                    "type": "community",
                    "url_alias": "grand-paris",
                    "website_url": "https://btcmap.org/community/grand-paris",
                    "description": "Großraum Paris"
                }
                """.trimIndent()
            )

            val area = api().getArea("grand-paris")

            val request = takeRequest()
            Assert.assertEquals("de", request.url.queryParameter("lang"))
            Assert.assertEquals("Großraum Paris", area.description)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun getAreasDelta_sendsCursorFieldsAndParsesBboxAndTombstone() = runTest {
        enqueueJson(AREAS_DELTA)

        val areas = api().getAreas(ZonedDateTime.parse("2025-06-11T00:00:00Z"), 500L)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/areas", request.url.encodedPath)
        Assert.assertEquals(
            "id,name,type,url_alias,icon,icon_wide,website_url,description,bbox,updated_at,deleted_at",
            request.url.queryParameter("fields"),
        )
        Assert.assertEquals(
            "2025-06-11T00:00:00Z",
            request.url.queryParameter("updated_since"),
        )
        Assert.assertEquals("500", request.url.queryParameter("limit"))
        Assert.assertEquals("true", request.url.queryParameter("include_deleted"))

        val area = areas.single()
        Assert.assertEquals(7L, area.id)
        Assert.assertEquals("Grand Paris", area.name)
        Assert.assertEquals("community", area.type)
        Assert.assertEquals("grand-paris", area.urlAlias)
        Assert.assertEquals("Greater Paris", area.description)
        Assert.assertEquals(2.22, area.bboxWest!!, 0.0001)
        Assert.assertEquals(48.91, area.bboxNorth!!, 0.0001)
        Assert.assertEquals("2025-06-11T00:00:00Z", area.updatedAt)
        Assert.assertEquals("2025-06-12T00:00:00Z", area.deletedAt)
    }

    @Test
    fun getAreasDelta_handlesMissingBboxAndBlankOptionalFields() = runTest {
        enqueueJson(AREAS_DELTA_MINIMAL)

        val area = api().getAreas(ZonedDateTime.parse("2025-06-11T00:00:00Z"), 1L).single()

        Assert.assertNull(area.icon)
        Assert.assertNull(area.iconWide)
        Assert.assertNull(area.description)
        Assert.assertNull(area.deletedAt)
        Assert.assertNull(area.bboxWest)
        Assert.assertNull(area.bboxNorth)
    }

    @Test
    fun saveArea_postsIdAndReturnsUpdatedList() = runTest {
        enqueueJson("[123,456]")

        val result = api().saveArea(123)

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/areas/saved", request.url.encodedPath)
        Assert.assertEquals("123", request.jsonBody())
        Assert.assertEquals(listOf(123L, 456L), result)
    }

    @Test
    fun removeSavedArea_deletesAndReturnsUpdatedList() = runTest {
        enqueueJson("[456]")

        val result = api().removeSavedArea(123)

        val request = takeRequest()
        Assert.assertEquals("DELETE", request.method)
        Assert.assertEquals("/v4/areas/saved/123", request.url.encodedPath)
        Assert.assertEquals(listOf(456L), result)
    }

    private companion object {
        const val AREAS_WITH_EVENTS = """
            [
                {
                    "id": 7,
                    "name": "Grand Paris",
                    "type": "community",
                    "url_alias": "grand-paris",
                    "website_url": "https://btcmap.org/community/grand-paris",
                    "upcoming_events": [
                        {
                            "id": 1,
                            "area_id": 7,
                            "lat": 7.9,
                            "lon": 98.3,
                            "name": "Meetup",
                            "website": "https://m.example",
                            "starts_at": "2026-09-25T19:00:00Z",
                            "ends_at": "2026-09-25T22:00:00Z"
                        },
                        {
                            "id": 2,
                            "area_id": null,
                            "lat": 3.0,
                            "lon": 4.0,
                            "name": "Open",
                            "website": "https://o.example",
                            "starts_at": "2026-09-26T19:00:00Z",
                            "ends_at": null
                        }
                    ]
                }
            ]
        """

        const val AREAS_DELTA = """
            [
                {
                    "id": 7,
                    "name": "Grand Paris",
                    "type": "community",
                    "url_alias": "grand-paris",
                    "icon": "https://static.example/icon.png",
                    "icon_wide": "https://static.example/wide.png",
                    "website_url": "https://btcmap.org/community/grand-paris",
                    "description": "Greater Paris",
                    "bbox": [2.22, 48.81, 2.47, 48.91],
                    "updated_at": "2025-06-11T00:00:00Z",
                    "deleted_at": "2025-06-12T00:00:00Z"
                }
            ]
        """

        const val AREAS_DELTA_MINIMAL = """
            [
                {
                    "id": 7,
                    "name": "Grand Paris",
                    "type": "community",
                    "url_alias": "grand-paris",
                    "icon": null,
                    "icon_wide": "  ",
                    "website_url": "https://btcmap.org/community/grand-paris",
                    "description": null,
                    "bbox": null,
                    "updated_at": "2025-06-11T00:00:00Z",
                    "deleted_at": null
                }
            ]
        """

        const val AREAS_WITHOUT_EVENTS = """
            [
                {
                    "id": 7,
                    "name": "Grand Paris",
                    "type": "community",
                    "url_alias": "grand-paris",
                    "website_url": "https://btcmap.org/community/grand-paris",
                    "upcoming_events": []
                }
            ]
        """
    }
}
