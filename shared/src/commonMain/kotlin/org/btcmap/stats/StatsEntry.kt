package org.btcmap.stats

/**
 * A label and its value inside a [StatsSection] card.
 *
 * [icon] is an optional Material Symbols ligature drawn before the label, for a
 * row that benefits from its own glyph (for example a per-platform breakdown).
 *
 * [details] are extra label/value rows hidden behind the row: when non-empty the
 * row becomes tappable and reveals them (indented) beneath it, for a compact
 * summary that still has the full breakdown one tap away.
 */
data class StatsEntry(
    val label: String,
    val value: String,
    val icon: String? = null,
    val details: List<StatsEntry> = emptyList(),
)
