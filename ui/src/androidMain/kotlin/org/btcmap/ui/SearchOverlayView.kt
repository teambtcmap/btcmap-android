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
import org.btcmap.search.SearchAdapterItem
import org.btcmap.ui.map.SearchActions
import org.btcmap.ui.map.SearchOverlay

/**
 * Hosts the shared [SearchOverlay] inside the Android Views hierarchy, replacing
 * the map screen's `SearchBar`/`SearchView` and its results adapter.
 */
class SearchOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var query: String by mutableStateOf("")

    var onQueryChange: (String) -> Unit by mutableStateOf({})

    var results: List<SearchAdapterItem> by mutableStateOf(emptyList())

    var onResultClick: (SearchAdapterItem) -> Unit by mutableStateOf({})

    var placeholder: String by mutableStateOf("Search")

    var onAddPlace: (() -> Unit)? by mutableStateOf(null)

    var onSettings: (() -> Unit)? by mutableStateOf(null)

    var iconTypeface: Typeface? by mutableStateOf(null)

    @Composable
    override fun Content() {
        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        val addPlace = onAddPlace
        val settings = onSettings

        AppTheme(iconFont = fontFamily) {
            SearchOverlay(
                query = query,
                onQueryChange = onQueryChange,
                results = results,
                onResultClick = onResultClick,
                placeholder = placeholder,
                actions = if (addPlace != null || settings != null) {
                    SearchActions(onAddPlace = addPlace, onSettings = settings)
                } else {
                    null
                },
            )
        }
    }
}
