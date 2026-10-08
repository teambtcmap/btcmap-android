package org.btcmap.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** The style image id the plain pin is registered under. */
const val MARKER_PIN_IMAGE_ID = "btcmap-marker"

/** The style image id a personal note's pin is registered under. */
const val NOTE_MARKER_IMAGE_NAME = "btcmap-note-marker"

/**
 * The background of a personal note's pin: a fixed amber, so notes stand out
 * from the merchant markers whatever marker colour the user has picked.
 */
val NOTE_MARKER_COLOR = Color(0xFFFFB300)

/** The glyph colour on a note's pin. */
val NOTE_MARKER_ICON_COLOR = Color.White

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
