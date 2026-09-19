package org.btcmap.offline

import org.junit.Assert
import org.junit.Test

class OfflineRegionMetadataTest {

    @Test
    fun roundTripsThroughBytes() {
        val metadata = OfflineRegionMetadata(
            areaId = 671L,
            areaName = "Phuket",
            styleUrl = "https://tiles.openfreemap.org/styles/liberty",
            maxZoom = 14,
        )

        Assert.assertEquals(metadata, parseOfflineRegionMetadata(metadata.toBytes()))
    }

    @Test
    fun foreignBytesAreIgnored() {
        Assert.assertNull(parseOfflineRegionMetadata(null))
        Assert.assertNull(parseOfflineRegionMetadata("exported".toByteArray()))
        Assert.assertNull(parseOfflineRegionMetadata("not json".toByteArray()))
        Assert.assertNull(parseOfflineRegionMetadata("[]".toByteArray()))
    }

    @Test
    fun incompletePayloadIsRejected() {
        Assert.assertNull(parseOfflineRegionMetadata("{}".toByteArray()))
        Assert.assertNull(
            parseOfflineRegionMetadata(
                """{"areaId":1,"areaName":"A","styleUrl":"https://example.com/style.json"}"""
                    .toByteArray(),
            ),
        )
        Assert.assertNull(
            parseOfflineRegionMetadata(
                """{"areaId":0,"areaName":"A","styleUrl":"https://example.com","maxZoom":10}"""
                    .toByteArray(),
            ),
        )
    }
}
