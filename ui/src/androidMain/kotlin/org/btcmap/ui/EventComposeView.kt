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
import org.btcmap.db.table.event.Event
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.bundledStyleJsonFor

/**
 * Hosts [EventScreen] inside the Android Views hierarchy. The app sets the
 * event, the style and the palette colours; this view holds no state of its own.
 */
class EventComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var event: Event? by mutableStateOf(null)

    var geoJson: String by mutableStateOf("")

    var styleUrl: String by mutableStateOf("")

    var labels: EventScreenLabels? by mutableStateOf(null)

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
        val event = event ?: return
        // The screen sets the style and the target together; until it does,
        // creating the map with an empty style would leave it stuck on the
        // style background.
        if (styleUrl.isEmpty()) return
        val labels = labels ?: return

        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        val styleJson = remember(styleUrl) { bundledStyleJsonFor(context, styleUrl) }

        AppTheme {
            EventScreen(
                event = event,
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
                iconFont = fontFamily,
                usingOpenFreeMap = usingOpenFreeMap,
                labels = labels,
            )
        }
    }
}
