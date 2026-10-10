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
        tables = "Tables",
        bundles = "Bundles",
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
    fun putsEveryTableInOneCardInTheFixedOrder() {
        val result = sections(
            tables = listOf(table(EVENT_TABLE), table(PLACE_TABLE), table("zeta")),
        )

        Assert.assertEquals(listOf("database", "tables"), result.map { it.key })
        Assert.assertEquals(
            listOf(PLACE_TABLE, EVENT_TABLE, "zeta"),
            result.first { it.key == "tables" }.entries.map { it.label },
        )
    }

    @Test
    fun omitsTheTablesCardWhenThereAreNoTables() {
        val result = sections(tables = emptyList())

        Assert.assertEquals(listOf("database"), result.map { it.key })
    }

    @Test
    fun putsEveryBundleInOneCardAfterTheTablesCard() {
        val bundle = BundleStats(
            location = "assets/bundled-places.json",
            sizeBytes = 100,
            visibleCount = 5,
            deletedCount = 1,
            maxUpdatedAt = "2026-01-01T00:00:00Z",
        )

        val result = sections(
            tables = listOf(table(PLACE_TABLE), table(COMMENT_TABLE)),
            bundles = mapOf(PLACE_TABLE to bundle),
        )

        Assert.assertEquals(
            listOf("database", "tables", "bundles"),
            result.map { it.key },
        )
        val entry = result.first { it.key == "bundles" }.entries.single()
        Assert.assertEquals(PLACE_TABLE, entry.label)
        Assert.assertEquals("storefront", entry.icon)
        Assert.assertEquals("5/1", entry.value)
        Assert.assertEquals(
            listOf("Location", "Size", "Visible rows", "Deleted rows", "Newest update"),
            entry.details.map { it.label },
        )
        Assert.assertEquals("100 B", entry.details.first { it.label == "Size" }.value)
    }

    @Test
    fun omitsTheBundlesCardWhenThereAreNoBundles() {
        val result = sections(tables = listOf(table(PLACE_TABLE)))

        Assert.assertEquals(listOf("database", "tables"), result.map { it.key })
    }

    @Test
    fun summarisesATrackedTableAndKeepsTheBreakdownAsDetails() {
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

        val entry = result.first { it.key == "tables" }.entries.single()
        Assert.assertEquals(PLACE_TABLE, entry.label)
        Assert.assertEquals("storefront", entry.icon)
        Assert.assertEquals("10/8/2", entry.value)
        Assert.assertEquals(
            listOf("Rows", "Visible rows", "Deleted rows", "Future rows", "Newest update"),
            entry.details.map { it.label },
        )
    }

    @Test
    fun anUntrackedTableSummarisesAsJustTheRowCount() {
        val result = sections(
            tables = listOf(TableStats("user", 42, null, null, null, null)),
        )

        val entry = result.first { it.key == "tables" }.entries.single()
        Assert.assertEquals("42", entry.value)
        Assert.assertEquals("person", entry.icon)
        Assert.assertEquals(listOf("Rows"), entry.details.map { it.label })
    }
}
