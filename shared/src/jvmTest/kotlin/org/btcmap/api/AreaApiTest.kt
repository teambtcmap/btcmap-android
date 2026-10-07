package org.btcmap.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert
import org.junit.Test
import kotlin.time.Instant
import java.util.Locale

class AreaApiTest : ApiTestBase() {
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

        val areas = api().getAreas(Instant.parse("2025-06-11T00:00:00Z"), 500L)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/areas", request.url.encodedPath)
        Assert.assertEquals(
            "id,name,type,url_alias,icon,icon_wide,website_url,description,verified_at," +
                "localized_name,localized_description,bbox,geo_json,updated_at,deleted_at",
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
        Assert.assertEquals("Большой Париж", area.localizedName!!.getValue("ru").jsonPrimitive.content)
        Assert.assertEquals("Greater Paris", area.localizedDescription!!.getValue("en").jsonPrimitive.content)
        Assert.assertEquals("2026-01-06", area.verifiedAt)
        Assert.assertEquals(2.22, area.bboxWest!!, 0.0001)
        Assert.assertEquals(48.91, area.bboxNorth!!, 0.0001)
        Assert.assertEquals(
            """{"type":"Polygon","coordinates":[[[2.22,48.81],[2.47,48.81],[2.47,48.91],[2.22,48.81]]]}""",
            area.geoJson,
        )
        Assert.assertEquals("2025-06-11T00:00:00Z", area.updatedAt)
        Assert.assertEquals("2025-06-12T00:00:00Z", area.deletedAt)
    }

    @Test
    fun getAreasDelta_handlesMissingBboxAndBlankOptionalFields() = runTest {
        enqueueJson(AREAS_DELTA_MINIMAL)

        val area = api().getAreas(Instant.parse("2025-06-11T00:00:00Z"), 1L).single()

        Assert.assertNull(area.icon)
        Assert.assertNull(area.iconWide)
        Assert.assertNull(area.description)
        Assert.assertNull(area.localizedName)
        Assert.assertNull(area.localizedDescription)
        Assert.assertNull(area.verifiedAt)
        Assert.assertNull(area.deletedAt)
        Assert.assertNull(area.bboxWest)
        Assert.assertNull(area.bboxNorth)
        Assert.assertNull(area.geoJson)
    }

    @Test
    fun verifyArea_patchesTheVerifiedDate() = runTest {
        enqueueJson(
            """
            {
                "id": 7,
                "name": "Grand Paris",
                "type": "community",
                "url_alias": "grand-paris",
                "website_url": "https://btcmap.org/community/grand-paris",
                "verified_at": "2026-10-07"
            }
            """.trimIndent()
        )

        api().verifyArea(7L, "2026-10-07")

        val request = takeRequest()
        Assert.assertEquals("PATCH", request.method)
        Assert.assertEquals("/v4/areas/7", request.url.encodedPath)
        Assert.assertEquals("""{"verified_at":"2026-10-07"}""", request.jsonBody())
    }

    @Test
    fun setAreaName_patchesTheName() = runTest {
        enqueueJson(
            """
            {
                "id": 7,
                "name": "New Name",
                "type": "community",
                "url_alias": "grand-paris",
                "website_url": "https://btcmap.org/community/grand-paris"
            }
            """.trimIndent()
        )

        api().setAreaName(7L, "New Name")

        val request = takeRequest()
        Assert.assertEquals("PATCH", request.method)
        Assert.assertEquals("/v4/areas/7", request.url.encodedPath)
        Assert.assertEquals("""{"name":"New Name"}""", request.jsonBody())
    }

    @Test
    fun setAreaDescription_patchesTheDescription() = runTest {
        enqueueJson(
            """
            {
                "id": 7,
                "name": "Grand Paris",
                "type": "community",
                "url_alias": "grand-paris",
                "website_url": "https://btcmap.org/community/grand-paris",
                "description": "New"
            }
            """.trimIndent()
        )

        api().setAreaDescription(7L, "New")

        Assert.assertEquals("""{"description":"New"}""", takeRequest().jsonBody())
    }

    @Test
    fun setAreaDescription_nullClearsIt() = runTest {
        enqueueJson(
            """
            {
                "id": 7,
                "name": "Grand Paris",
                "type": "community",
                "url_alias": "grand-paris",
                "website_url": "https://btcmap.org/community/grand-paris",
                "description": null
            }
            """.trimIndent()
        )

        api().setAreaDescription(7L, null)

        Assert.assertEquals("""{"description":null}""", takeRequest().jsonBody())
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
                    "localized_name": {"en": "Grand Paris", "ru": "Большой Париж"},
                    "localized_description": {"en": "Greater Paris"},
                    "bbox": [2.22, 48.81, 2.47, 48.91],
                    "geo_json": {"type":"Polygon","coordinates":[[[2.22,48.81],[2.47,48.81],[2.47,48.91],[2.22,48.81]]]},
                    "verified_at": "2026-01-06",
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
    }
}
