package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import org.btcmap.api.Api
import org.btcmap.api.getPendingEvents
import org.btcmap.api.setEventStatus
import org.btcmap.ui.map.EventMiniMap
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.bundledStyleJsonFor

/**
 * Hosts [EventReviewScreen] inside the Android Views hierarchy. The app sets the
 * API, the style, the labels, the icon typeface and the marker colours; this view
 * holds no state of its own.
 */
class EventReviewComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var api: Api? by mutableStateOf(null)

    var styleUrl: String by mutableStateOf("")

    var labels: EventReviewLabels? by mutableStateOf(null)

    var title: String by mutableStateOf("")

    /** The Material Symbols typeface the screen's icon draws with. */
    var iconTypeface: Typeface? by mutableStateOf(null)

    /** The user's marker colours, so the previews match the map's markers. */
    var palette: MarkerPalette? by mutableStateOf(null)

    /** Opens a submitted event's website, supplied by the host. */
    var onOpenUrl: (String) -> Unit = {}

    var onBack: () -> Unit = {}

    @Composable
    override fun Content() {
        val api = api ?: return
        val labels = labels ?: return
        val palette = palette ?: return
        // The screen needs a style before it can create the map previews.
        if (styleUrl.isEmpty()) return
        val styleJson = remember(styleUrl) { bundledStyleJsonFor(context, styleUrl) }
        val iconFont = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }

        EventReviewScreen(
            labels = labels,
            load = {
                api.getPendingEvents().map { event ->
                    PendingEventUi(
                        id = event.id,
                        lat = event.lat,
                        lon = event.lon,
                        name = event.name,
                        website = event.website?.toString().orEmpty(),
                        startsAt = event.startsAt,
                        endsAt = event.endsAt,
                    )
                }
            },
            approve = { api.setEventStatus(it.id, "live") },
            reject = { api.setEventStatus(it.id, "rejected") },
            onOpenUrl = onOpenUrl,
            onBack = onBack,
            title = title,
            iconFont = iconFont,
            map = { event, modifier ->
                EventMiniMap(
                    lat = event.lat,
                    lon = event.lon,
                    styleUrl = styleUrl,
                    styleJson = styleJson,
                    palette = palette,
                    modifier = modifier,
                )
            },
        )
    }
}
