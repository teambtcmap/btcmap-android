package org.btcmap.dbstats

import org.btcmap.db.table.area.TABLE as AREA_TABLE
import org.btcmap.db.table.comment.TABLE as COMMENT_TABLE
import org.btcmap.db.table.event.TABLE as EVENT_TABLE
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

/**
 * The per-host labels the database stats cards are built with. Card titles name
 * a table or a bundle, so those are functions.
 */
data class DbStatsLabels(
    val database: String,
    val file: String,
    val version: String,
    val size: String,
    val table: (name: String) -> String,
    val bundle: (name: String) -> String,
    val location: String,
    val visibleRows: String,
    val deletedRows: String,
    val futureRows: String,
    val rows: String,
    val newestUpdate: String,
)

/**
 * Builds the database, per-table and bundle cards shared by the Android and
 * desktop stats screens. A bundled snapshot seeds its table, so its card sits
 * right behind it.
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

    tables
        .sortedBy { table ->
            DB_TABLE_ORDER.indexOf(table.name).let { if (it == -1) DB_TABLE_ORDER.size else it }
        }
        .forEach { table ->
            sections.add(
                StatsSection(
                    key = "table:${table.name}",
                    title = labels.table(table.name),
                    icon = "table",
                    entries = tableEntries(table, labels),
                ),
            )
            bundles[table.name]?.let { sections.add(bundleSection(table.name, it, labels, formatBytes)) }
        }

    return sections
}

private fun bundleSection(
    table: String,
    bundle: BundleStats,
    labels: DbStatsLabels,
    formatBytes: (Long) -> String,
): StatsSection {
    return StatsSection(
        key = "bundle:$table",
        title = labels.bundle(table),
        icon = "inventory_2",
        entries = buildList {
            add(StatsEntry(labels.location, bundle.location))
            add(StatsEntry(labels.size, formatBytes(bundle.sizeBytes)))
            add(StatsEntry(labels.visibleRows, formatInteger(bundle.visibleCount)))
            add(StatsEntry(labels.deletedRows, formatInteger(bundle.deletedCount)))
            bundle.maxUpdatedAt?.let { add(StatsEntry(labels.newestUpdate, it)) }
        },
    )
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
