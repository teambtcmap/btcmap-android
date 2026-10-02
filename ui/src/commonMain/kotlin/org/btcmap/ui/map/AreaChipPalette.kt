package org.btcmap.ui.map

import androidx.compose.ui.graphics.Color

/**
 * The colors the map's controls use, from the app's button and badge roles: the
 * area chips, the marker filter and the round action buttons all draw from the
 * button background, icon and border, and the chips from the badge pair too.
 */
data class AreaChipPalette(
    val buttonBackground: Color,
    val buttonIcon: Color,
    /** Rings the selected marker filter button, as the Views button did. */
    val buttonBorder: Color,
    val badgeBackground: Color,
    val badgeText: Color,
)
