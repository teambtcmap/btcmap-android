package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class PlaceImageApiTest : ApiTestBase() {
    @Test
    fun getPlaceImages_parsesMetadata() = runTest {
        enqueueJson(
            """
            [
                {
                    "id": 3,
                    "place_id": 42,
                    "type": "report",
                    "width": 1024,
                    "height": 768,
                    "size_bytes": 184320,
                    "created_at": "2026-10-01T04:22:53.706Z"
                }
            ]
            """.trimIndent()
        )

        val images = api().getPlaceImages(placeId = 42)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/places/42/images", request.url.encodedPath)

        val image = images.single()
        Assert.assertEquals(3L, image.id)
        Assert.assertEquals(42L, image.placeId)
        Assert.assertEquals("report", image.type)
        Assert.assertEquals(1024, image.width)
        Assert.assertEquals(768, image.height)
        Assert.assertEquals(184320L, image.sizeBytes)
        Assert.assertEquals("2026-10-01T04:22:53.706Z", image.createdAt)
    }

    @Test
    fun getPlaceImages_returnsEmptyListForAPlaceWithoutImages() = runTest {
        enqueueJson("[]")

        val images = api().getPlaceImages(placeId = 42)

        Assert.assertTrue(images.isEmpty())
    }

    @Test
    fun placeImageUrl_buildsPublicImageUrlWithDimensions() {
        val client = api()

        Assert.assertEquals(
            "${server.url("/")}v4/places/42/images/3",
            client.placeImageUrl(placeId = 42, imageId = 3),
        )
        Assert.assertEquals(
            "${server.url("/")}v4/places/42/images/3?w=320&h=320",
            client.placeImageUrl(placeId = 42, imageId = 3, width = 320, height = 320),
        )
    }
}
