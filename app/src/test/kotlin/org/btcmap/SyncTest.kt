package org.btcmap

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
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
            httpClient = okhttp3.OkHttpClient(),
            url = serverRule.server.url("/")
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
    }

    @Test
    fun syncComments_withDeletedComment() = runTest {
        val db = createDatabase()
        val api = createApi()

        db.comment.insert(
            listOf(
                org.btcmap.db.table.Comment(
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
        val comments = db.comment.selectByPlaceId(100)
        Assert.assertTrue(comments.isEmpty())
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
                org.btcmap.db.table.Event(
                    id = 999,
                    lat = 40.7128,
                    lon = -74.0060,
                    name = "Old Event",
                    website = "https://example.com".toHttpUrl(),
                    startsAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
                    endsAt = null
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