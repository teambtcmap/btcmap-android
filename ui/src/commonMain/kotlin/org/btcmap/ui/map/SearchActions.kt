package org.btcmap.ui.map

/**
 * The map search field's actions, mirrored from the `map` toolbar menu: add a
 * location and open the settings. A host offers only the ones it has; a null
 * callback's icon is not shown.
 *
 * [onAddPlace] and [onAddEvent] are the two kinds of location the add-location
 * action can create. When both are offered the icon opens a chooser; when only
 * one is, tapping it acts directly.
 */
data class SearchActions(
    val onAddPlace: (() -> Unit)? = null,
    val onAddEvent: (() -> Unit)? = null,
    val onSettings: (() -> Unit)? = null,
)

/** The labels of the add-location chooser the search field opens. */
data class AddLocationLabels(
    val addPlace: String,
    val addEvent: String,
)
