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
import org.btcmap.map.MapArea
import org.btcmap.ui.map.AreaChipPalette
import org.btcmap.ui.map.AreaChips

/**
 * Hosts the shared [AreaChips] inside the Android Views hierarchy, replacing
 * the map screen's `AreasAdapter` and its item layout.
 */
class AreaChipsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var areas: List<MapArea> by mutableStateOf(emptyList())

    var apiUrl: String by mutableStateOf("")

    var buttonBackgroundColor: Color by mutableStateOf(Color.Transparent)

    var buttonIconColor: Color by mutableStateOf(Color.White)

    var badgeBackgroundColor: Color by mutableStateOf(Color.Transparent)

    var badgeTextColor: Color by mutableStateOf(Color.White)

    var onAreaClick: (MapArea) -> Unit by mutableStateOf({})

    var iconTypeface: Typeface? by mutableStateOf(null)

    @Composable
    override fun Content() {
        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        AppTheme(iconFont = fontFamily) {
            AreaChips(
                areas = areas,
                apiUrl = apiUrl,
                palette = AreaChipPalette(
                    buttonBackground = buttonBackgroundColor,
                    buttonIcon = buttonIconColor,
                    badgeBackground = badgeBackgroundColor,
                    badgeText = badgeTextColor,
                ),
                onAreaClick = onAreaClick,
            )
        }
    }
}
