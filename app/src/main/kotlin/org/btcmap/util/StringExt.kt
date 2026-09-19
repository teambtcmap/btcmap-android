package org.btcmap.util

/**
 * Shortens a path that lives under [home] by replacing that prefix with `~`,
 * so `/data/user/0/app/cache/foo` reads as `~/cache/foo`. Returns the path
 * unchanged when it is not under [home].
 */
fun String.abbreviateHome(home: String): String {
    val prefix = home.trimEnd('/')
    if (prefix.isEmpty()) return this
    return when {
        this == prefix -> "~"
        startsWith("$prefix/") -> "~" + removePrefix(prefix)
        else -> this
    }
}
