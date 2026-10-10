package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class DashboardApiTest : ApiTestBase() {
    @Test
    fun getDashboard_getsV4EndpointAndParsesResult() = runTest {
        enqueueJson(
            """
            {
                "started_at": "2024-12-31T23:59:00Z",
                "finished_at": "2024-12-31T23:59:01Z",
                "generation_time_ms": 12,
                "places": {
                    "added": {"d1": 10, "d7": 50, "d30": 200},
                    "updated": {"d1": 5, "d7": 30, "d30": 150},
                    "deleted": {"d1": 1, "d7": 5, "d30": 20}
                },
                "imports": [
                    {
                        "origin": "square",
                        "total": {"d1": 50, "d7": 300, "d30": 1200},
                        "pending": {"d1": 10, "d7": 40, "d30": 80},
                        "revoked": {"d1": 1, "d7": 4, "d30": 15}
                    }
                ],
                "logs": {
                    "file_size_bytes": 2230968320,
                    "requests": {"d1": 12000, "d7": 80000, "d30": 320000},
                    "top_rpcs": [{"method": "get_area_dashboard", "count": 2000}],
                    "top_rest_api_calls": [
                        {"method": "GET", "path": "/v4/places/search", "count": 16000}
                    ],
                    "top_users": [{"user_id": 10, "name": "Scheduler", "count": 1819}]
                },
                "unique_ips_24h": {
                    "web": 238, "android": 214, "ios": 89,
                    "other_humans": 1668, "bots": 854
                },
                "storage": {
                    "disks": [
                        {
                            "device": "/dev/mapper/root",
                            "mount_point": "/",
                            "total_bytes": 982277472256,
                            "used_bytes": 475595489280,
                            "available_bytes": 456709595136,
                            "used_percent": 52.0
                        }
                    ]
                },
                "lnd": {
                    "onchain_total_sat": 1500000,
                    "onchain_confirmed_sat": 1500000,
                    "onchain_unconfirmed_sat": 0,
                    "outbound_liquidity_sat": 2750000,
                    "inbound_liquidity_sat": 3200000,
                    "pending_outbound_liquidity_sat": 0,
                    "pending_inbound_liquidity_sat": 0,
                    "total_balance_sat": 4250000
                },
                "sync_runs": [
                    {
                        "id": 42,
                        "started_at": "2024-12-31T23:50:00Z",
                        "finished_at": "2024-12-31T23:55:00Z",
                        "duration_s": 300.5,
                        "overpass_response_time_s": 12.3,
                        "elements_affected": 120,
                        "elements_created": 10,
                        "elements_updated": 80,
                        "elements_deleted": 30,
                        "failed_at": null,
                        "fail_reason": null
                    }
                ],
                "wallets": {
                    "wallets": [
                        {
                            "id": 1,
                            "name": "Spending",
                            "xpub": "xpub6...",
                            "cached_balance_sats": 123456,
                            "cached_tx": [
                                {"id": "abc", "received": 100000, "sent": 0, "delta": 100000}
                            ],
                            "cached_at": "2024-12-31T23:55:00Z"
                        }
                    ]
                }
            }
            """.trimIndent()
        )

        val dashboard = api().getDashboard()

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/dashboard/infra", request.url.encodedPath)

        Assert.assertEquals(12L, dashboard.generationTimeMs)
        Assert.assertEquals(200L, dashboard.places.added.d30)
        Assert.assertEquals("square", dashboard.imports.single().origin)
        Assert.assertEquals(80L, dashboard.imports.single().pending.d30)
        Assert.assertEquals(2230968320L, dashboard.logs.fileSizeBytes)
        Assert.assertEquals("get_area_dashboard", dashboard.logs.topRpcs.single().method)
        Assert.assertEquals("/v4/places/search", dashboard.logs.topRestApiCalls.single().path)
        Assert.assertEquals(10L, dashboard.logs.topUsers.single().userId)
        Assert.assertEquals("Scheduler", dashboard.logs.topUsers.single().name)
        Assert.assertEquals(1819L, dashboard.logs.topUsers.single().count)
        Assert.assertEquals(1668L, dashboard.uniqueIps24h.otherHumans)
        Assert.assertEquals("/dev/mapper/root", dashboard.storage.disks.single().device)
        Assert.assertEquals(52.0, dashboard.storage.disks.single().usedPercent, 0.0)
        Assert.assertEquals(4250000L, dashboard.lnd!!.totalBalanceSat)
        Assert.assertEquals(42L, dashboard.syncRuns.single().id)
        Assert.assertEquals(300.5, dashboard.syncRuns.single().durationS!!, 0.0)
        Assert.assertEquals("Spending", dashboard.wallets.wallets.single().name)
        Assert.assertEquals(123456L, dashboard.wallets.wallets.single().cachedBalanceSats)
        Assert.assertEquals(100000L, dashboard.wallets.wallets.single().cachedTx.single().delta)
    }

    @Test
    fun getDashboard_allowsANamelessTopUser() = runTest {
        enqueueJson(
            """{"logs": {"top_users": [{"user_id": 10, "name": null, "count": 5}]}}"""
        )

        val dashboard = api().getDashboard()

        Assert.assertNull(dashboard.logs.topUsers.single().name)
        Assert.assertEquals(5L, dashboard.logs.topUsers.single().count)
    }

    @Test
    fun getDashboard_forbiddenIsReported() = runTest {
        enqueueJson("""{"code": "forbidden", "message": "Forbidden"}""", code = 403)

        try {
            api().getDashboard()
            Assert.fail("Expected ApiException")
        } catch (e: ApiException) {
            Assert.assertEquals(403, e.code)
            Assert.assertTrue(e.message!!.contains("Forbidden"))
        }
    }
}
