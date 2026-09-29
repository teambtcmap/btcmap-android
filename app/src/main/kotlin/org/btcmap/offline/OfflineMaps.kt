package org.btcmap.offline

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * so all work runs on the main dispatcher. [download] and [delete] hand off to
 * the app-scoped [scope] and return immediately, so the sequence that activates
 * a pack survives the screen that started it. Downloads keep running while the
 * process lives; [refresh] re-attaches observers and resumes any pack that was
 * still incomplete when the app last exited.
 */
internal class OfflineMaps(context: Context) {

    private val appContext = context.applicationContext
    private val manager = OfflineManager.getInstance(appContext)
    private val regions = mutableMapOf<Long, OfflineRegion>()

    /**
     * App-scoped so the create/observe/activate sequence a download needs runs
     * to completion even when the screen that started it is closed meanwhile.
     * MapLibre delivers its callbacks on the main thread, so the scope does too.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Serializes every MapLibre offline operation. They all suspend across the
     * manager's callbacks, so without this a download could interleave with
     * [refresh] (e.g. started before the initial refresh populated [regions]),
     * see no existing pack, and create a duplicate region for the same area that
     * [refresh] then silently drops — an orphaned pack stuck on disk forever.
     */
    private val mutex = Mutex()

    /**
     * Whether [regions] reflects MapLibre's database. It stays false until a
     * [refreshRegions] succeeds, so a download can tell "no pack for this area"
     * apart from "the region list was never loaded" and reload before acting.
     */
    private var regionsLoaded = false

    private val _states = MutableStateFlow<Map<Long, OfflineAreaState>>(emptyMap())
    val states: StateFlow<Map<Long, OfflineAreaState>> = _states.asStateFlow()

    /**
     * Test-only: publishes [states] as the tracked pack states without touching
     * MapLibre's database or the network, so the offline UI can be driven to a
     * given state in a test.
     */
    internal fun setStatesForTesting(states: Map<Long, OfflineAreaState>) {
        _states.value = states
    }

    init {
        // MapLibre inherited a hard tile count limit from Mapbox; lift it so a
        // city-sized pack is not silently truncated.
        runCatching { manager.setOfflineMapboxTileCountLimit(Long.MAX_VALUE) }
    }

    /** Rebuilds the state from MapLibre's database and resumes unfinished packs. */
    suspend fun refresh() = mutex.withLock { refreshRegions() }

    private suspend fun refreshRegions() = withContext(Dispatchers.Main.immediate) {
        val listed = try {
            awaitRegions()
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            return@withContext
        }

        regions.clear()
        val present = mutableSetOf<Long>()
        for (region in listed) {
            val metadata = parseOfflineRegionMetadata(region.metadata) ?: continue
            regions[metadata.areaId] = region
            present += metadata.areaId

            val status = try {
                awaitStatus(region)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                continue
            }

            if (!status.isComplete) {
                observe(region, metadata)
                region.setDownloadState(OfflineRegion.STATE_ACTIVE)
            }
            // Publish each area as it is read rather than replacing the whole
            // map at the end, so an observer update that lands while this is
            // running (a pack can finish during the loop) is not overwritten.
            setState(metadata.areaId, status.toAreaState(metadata))
        }

        // Drop the states of areas whose pack is gone, keeping everything else.
        _states.value = _states.value.filterKeys { it in present }
        regionsLoaded = true
    }

    /**
     * Replaces any pack for [areaId] with a fresh download of [bounds] in
     * [styleUrl] up to [maxZoom]. Returns immediately; the work runs on the
     * app-scoped [scope] so closing the area screen cannot cancel it before
     * MapLibre activates the new region.
     */
    fun download(
        areaId: Long,
        areaName: String,
        bounds: OfflineBounds,
        styleUrl: String,
        maxZoom: Int,
    ) {
        scope.launch { performDownload(areaId, areaName, bounds, styleUrl, maxZoom) }
    }

    private suspend fun performDownload(
        areaId: Long,
        areaName: String,
        bounds: OfflineBounds,
        styleUrl: String,
        maxZoom: Int,
    ) = mutex.withLock {
        withContext(Dispatchers.Main.immediate) {
            // A download can be started before the initial refresh populated
            // [regions], or after that refresh failed; reload first so an
            // existing pack for this area is replaced instead of duplicated.
            if (!regionsLoaded) refreshRegions()

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
    }

    /**
     * Deletes the pack for [areaId], if any. Returns immediately; the work runs
     * on the app-scoped [scope].
     */
    fun delete(areaId: Long) {
        scope.launch { performDelete(areaId) }
    }

    private suspend fun performDelete(areaId: Long) = mutex.withLock {
        withContext(Dispatchers.Main.immediate) {
            // A region can be missing from the in-memory map if the initial
            // refresh failed; reload before giving up so delete still works.
            if (regions[areaId] == null) refreshRegions()

            val region = regions.remove(areaId)
            if (region == null) {
                // Nothing on disk for this area (for example, creating the pack
                // failed before a region existed); clear the state so a failed
                // pack's Delete action actually dismisses the panel.
                setState(areaId, OfflineAreaState.None)
                return@withContext
            }
            try {
                awaitDelete(region)
                setState(areaId, OfflineAreaState.None)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                setState(areaId, OfflineAreaState.Failed(t.message ?: ""))
            }
        }
    }

    private fun observe(region: OfflineRegion, metadata: OfflineRegionMetadata) {
        region.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                setState(metadata.areaId, status.toAreaState(metadata))
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

private fun OfflineRegionStatus.toAreaState(
    metadata: OfflineRegionMetadata,
): OfflineAreaState = offlineAreaState(
    metadata = metadata,
    isComplete = isComplete,
    completedResourceSize = completedResourceSize,
    isRequiredResourceCountPrecise = isRequiredResourceCountPrecise,
    requiredResourceCount = requiredResourceCount,
    completedResourceCount = completedResourceCount,
)

private fun OfflineBounds.toLatLngBounds(): LatLngBounds = LatLngBounds.from(
    latNorth = north,
    lonEast = east,
    latSouth = south,
    lonWest = west,
)
