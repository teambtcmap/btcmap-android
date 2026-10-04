package org.btcmap.api

import io.ktor.http.HttpMethod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.btcmap.json.btcmapJson
import org.btcmap.json.parseJson

/** A `d1`/`d7`/`d30` count triple used throughout the dashboard. */
@Serializable
data class DashboardWindow(
    val d1: Long = 0,
    val d7: Long = 0,
    val d30: Long = 0,
)

/** Place create/update/delete counts over the three windows. */
@Serializable
data class DashboardPlaces(
    val added: DashboardWindow = DashboardWindow(),
    val updated: DashboardWindow = DashboardWindow(),
    val deleted: DashboardWindow = DashboardWindow(),
)

/** Imported-place counts for one import origin. */
@Serializable
data class DashboardImport(
    val origin: String = "",
    val total: DashboardWindow = DashboardWindow(),
    val pending: DashboardWindow = DashboardWindow(),
    val revoked: DashboardWindow = DashboardWindow(),
)

/** A most-called RPC method and its count. */
@Serializable
data class DashboardMethodCount(
    val method: String = "",
    val count: Long = 0,
)

/** A most-called REST endpoint and its count. */
@Serializable
data class DashboardEndpointCount(
    val method: String = "",
    val path: String = "",
    val count: Long = 0,
)

/** Request-log stats. */
@Serializable
data class DashboardLogs(
    @SerialName("file_size_bytes") val fileSizeBytes: Long = 0,
    val requests: DashboardWindow = DashboardWindow(),
    @SerialName("top_rpcs") val topRpcs: List<DashboardMethodCount> = emptyList(),
    @SerialName("top_rest_api_calls")
    val topRestApiCalls: List<DashboardEndpointCount> = emptyList(),
)

/** Unique client IPs in the last 24 hours, bucketed by platform. */
@Serializable
data class DashboardUniqueIps(
    val web: Long = 0,
    val android: Long = 0,
    val ios: Long = 0,
    @SerialName("other_humans") val otherHumans: Long = 0,
    val bots: Long = 0,
)

/** One block device's disk usage. */
@Serializable
data class DashboardDisk(
    val device: String = "",
    @SerialName("mount_point") val mountPoint: String = "",
    @SerialName("total_bytes") val totalBytes: Long = 0,
    @SerialName("used_bytes") val usedBytes: Long = 0,
    @SerialName("available_bytes") val availableBytes: Long = 0,
    @SerialName("used_percent") val usedPercent: Double = 0.0,
)

/** Disk usage across the host's real block devices. */
@Serializable
data class DashboardStorage(
    val disks: List<DashboardDisk> = emptyList(),
)

/** Balances probed from the LND node; null when unreachable or unconfigured. */
@Serializable
data class DashboardLnd(
    @SerialName("onchain_total_sat") val onchainTotalSat: Long = 0,
    @SerialName("onchain_confirmed_sat") val onchainConfirmedSat: Long = 0,
    @SerialName("onchain_unconfirmed_sat") val onchainUnconfirmedSat: Long = 0,
    @SerialName("outbound_liquidity_sat") val outboundLiquiditySat: Long = 0,
    @SerialName("inbound_liquidity_sat") val inboundLiquiditySat: Long = 0,
    @SerialName("pending_outbound_liquidity_sat") val pendingOutboundLiquiditySat: Long = 0,
    @SerialName("pending_inbound_liquidity_sat") val pendingInboundLiquiditySat: Long = 0,
    @SerialName("total_balance_sat") val totalBalanceSat: Long = 0,
)

/** One recorded OSM sync run. */
@Serializable
data class DashboardSyncRun(
    val id: Long = 0,
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("duration_s") val durationS: Double? = null,
    @SerialName("overpass_response_time_s") val overpassResponseTimeS: Double? = null,
    @SerialName("elements_affected") val elementsAffected: Long = 0,
    @SerialName("elements_created") val elementsCreated: Long = 0,
    @SerialName("elements_updated") val elementsUpdated: Long = 0,
    @SerialName("elements_deleted") val elementsDeleted: Long = 0,
    @SerialName("failed_at") val failedAt: String? = null,
    @SerialName("fail_reason") val failReason: String? = null,
)

/** One on-chain wallet transaction. */
@Serializable
data class DashboardWalletTx(
    val id: String = "",
    val received: Long = 0,
    val sent: Long = 0,
    val delta: Long = 0,
)

/** A configured wallet, its cached balance and its recent transactions. */
@Serializable
data class DashboardWallet(
    val id: Long = 0,
    val name: String = "",
    val xpub: String = "",
    @SerialName("cached_balance_sats") val cachedBalanceSats: Long = 0,
    @SerialName("cached_tx") val cachedTx: List<DashboardWalletTx> = emptyList(),
    @SerialName("cached_at") val cachedAt: String? = null,
)

/** The configured BTC Map wallets; the server nests the list under `wallets`. */
@Serializable
data class DashboardWallets(
    val wallets: List<DashboardWallet> = emptyList(),
)

/** The full `dashboard` RPC snapshot. */
@Serializable
data class Dashboard(
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("finished_at") val finishedAt: String = "",
    @SerialName("generation_time_ms") val generationTimeMs: Long = 0,
    val places: DashboardPlaces = DashboardPlaces(),
    val imports: List<DashboardImport> = emptyList(),
    val logs: DashboardLogs = DashboardLogs(),
    @SerialName("unique_ips_24h") val uniqueIps24h: DashboardUniqueIps = DashboardUniqueIps(),
    val storage: DashboardStorage = DashboardStorage(),
    val lnd: DashboardLnd? = null,
    @SerialName("sync_runs") val syncRuns: List<DashboardSyncRun> = emptyList(),
    val wallets: DashboardWallets = DashboardWallets(),
)

/**
 * Fetches the infrastructure dashboard over the JSON-RPC endpoint (`POST /rpc`,
 * method `dashboard`). The stored session token is attached by [call]; the
 * server requires it to hold an admin or root role.
 */
suspend fun Api.getDashboard(): Dashboard {
    val url = buildUrl("rpc")
    val body = buildJsonObject {
        put("jsonrpc", "2.0")
        put("method", "dashboard")
        putJsonObject("params") {}
        put("id", 1)
    }

    return call(HttpMethod.Post, url, body = body) { it.toDashboard() }
}

private fun String.toDashboard(): Dashboard {
    val root = parseJson(this).jsonObject

    // A JSON-RPC error is reported under `error`, even on a 200 response.
    root["error"]?.let { error ->
        val message = runCatching {
            error.jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull()
        throw ApiException(
            code = 200,
            message = message ?: "Dashboard request failed",
        )
    }

    val result = root["result"]
        ?: throw ApiParseException("Dashboard response is missing 'result'")

    return btcmapJson.decodeFromJsonElement(Dashboard.serializer(), result)
}
