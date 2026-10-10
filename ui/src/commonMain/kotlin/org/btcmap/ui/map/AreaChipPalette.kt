package org.btcmap.ui.map

import androidx.compose.ui.graphics.Color

/**
 * The colors the map's controls use, from the app's button and badge roles: the
 * area chips, the marker filter and the round action buttons all draw from the
 * button background and icon, the chips from the badge pair, and the selected
 * marker filter from the button accent too.
 */
data class AreaChipPalette(
    val buttonBackground: Color,
    val buttonIcon: Color,
    /** Tints the selected marker filter icon. */
    val buttonAccent: Color,
    val badgeBackground: Color,
    val badgeText: Color,
)
