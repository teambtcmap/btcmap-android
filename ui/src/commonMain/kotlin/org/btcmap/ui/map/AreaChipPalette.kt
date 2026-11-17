package org.btcmap.ui.map

import androidx.compose.ui.graphics.Color

/**
 * The colors the map's controls use, from the app's button and badge roles: the
 * area chips, the marker filter and the round action buttons all draw from the
 * button background and icon, and the chips from the badge pair. The marker
 * filter marks a visible kind with a full-strength icon and fades a hidden one.
 */
data class AreaChipPalette(
    val buttonBackground: Color,
    val buttonIcon: Color,
    /** The button accent, currently unused after the marker filter moved to opacity. */
    val buttonAccent: Color,
    val badgeBackground: Color,
    val badgeText: Color,
)
