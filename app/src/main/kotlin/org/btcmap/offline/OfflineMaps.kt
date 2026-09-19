package org.btcmap.offline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.btcmap.util.rethrowIfCancellation
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Creates and tracks MapLibre offline regions, one per area, on top of the
 * offline manager's callback API.
 *
 * The manager owns its own database and can only be driven from the main thread,
 * so every public method switches to the main dispatcher. Downloads keep running
 * while the process lives; [refresh] re-attaches observers and resumes any pack
 * that was still incomplete when the app last exited.
 */
internal class OfflineMaps(context: Context) {

    private val appContext = context.applicationContext
    private val manager = OfflineManager.getInstance(appContext)
    private val regions = mutableMapOf<Long, OfflineRegion>()

    private val _states = MutableStateFlow<Map<Long, OfflineAreaState>>(emptyMap())
    val states: StateFlow<Map<Long, OfflineAreaState>> = _states.asStateFlow()

    init {
        // MapLibre inherited a hard tile count limit from Mapbox; lift it so a
        // city-sized pack is not silently truncated.
        runCatching { manager.setOfflineMapboxTileCountLimit(Long.MAX_VALUE) }
    }

    /** Rebuilds the state from MapLibre's database and resumes unfinished packs. */
    suspend fun refresh() = withContext(Dispatchers.Main.immediate) {
        val listed = try {
            awaitRegions()
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            return@withContext
        }

        regions.clear()
        val states = mutableMapOf<Long, OfflineAreaState>()
        for (region in listed) {
            val metadata = parseOfflineRegionMetadata(region.metadata) ?: continue
            regions[metadata.areaId] = region

            val status = try {
                awaitStatus(region)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                continue
            }

            if (status.isComplete) {
                states[metadata.areaId] = status.toCompleteState(metadata)
            } else {
                observe(region, metadata)
                region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                states[metadata.areaId] = status.toDownloadingState()
            }
        }
        _states.value = states
    }

    /**
     * Replaces any pack for [areaId] with a fresh download of [bounds] in
     * [styleUrl] up to [maxZoom].
     */
    suspend fun download(
        areaId: Long,
        areaName: String,
        bounds: OfflineBounds,
        styleUrl: String,
        maxZoom: Int,
    ) = withContext(Dispatchers.Main.immediate) {
        regions.remove(areaId)?.let { existing ->
            try {
                awaitDelete(existing)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
            }
        }

        val metadata = OfflineRegionMetadata(areaId, areaName, styleUrl, maxZoom)
        setState(areaId, OfflineAreaState.Downloading(completedBytes = 0L, progress = null))

        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl,
            bounds.toLatLngBounds(),
            OfflineRegionEstimates.MIN_ZOOM.toDouble(),
            maxZoom.toDouble(),
            appContext.resources.displayMetrics.density,
        )

        try {
            val region = awaitCreate(definition, metadata.toBytes())
            regions[areaId] = region
            observe(region, metadata)
            region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            setState(areaId, OfflineAreaState.Failed(t.message ?: ""))
        }
    }

    suspend fun delete(areaId: Long) = withContext(Dispatchers.Main.immediate) {
        // A region can be missing from the in-memory map if the initial refresh
        // failed; reload before giving up so delete still works.
        if (regions[areaId] == null) refresh()

        val region = regions.remove(areaId) ?: return@withContext
        try {
            awaitDelete(region)
            setState(areaId, OfflineAreaState.None)
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            setState(areaId, OfflineAreaState.Failed(t.message ?: ""))
        }
    }

    private fun observe(region: OfflineRegion, metadata: OfflineRegionMetadata) {
        region.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                setState(
                    metadata.areaId,
                    if (status.isComplete) {
                        status.toCompleteState(metadata)
                    } else {
                        status.toDownloadingState()
                    },
                )
            }

            override fun onError(error: OfflineRegionError) {
                setState(
                    metadata.areaId,
                    OfflineAreaState.Failed(error.message.ifBlank { error.reason }),
                )
            }

            override fun mapboxTileCountLimitExceeded(limit: Long) {
                setState(metadata.areaId, OfflineAreaState.Failed("Tile limit exceeded: $limit"))
            }
        })
    }

    private fun setState(areaId: Long, state: OfflineAreaState) {
        _states.value = _states.value + (areaId to state)
    }

    private suspend fun awaitRegions(): Array<OfflineRegion> =
        suspendCancellableCoroutine { continuation ->
            manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    if (continuation.isActive) {
                        continuation.resume(offlineRegions ?: emptyArray())
                    }
                }

                override fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }
                }
            })
        }

    private suspend fun awaitStatus(region: OfflineRegion): OfflineRegionStatus =
        suspendCancellableCoroutine { continuation ->
            region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                override fun onStatus(status: OfflineRegionStatus?) {
                    if (!continuation.isActive) return
                    if (status == null) {
                        continuation.resumeWithException(IllegalStateException("No status"))
                    } else {
                        continuation.resume(status)
                    }
                }

                override fun onError(error: String?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException(error ?: "No status"))
                    }
                }
            })
        }

    private suspend fun awaitCreate(
        definition: OfflineTilePyramidRegionDefinition,
        metadata: ByteArray,
    ): OfflineRegion = suspendCancellableCoroutine { continuation ->
        manager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    if (continuation.isActive) continuation.resume(offlineRegion)
                }

                override fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }
                }
            },
        )
    }

    private suspend fun awaitDelete(region: OfflineRegion) =
        suspendCancellableCoroutine { continuation ->
            region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(error: String) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException(error))
                    }
                }
            })
        }
}

private fun OfflineRegionStatus.toCompleteState(
    metadata: OfflineRegionMetadata,
): OfflineAreaState.Complete = OfflineAreaState.Complete(
    bytes = completedResourceSize,
    maxZoom = metadata.maxZoom,
    styleUrl = metadata.styleUrl,
)

private fun OfflineRegionStatus.toDownloadingState(): OfflineAreaState.Downloading =
    OfflineAreaState.Downloading(
        completedBytes = completedResourceSize,
        progress = if (isRequiredResourceCountPrecise && requiredResourceCount > 0) {
            completedResourceCount.toFloat() / requiredResourceCount.toFloat()
        } else {
            null
        },
    )

private fun OfflineBounds.toLatLngBounds(): LatLngBounds = LatLngBounds.from(
    latNorth = north,
    lonEast = east,
    latSouth = south,
    lonWest = west,
)
