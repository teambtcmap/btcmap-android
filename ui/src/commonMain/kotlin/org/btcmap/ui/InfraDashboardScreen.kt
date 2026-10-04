package org.btcmap.ui

import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong
import kotlin.time.Instant
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import org.btcmap.api.DashboardEndpointCount
import org.btcmap.api.DashboardImport
import org.btcmap.api.DashboardLnd
import org.btcmap.api.DashboardMethodCount
import org.btcmap.api.DashboardSyncRun
import org.btcmap.api.DashboardWallet
import org.btcmap.api.DashboardWindow
import org.btcmap.platform.formatInteger
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.util.rethrowIfCancellation

/** Test tag on the retry shown after the dashboard fails to load. */
const val INFRA_DASHBOARD_RETRY_TAG = "infra-dashboard-retry"

/**
 * The infrastructure dashboard: an admin-only snapshot fetched over the
 * `dashboard` RPC method and rendered as the shared [StatsGrid] cards.
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
    var dialog by remember { mutableStateOf<DashboardDialog?>(null) }
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
            sections = infraDashboardSections(
                d = current,
                onShowImportSources = { dialog = DashboardDialog.ImportSources },
                onShowApiCalls = { dialog = DashboardDialog.ApiCalls },
                onShowLightning = { dialog = DashboardDialog.Lightning },
                onShowWallets = { dialog = DashboardDialog.Wallets },
                onShowSyncRuns = { dialog = DashboardDialog.SyncRuns },
            ),
            modifier = modifier,
        )
    }

    when (dialog) {
        DashboardDialog.ImportSources -> ImportSourcesDialog(
            imports = current?.imports.orEmpty(),
            onDismiss = { dialog = null },
        )

        DashboardDialog.ApiCalls -> ApiCallsDialog(
            rpc = current?.logs?.topRpcs.orEmpty(),
            rest = current?.logs?.topRestApiCalls.orEmpty(),
            onDismiss = { dialog = null },
        )

        DashboardDialog.Lightning -> LightningDialog(
            lnd = current?.lnd,
            onDismiss = { dialog = null },
        )

        DashboardDialog.Wallets -> WalletsDialog(
            wallets = current?.wallets?.wallets.orEmpty(),
            onDismiss = { dialog = null },
        )

        DashboardDialog.SyncRuns -> SyncRunsDialog(
            runs = current?.syncRuns.orEmpty(),
            onDismiss = { dialog = null },
        )

        null -> {}
    }
}

/** The dashboard's tap-to-open detail dialogs. */
private enum class DashboardDialog { ImportSources, ApiCalls, Lightning, Wallets, SyncRuns }

@Composable
private fun ImportSourcesDialog(
    imports: List<DashboardImport>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import sources") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                imports.forEachIndexed { index, import ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Text(
                        text = import.origin,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    DashboardDetailRow("Total", formatWindow(import.total))
                    DashboardDetailRow("Pending", formatWindow(import.pending))
                    DashboardDetailRow("Revoked", formatWindow(import.revoked))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ApiCallsDialog(
    rpc: List<DashboardMethodCount>,
    rest: List<DashboardEndpointCount>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Top API calls (24h)") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (rpc.isNotEmpty()) {
                    Text(
                        text = "RPC methods",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    rpc.forEach { DashboardDetailRow(it.method, formatInteger(it.count)) }
                }
                if (rest.isNotEmpty()) {
                    if (rpc.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Text(
                        text = "REST endpoints",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    rest.forEach {
                        DashboardDetailRow(
                            "${it.method} ${it.path}".trim(),
                            formatInteger(it.count),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun LightningDialog(
    lnd: DashboardLnd?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lightning node") },
        text = {
            if (lnd != null) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    DashboardDetailRow("On-chain total", formatSats(lnd.onchainTotalSat))
                    DashboardDetailRow("On-chain confirmed", formatSats(lnd.onchainConfirmedSat))
                    DashboardDetailRow("On-chain unconfirmed", formatSats(lnd.onchainUnconfirmedSat))
                    DashboardDetailRow("Inbound liquidity", formatSats(lnd.inboundLiquiditySat))
                    DashboardDetailRow("Outbound liquidity", formatSats(lnd.outboundLiquiditySat))
                    DashboardDetailRow("Pending outbound", formatSats(lnd.pendingOutboundLiquiditySat))
                    DashboardDetailRow("Pending inbound", formatSats(lnd.pendingInboundLiquiditySat))
                    DashboardDetailRow("Total balance", formatSats(lnd.totalBalanceSat))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun WalletsDialog(
    wallets: List<DashboardWallet>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wallets") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                wallets.forEachIndexed { index, wallet ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    DashboardDetailRow("Balance", formatSats(wallet.cachedBalanceSats))
                    DashboardDetailRow("Cached", formatTime(wallet.cachedAt))
                    wallet.cachedTx.forEach { tx ->
                        DashboardDetailRow(tx.id.take(12) + "…", signedSats(tx.delta))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun SyncRunsDialog(
    runs: List<DashboardSyncRun>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OSM syncs") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                runs.forEach { run ->
                    DashboardDetailRow(
                        label = "#${run.id} · ${formatTime(run.startedAt)}",
                        value = syncRunValue(run),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

private fun syncRunValue(run: DashboardSyncRun): String =
    if (run.failedAt != null) {
        "Failed: ${run.failReason ?: "unknown"}"
    } else {
        "${formatDuration(run.durationS)} · " +
            "+${formatInteger(run.elementsCreated)} " +
            "~${formatInteger(run.elementsUpdated)} " +
            "-${formatInteger(run.elementsDeleted)}"
    }

@Composable
private fun DashboardDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Maps a dashboard snapshot to the label/value cards [StatsGrid] renders. */
internal fun infraDashboardSections(
    d: Dashboard,
    onShowImportSources: () -> Unit = {},
    onShowApiCalls: () -> Unit = {},
    onShowLightning: () -> Unit = {},
    onShowWallets: () -> Unit = {},
    onShowSyncRuns: () -> Unit = {},
): List<StatsSection> = buildList {
    add(
        StatsSection(
            key = "unique-ips",
            title = "Unique clients (24h)",
            icon = "group",
            entries = listOf(
                StatsEntry("Web", formatInteger(d.uniqueIps24h.web), icon = "public"),
                StatsEntry("Android", formatInteger(d.uniqueIps24h.android), icon = "android"),
                StatsEntry("iOS", formatInteger(d.uniqueIps24h.ios), icon = "phone_iphone"),
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
        // One card for every source; the per-source breakdown opens on tap.
        add(
            StatsSection(
                key = "imports",
                title = "Imports",
                icon = "download",
                entries = listOf(
                    StatsEntry("Total (1d / 7d / 30d)", formatWindow(d.imports.sumWindows { it.total })),
                    StatsEntry(
                        "Pending (1d / 7d / 30d)",
                        formatWindow(d.imports.sumWindows { it.pending }),
                    ),
                    StatsEntry(
                        "Revoked (1d / 7d / 30d)",
                        formatWindow(d.imports.sumWindows { it.revoked }),
                    ),
                ),
                onClick = onShowImportSources,
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
    val hasApiCalls = rpcCalls.isNotEmpty() || restCalls.isNotEmpty()
    // One card for both kinds; the top-10 lists open on tap.
    add(
        StatsSection(
            key = "api-calls",
            title = "API calls (24h)",
            icon = "api",
            entries = listOf(
                StatsEntry("RPC (top 10)", formatInteger(rpcCalls.sumOf { it.count })),
                StatsEntry("REST (top 10)", formatInteger(restCalls.sumOf { it.count })),
            ),
            onClick = if (hasApiCalls) onShowApiCalls else null,
        )
    )

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
        // Only the liquidity split is shown; the full node detail opens on tap.
        add(
            StatsSection(
                key = "lnd",
                title = "Lightning node",
                icon = "bolt",
                entries = listOf(
                    StatsEntry("Inbound liquidity", formatSats(lnd.inboundLiquiditySat)),
                    StatsEntry("Outbound liquidity", formatSats(lnd.outboundLiquiditySat)),
                ),
                onClick = onShowLightning,
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
        // Only the success rate is on the card; the runs open on tap.
        val succeeded = d.syncRuns.count { it.failedAt == null }
        add(
            StatsSection(
                key = "sync-runs",
                title = "OSM syncs",
                icon = "sync",
                entries = listOf(
                    StatsEntry("Success rate", "$succeeded/${d.syncRuns.size}"),
                ),
                onClick = onShowSyncRuns,
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
        // One row per wallet; each wallet's transactions open on tap.
        add(
            StatsSection(
                key = "wallets",
                title = "Wallets",
                icon = "account_balance_wallet",
                entries = d.wallets.wallets.map {
                    StatsEntry(it.name, formatSats(it.cachedBalanceSats))
                },
                onClick = onShowWallets,
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

private fun formatDuration(seconds: Double?): String =
    if (seconds == null) "—" else "${(seconds * 10).roundToLong() / 10.0}s"

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatTime(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: return raw
    return instant.format(TIME_FORMATTER)
}
