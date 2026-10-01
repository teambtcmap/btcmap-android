package org.btcmap.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import org.btcmap.db.Database
import org.btcmap.map.MapArea
import org.btcmap.map.MapAreasController
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.StyleLoadState

/**
 * The areas containing the map centre, looked up locally as the camera settles,
 * driving [MapAreasController].
 */
@Composable
fun rememberMapAreas(state: MapState, db: Database, reloadKey: Int = 0): List<MapArea> {
    val controller = remember(db) { MapAreasController(db) }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }

    val areas by controller.areas.collectAsState()
    val loadState = state.style.loadState

    LaunchedEffect(state, controller, loadState, reloadKey) {
        if (loadState !is StyleLoadState.Ready) return@LaunchedEffect

        fun load() {
            val camera = state.cameraPosition ?: return
            controller.load(camera.target.latitude, camera.target.longitude)
        }

        load()
        state.events.filterIsInstance<MapEvent.CameraMoveEnded>().collectLatest {
            delay(CAMERA_SETTLE_MS)
            load()
        }
    }

    return areas
}
