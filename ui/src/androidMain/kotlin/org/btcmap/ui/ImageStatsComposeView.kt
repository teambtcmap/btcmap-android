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
import coil3.ImageLoader
import org.btcmap.imagestats.ImageStatsLabels

/**
 * Hosts [ImageStatsPage] inside the Android Views hierarchy. The app sets the
 * image loader, the home directory and the labels; [refreshKey] re-reads the
 * snapshot.
 */
class ImageStatsComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var imageLoader: ImageLoader? by mutableStateOf(null)

    var homeDirectory: String by mutableStateOf("")

    var labels: ImageStatsLabels? by mutableStateOf(null)

    var refreshKey: Int by mutableStateOf(0)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onError: (Throwable) -> Unit = {}

    @Composable
    override fun Content() {
        val imageLoader = imageLoader ?: return
        val labels = labels ?: return
        AppTheme(iconFont = iconTypeface?.let { FontFamily(it) }) {
            ImageStatsPage(
                imageLoader = imageLoader,
                homeDirectory = homeDirectory,
                labels = labels,
                refreshKey = refreshKey,
                onError = onError,
            )
        }
    }
}
