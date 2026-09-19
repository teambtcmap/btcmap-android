package org.btcmap.stats

/**
 * One card on a stats screen: a titled group of label/value rows.
 *
 * [icon] is a Material Symbols ligature name rendered with the bundled icon
 * typeface, or null for a card without a leading icon.
 */
data class StatsSection(
    val title: String,
    val entries: List<StatsEntry>,
    val icon: String? = null,
) {

    /** Stable identity for the list diff. */
    val key: String
        get() = "section:$title"
}
