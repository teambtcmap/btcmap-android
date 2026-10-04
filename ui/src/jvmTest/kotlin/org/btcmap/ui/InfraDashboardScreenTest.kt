package org.btcmap.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.btcmap.api.Dashboard
import org.btcmap.api.DashboardEndpointCount
import org.btcmap.api.DashboardImport
import org.btcmap.api.DashboardLogs
import org.btcmap.api.DashboardLnd
import org.btcmap.api.DashboardMethodCount
import org.btcmap.api.DashboardSyncRun
import org.btcmap.api.DashboardWallet
import org.btcmap.api.DashboardWallets
import org.btcmap.api.DashboardWindow

class InfraDashboardScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun importsCardOpensTheSourcesDialog() {
        val dashboard = Dashboard(
            imports = listOf(
                DashboardImport(
                    origin = "square",
                    total = DashboardWindow(d30 = 7),
                ),
            ),
        )

        runComposeUiTest {
            setContent {
                InfraDashboardScreen(load = { dashboard })
            }

            onNodeWithText("Unique clients (24h)").assertIsDisplayed()
            onNodeWithText("Imports").performClick()
            onNodeWithText("Import sources").assertIsDisplayed()
            onNodeWithText("square").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun apiCallsCardOpensTheDialog() {
        val dashboard = Dashboard(
            logs = DashboardLogs(
                topRpcs = listOf(DashboardMethodCount("get_area", 5)),
                topRestApiCalls = listOf(
                    DashboardEndpointCount(method = "GET", path = "/v4/places", count = 3),
                ),
            ),
        )

        runComposeUiTest {
            setContent {
                InfraDashboardScreen(load = { dashboard })
            }

            onNodeWithText("API calls (24h)").performClick()
            onNodeWithText("Top API calls (24h)").assertIsDisplayed()
            onNodeWithText("get_area").assertIsDisplayed()
            onNodeWithText("GET /v4/places").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lightningCardOpensTheDialog() {
        val dashboard = Dashboard(
            lnd = DashboardLnd(
                outboundLiquiditySat = 1,
                inboundLiquiditySat = 2,
                totalBalanceSat = 3,
            ),
        )

        runComposeUiTest {
            setContent {
                InfraDashboardScreen(load = { dashboard })
            }

            onNodeWithText("Outbound liquidity").assertIsDisplayed()
            onNodeWithText("Inbound liquidity").assertIsDisplayed()
            onNodeWithText("Lightning node").performClick()
            onNodeWithText("On-chain total").assertIsDisplayed()
            onNodeWithText("Total balance").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun walletsCardOpensTheDialog() {
        val dashboard = Dashboard(
            wallets = DashboardWallets(
                wallets = listOf(
                    DashboardWallet(id = 1, name = "Spending", cachedBalanceSats = 10),
                ),
            ),
        )

        runComposeUiTest {
            setContent {
                InfraDashboardScreen(load = { dashboard })
            }

            onNodeWithText("Spending").assertIsDisplayed()
            onNodeWithText("Wallets").performClick()
            onNodeWithText("Balance").assertIsDisplayed()
            onNodeWithText("Cached").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun syncRunsCardOpensTheDialog() {
        val dashboard = Dashboard(
            syncRuns = listOf(
                DashboardSyncRun(
                    id = 42,
                    durationS = 10.0,
                    elementsCreated = 3,
                    elementsUpdated = 4,
                    elementsDeleted = 5,
                ),
            ),
        )

        runComposeUiTest {
            setContent {
                InfraDashboardScreen(load = { dashboard })
            }

            onNodeWithText("1/1").assertIsDisplayed()
            onNodeWithText("OSM syncs").performClick()
            onNodeWithText("10.0s · +3 ~4 -5").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun refreshKeyReloadsTheDashboard() {
        var loads = 0

        runComposeUiTest {
            val key = mutableStateOf(0)
            setContent {
                InfraDashboardScreen(load = { loads++; Dashboard() }, refreshKey = key.value)
            }

            waitForIdle()
            assertEquals(1, loads)

            key.value = 1
            waitForIdle()
            assertEquals(2, loads)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reportsTheLoadingStateAroundALoad() {
        val states = mutableListOf<Boolean>()

        runComposeUiTest {
            setContent {
                InfraDashboardScreen(
                    load = { Dashboard() },
                    onLoadingChange = { states += it },
                )
            }
            waitForIdle()
        }

        assertEquals(listOf(true, false), states)
    }
}
