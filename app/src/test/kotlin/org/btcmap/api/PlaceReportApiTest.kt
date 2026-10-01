package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.btcmap.userAgent
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
            """{"place_id":42,"type":"verification","extra_fields":{"origin":"$userAgent","comment":"Looks closed"}}""",
            request.jsonBody(),
        )
        Assert.assertEquals(18108L, response.id)
        Assert.assertEquals("user", response.origin)
    }

    @Test
    fun reportPlace_sendsOriginWithoutCommentWhenCommentBlank() = runTest {
        enqueueJson("""{"id":18109,"origin":"user"}""")

        api().reportPlace(placeId = 42, type = "verification", comment = "  ")

        val request = takeRequest()
        Assert.assertEquals(
            """{"place_id":42,"type":"verification","extra_fields":{"origin":"$userAgent"}}""",
            request.jsonBody(),
        )
    }

    @Test
    fun reportPlace_sendsPhotosAsBase64AndReadsTheirIds() = runTest {
        enqueueJson("""{"id":18110,"origin":"user","photo_ids":[3,4]}""")

        val response = api().reportPlace(
            placeId = 42,
            type = "verification",
            comment = null,
            photos = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5)),
        )

        val request = takeRequest()
        Assert.assertEquals(
            """{"place_id":42,"type":"verification","extra_fields":{"origin":"$userAgent"},"photos":[{"data_base64":"AQID"},{"data_base64":"BAU="}]}""",
            request.jsonBody(),
        )
        Assert.assertEquals(listOf(3L, 4L), response.photoIds)
    }

    @Test
    fun reportPlace_omitsPhotosAndDefaultsToNoIdsWhenNoneProvided() = runTest {
        enqueueJson("""{"id":18111,"origin":"user"}""")

        val response = api().reportPlace(placeId = 42, type = "verification", comment = null)

        val request = takeRequest()
        Assert.assertEquals(
            """{"place_id":42,"type":"verification","extra_fields":{"origin":"$userAgent"}}""",
            request.jsonBody(),
        )
        Assert.assertTrue(response.photoIds.isEmpty())
    }
}
