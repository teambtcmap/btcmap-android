package org.btcmap.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.btcmap.api.Dashboard
import org.btcmap.api.DashboardEndpointCount
import org.btcmap.api.DashboardImport
import org.btcmap.api.DashboardLnd
import org.btcmap.api.DashboardLogs
import org.btcmap.api.DashboardMethodCount
import org.btcmap.api.DashboardPlaces
import org.btcmap.api.DashboardSyncRun
import org.btcmap.api.DashboardUniqueIps
import org.btcmap.api.DashboardWallet
import org.btcmap.api.DashboardWalletTx
import org.btcmap.api.DashboardWallets
import org.btcmap.api.DashboardWindow

class InfraDashboardSectionsTest {

    @Test
    fun mapsTheDashboardToCards() {
        val dashboard = Dashboard(
            generationTimeMs = 12,
            places = DashboardPlaces(added = DashboardWindow(d1 = 1, d7 = 2, d30 = 3)),
            imports = listOf(
                DashboardImport(
                    origin = "square",
                    total = DashboardWindow(d30 = 9),
                    pending = DashboardWindow(d30 = 2),
                ),
                DashboardImport(
                    origin = "blink",
                    total = DashboardWindow(d1 = 1, d30 = 1),
                ),
            ),
            logs = DashboardLogs(
                fileSizeBytes = 2048,
                requests = DashboardWindow(d1 = 4),
                topRpcs = listOf(DashboardMethodCount("get_area", 5)),
                topRestApiCalls = listOf(
                    DashboardEndpointCount(method = "GET", path = "/v4/places", count = 3),
                ),
            ),
            uniqueIps24h = DashboardUniqueIps(web = 6, bots = 7),
            lnd = DashboardLnd(
                outboundLiquiditySat = 6,
                inboundLiquiditySat = 7,
                totalBalanceSat = 8,
            ),
            syncRuns = listOf(
                DashboardSyncRun(
                    id = 42,
                    durationS = 10.0,
                    elementsCreated = 3,
                    elementsUpdated = 4,
                    elementsDeleted = 5,
                ),
            ),
            wallets = DashboardWallets(
                wallets = listOf(
                    DashboardWallet(
                        id = 1,
                        name = "Spending",
                        cachedBalanceSats = 10,
                        cachedTx = listOf(DashboardWalletTx(id = "abcdef0123456789", delta = -2)),
                    ),
                ),
            ),
        )

        val sections = infraDashboardSections(dashboard)
        val byKey = sections.associateBy { it.key }

        assertEquals("unique-ips", sections.first().key)
        assertEquals(
            listOf("public", "android", "phone_iphone", "person", "smart_toy"),
            byKey.getValue("unique-ips").entries.map { it.icon },
        )
        assertEquals("1 / 2 / 3", byKey.getValue("places").entries.first().value)
        // All sources are merged into one clickable card with summed totals.
        val imports = byKey.getValue("imports")
        assertEquals(
            listOf("1 / 0 / 10", "0 / 0 / 2", "0 / 0 / 0"),
            imports.entries.map { it.value },
        )
        assertTrue(imports.onClick != null)
        // Both kinds of API call share one clickable card with their totals.
        val apiCalls = byKey.getValue("api-calls")
        assertEquals(listOf("5", "3"), apiCalls.entries.map { it.value })
        assertTrue(apiCalls.onClick != null)
        // Only the liquidity split is on the card; the rest is in the dialog.
        val lnd = byKey.getValue("lnd")
        assertEquals(listOf("7 sat", "6 sat"), lnd.entries.map { it.value })
        assertTrue(lnd.onClick != null)
        // Only the success rate is on the card; the runs are in the dialog.
        val syncRuns = byKey.getValue("sync-runs")
        assertEquals("Success rate", syncRuns.entries.single().label)
        assertEquals("1/1", syncRuns.entries.single().value)
        assertTrue(syncRuns.onClick != null)
        // One card lists every wallet; transactions are in the dialog.
        val wallets = byKey.getValue("wallets")
        assertEquals(listOf("10 sat"), wallets.entries.map { it.value })
        assertTrue(wallets.onClick != null)
        assertTrue(byKey.containsKey("unique-ips"))
    }

    @Test
    fun showsPlaceholdersWhenOptionalSectionsAreEmpty() {
        val byKey = infraDashboardSections(Dashboard()).associateBy { it.key }

        assertEquals("None", byKey.getValue("imports").entries.single().value)
        kotlin.test.assertNull(byKey.getValue("imports").onClick)
        assertEquals("Unavailable", byKey.getValue("lnd").entries.single().value)
        kotlin.test.assertNull(byKey.getValue("lnd").onClick)
        assertEquals("None", byKey.getValue("wallets").entries.single().value)
        kotlin.test.assertNull(byKey.getValue("wallets").onClick)
        kotlin.test.assertNull(byKey.getValue("api-calls").onClick)
        assertEquals("None", byKey.getValue("sync-runs").entries.single().value)
        kotlin.test.assertNull(byKey.getValue("sync-runs").onClick)
    }
}
