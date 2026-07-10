package org.btcmap.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.maplibre.android.maps.MapLibreMap
import java.util.concurrent.atomic.AtomicReference

class EventsCache(
    private val map: MapLibreMap,
    private val db: Database,
) : MapLibreMap.OnCameraIdleListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingQuery = AtomicReference<Job?>(null)
    private val seenIds: MutableSet<Long> = mutableSetOf()
    private val events: MutableSet<Event> = mutableSetOf()
    val geoJson = MutableStateFlow(events.toEventGeoJson())

    init {
        map.addOnCameraIdleListener(this)
    }

    override fun onCameraIdle() {
        val bounds = map.projection.visibleRegion.latLngBounds
        val expandedBounds = bounds.expand()

        pendingQuery.getAndSet(
            scope.launch {
                val (lonRange1, lonRange2) = expandedBounds.splitAtAntimeridian()
                val eventsInBounds = withContext(Dispatchers.IO) {
                    if (lonRange2 == null) {
                        db.event.selectByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            lonRange1.first,
                            lonRange1.second,
                        ).toHashSet()
                    } else {
                        val first = db.event.selectByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            lonRange1.first,
                            lonRange1.second,
                        )
                        val second = db.event.selectByBounds(
                            expandedBounds.latitudeSouth,
                            expandedBounds.latitudeNorth,
                            lonRange2.first,
                            lonRange2.second,
                        )
                        (first + second).toHashSet()
                    }
                }

                val newOnes = eventsInBounds.filter { it.id !in seenIds }
                if (newOnes.isEmpty()) return@launch

                seenIds.addAll(newOnes.map { it.id })
                events.addAll(newOnes)

                val next = withContext(Dispatchers.Default) { events.toEventGeoJson() }
                geoJson.value = next
            }
        )?.cancel()
    }

    fun destroy() {
        map.removeOnCameraIdleListener(this)
        pendingQuery.getAndSet(null)?.cancel()
        scope.cancel()
    }
}
