package org.btcmap.offline

import org.junit.Assert
import org.junit.Test

class OfflineAreaStateTest {

    private val metadata = OfflineRegionMetadata(
        areaId = 7L,
        areaName = "Phuket",
        styleUrl = "https://example.com/style.json",
        maxZoom = 14,
    )

    @Test
    fun completeStatusBecomesCompleteWithMetadata() {
        val state = offlineAreaState(
            metadata = metadata,
            isComplete = true,
            completedResourceSize = 1234L,
            isRequiredResourceCountPrecise = true,
            requiredResourceCount = 100L,
            completedResourceCount = 100L,
        )

        Assert.assertEquals(
            OfflineAreaState.Complete(bytes = 1234L, maxZoom = 14, styleUrl = metadata.styleUrl),
            state,
        )
    }

    @Test
    fun incompletePreciseStatusReportsFractionalProgress() {
        val state = offlineAreaState(
            metadata = metadata,
            isComplete = false,
            completedResourceSize = 512L,
            isRequiredResourceCountPrecise = true,
            requiredResourceCount = 100L,
            completedResourceCount = 25L,
        )

        Assert.assertEquals(
            OfflineAreaState.Downloading(completedBytes = 512L, progress = 0.25f),
            state,
        )
    }

    @Test
    fun impreciseRequiredCountHasNoProgress() {
        val state = offlineAreaState(
            metadata = metadata,
            isComplete = false,
            completedResourceSize = 512L,
            isRequiredResourceCountPrecise = false,
            requiredResourceCount = 100L,
            completedResourceCount = 25L,
        )

        Assert.assertEquals(
            OfflineAreaState.Downloading(completedBytes = 512L, progress = null),
            state,
        )
    }

    @Test
    fun zeroRequiredCountHasNoProgress() {
        val state = offlineAreaState(
            metadata = metadata,
            isComplete = false,
            completedResourceSize = 0L,
            isRequiredResourceCountPrecise = true,
            requiredResourceCount = 0L,
            completedResourceCount = 0L,
        )

        Assert.assertEquals(
            OfflineAreaState.Downloading(completedBytes = 0L, progress = null),
            state,
        )
    }
}
