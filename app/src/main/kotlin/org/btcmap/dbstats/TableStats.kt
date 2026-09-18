package org.btcmap.dbstats

/**
 * Row counts and sync cursor for one table.
 *
 * [visibleRowCount] and [deletedRowCount] are only reported for tables that
 * carry the sync tracking fields, i.e. an updated_at or deleted_at column.
 * Tables without a deleted_at column keep every row, so all of them count as
 * visible. They are null for tables with neither field, such as `user` or
 * `preference`. [maxUpdatedAt] is null when the table has no updated_at column
 * or has no rows. [futureRowCount] is only reported for the events table and
 * counts visible rows whose starts_at is still ahead.
 */
data class TableStats(
    val name: String,
    val rowCount: Long,
    val visibleRowCount: Long?,
    val deletedRowCount: Long?,
    val maxUpdatedAt: String?,
    val futureRowCount: Long?,
)
