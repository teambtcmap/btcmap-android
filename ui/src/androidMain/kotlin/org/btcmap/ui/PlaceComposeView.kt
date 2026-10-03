package org.btcmap.ui

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.btcmap.comment.CommentsAdapterItem
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.place.toMarker
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.PlacePreviewMap
import org.btcmap.ui.map.bundledStyleJsonFor

/**
 * Hosts the shared [PlaceDetails] body of the standalone place screen inside the
 * Android Views hierarchy, under the Views top bar the screen owns. The app sets
 * the place, its comments, photos and style; the preview map is drawn here so
 * the host does not need its own.
 */
class PlaceComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var place: Place? by mutableStateOf(null)

    var comments: List<CommentsAdapterItem> by mutableStateOf(emptyList())

    var photos: List<PlacePhoto> by mutableStateOf(emptyList())

    var bookmarked: Boolean by mutableStateOf(false)

    var strings: PlaceSheetStrings? by mutableStateOf(null)

    /** The style of the preview map; the map is omitted until it is set. */
    var styleUrl: String by mutableStateOf("")

    var usingOpenFreeMap: Boolean by mutableStateOf(true)

    var markerBackgroundColor: Color by mutableStateOf(Color(0xFFF7931A))

    var markerIconColor: Color by mutableStateOf(Color.White)

    var boostedMarkerBackgroundColor: Color by mutableStateOf(Color(0xFF7B3FE4))

    var boostedMarkerIconColor: Color by mutableStateOf(Color.White)

    var markerBadgeBackgroundColor: Color by mutableStateOf(Color(0xFFE53935))

    var markerBadgeTextColor: Color by mutableStateOf(Color.White)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var onAction: (PlaceAction) -> Unit = {}

    /** Deletes a gallery photo the signed-in user may remove; null hides the action. */
    var onDeletePhoto: ((PlacePhoto) -> Unit)? by mutableStateOf(null)

    var onPreviewMapClick: (() -> Unit)? by mutableStateOf(null)

    @Composable
    override fun Content() {
        val place = place ?: return
        val strings = strings ?: return

        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        val styleJson = remember(styleUrl) { bundledStyleJsonFor(context, styleUrl) }
        val previewMapClick = onPreviewMapClick

        AppTheme(iconFont = fontFamily) {
            PlaceDetails(
                place = place,
                comments = comments,
                photos = photos,
                bookmarked = bookmarked,
                strings = strings,
                onAction = onAction,
                onDeletePhoto = onDeletePhoto,
                showHeader = false,
                previewMap = if (styleUrl.isEmpty()) {
                    null
                } else {
                    {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(PREVIEW_MAP_HEIGHT)
                                .then(
                                    if (previewMapClick != null) {
                                        Modifier.clickable(onClick = previewMapClick)
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            PlacePreviewMap(
                                lat = place.lat,
                                lon = place.lon,
                                marker = place.toMarker(),
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
                                usingOpenFreeMap = usingOpenFreeMap,
                                iconFont = fontFamily,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
            )
        }
    }
}

/** The height of the place's preview map, matching the Views layout. */
private val PREVIEW_MAP_HEIGHT = 192.dp
