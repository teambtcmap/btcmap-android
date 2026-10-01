package org.btcmap.api

/**
 * A saved place or area as the `/users` endpoints return it: an id and the
 * entity's base name. Kept separate from the database's row type so a wire
 * format change does not reach the storage layer.
 */
data class SavedItem(
    val id: Long,
    val name: String,
)
