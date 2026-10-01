package org.btcmap.ui.map

/**
 * The map search field's actions, mirrored from the `map` toolbar menu: add a
 * place and open the settings. Null hides them.
 */
data class SearchActions(
    val onAddPlace: () -> Unit,
    val onSettings: () -> Unit,
)
