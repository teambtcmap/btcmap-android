package org.btcmap.ui.map

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** The style image id the plain pin is registered under. */
const val MARKER_PIN_IMAGE_ID = "btcmap-marker"

/**
 * The style image id a personal note's pin is registered under, one image per
 * note icon: [noteMarkerImageName] appends the icon name.
 */
const val NOTE_MARKER_IMAGE_PREFIX = "btcmap-note-marker-"

/** The style image id a note's pin carrying [icon] is registered under. */
fun noteMarkerImageName(icon: String): String = "$NOTE_MARKER_IMAGE_PREFIX$icon"

/**
 * Opacity of an outdated marker's glyph, matching the alpha the Views marker
 * builder gave it.
 */
const val OUTDATED_ICON_ALPHA = 0.6f

/**
 * How far a count badge is nudged in from its button's top-end corner, so it
 * hugs the button rather than hanging off it. Shared by the area chip badges and
 * the review queue badge so they sit identically.
 */
val MAP_BADGE_OFFSET = DpOffset(x = (-4).dp, y = 4.dp)
