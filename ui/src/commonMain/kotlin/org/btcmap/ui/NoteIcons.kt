package org.btcmap.ui

/**
 * The icon a note is created with when its author does not pick one. It matches
 * the API's own default and is the glyph the map falls back to for any icon it
 * cannot render.
 */
const val DEFAULT_NOTE_ICON = "notes"

/**
 * The note icons the add-note screen offers, as Material Symbols ligature names.
 *
 * The API stores the icon verbatim and accepts any non-empty string, so a note
 * created by another client may carry a name outside this list. Such a note is
 * still drawn — [org.btcmap.ui.map.MarkerBitmapFactory] falls back to a generic
 * glyph it cannot draw — but only these names are offered here.
 */
val NOTE_ICONS: List<String> = listOf(
    DEFAULT_NOTE_ICON,
    "star",
    "favorite",
    "local_atm",
    "local_cafe",
    "restaurant",
    "shopping_cart",
    "warning",
    "flag",
    "bolt",
    "home",
    "work",
    "menu_book",
    "auto_stories",
    "local_library",
    "flight",
)
