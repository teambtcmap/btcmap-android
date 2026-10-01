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
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.map.MapArea
import org.btcmap.ui.map.AreaChipPalette
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.SearchActions
import java.time.ZonedDateTime

/**
 * Hosts [MapScreen] inside the Android Views hierarchy (Phase 3 spike). The
 * fragment/activity sets the database and the state, which triggers
 * recomposition.
 */
class MapComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var database: Database? by mutableStateOf(null)

    var styleUrl: String by mutableStateOf("https://tiles.openfreemap.org/styles/liberty")

    var styleJson: String? by mutableStateOf(null)

    var apiUrl: String by mutableStateOf("https://api.btcmap.org")

    var initialLat: Double by mutableStateOf(0.0)

    var initialLon: Double by mutableStateOf(0.0)

    var initialZoom: Double by mutableStateOf(2.0)

    var minVerifiedAt: ZonedDateTime? by mutableStateOf(null)

    var markerBackgroundColor: Color by mutableStateOf(Color(0xFFF7931A))

    var markerIconColor: Color by mutableStateOf(Color.White)

    var boostedMarkerBackgroundColor: Color by mutableStateOf(Color(0xFF7B3FE4))

    var boostedMarkerIconColor: Color by mutableStateOf(Color.White)

    var markerBadgeBackgroundColor: Color by mutableStateOf(Color(0xFFE53935))

    var markerBadgeTextColor: Color by mutableStateOf(Color.White)

    var areaChipButtonColor: Color by mutableStateOf(Color(0xFF1B1B1B))

    var areaChipIconColor: Color by mutableStateOf(Color.White)

    var usingOpenFreeMap: Boolean by mutableStateOf(true)

    var iconTypeface: Typeface? by mutableStateOf(null)

    var placeSheetStrings: PlaceSheetStrings by mutableStateOf(
        PlaceSheetStrings(
            directions = "",
            share = "",
            viewOnBtcmap = "",
            viewOnOsm = "",
            editOnOsm = "",
            notVerified = "",
            verificationWarningTitle = "",
            verificationWarningOutdated = "",
            verificationWarningNotVerified = "",
            ok = "",
            companionWarning = { it },
            verify = "",
            report = "",
            boost = "",
            comments = { it.toString() },
            commentsTitle = { it.toString() },
            addComment = "",
            save = "",
            addPhoto = "",
        )
    )

    var photos: List<String> by mutableStateOf(emptyList())

    var bookmarked: Boolean by mutableStateOf(false)

    var onPlaceSelected: (Place) -> Unit by mutableStateOf({})

    var onPlaceAction: (Place, PlaceAction) -> Unit by mutableStateOf({ _, _ -> })

    var onEventSelected: (Event) -> Unit by mutableStateOf({})

    var onAreaSelected: (Long) -> Unit by mutableStateOf({})

    var formatDistance: (Double) -> String by mutableStateOf({ it.toString() })

    var searchActions: SearchActions? by mutableStateOf(null)

    var onAddPlace: ((Double, Double) -> Unit)? by mutableStateOf(null)

    var onOpenFeed: ((List<MapArea>) -> Unit)? by mutableStateOf(null)

    var openPlaceId: Long? by mutableStateOf(null)

    var reloadKey: Int by mutableStateOf(0)

    var onCameraIdle: ((Double, Double, Double) -> Unit)? by mutableStateOf(null)

    var onFeaturesDrawn: (() -> Unit)? by mutableStateOf(null)

    var placeSheet: Boolean by mutableStateOf(true)

    var showAttribution: Boolean by mutableStateOf(true)

    var openTarget: Pair<Double, Double>? by mutableStateOf(null)

    @Composable
    override fun Content() {
        val fontFamily = remember(iconTypeface) { iconTypeface?.let { FontFamily(it) } }
        val db = database ?: return
        MapScreen(
            db = db,
            styleUrl = styleUrl,
            styleJson = styleJson,
            initialLat = initialLat,
            initialLon = initialLon,
            initialZoom = initialZoom,
            minVerifiedAt = minVerifiedAt,
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
            areaChipPalette = AreaChipPalette(
                buttonBackground = areaChipButtonColor,
                buttonIcon = areaChipIconColor,
                badgeBackground = markerBadgeBackgroundColor,
                badgeText = markerBadgeTextColor,
            ),
            apiUrl = apiUrl,
            placeSheetStrings = placeSheetStrings,
            searchActions = searchActions,
            onAddPlace = onAddPlace,
            onOpenFeed = onOpenFeed,
            openPlaceId = openPlaceId,
            reloadKey = reloadKey,
            onCameraIdle = onCameraIdle,
            onFeaturesDrawn = onFeaturesDrawn,
            placeSheet = placeSheet,
            showAttribution = showAttribution,
            openTarget = openTarget,
            photos = photos,
            bookmarked = bookmarked,
            onPlaceSelected = onPlaceSelected,
            onPlaceAction = onPlaceAction,
            onSelectEvent = onEventSelected,
            onSelectArea = onAreaSelected,
            formatDistance = formatDistance,
        )
    }
}
