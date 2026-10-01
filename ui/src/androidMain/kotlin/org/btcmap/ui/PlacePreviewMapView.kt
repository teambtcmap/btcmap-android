package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import org.btcmap.db.table.place.Marker
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.PlacePreviewMap
import org.btcmap.ui.map.bundledStyleJsonFor

/**
 * Hosts the shared [PlacePreviewMap] inside the Android Views hierarchy,
 * replacing the `MapView` the place screen used to embed.
 */
class PlacePreviewMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var lat: Double by mutableStateOf(0.0)

    var lon: Double by mutableStateOf(0.0)

    var marker: Marker? by mutableStateOf(null)

    var styleUrl: String by mutableStateOf("")

    var markerBackgroundColor: Color by mutableStateOf(Color(0xFFF7931A))

    var markerIconColor: Color by mutableStateOf(Color.White)

    var boostedMarkerBackgroundColor: Color by mutableStateOf(Color(0xFF7B3FE4))

    var boostedMarkerIconColor: Color by mutableStateOf(Color.White)

    var markerBadgeBackgroundColor: Color by mutableStateOf(Color(0xFFE53935))

    var markerBadgeTextColor: Color by mutableStateOf(Color.White)

    var usingOpenFreeMap: Boolean by mutableStateOf(true)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onClick: (() -> Unit)? by mutableStateOf(null)

    @Composable
    override fun Content() {
        // The place screen sets the style and the target together; until it
        // does, there is nothing to draw and creating the map with an empty
        // style would leave it stuck on the style background.
        if (styleUrl.isEmpty()) return

        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        val styleJson = remember(styleUrl) { bundledStyleJsonFor(context, styleUrl) }
        val click = onClick

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (click != null) Modifier.clickable(onClick = click) else Modifier),
        ) {
            PlacePreviewMap(
                lat = lat,
                lon = lon,
                marker = marker,
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
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
