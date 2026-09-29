package org.btcmap.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.concurrent.atomic.AtomicReference

/**
 * Loads the features in the viewport from the local cache and keeps [source]
 * up to date as the camera moves.
 *
 * The cache owns the source and the coroutine that feeds it, so its lifetime is
 * exactly the cache's: [destroy] cancels both together. A caller that swaps the
 * cache (for example on a filter change) therefore cannot leave a collector
 * behind.
 */
abstract class ViewportCache<T : Any>(
    private val map: MapLibreMap,
    private val source: GeoJsonSource,
    /**
     * Called once, after the first non-empty snapshot has been handed to the
     * map, so a caller can tell that the layer now has real content to draw
     * (for example to report the app as fully drawn).
     */
    private val onFirstDataDrawn: (() -> Unit)? = null,
) : MapLibreMap.OnCameraIdleListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val pendingQuery = AtomicReference<Job?>(null)
    private val store = FeatureStore<T>({ idOf(it) })
    private val snapshots = MutableStateFlow<Set<T>>(emptySet())
    private var firstDataReported = false

    init {
        map.addOnCameraIdleListener(this)

        scope.launch {
            snapshots.collectLatest { snapshot ->
                // Serialising a large viewport is not main-thread work.
                val geoJson = withContext(Dispatchers.Default) { snapshot.toGeoJson() }
                onSnapshot(snapshot)
                source.setGeoJson(geoJson)

                if (snapshot.isNotEmpty() && !firstDataReported) {
                    firstDataReported = true
                    onFirstDataDrawn?.invoke()
                }
            }
        }

        loadInBounds(map.projection.visibleRegion.latLngBounds.expand())
    }

    override fun onCameraIdle() {
        loadInBounds(map.projection.visibleRegion.latLngBounds.expand())
    }

    private fun loadInBounds(expandedBounds: LatLngBounds) {
        pendingQuery.getAndSet(
            scope.launch {
                val fetched = withContext(Dispatchers.IO) {
                    fetch(expandedBounds)
                }

                val snapshot = store.merge(fetched) ?: return@launch
                if (snapshot == snapshots.value) return@launch
                snapshots.value = snapshot
            }
        )?.cancel()
    }

    protected abstract suspend fun fetch(bounds: LatLngBounds): Set<T>

    protected abstract fun idOf(item: T): Long

    protected abstract fun Set<T>.toGeoJson(): String

    /**
     * Called on the main thread with each snapshot just before it is drawn, so
     * a subclass can prepare anything the GeoJSON references (such as marker
     * images).
     */
    protected open suspend fun onSnapshot(snapshot: Set<T>) = Unit

    fun refresh() {
        loadInBounds(map.projection.visibleRegion.latLngBounds.expand())
    }

    fun forceRebuild() {
        store.clear()
        loadInBounds(map.projection.visibleRegion.latLngBounds.expand())
    }

    fun destroy() {
        map.removeOnCameraIdleListener(this)
        pendingQuery.getAndSet(null)?.cancel()
        scope.cancel()
    }
}
