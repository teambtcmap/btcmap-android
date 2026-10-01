package org.btcmap.stats

/**
 * One card on a stats screen: a titled group of label/value rows.
 *
 * [key] is the card's identity for the list diff and must be unique within a
 * screen. It is deliberately separate from [title], which is only what is
 * displayed, so two cards can share a title without the diff conflating them.
 *
 * [icon] is a Material Symbols ligature name rendered with the bundled icon
 * typeface, or null for a card without a leading icon.
 */
data class StatsSection(
    val key: String,
    val title: String,
    val entries: List<StatsEntry>,
    val icon: String? = null,
)
