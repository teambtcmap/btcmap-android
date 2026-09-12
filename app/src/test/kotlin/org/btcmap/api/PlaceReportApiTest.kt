package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class PlaceReportApiTest : ApiTestBase() {
    @Test
    fun reportPlace_postsReportWithComment() = runTest {
        enqueueJson("""{"id":18108,"origin":"user"}""")

        val response = api().reportPlace(placeId = 42, type = "verification", comment = "Looks closed")

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/place-reports", request.url.encodedPath)
        Assert.assertEquals(
            """{"place_id":42,"type":"verification","extra_fields":{"comment":"Looks closed"}}""",
            request.jsonBody(),
        )
        Assert.assertEquals(18108L, response.id)
        Assert.assertEquals("user", response.origin)
    }

    @Test
    fun reportPlace_omitsExtraFieldsWhenCommentBlank() = runTest {
        enqueueJson("""{"id":18109,"origin":"user"}""")

        api().reportPlace(placeId = 42, type = "verification", comment = "  ")

        val request = takeRequest()
        Assert.assertEquals("""{"place_id":42,"type":"verification"}""", request.jsonBody())
    }
}
