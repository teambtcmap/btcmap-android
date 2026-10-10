package org.btcmap.dbstats

import org.btcmap.db.table.area.TABLE as AREA_TABLE
import org.btcmap.db.table.comment.TABLE as COMMENT_TABLE
import org.btcmap.db.table.event.TABLE as EVENT_TABLE
import org.btcmap.db.table.note.TABLE as NOTE_TABLE
import org.btcmap.db.table.place.TABLE as PLACE_TABLE
import org.btcmap.db.table.preference.TABLE as PREF_TABLE
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.platform.formatInteger

/** Display order of the database table cards, rather than alphabetical. */
val DB_TABLE_ORDER = listOf(
    PLACE_TABLE,
    COMMENT_TABLE,
    AREA_TABLE,
    EVENT_TABLE,
    PREF_TABLE,
)

/** The per-host labels the database stats cards are built with. */
data class DbStatsLabels(
    val database: String,
    val file: String,
    val version: String,
    val size: String,
    val tables: String,
    val bundles: String,
    val location: String,
    val visibleRows: String,
    val deletedRows: String,
    val futureRows: String,
    val rows: String,
    val newestUpdate: String,
)

/**
 * Builds the database, tables and bundles cards shared by the Android and
 * desktop stats screens. Every table is one row of a single "tables" card, and
 * every bundled snapshot one row of a single "bundles" card: a row's value is a
 * compact `visible/deleted` summary and tapping it reveals the labelled
 * breakdown (location, size, max updated at, …). Both cards list their rows in
 * the same fixed table order.
 *
 * The sync card is not built here: the two hosts order it differently, so each
 * adds its own. [formatBytes] renders a byte size, since the hosts use
 * different formatters.
 */
fun dbStatsSections(
    file: DatabaseFile?,
    version: Int,
    tables: List<TableStats>,
    bundles: Map<String, BundleStats>,
    labels: DbStatsLabels,
    formatBytes: (Long) -> String,
): List<StatsSection> {
    val sections = mutableListOf<StatsSection>()

    sections.add(
        StatsSection(
            key = "database",
            title = labels.database,
            icon = "database",
            entries = buildList {
                file?.let { add(StatsEntry(labels.file, it.name)) }
                add(StatsEntry(labels.version, version.toString()))
                file?.let { add(StatsEntry(labels.size, formatBytes(it.sizeBytes))) }
            },
        ),
    )

    val orderedTables = tables.sortedBy { table ->
        DB_TABLE_ORDER.indexOf(table.name).let { if (it == -1) DB_TABLE_ORDER.size else it }
    }

    if (orderedTables.isNotEmpty()) {
        sections.add(
            StatsSection(
                key = "tables",
                title = labels.tables,
                icon = "table",
                entries = orderedTables.map { table ->
                    StatsEntry(
                        label = table.name,
                        value = tableSummary(table),
                        icon = tableIcon(table.name),
                        details = tableEntries(table, labels),
                    )
                },
            ),
        )
    }

    val bundleEntries = bundles.entries
        .sortedBy { (table, _) ->
            DB_TABLE_ORDER.indexOf(table).let { if (it == -1) DB_TABLE_ORDER.size else it }
        }
        .map { (table, bundle) ->
            StatsEntry(
                label = table,
                value = bundleSummary(bundle),
                icon = tableIcon(table),
                details = bundleDetails(bundle, labels, formatBytes),
            )
        }

    if (bundleEntries.isNotEmpty()) {
        sections.add(
            StatsSection(
                key = "bundles",
                title = labels.bundles,
                icon = "inventory_2",
                entries = bundleEntries,
            ),
        )
    }

    return sections
}

/** The compact row summary for [bundle]: `visible/deleted` records. */
private fun bundleSummary(bundle: BundleStats): String =
    "${formatInteger(bundle.visibleCount)}/${formatInteger(bundle.deletedCount)}"

/**
 * The compact row summary for [table]: `rows/visible/deleted`, or just the row
 * count for a table with no sync tracking (whose rows are all visible and none
 * are tombstoned, so the extra numbers would repeat the total).
 */
private fun tableSummary(table: TableStats): String {
    val rows = formatInteger(table.rowCount)
    val visible = table.visibleRowCount
    val deleted = table.deletedRowCount
    return if (visible != null && deleted != null) {
        "$rows/${formatInteger(visible)}/${formatInteger(deleted)}"
    } else {
        rows
    }
}

/**
 * The Material Symbols glyph for a table row: a row glyph that names what the
 * table holds, falling back to a generic table for a table this build does not
 * know (a future one, or a test fixture).
 */
private fun tableIcon(name: String): String = when (name) {
    PLACE_TABLE -> "storefront"
    COMMENT_TABLE -> "comment"
    AREA_TABLE -> "map"
    EVENT_TABLE -> "event"
    PREF_TABLE -> "settings"
    NOTE_TABLE -> "edit_note"
    "user" -> "person"
    else -> "table"
}

private fun bundleDetails(
    bundle: BundleStats,
    labels: DbStatsLabels,
    formatBytes: (Long) -> String,
): List<StatsEntry> {
    return buildList {
        add(StatsEntry(labels.location, bundle.location))
        add(StatsEntry(labels.size, formatBytes(bundle.sizeBytes)))
        add(StatsEntry(labels.visibleRows, formatInteger(bundle.visibleCount)))
        add(StatsEntry(labels.deletedRows, formatInteger(bundle.deletedCount)))
        bundle.maxUpdatedAt?.let { add(StatsEntry(labels.newestUpdate, it)) }
    }
}

private fun tableEntries(table: TableStats, labels: DbStatsLabels): List<StatsEntry> {
    return buildList {
        add(StatsEntry(labels.rows, formatInteger(table.rowCount)))
        table.visibleRowCount?.let { add(StatsEntry(labels.visibleRows, formatInteger(it))) }
        table.deletedRowCount?.let { add(StatsEntry(labels.deletedRows, formatInteger(it))) }
        table.futureRowCount?.let { add(StatsEntry(labels.futureRows, formatInteger(it))) }
        table.maxUpdatedAt?.let { add(StatsEntry(labels.newestUpdate, it)) }
    }
}
