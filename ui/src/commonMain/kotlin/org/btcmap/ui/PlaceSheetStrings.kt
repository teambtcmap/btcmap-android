package org.btcmap.ui

/**
 * The strings the place sheet needs, resolved by the host so `:ui` stays
 * resource-free.
 */
data class PlaceSheetStrings(
    val notVerified: String,
    val companionWarning: (String) -> String,
    val verify: String,
    val report: String,
    val boost: String,
    val comments: (Long) -> String,
    val commentsTitle: (Long) -> String,
    val addComment: String,
)
