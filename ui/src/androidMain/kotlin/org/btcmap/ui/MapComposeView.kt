package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import org.btcmap.db.table.place.Marker

/**
 * Hosts [MapScreen] inside the Android Views hierarchy (Phase 3 spike). The
 * fragment/activity sets the state, which triggers recomposition.
 */
class MapComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var styleUrl: String by mutableStateOf("https://tiles.openfreemap.org/styles/liberty")

    var markers: List<Marker> by mutableStateOf(emptyList())

    var initialLat: Double by mutableStateOf(0.0)

    var initialLon: Double by mutableStateOf(0.0)

    var initialZoom: Double by mutableStateOf(2.0)

    var markerBackgroundColor: Color by mutableStateOf(Color(0xFFF7931A))

    var markerIconColor: Color by mutableStateOf(Color.White)

    var boostedMarkerBackgroundColor: Color by mutableStateOf(Color(0xFF7B3FE4))

    var usingOpenFreeMap: Boolean by mutableStateOf(true)

    @Composable
    override fun Content() {
        MapScreen(
            styleUrl = styleUrl,
            markers = markers,
            initialLat = initialLat,
            initialLon = initialLon,
            initialZoom = initialZoom,
            markerBackgroundColor = markerBackgroundColor,
            markerIconColor = markerIconColor,
            boostedMarkerBackgroundColor = boostedMarkerBackgroundColor,
            usingOpenFreeMap = usingOpenFreeMap,
        )
    }
}
