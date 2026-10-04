package org.btcmap.stats

/**
 * A label and its value inside a [StatsSection] card.
 *
 * [icon] is an optional Material Symbols ligature drawn before the label, for a
 * row that benefits from its own glyph (for example a per-platform breakdown).
 */
data class StatsEntry(
    val label: String,
    val value: String,
    val icon: String? = null,
)
