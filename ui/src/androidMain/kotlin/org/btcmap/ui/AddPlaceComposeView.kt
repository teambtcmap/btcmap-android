package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import org.btcmap.ui.map.bundledStyleJsonFor

/**
 * Hosts [AddPlaceScreen] inside the Android Views hierarchy. The app sets the
 * position, the style, the labels and the submit callback; this view holds no
 * state of its own.
 */
class AddPlaceComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var lat: Double by mutableStateOf(0.0)

    var lon: Double by mutableStateOf(0.0)

    var styleUrl: String by mutableStateOf("")

    var labels: AddPlaceLabels? by mutableStateOf(null)

    var submit: suspend (AddPlaceDraft) -> Unit = {}

    var onBack: () -> Unit = {}

    @Composable
    override fun Content() {
        val labels = labels ?: return
        // The screen needs a style before it can create the map.
        if (styleUrl.isEmpty()) return
        val styleJson = remember(styleUrl) { bundledStyleJsonFor(context, styleUrl) }

        AppTheme {
            AddPlaceScreen(
                lat = lat,
                lon = lon,
                styleUrl = styleUrl,
                styleJson = styleJson,
                labels = labels,
                submit = submit,
                onBack = onBack,
            )
        }
    }
}
