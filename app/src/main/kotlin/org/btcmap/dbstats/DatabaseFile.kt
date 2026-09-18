package org.btcmap.dbstats

/** A database file in the app's databases directory. */
data class DatabaseFile(
    val name: String,
    val sizeBytes: Long,
    val isCurrent: Boolean,
)
