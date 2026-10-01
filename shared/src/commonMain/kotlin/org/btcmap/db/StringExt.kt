package org.btcmap.db

/**
 * Escapes the LIKE wildcards (`%`, `_`) and the escape character itself so a
 * user-supplied search term is matched literally by a `LIKE ... ESCAPE '\'`
 * clause instead of being interpreted as a pattern. Without this, a query for
 * `50%` would also match `500` and `a_b` would match `acb`.
 */
internal fun String.escapeLikePattern(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
