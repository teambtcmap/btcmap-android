package org.btcmap.search

/**
 * Mirrors the server's ranking, evaluated across every name the entity is known
 * by (its base name and any translations): an exact match ranks 0, a prefix 1,
 * a substring 2, and the best rank across all the names wins. Returns null when
 * no name contains [query], so a row a broader SQL pre-filter returned for an
 * unrelated reason is dropped. Everything local matches on the name, so there
 * is no lower "matched another tag" rank.
 */
internal fun nameMatchRank(names: List<String>, query: String): Int? {
    var best: Int? = null
    for (name in names) {
        if (!name.contains(query, ignoreCase = true)) continue
        val rank = when {
            name.equals(query, ignoreCase = true) -> 0
            name.startsWith(query, ignoreCase = true) -> 1
            else -> 2
        }
        best = best?.let { minOf(it, rank) } ?: rank
    }
    return best
}
