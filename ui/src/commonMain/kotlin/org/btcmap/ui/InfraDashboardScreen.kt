package org.btcmap.ui

import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong
import kotlin.time.Instant
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.btcmap.api.Dashboard
import org.btcmap.api.DashboardImport
import org.btcmap.api.DashboardSyncRun
import org.btcmap.api.DashboardWindow
import org.btcmap.platform.formatInteger
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.util.rethrowIfCancellation

/** Test tag on the retry shown after the dashboard fails to load. */
const val INFRA_DASHBOARD_RETRY_TAG = "infra-dashboard-retry"

/**
 * The infrastructure dashboard: an admin-only snapshot fetched from
 * `GET /v4/dashboard/infra` and rendered as the shared [StatsGrid] cards.
 *
 * The load state machine lives here, like the activity feed's: [load] runs on
 * first composition and on every retry, a failure is shown with a retry, and a
 * success is mapped to cards by [infraDashboardSections].
 */
@Composable
fun InfraDashboardScreen(
    load: suspend () -> Dashboard,
    modifier: Modifier = Modifier,
    refreshKey: Int = 0,
    /** Reports whether a load is in flight, so a host can disable its refresh. */
    onLoadingChange: (Boolean) -> Unit = {},
) {
    var dashboard by remember { mutableStateOf<Dashboard?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    val currentOnLoadingChange by rememberUpdatedState(onLoadingChange)

    LaunchedEffect(retryKey, refreshKey) {
        // A retry from the error state has nothing to keep, so it clears the
        // error and shows the spinner. A refresh keeps the current snapshot
        // visible and hot-swaps it when the new one arrives, so the screen
        // never blanks out; a failed refresh keeps the last snapshot too.
        if (dashboard == null) {
            error = null
        }
        currentOnLoadingChange(true)
        try {
            dashboard = load()
            error = null
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            if (dashboard == null) {
                error = t.message ?: t.toString()
            }
        } finally {
            currentOnLoadingChange(false)
        }
    }

    val currentError = error
    val current = dashboard
    when {
        currentError != null -> Box(modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
            ) {
                MaterialSymbol(
                    glyph = "error",
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = currentError,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = { retryKey++ },
                    modifier = Modifier.testTag(INFRA_DASHBOARD_RETRY_TAG),
                ) {
                    Text("Retry")
                }
            }
        }

        current == null -> Box(modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        else -> StatsGrid(
            sections = infraDashboardSections(current),
            modifier = modifier,
        )
    }
}

/**
 * Maps a dashboard snapshot to the label/value cards [StatsGrid] renders.
 *
 * A card keeps its summary rows on the card and reveals the fuller breakdown
 * when a row is tapped, so a card that would otherwise open a dialog instead
 * expands in place.
 */
internal fun infraDashboardSections(d: Dashboard): List<StatsSection> = buildList {
    add(
        StatsSection(
            key = "unique-ips",
            title = "Unique clients (24h)",
            icon = "group",
            entries = listOf(
                StatsEntry("Web", formatInteger(d.uniqueIps24h.web), icon = "public"),
                StatsEntry("Android", formatInteger(d.uniqueIps24h.android), icon = "android"),
                StatsEntry("iOS", formatInteger(d.uniqueIps24h.ios), icon = "ios"),
                StatsEntry(
                    "Other humans",
                    formatInteger(d.uniqueIps24h.otherHumans),
                    icon = "person",
                ),
                StatsEntry("Bots", formatInteger(d.uniqueIps24h.bots), icon = "smart_toy"),
            ),
        )
    )

    add(
        StatsSection(
            key = "places",
            title = "Places",
            icon = "storefront",
            entries = listOf(
                StatsEntry("Added (1d / 7d / 30d)", formatWindow(d.places.added)),
                StatsEntry("Updated (1d / 7d / 30d)", formatWindow(d.places.updated)),
                StatsEntry("Deleted (1d / 7d / 30d)", formatWindow(d.places.deleted)),
            ),
        )
    )

    if (d.imports.isEmpty()) {
        add(
            StatsSection(
                key = "imports",
                title = "Imports",
                icon = "download",
                entries = listOf(StatsEntry("Origins", "None")),
            )
        )
    } else {
        // One card for every source; each summed row expands to its per-source
        // breakdown.
        add(
            StatsSection(
                key = "imports",
                title = "Imports",
                icon = "download",
                entries = listOf(
                    StatsEntry(
                        "Total (1d / 7d / 30d)",
                        formatWindow(d.imports.sumWindows { it.total }),
                        details = d.imports.map { StatsEntry(it.origin, formatWindow(it.total)) },
                    ),
                    StatsEntry(
                        "Pending (1d / 7d / 30d)",
                        formatWindow(d.imports.sumWindows { it.pending }),
                        details = d.imports.map { StatsEntry(it.origin, formatWindow(it.pending)) },
                    ),
                    StatsEntry(
                        "Revoked (1d / 7d / 30d)",
                        formatWindow(d.imports.sumWindows { it.revoked }),
                        details = d.imports.map { StatsEntry(it.origin, formatWindow(it.revoked)) },
                    ),
                ),
            )
        )
    }

    add(
        StatsSection(
            key = "logs",
            title = "Request logs",
            icon = "receipt_long",
            entries = listOf(
                StatsEntry("Log file size", formatBytes(d.logs.fileSizeBytes)),
                StatsEntry("Requests (1d / 7d / 30d)", formatWindow(d.logs.requests)),
            ),
        )
    )

    val rpcCalls = d.logs.topRpcs
    val restCalls = d.logs.topRestApiCalls
    // Both kinds of API call share one card; each kind's top-10 list expands
    // under its total.
    add(
        StatsSection(
            key = "api-calls",
            title = "API calls (24h)",
            icon = "api",
            entries = listOf(
                StatsEntry(
                    "RPC (top 10)",
                    formatInteger(rpcCalls.sumOf { it.count }),
                    details = rpcCalls.map { StatsEntry(it.method, formatInteger(it.count)) },
                ),
                StatsEntry(
                    "REST (top 10)",
                    formatInteger(restCalls.sumOf { it.count }),
                    details = restCalls.map {
                        StatsEntry("${it.method} ${it.path}".trim(), formatInteger(it.count))
                    },
                ),
            ),
        )
    )

    val topUsers = d.logs.topUsers
    if (topUsers.isEmpty()) {
        add(
            StatsSection(
                key = "top-users",
                title = "Top users (24h)",
                icon = "person",
                entries = listOf(StatsEntry("Users", "None")),
            )
        )
    } else {
        // One row per user, named where the server knows the account and
        // falling back to the id otherwise.
        add(
            StatsSection(
                key = "top-users",
                title = "Top users (24h)",
                icon = "person",
                entries = topUsers.map { user ->
                    val label = user.name?.takeIf { it.isNotBlank() } ?: "#${user.userId}"
                    StatsEntry(label, formatInteger(user.count))
                },
            )
        )
    }

    val lnd = d.lnd
    if (lnd == null) {
        add(
            StatsSection(
                key = "lnd",
                title = "Lightning node",
                icon = "bolt",
                entries = listOf(StatsEntry("Status", "Unavailable")),
            )
        )
    } else {
        // The node's balance and liquidity on the card, each expanding to its
        // pending or confirmation split.
        add(
            StatsSection(
                key = "lnd",
                title = "Lightning node",
                icon = "bolt",
                entries = listOf(
                    StatsEntry(
                        "On-chain total",
                        formatSats(lnd.onchainTotalSat),
                        details = listOf(
                            StatsEntry("Confirmed", formatSats(lnd.onchainConfirmedSat)),
                            StatsEntry("Unconfirmed", formatSats(lnd.onchainUnconfirmedSat)),
                        ),
                    ),
                    StatsEntry(
                        "Inbound liquidity",
                        formatSats(lnd.inboundLiquiditySat),
                        details = listOf(
                            StatsEntry("Pending inbound", formatSats(lnd.pendingInboundLiquiditySat)),
                        ),
                    ),
                    StatsEntry(
                        "Outbound liquidity",
                        formatSats(lnd.outboundLiquiditySat),
                        details = listOf(
                            StatsEntry("Pending outbound", formatSats(lnd.pendingOutboundLiquiditySat)),
                        ),
                    ),
                    StatsEntry("Total balance", formatSats(lnd.totalBalanceSat)),
                ),
            )
        )
    }

    if (d.syncRuns.isEmpty()) {
        add(
            StatsSection(
                key = "sync-runs",
                title = "OSM syncs",
                icon = "sync",
                entries = listOf(StatsEntry("Runs", "None")),
            )
        )
    } else {
        // The success rate on the card; each run expands under it.
        val succeeded = d.syncRuns.count { it.failedAt == null }
        add(
            StatsSection(
                key = "sync-runs",
                title = "OSM syncs",
                icon = "sync",
                entries = listOf(
                    StatsEntry(
                        "Success rate",
                        "$succeeded/${d.syncRuns.size}",
                        details = d.syncRuns.map {
                            StatsEntry("#${it.id} · ${formatTime(it.startedAt)}", syncRunValue(it))
                        },
                    ),
                ),
            )
        )
    }

    if (d.wallets.wallets.isEmpty()) {
        add(
            StatsSection(
                key = "wallets",
                title = "Wallets",
                icon = "account_balance_wallet",
                entries = listOf(StatsEntry("Wallets", "None")),
            )
        )
    } else {
        // One row per wallet; each wallet expands to its cache time and
        // transactions.
        add(
            StatsSection(
                key = "wallets",
                title = "Wallets",
                icon = "account_balance_wallet",
                entries = d.wallets.wallets.map { wallet ->
                    StatsEntry(
                        wallet.name,
                        formatSats(wallet.cachedBalanceSats),
                        details = buildList {
                            add(StatsEntry("Cached", formatTime(wallet.cachedAt)))
                            wallet.cachedTx.forEach { tx ->
                                add(StatsEntry(tx.id.take(12) + "…", signedSats(tx.delta)))
                            }
                        },
                    )
                },
            )
        )
    }
}

private fun List<DashboardImport>.sumWindows(
    selector: (DashboardImport) -> DashboardWindow,
): DashboardWindow = fold(DashboardWindow()) { acc, import ->
    val window = selector(import)
    DashboardWindow(
        d1 = acc.d1 + window.d1,
        d7 = acc.d7 + window.d7,
        d30 = acc.d30 + window.d30,
    )
}

private fun formatWindow(window: DashboardWindow): String =
    "${formatInteger(window.d1)} / ${formatInteger(window.d7)} / ${formatInteger(window.d30)}"

private fun formatSats(satoshi: Long): String = "${formatInteger(satoshi)} sat"

private fun signedSats(satoshi: Long): String =
    (if (satoshi >= 0) "+" else "") + formatSats(satoshi)

private fun syncRunValue(run: DashboardSyncRun): String =
    if (run.failedAt != null) {
        "Failed: ${run.failReason ?: "unknown"}"
    } else {
        "${formatDuration(run.durationS)} · " +
            "+${formatInteger(run.elementsCreated)} " +
            "~${formatInteger(run.elementsUpdated)} " +
            "-${formatInteger(run.elementsDeleted)}"
    }

private fun formatDuration(seconds: Double?): String =
    if (seconds == null) "—" else "${(seconds * 10).roundToLong() / 10.0}s"

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw
    return instant.format(TIME_FORMATTER)
}
