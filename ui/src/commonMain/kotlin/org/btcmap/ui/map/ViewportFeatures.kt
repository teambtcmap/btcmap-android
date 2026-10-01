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
 * ported from `org.btcmap.map.ViewportCache`. The features accumulate in a
 * [FeatureStore] so panning back over an already-loaded area does not refetch
 * it. The query runs when the camera comes to rest, not on every frame.
 */
@Composable
fun <T : Any> rememberViewportFeatures(
    state: MapState,
    idOf: (T) -> Long,
    toGeoJson: (Set<T>) -> String,
    fetch: suspend (ViewportBounds) -> List<T>,
): ViewportFeatures<T> {
    val latestToGeoJson by rememberUpdatedState(toGeoJson)
    val latestFetch by rememberUpdatedState(fetch)

    val store = remember { FeatureStore(idOf) }
    var snapshot by remember { mutableStateOf<Set<T>>(emptySet()) }
    var geoJson by remember { mutableStateOf(EMPTY_GEOJSON) }

    LaunchedEffect(state, state.style.loadState) {
        if (state.style.loadState !is StyleLoadState.Ready) return@LaunchedEffect

        suspend fun load() {
            val viewport = state.viewport ?: return
            val bounds = ViewportBounds.expand(viewport.visibleBounds)
            val fetched = withContext(Dispatchers.Default) { latestFetch(bounds) }
            val merged = store.merge(fetched) ?: return
            val json = withContext(Dispatchers.Default) { latestToGeoJson(merged) }
            snapshot = merged
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
