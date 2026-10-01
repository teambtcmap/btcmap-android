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
import org.btcmap.db.Database
import org.btcmap.ui.map.MarkerPalette
import java.time.ZonedDateTime

/**
 * Hosts [MapScreen] inside the Android Views hierarchy (Phase 3 spike). The
 * fragment/activity sets the database and the state, which triggers
 * recomposition.
 */
class MapComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var database: Database? by mutableStateOf(null)

    var styleUrl: String by mutableStateOf("https://tiles.openfreemap.org/styles/liberty")

    var initialLat: Double by mutableStateOf(0.0)

    var initialLon: Double by mutableStateOf(0.0)

    var initialZoom: Double by mutableStateOf(2.0)

    var minVerifiedAt: ZonedDateTime? by mutableStateOf(null)

    var markerBackgroundColor: Color by mutableStateOf(Color(0xFFF7931A))

    var markerIconColor: Color by mutableStateOf(Color.White)

    var boostedMarkerBackgroundColor: Color by mutableStateOf(Color(0xFF7B3FE4))

    var boostedMarkerIconColor: Color by mutableStateOf(Color.White)

    var markerBadgeBackgroundColor: Color by mutableStateOf(Color(0xFFE53935))

    var markerBadgeTextColor: Color by mutableStateOf(Color.White)

    var usingOpenFreeMap: Boolean by mutableStateOf(true)

    var iconTypeface: Typeface? by mutableStateOf(null)

    @Composable
    override fun Content() {
        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        val db = database ?: return
        MapScreen(
            db = db,
            styleUrl = styleUrl,
            initialLat = initialLat,
            initialLon = initialLon,
            initialZoom = initialZoom,
            minVerifiedAt = minVerifiedAt,
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
        )
    }
}
