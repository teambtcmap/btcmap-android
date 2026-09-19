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
