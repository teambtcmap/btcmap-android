package org.btcmap.ui

/**
 * The strings the place sheet needs, resolved by the host so `:ui` stays
 * resource-free.
 */
data class PlaceSheetStrings(
    val directions: String,
    val share: String,
    val viewOnBtcmap: String,
    val viewOnOsm: String,
    val editOnOsm: String,
    val notVerified: String,
    val verificationWarningTitle: String,
    val verificationWarningOutdated: String,
    val verificationWarningNotVerified: String,
    val ok: String,
    val companionWarning: (String) -> String,
    val verify: String,
    val report: String,
    val boost: String,
    val comments: (Long) -> String,
    val commentsTitle: (Long) -> String,
    val addComment: String,
)
