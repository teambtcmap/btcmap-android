package org.btcmap.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import java.util.concurrent.atomic.AtomicReference

abstract class ViewportCache<T : Any>(
    private val map: MapLibreMap,
) : MapLibreMap.OnCameraIdleListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingQuery = AtomicReference<Job?>(null)
    private val seenIds: MutableSet<Long> = mutableSetOf()
    private val items: MutableSet<T> = mutableSetOf()
    val geoJson: MutableStateFlow<String>

    init {
        geoJson = MutableStateFlow(emptySet<T>().toGeoJson())
        map.addOnCameraIdleListener(this)
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

                val newOnes = fetched.filter { idOf(it) !in seenIds }
                if (newOnes.isEmpty()) return@launch

                seenIds.addAll(newOnes.map { idOf(it) })
                items.addAll(newOnes)

                val next = withContext(Dispatchers.Default) { items.toGeoJson() }
                geoJson.value = next
            }
        )?.cancel()
    }

    protected abstract suspend fun fetch(bounds: LatLngBounds): Set<T>

    protected abstract fun idOf(item: T): Long

    protected abstract fun Set<T>.toGeoJson(): String

    fun destroy() {
        map.removeOnCameraIdleListener(this)
        pendingQuery.getAndSet(null)?.cancel()
        scope.cancel()
    }
}
