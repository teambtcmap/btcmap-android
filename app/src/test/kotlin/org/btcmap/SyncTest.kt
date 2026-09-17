package org.btcmap

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.btcmap.api.Api
import org.btcmap.db.table.comment.Comment
import org.btcmap.db.table.event.Event
import org.junit.Rule

class SyncTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun createDatabase(): Database {
        return Database(BundledSQLiteDriver(), ":memory:")
    }

    private fun createApi(): Api {
        return Api(
            httpClient = OkHttpClient(),
            baseUrl = { serverRule.server.url("/") }
        )
    }

    @Test
    fun syncPlaces_success() = runTest {
        val db = createDatabase()
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body(
                """
                [
                    {
                        "id": 1,
                        "lat": 40.7128,
                        "lon": -74.0060,
                        "icon": "coffee",
                        "name": "Coffee Shop",
                        "localized_name": null,
                        "updated_at": "2024-01-01T10:00:00Z",
                        "deleted_at": null,
                        "required_app_url": null,
                        "boosted_until": null,
                        "verified_at": null,
                        "address": null,
                        "opening_hours": null,
                        "localized_opening_hours": null,
                        "website": null,
                        "phone": null,
                        "email": null,
                        "twitter": null,
                        "facebook": null,
                        "instagram": null,
                        "line": null,
                        "comments": 0,
                        "telegram": null
                    }
                ]
            """.trimIndent()
            ).build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncPlaces()

        Assert.assertEquals(1L, report.rowsAffected)
        Assert.assertNotNull(db.place.selectById(1L))
        Assert.assertEquals("Coffee Shop", db.place.selectById(1L)!!.name)
    }

    @Test
    fun syncPlaces_emptyResponse() = runTest {
        val db = createDatabase()
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body("[]").build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncPlaces()

        Assert.assertEquals(0L, report.rowsAffected)
        Assert.assertEquals(0L, db.place.selectCount())
    }

    @Test
    fun syncPlaces_withDeletedPlace() = runTest {
        val db = createDatabase()
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body(
                """
                [
                    {
                        "id": 1,
                        "lat": 40.7128,
                        "lon": -74.0060,
                        "icon": "coffee",
                        "name": "Coffee Shop",
                        "localized_name": null,
                        "updated_at": "2024-01-01T10:00:00Z",
                        "deleted_at": "2024-01-02T10:00:00Z",
                        "required_app_url": null,
                        "boosted_until": null,
                        "verified_at": null,
                        "address": null,
                        "opening_hours": null,
                        "localized_opening_hours": null,
                        "website": null,
                        "phone": null,
                        "email": null,
                        "twitter": null,
                        "facebook": null,
                        "instagram": null,
                        "line": null,
                        "comments": 0,
                        "telegram": null
                    }
                ]
            """.trimIndent()
            ).build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncPlaces()

        Assert.assertEquals(1L, report.rowsAffected)
        Assert.assertNull(db.place.selectById(1L))
    }

    @Test
    fun syncPlaces_withMultiplePlaces() = runTest {
        val db = createDatabase()
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body(
                """
                [
                    {
                        "id": 1,
                        "lat": 40.7128,
                        "lon": -74.0060,
                        "icon": "coffee",
                        "name": "Place 1",
                        "localized_name": null,
                        "updated_at": "2024-01-01T10:00:00Z",
                        "deleted_at": null,
                        "required_app_url": null,
                        "boosted_until": null,
                        "verified_at": null,
                        "address": null,
                        "opening_hours": null,
                        "localized_opening_hours": null,
                        "website": null,
                        "phone": null,
                        "email": null,
                        "twitter": null,
                        "facebook": null,
                        "instagram": null,
                        "line": null,
                        "comments": 0,
                        "telegram": null
                    },
                    {
                        "id": 2,
                        "lat": 40.7129,
                        "lon": -74.0061,
                        "icon": "restaurant",
                        "name": "Place 2",
                        "localized_name": null,
                        "updated_at": "2024-01-02T10:00:00Z",
                        "deleted_at": null,
                        "required_app_url": null,
                        "boosted_until": null,
                        "verified_at": null,
                        "address": null,
                        "opening_hours": null,
                        "localized_opening_hours": null,
                        "website": null,
                        "phone": null,
                        "email": null,
                        "twitter": null,
                        "facebook": null,
                        "instagram": null,
                        "line": null,
                        "comments": 0,
                        "telegram": null
                    }
                ]
            """.trimIndent()
            ).build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncPlaces()

        Assert.assertEquals(2L, report.rowsAffected)
        Assert.assertEquals(2L, db.place.selectCount())
    }

    @Test
    fun syncComments_success() = runTest {
        val db = createDatabase()
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body(
                """
                [
                    {
                        "id": 1,
                        "place_id": 100,
                        "text": "Great coffee!",
                        "created_at": "2024-01-01T10:00:00Z",
                        "updated_at": "2024-01-01T10:00:00Z",
                        "deleted_at": null
                    }
                ]
            """.trimIndent()
            ).build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncComments()

        Assert.assertEquals(1L, report.rowsAffected)
        Assert.assertEquals(1L, report.upserted)
        val comments = db.comment.selectByPlaceId(100)
        Assert.assertEquals(1, comments.size)
        Assert.assertEquals("Great coffee!", comments[0].comment)
    }

    @Test
    fun syncComments_emptyResponse() = runTest {
        val db = createDatabase()
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body("[]").build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncComments()

        Assert.assertEquals(0L, report.rowsAffected)
        Assert.assertEquals(0L, report.upserted)
    }

    @Test
    fun syncComments_withDeletedComment() = runTest {
        val db = createDatabase()
        val api = createApi()

        db.comment.insert(
            listOf(
                Comment(
                    id = 1,
                    placeId = 100,
                    comment = "Old comment",
                    createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
                    updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z")
                )
            )
        )

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body(
                """
                [
                    {
                        "id": 1,
                        "place_id": 100,
                        "text": "Updated comment",
                        "created_at": "2024-01-01T10:00:00Z",
                        "updated_at": "2024-01-02T10:00:00Z",
                        "deleted_at": "2024-01-02T10:00:00Z"
                    }
                ]
            """.trimIndent()
            ).build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncComments()

        Assert.assertEquals(1L, report.rowsAffected)
        Assert.assertEquals(0L, report.upserted)
        val comments = db.comment.selectByPlaceId(100)
        Assert.assertTrue(comments.isEmpty())
    }

    @Test
    fun syncComments_storesACommentOnceItIsPublished() = runTest {
        val db = createDatabase()
        val api = createApi()

        fun response(deletedAt: String, updatedAt: String): MockResponse =
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(
                    """
                    [
                        {
                            "id": 1,
                            "place_id": 100,
                            "text": "gm",
                            "created_at": "2024-01-01T10:00:00Z",
                            "updated_at": "$updatedAt",
                            "deleted_at": $deletedAt
                        }
                    ]
                    """.trimIndent()
                ).build()

        // First the server hands out the comment while it is still hidden, then
        // the same id once the payment published it.
        serverRule.server.enqueue(response("\"2024-01-01T10:05:00Z\"", "2024-01-01T10:05:00Z"))
        serverRule.server.enqueue(response("null", "2024-01-01T10:10:00Z"))

        val sync = Sync(api, db)

        val hidden = sync.syncComments()
        Assert.assertEquals(1L, hidden.rowsAffected)
        Assert.assertEquals("a hidden comment must not count as stored", 0L, hidden.upserted)
        Assert.assertTrue(db.comment.selectByPlaceId(100).isEmpty())

        val published = sync.syncComments()
        Assert.assertEquals(1L, published.rowsAffected)
        Assert.assertEquals(1L, published.upserted)
        Assert.assertEquals("gm", db.comment.selectByPlaceId(100).single().comment)
    }

    @Test
    fun syncComments_widensBatchWhenAllRowsShareTimestamp() = runTest {
        val db = createDatabase()
        val api = createApi()

        data class Row(val id: Long, val updatedAt: String)
        val tieTimestamp = "2024-01-01T00:00:00Z"
        val nextTimestamp = "2024-01-02T00:00:00Z"
        val rows = (1L..1500L).map { Row(it, tieTimestamp) } + Row(1501L, nextTimestamp)
        val requestedLimits = java.util.Collections.synchronizedList(mutableListOf<String>())

        serverRule.server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val limit = request.url.queryParameter("limit")!!.toLong()
                requestedLimits += limit.toString()
                val since = request.url.queryParameter("updated_since")
                    ?.let { ZonedDateTime.parse(it) }
                val page = rows
                    .filter { since == null || ZonedDateTime.parse(it.updatedAt) > since }
                    .sortedWith(compareBy({ it.updatedAt }, { it.id }))
                    .take(limit.toInt())
                val body = buildString {
                    append("[")
                    page.forEachIndexed { index, row ->
                        if (index > 0) append(",")
                        append(
                            """{"id":${row.id},"place_id":100,"text":"c","created_at":"${row.updatedAt}","updated_at":"${row.updatedAt}","deleted_at":null}"""
                        )
                    }
                    append("]")
                }
                return MockResponse.Builder()
                    .addHeader("Content-Type", "application/json")
                    .body(body)
                    .build()
            }
        }

        val report = Sync(api, db).syncComments()

        Assert.assertEquals(listOf("1000", "2000"), requestedLimits.toList())
        Assert.assertEquals(1501L, report.rowsAffected)
        Assert.assertEquals(1501, db.comment.selectByPlaceId(100).size)
    }

    @Test
    fun syncEvents_success() = runTest {
        val db = createDatabase()
        val api = createApi()

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body(
                """
                [
                    {
                        "id": 1,
                        "lat": 40.7128,
                        "lon": -74.0060,
                        "name": "Bitcoin Conference",
                        "website": "https://example.com",
                        "starts_at": "2024-06-01T09:00:00Z",
                        "ends_at": "2024-06-03T18:00:00Z"
                    }
                ]
            """.trimIndent()
            ).build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncEvents()

        Assert.assertEquals(1L, report.rowsAffected)
        val events = db.event.selectAll()
        Assert.assertEquals(1, events.size)
        Assert.assertEquals("Bitcoin Conference", events[0].name)
    }

    @Test
    fun syncEvents_emptyResponse() = runTest {
        val db = createDatabase()
        val api = createApi()

        db.event.insert(
            listOf(
                Event(
                    id = 999,
                    areaId = null,
                    lat = 40.7128,
                    lon = -74.0060,
                    name = "Old Event",
                    website = "https://example.com".toHttpUrl(),
                    startsAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
                    endsAt = null,
                )
            )
        )

        val response = MockResponse.Builder()
            .addHeader("Content-Type", "application/json")
            .body("[]").build()

        serverRule.server.enqueue(response)

        val sync = Sync(api, db)
        val report = sync.syncEvents()

        Assert.assertEquals(0L, report.rowsAffected)
        val events = db.event.selectAll()
        Assert.assertTrue(events.isEmpty())
    }
}