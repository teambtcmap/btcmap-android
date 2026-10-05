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
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.bundledStyleJsonFor
import java.time.LocalDateTime

/**
 * Hosts [AddEventScreen] inside the Android Views hierarchy. The app sets the
 * position, the style, the labels, the icon typeface, the marker colours and the
 * submit callback; this view holds no state of its own.
 */
class AddEventComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var lat: Double by mutableStateOf(0.0)

    var lon: Double by mutableStateOf(0.0)

    var styleUrl: String by mutableStateOf("")

    var labels: AddEventLabels? by mutableStateOf(null)

    /** The Material Symbols typeface the screen's pin and back arrow draw with. */
    var iconTypeface: Typeface? by mutableStateOf(null)

    /** The user's marker colours, so the pin matches the map's merchant markers. */
    var palette: MarkerPalette? by mutableStateOf(null)

    /** Optional pre-fill for a duplicate; blank/null for a brand new event. */
    var initialName: String by mutableStateOf("")

    var initialWebsite: String by mutableStateOf("")

    var initialStartsAt: LocalDateTime? by mutableStateOf(null)

    var initialEndsAt: LocalDateTime? by mutableStateOf(null)

    var submit: suspend (AddEventDraft) -> Unit = {}

    var onBack: () -> Unit = {}

    @Composable
    override fun Content() {
        val labels = labels ?: return
        val palette = palette ?: return
        // The screen needs a style before it can create the map.
        if (styleUrl.isEmpty()) return
        val styleJson = remember(styleUrl) { bundledStyleJsonFor(context, styleUrl) }
        val iconFont = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }

        AddEventScreen(
            lat = lat,
            lon = lon,
            styleUrl = styleUrl,
            styleJson = styleJson,
            labels = labels,
            iconFont = iconFont,
            palette = palette,
            submit = submit,
            onBack = onBack,
            initialName = initialName,
            initialWebsite = initialWebsite,
            initialStartsAt = initialStartsAt,
            initialEndsAt = initialEndsAt,
        )
    }
}
