package org.btcmap.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.flow.filterIsInstance
import org.btcmap.ui.DEFAULT_NOTE_ICON
import org.btcmap.ui.LocalIconFont
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/** The zoom a location picker opens at: close enough to place a pin on a street. */
private const val LOCATION_PICKER_ZOOM = 16.0

/** Which stock pin the location picker previews at its centre. */
enum class LocationPickerPin { Place, Event, Note }

/**
 * The positioning map shared by the add-place, add-event and add-note screens:
 * pan and zoom to move the centre the pin marks. The pin is the stock pin the map
 * draws for the chosen [LocationPickerPin] — [noteIcon] picks the add-note pin's
 * glyph — built with the icon font the theme provides and the user's marker
 * colours, so the temporary overlay matches the map underneath.
 *
 * [onCenterChanged] fires whenever the camera comes to rest, with the new centre
 * in latitude/longitude.
 */
@Composable
fun LocationPickerMap(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    palette: MarkerPalette,
    onCenterChanged: (Double, Double) -> Unit,
    pin: LocationPickerPin = LocationPickerPin.Place,
    /** The glyph previewed when [pin] is [LocationPickerPin.Note]. */
    noteIcon: String = DEFAULT_NOTE_ICON,
) {
    val state = rememberMapState(
        baseStyle = if (styleJson != null) BaseStyle.Json(styleJson) else BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(target = Position(lon, lat), zoom = LOCATION_PICKER_ZOOM),
    ) { }

    LaunchedEffect(state, onCenterChanged) {
        state.events.filterIsInstance<MapEvent.CameraMoveEnded>().collect {
            state.cameraPosition?.target?.let { onCenterChanged(it.latitude, it.longitude) }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val iconFont = LocalIconFont.current
    val factory = remember(textMeasurer, iconFont, density, palette) {
        MarkerBitmapFactory(
            textMeasurer = textMeasurer,
            iconFont = iconFont,
            density = density,
            palette = palette,
        )
    }
    val pin = remember(factory, pin, noteIcon) {
        when (pin) {
            LocationPickerPin.Place -> factory.merchantPin()
            LocationPickerPin.Event -> factory.eventPin()
            LocationPickerPin.Note -> factory.notePin(noteIcon)
        }
    }
    // The pin's tip, not its centre, marks the position, so the bitmap is lifted
    // by half its height to sit on the map centre.
    val pinHeight = with(density) { pin.height.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        // The positioning map is a temporary overlay, not a place map, so the
        // MapLibre logo and the expanding attribution pill are dropped. Its
        // `overlay = {}` is what removes them; `MapUiOptions` does not control
        // the overlay.
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = state,
            overlay = {},
        )
        Image(
            bitmap = pin,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -(pinHeight / 2)),
        )
    }
}
