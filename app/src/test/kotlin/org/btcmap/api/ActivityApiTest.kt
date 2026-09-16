package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class ActivityApiTest : ApiTestBase() {
    @Test
    fun getActivity_sendsScopesAndParsesAllFields() = runTest {
        enqueueJson(ACTIVITY)

        val items = api().getActivity(areaIds = listOf("germany", "berlin"), days = 30)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/activity", request.url.encodedPath)
        Assert.assertEquals("germany,berlin", request.url.queryParameter("areas"))
        Assert.assertEquals("30", request.url.queryParameter("days"))

        Assert.assertEquals(2, items.size)
        val item = items[0]
        Assert.assertEquals("place_boosted", item.type)
        Assert.assertEquals(38625L, item.placeId)
        Assert.assertEquals("Example Cafe", item.placeName)
        Assert.assertEquals(12345L, item.osmUserId)
        Assert.assertEquals("alice", item.osmUserName)
        Assert.assertEquals("lightning:alice@example", item.osmUserTip)
        Assert.assertEquals("https://api.btcmap.org/og/element/38625", item.image)
        Assert.assertEquals("2026-04-20T12:00:00Z", item.date)
        Assert.assertEquals(30L, item.durationDays)
        Assert.assertEquals("great spot", item.comment)
    }

    @Test
    fun getActivity_parsesNullAndBlankOptionalFields() = runTest {
        enqueueJson(
            """
            [
                {
                    "type": "place_added",
                    "place_id": 1,
                    "osm_user_id": null,
                    "osm_user_name": "  ",
                    "osm_user_tip": null,
                    "image": null,
                    "date": "2026-04-20T12:00:00Z",
                    "duration_days": null,
                    "comment": null
                }
            ]
            """.trimIndent()
        )

        val item = api().getActivity(areaIds = listOf("1"), days = 7).single()

        Assert.assertNull(item.placeName)
        Assert.assertNull(item.osmUserId)
        Assert.assertNull(item.osmUserName)
        Assert.assertNull(item.osmUserTip)
        Assert.assertNull(item.image)
        Assert.assertNull(item.durationDays)
        Assert.assertNull(item.comment)
    }

    private companion object {
        const val ACTIVITY = """
            [
                {
                    "type": "place_boosted",
                    "place_id": 38625,
                    "place_name": "Example Cafe",
                    "osm_user_id": 12345,
                    "osm_user_name": "alice",
                    "osm_user_tip": "lightning:alice@example",
                    "image": "https://api.btcmap.org/og/element/38625",
                    "date": "2026-04-20T12:00:00Z",
                    "duration_days": 30,
                    "comment": "great spot"
                },
                {
                    "type": "place_added",
                    "place_id": 1,
                    "place_name": "New",
                    "date": "2026-04-20T12:00:00Z"
                }
            ]
        """
    }
}
