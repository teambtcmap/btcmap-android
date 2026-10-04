package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import org.btcmap.api.Dashboard

/**
 * Hosts the shared [InfraDashboardScreen] inside the Android Views hierarchy:
 * the app supplies the suspend [load], and the screen owns the load state
 * machine.
 */
class InfraDashboardComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var load: (suspend () -> Dashboard)? by mutableStateOf(null)

    var refreshKey: Int by mutableStateOf(0)

    /** Reports whether a load is in flight, so the fragment can disable refresh. */
    var onLoadingChange: (Boolean) -> Unit = {}

    var iconTypeface: Typeface? by mutableStateOf(null)

    @Composable
    override fun Content() {
        val load = load ?: return

        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            InfraDashboardScreen(
                load = load,
                refreshKey = refreshKey,
                onLoadingChange = onLoadingChange,
            )
        }
    }
}
