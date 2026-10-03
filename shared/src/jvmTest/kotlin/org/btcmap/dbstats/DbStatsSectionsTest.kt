package org.btcmap.dbstats

import org.btcmap.db.table.comment.TABLE as COMMENT_TABLE
import org.btcmap.db.table.event.TABLE as EVENT_TABLE
import org.btcmap.db.table.place.TABLE as PLACE_TABLE
import org.junit.Assert
import org.junit.Test

class DbStatsSectionsTest {

    private val labels = DbStatsLabels(
        database = "Database",
        file = "File",
        version = "Version",
        size = "Size",
        table = { "Table $it" },
        bundle = { "Bundle $it" },
        location = "Location",
        visibleRows = "Visible rows",
        deletedRows = "Deleted rows",
        futureRows = "Future rows",
        rows = "Rows",
        newestUpdate = "Newest update",
    )

    private fun table(name: String, rows: Long = 10) = TableStats(
        name = name,
        rowCount = rows,
        visibleRowCount = rows,
        deletedRowCount = 0,
        maxUpdatedAt = null,
        futureRowCount = null,
    )

    private fun sections(
        tables: List<TableStats>,
        bundles: Map<String, BundleStats> = emptyMap(),
    ) = dbStatsSections(
        file = null,
        version = 3,
        tables = tables,
        bundles = bundles,
        labels = labels,
        formatBytes = { "$it B" },
    )

    @Test
    fun ordersTableCardsByTheFixedOrder() {
        val result = sections(
            tables = listOf(table(EVENT_TABLE), table(PLACE_TABLE), table("zeta")),
        )

        Assert.assertEquals(
            listOf("database", "table:$PLACE_TABLE", "table:$EVENT_TABLE", "table:zeta"),
            result.map { it.key },
        )
    }

    @Test
    fun placesABundleCardBehindItsTable() {
        val bundle = BundleStats(
            location = "bundled-places.json",
            sizeBytes = 100,
            visibleCount = 5,
            deletedCount = 1,
            maxUpdatedAt = null,
        )

        val result = sections(
            tables = listOf(table(PLACE_TABLE), table(COMMENT_TABLE)),
            bundles = mapOf(PLACE_TABLE to bundle),
        )

        Assert.assertEquals(
            listOf("database", "table:$PLACE_TABLE", "bundle:$PLACE_TABLE", "table:$COMMENT_TABLE"),
            result.map { it.key },
        )
    }

    @Test
    fun includesTheOptionalTableRows() {
        val result = sections(
            tables = listOf(
                TableStats(
                    name = PLACE_TABLE,
                    rowCount = 10,
                    visibleRowCount = 8,
                    deletedRowCount = 2,
                    maxUpdatedAt = "2026-01-01T00:00:00Z",
                    futureRowCount = 1,
                ),
            ),
        )

        val card = result.first { it.key == "table:$PLACE_TABLE" }
        Assert.assertEquals(
            listOf("Rows", "Visible rows", "Deleted rows", "Future rows", "Newest update"),
            card.entries.map { it.label },
        )
    }
}
