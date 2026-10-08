package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.withContext
import org.btcmap.map.EMPTY_GEOJSON
import org.btcmap.map.FeatureStore
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.StyleLoadState

/** The features currently loaded for the viewport, and their GeoJSON. */
class ViewportFeatures<T>(val snapshot: Set<T>, val geoJson: String)

/**
 * Keeps a GeoJSON string up to date with the features in the map's viewport,
 * ported from the app's Viewport Views cache. The features accumulate in a
 * [FeatureStore] so panning back over an already-loaded area does not refetch
 * it. The query runs when the camera comes to rest, not on every frame.
 */
@Composable
fun <T : Any> rememberViewportFeatures(
    state: MapState,
    /**
     * Bumped by the host when the synced data behind these features changed, so
     * the viewport is queried again instead of being left stale until the next
     * camera move.
     */
    reloadKey: Int = 0,
    idOf: (T) -> Long,
    /** Latitude and longitude of a feature, to keep only the ones near the map. */
    locationOf: (T) -> Pair<Double, Double>,
    toGeoJson: (Set<T>) -> String,
    fetch: suspend (ViewportBounds) -> List<T>,
): ViewportFeatures<T> {
    val latestToGeoJson by rememberUpdatedState(toGeoJson)
    val latestFetch by rememberUpdatedState(fetch)
    val latestLocationOf by rememberUpdatedState(locationOf)

    val store = remember { FeatureStore(idOf) }
    var snapshot by remember { mutableStateOf<Set<T>>(emptySet()) }
    var geoJson by remember { mutableStateOf(EMPTY_GEOJSON) }

    LaunchedEffect(state, state.style.loadState, reloadKey) {
        if (state.style.loadState !is StyleLoadState.Ready) return@LaunchedEffect

        // A reload means the synced data behind these features changed, and a
        // row may have been deleted (a tombstone). The store only ever merges
        // fetched rows, so drop it and rebuild the viewport from the database;
        // otherwise a deleted feature would linger in the store and stay drawn
        // even though its row is gone.
        store.clear()

        suspend fun load() {
            val viewport = state.viewport ?: return
            val bounds = ViewportBounds.expand(viewport.visibleBounds)
            val fetched = withContext(Dispatchers.Default) { latestFetch(bounds) }
            // The store keeps everything seen so panning back does not refetch,
            // but only the features around the current viewport are handed to the
            // map: an ever-growing source is re-parsed and re-tiled on every
            // camera move, and a session that has panned around would otherwise
            // keep paying for features that are nowhere near the screen.
            store.merge(fetched)
            val near = store.snapshot().filterTo(mutableListOf()) { item ->
                val (lat, lon) = latestLocationOf(item)
                bounds.contains(lat, lon)
            }.toSet()
            if (near == snapshot) return
            val json = withContext(Dispatchers.Default) { latestToGeoJson(near) }
            snapshot = near
            geoJson = json
        }

        load()

        // The camera reports a move end repeatedly while a gesture is still
        // settling, so wait for a quiet moment before querying.
        state.events.filterIsInstance<MapEvent.CameraMoveEnded>().collectLatest {
            delay(CAMERA_SETTLE_MS)
            load()
        }
    }

    return ViewportFeatures(snapshot, geoJson)
}

internal const val CAMERA_SETTLE_MS = 200L
