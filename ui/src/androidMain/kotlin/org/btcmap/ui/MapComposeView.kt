package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView

/**
 * Hosts [MapScreen] inside the Android Views hierarchy (Phase 3 spike).
 */
class MapComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    @Composable
    override fun Content() {
        MapScreen()
    }
}
