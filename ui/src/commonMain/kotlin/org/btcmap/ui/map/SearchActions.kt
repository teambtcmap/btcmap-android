package org.btcmap.ui.map

/**
 * The map search field's actions, mirrored from the `map` toolbar menu: add a
 * place and open the settings. A host offers only the ones it has; a null
 * callback's icon is not shown.
 */
data class SearchActions(
    val onAddPlace: (() -> Unit)? = null,
    val onSettings: (() -> Unit)? = null,
)
