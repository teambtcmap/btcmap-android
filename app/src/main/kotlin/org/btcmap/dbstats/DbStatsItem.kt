package org.btcmap.dbstats

/** One entry on the database stats screen. */
sealed interface DbStatsItem {

    /** Stable identity for the list diff. */
    val key: String

    data class Header(val title: String) : DbStatsItem {
        override val key: String
            get() = "header:$title"
    }

    data class Entry(val title: String, val detail: String) : DbStatsItem {
        override val key: String
            get() = "entry:$title"
    }
}
