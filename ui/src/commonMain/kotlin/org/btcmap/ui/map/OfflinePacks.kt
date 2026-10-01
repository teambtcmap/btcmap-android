package org.btcmap.ui.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.btcmap.offline.OfflineAreaState
import org.btcmap.offline.OfflineBounds
import org.btcmap.offline.OfflineRegionEstimates
import org.btcmap.offline.OfflineRegionMetadata
import org.btcmap.offline.offlineAreaState
import org.btcmap.offline.parseOfflineRegionMetadata
import org.btcmap.offline.toBytes
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.offline.DownloadProgress
import org.maplibre.compose.offline.DownloadStatus
import org.maplibre.compose.offline.OfflinePack
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.spatialk.geojson.BoundingBox

/**
 * Creates and tracks MapLibre offline regions, one per area, ported from
 * `org.btcmap.offline.OfflineMaps` to the Compose offline manager. The manager
 * keeps its own database and publishes the packs as a flow, so this only has to
 * map each pack's progress onto [OfflineAreaState].
 */
class OfflinePacks(private val pixelRatio: Float) {

    private val manager = DefaultMapRuntime.instance.offlineManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The observation running per area, and the pack it belongs to. */
    private val observations = mutableMapOf<Long, Pair<Job, OfflinePack>>()

    /**
     * Areas whose download has been asked for but whose pack may not be in
     * [org.maplibre.compose.offline.OfflineManager.packs] yet, so a packs
     * emission does not drop the state the UI is already showing.
     */
    private val pending = mutableSetOf<Long>()

    private val _states = MutableStateFlow<Map<Long, OfflineAreaState>>(emptyMap())
    val states: StateFlow<Map<Long, OfflineAreaState>> = _states.asStateFlow()

    init {
        scope.launch {
            manager.packs.collect(::syncPacks)
        }
    }

    fun download(
        areaId: Long,
        areaName: String,
        bounds: OfflineBounds,
        styleUrl: String,
        maxZoom: Int,
    ) {
        pending += areaId
        setState(areaId, OfflineAreaState.Downloading(completedBytes = 0L, progress = null))
        scope.launch {
            val metadata = OfflineRegionMetadata(areaId, areaName, styleUrl, maxZoom)

            // Replace any pack the area already has; creating a second region for
            // the same area would orphan the first one on disk.
            existingPack(areaId)?.let { existing ->
                runCatching { manager.delete(existing) }
            }

            runCatching {
                val pack = manager.create(
                    definition = OfflinePackDefinition.TilePyramid(
                        styleUrl = styleUrl,
                        bounds = bounds.toBoundingBox(),
                        pixelRatio = pixelRatio,
                        minZoom = OfflineRegionEstimates.MIN_ZOOM,
                        maxZoom = maxZoom,
                    ),
                    metadata = metadata.toBytes(),
                )
                observePack(areaId, pack, metadata)
                manager.resume(pack)
            }.onFailure { error ->
                pending -= areaId
                setState(areaId, OfflineAreaState.Failed(error.message ?: ""))
            }
        }
    }

    fun delete(areaId: Long) {
        scope.launch {
            pending -= areaId
            observations.remove(areaId)?.first?.cancel()
            val pack = existingPack(areaId)
            if (pack == null) {
                setState(areaId, OfflineAreaState.None)
                return@launch
            }
            runCatching { manager.delete(pack) }
                .onSuccess { setState(areaId, OfflineAreaState.None) }
                .onFailure { setState(areaId, OfflineAreaState.Failed(it.message ?: "")) }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun existingPack(areaId: Long): OfflinePack? =
        manager.packs.value.firstOrNull { pack ->
            parseOfflineRegionMetadata(pack.metadata.value)?.areaId == areaId
        }

    private fun syncPacks(packs: Set<OfflinePack>) {
        val byArea = packs.mapNotNull { pack ->
            parseOfflineRegionMetadata(pack.metadata.value)?.let { it.areaId to (pack to it) }
        }.toMap()

        (observations.keys - byArea.keys).forEach { areaId ->
            observations.remove(areaId)?.first?.cancel()
        }

        byArea.forEach { (areaId, pair) ->
            val (pack, metadata) = pair
            if (observations[areaId]?.second !== pack) {
                observations.remove(areaId)?.first?.cancel()
                observePack(areaId, pack, metadata)
            }
        }

        val present = byArea.keys + pending
        _states.value = _states.value.filterKeys { it in present }
    }

    private fun observePack(areaId: Long, pack: OfflinePack, metadata: OfflineRegionMetadata) {
        observations[areaId] = scope.launch {
            pack.downloadProgress.collect { progress ->
                setState(areaId, progress.toAreaState(metadata))
                if (progress is DownloadProgress.Healthy &&
                    progress.status == DownloadStatus.Complete
                ) {
                    pending -= areaId
                }
            }
        } to pack
    }

    private fun setState(areaId: Long, state: OfflineAreaState) {
        _states.value = _states.value + (areaId to state)
    }
}

private fun DownloadProgress.toAreaState(metadata: OfflineRegionMetadata): OfflineAreaState =
    when (this) {
        is DownloadProgress.Healthy -> offlineAreaState(
            metadata = metadata,
            isComplete = status == DownloadStatus.Complete,
            completedResourceSize = completedResourceBytes,
            isRequiredResourceCountPrecise = isRequiredResourceCountPrecise,
            requiredResourceCount = requiredResourceCount,
            completedResourceCount = completedResourceCount,
        )

        is DownloadProgress.Error -> OfflineAreaState.Failed(message.ifBlank { reason })

        is DownloadProgress.TileLimitExceeded ->
            OfflineAreaState.Failed("Tile limit exceeded: $limit")

        DownloadProgress.Unknown -> OfflineAreaState.Downloading(completedBytes = 0L, progress = null)
    }

private fun OfflineBounds.toBoundingBox(): BoundingBox = BoundingBox(
    west = west,
    south = south,
    east = east,
    north = north,
)
