package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class PlaceSubmissionApiTest : ApiTestBase() {
    @Test
    fun submitPlace_postsCoordinatesAndExtraFields() = runTest {
        enqueueJson("""{"id":18108,"origin":"user"}""")

        val response = api().submitPlace(
            lat = 18.2649,
            lon = 98.5013,
            category = "cafe",
            name = "Satoshi Cafe",
            address = "1 Main St",
            website = "https://example.com",
            description = "Great coffee",
        )

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/place-submissions", request.url.encodedPath)
        Assert.assertEquals(
            """{"lat":18.2649,"lon":98.5013,"category":"cafe","name":"Satoshi Cafe","extra_fields":{"address":"1 Main St","website":"https://example.com","description":"Great coffee"}}""",
            request.jsonBody(),
        )
        Assert.assertEquals(18108L, response.id)
    }

    @Test
    fun submitPlace_omitsBlankExtraFields() = runTest {
        enqueueJson("""{"id":18109,"origin":"user"}""")

        api().submitPlace(
            lat = 1.0,
            lon = 2.0,
            category = "cafe",
            name = "Satoshi Cafe",
            address = "  ",
            website = null,
            description = null,
        )

        val request = takeRequest()
        Assert.assertEquals(
            """{"lat":1.0,"lon":2.0,"category":"cafe","name":"Satoshi Cafe"}""",
            request.jsonBody(),
        )
    }
}
