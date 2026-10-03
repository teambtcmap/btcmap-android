package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import org.btcmap.settings.Settings

/**
 * Hosts [ColorsPage] inside the Android Views hierarchy. The app sets [settings]
 * and [labels]; this view holds no state of its own.
 */
class ColorSettingsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var settings: Settings? by mutableStateOf(null)

    var labels: ColorsPageLabels? by mutableStateOf(null)

    @Composable
    override fun Content() {
        val settings = this.settings ?: return
        val labels = this.labels ?: return
        AppTheme {
            ColorsPage(settings = settings, labels = labels)
        }
    }
}
