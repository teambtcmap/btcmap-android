package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class SearchApiTest : ApiTestBase() {
    @Test
    fun search_sendsParametersAndParsesMixedResults() = runTest {
        enqueueJson(SEARCH_RESULTS)

        val results = api().search(query = "prague", lat = 50.08, lon = 14.43, limit = 5)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/search/", request.url.encodedPath)
        Assert.assertEquals("prague", request.url.queryParameter("q"))
        Assert.assertEquals("50.08", request.url.queryParameter("lat"))
        Assert.assertEquals("14.43", request.url.queryParameter("lon"))
        Assert.assertEquals("5", request.url.queryParameter("limit"))

        Assert.assertEquals(2, results.size)

        val area = results[0] as SearchResult.Area
        Assert.assertEquals(1L, area.id)
        Assert.assertEquals("Prague", area.name)
        Assert.assertEquals(listOf(14.22, 49.94, 14.71, 50.18), area.bbox)
        Assert.assertEquals("https://example.com/cz.svg", area.iconUrl)

        val place = results[1] as SearchResult.Place
        Assert.assertEquals(28779L, place.id)
        Assert.assertEquals("Bitcoin Coffee", place.name)
        Assert.assertEquals("local_cafe", place.icon)
        Assert.assertEquals(50.08, place.lat, 0.0)
        Assert.assertEquals(14.43, place.lon, 0.0)
    }

    @Test
    fun search_withoutCoordinatesOmitsLatLon() = runTest {
        enqueueJson("""{"results":[]}""")

        val results = api().search(query = "prague", lat = null, lon = null)

        val request = takeRequest()
        Assert.assertNull(request.url.queryParameter("lat"))
        Assert.assertNull(request.url.queryParameter("lon"))
        Assert.assertEquals("20", request.url.queryParameter("limit"))
        Assert.assertTrue(results.isEmpty())
    }

    @Test
    fun search_parsesAreaWithNullOptionalFields() = runTest {
        enqueueJson(
            """
            {
                "results": [
                    {
                        "type": "area",
                        "id": 5,
                        "name": "No Bbox",
                        "bbox": null,
                        "icon": null
                    }
                ]
            }
            """.trimIndent()
        )

        val area = api().search(query = "no", lat = null, lon = null).single() as SearchResult.Area

        Assert.assertNull(area.bbox)
        Assert.assertNull(area.iconUrl)
    }

    private companion object {
        const val SEARCH_RESULTS = """
            {
                "results": [
                    {
                        "type": "area",
                        "id": 1,
                        "name": "Prague",
                        "alias": "prague",
                        "bbox": [14.22, 49.94, 14.71, 50.18],
                        "icon": "https://example.com/cz.svg"
                    },
                    {
                        "type": "place",
                        "id": 28779,
                        "name": "Bitcoin Coffee",
                        "lat": 50.08,
                        "lon": 14.43,
                        "icon": "local_cafe"
                    }
                ]
            }
        """
    }
}
