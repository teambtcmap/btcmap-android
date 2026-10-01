package org.btcmap.ui.map

import androidx.compose.ui.graphics.Color

/**
 * The marker colors the app supplies to the shared map, mirroring the color
 * roles `MapSetupController` is given on Android.
 */
data class MarkerPalette(
    val markerBackground: Color,
    val markerIcon: Color,
    val boostedMarkerBackground: Color,
    val boostedMarkerIcon: Color,
    val badgeBackground: Color,
    val badgeText: Color,
)
