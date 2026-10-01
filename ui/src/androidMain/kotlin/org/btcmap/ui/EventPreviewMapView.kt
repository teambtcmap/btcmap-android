package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import org.btcmap.ui.map.EventPreviewMap
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.bundledStyleJsonFor
import org.maplibre.compose.map.MapState

/**
 * Hosts the shared [EventPreviewMap] inside the Android Views hierarchy,
 * replacing the `MapView` the event screen used to embed.
 */
class EventPreviewMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var lat: Double by mutableStateOf(0.0)

    var lon: Double by mutableStateOf(0.0)

    var geoJson: String by mutableStateOf("")

    var styleUrl: String by mutableStateOf("")

    var markerBackgroundColor: Color by mutableStateOf(Color(0xFFF7931A))

    var markerIconColor: Color by mutableStateOf(Color.White)

    var boostedMarkerBackgroundColor: Color by mutableStateOf(Color(0xFF7B3FE4))

    var boostedMarkerIconColor: Color by mutableStateOf(Color.White)

    var markerBadgeBackgroundColor: Color by mutableStateOf(Color(0xFFE53935))

    var markerBadgeTextColor: Color by mutableStateOf(Color.White)

    var usingOpenFreeMap: Boolean by mutableStateOf(true)

    var iconTypeface: Typeface? by mutableStateOf(null)

    /** Set from the map's state, so the zoom buttons can drive the camera. */
    private var mapState: MapState? = null

    fun zoomIn() = zoomBy(1.0)

    fun zoomOut() = zoomBy(-1.0)

    private fun zoomBy(delta: Double) {
        val state = mapState ?: return
        val camera = state.cameraPosition
        state.setCameraPosition(camera.copy(zoom = camera.zoom + delta))
    }

    @Composable
    override fun Content() {
        // The event screen sets the style and the target together; until it
        // does, creating the map with an empty style would leave it stuck on
        // the style background.
        if (styleUrl.isEmpty()) return

        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        val styleJson = remember(styleUrl) { bundledStyleJsonFor(context, styleUrl) }

        EventPreviewMap(
            lat = lat,
            lon = lon,
            geoJson = geoJson,
            styleUrl = styleUrl,
            styleJson = styleJson,
            palette = MarkerPalette(
                markerBackground = markerBackgroundColor,
                markerIcon = markerIconColor,
                boostedMarkerBackground = boostedMarkerBackgroundColor,
                boostedMarkerIcon = boostedMarkerIconColor,
                badgeBackground = markerBadgeBackgroundColor,
                badgeText = markerBadgeTextColor,
            ),
            usingOpenFreeMap = usingOpenFreeMap,
            iconFont = fontFamily,
            onState = { mapState = it },
        )
    }
}
