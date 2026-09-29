package org.btcmap.offline

/** The offline-map state of a single area. */
internal sealed interface OfflineAreaState {

    /** No pack exists for the area. */
    data object None : OfflineAreaState

    data class Downloading(
        val completedBytes: Long,
        /** Null until MapLibre knows how many resources the pack needs. */
        val progress: Float?,
    ) : OfflineAreaState

    data class Complete(
        val bytes: Long,
        val maxZoom: Int,
        val styleUrl: String,
    ) : OfflineAreaState

    data class Failed(val message: String) : OfflineAreaState
}

/**
 * Maps MapLibre's status fields onto the UI state.
 *
 * Kept free of MapLibre types so the mapping — which decides whether a pack
 * reads as complete and how progress is reported — is unit testable.
 */
internal fun offlineAreaState(
    metadata: OfflineRegionMetadata,
    isComplete: Boolean,
    completedResourceSize: Long,
    isRequiredResourceCountPrecise: Boolean,
    requiredResourceCount: Long,
    completedResourceCount: Long,
): OfflineAreaState = if (isComplete) {
    OfflineAreaState.Complete(
        bytes = completedResourceSize,
        maxZoom = metadata.maxZoom,
        styleUrl = metadata.styleUrl,
    )
} else {
    OfflineAreaState.Downloading(
        completedBytes = completedResourceSize,
        // MapLibre reports the required count as imprecise until it has parsed
        // the style, so an early zero would otherwise read as 0% forever.
        progress = if (isRequiredResourceCountPrecise && requiredResourceCount > 0) {
            completedResourceCount.toFloat() / requiredResourceCount.toFloat()
        } else {
            null
        },
    )
}
