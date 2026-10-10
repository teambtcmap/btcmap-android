package org.btcmap.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import org.btcmap.api.Dashboard
import org.btcmap.api.DashboardEndpointCount
import org.btcmap.api.DashboardImport
import org.btcmap.api.DashboardLnd
import org.btcmap.api.DashboardLogs
import org.btcmap.api.DashboardMethodCount
import org.btcmap.api.DashboardPlaces
import org.btcmap.api.DashboardSyncRun
import org.btcmap.api.DashboardTopUser
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
                topUsers = listOf(DashboardTopUser(userId = 10, name = "Scheduler", count = 9)),
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
            listOf("public", "android", "ios", "person", "smart_toy"),
            byKey.getValue("unique-ips").entries.map { it.icon },
        )
        assertEquals("1 / 2 / 3", byKey.getValue("places").entries.first().value)

        // All sources are merged into one card with summed totals; each summed
        // row expands to its per-source breakdown.
        val imports = byKey.getValue("imports")
        assertEquals(
            listOf("1 / 0 / 10", "0 / 0 / 2", "0 / 0 / 0"),
            imports.entries.map { it.value },
        )
        assertEquals(
            listOf("square" to "0 / 0 / 9", "blink" to "1 / 0 / 1"),
            imports.entries.first().details.map { it.label to it.value },
        )

        // Both kinds of API call share one card; each kind's top-10 list
        // expands under its total.
        val apiCalls = byKey.getValue("api-calls")
        assertEquals(listOf("5", "3"), apiCalls.entries.map { it.value })
        assertEquals(
            listOf("get_area" to "5"),
            apiCalls.entries[0].details.map { it.label to it.value },
        )
        assertEquals(
            listOf("GET /v4/places" to "3"),
            apiCalls.entries[1].details.map { it.label to it.value },
        )

        // One row per most-active user, with the server's name when known.
        val topUsers = byKey.getValue("top-users")
        assertEquals(listOf("Scheduler" to "9"), topUsers.entries.map { it.label to it.value })

        // The node's balance and liquidity on the card, each expanding to its
        // split.
        val lnd = byKey.getValue("lnd")
        assertEquals(
            listOf("On-chain total", "Inbound liquidity", "Outbound liquidity", "Total balance"),
            lnd.entries.map { it.label },
        )
        assertEquals(
            listOf("Confirmed", "Unconfirmed"),
            lnd.entries[0].details.map { it.label },
        )
        assertEquals(listOf("Pending inbound"), lnd.entries[1].details.map { it.label })
        assertEquals(listOf("Pending outbound"), lnd.entries[2].details.map { it.label })

        // Only the success rate is on the card; each run expands under it.
        val syncRuns = byKey.getValue("sync-runs")
        assertEquals("Success rate", syncRuns.entries.single().label)
        assertEquals("1/1", syncRuns.entries.single().value)
        assertEquals(
            listOf("10.0s · +3 ~4 -5"),
            syncRuns.entries.single().details.map { it.value },
        )

        // One card lists every wallet; each expands to its cache time and
        // transactions.
        val wallets = byKey.getValue("wallets")
        assertEquals(listOf("10 sat"), wallets.entries.map { it.value })
        assertEquals(
            listOf("Cached", "abcdef012345…"),
            wallets.entries.single().details.map { it.label },
        )
    }

    @Test
    fun showsPlaceholdersWhenOptionalSectionsAreEmpty() {
        val byKey = infraDashboardSections(Dashboard()).associateBy { it.key }

        assertEquals("None", byKey.getValue("imports").entries.single().value)
        assertEquals(0, byKey.getValue("imports").entries.single().details.size)
        assertEquals("Unavailable", byKey.getValue("lnd").entries.single().value)
        assertEquals(0, byKey.getValue("lnd").entries.single().details.size)
        assertEquals("None", byKey.getValue("wallets").entries.single().value)
        assertEquals("None", byKey.getValue("sync-runs").entries.single().value)
        assertEquals("None", byKey.getValue("top-users").entries.single().value)
    }
}
