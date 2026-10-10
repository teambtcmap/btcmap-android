package org.btcmap.note

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.btcmap.api.Api
import org.btcmap.db.Database
import org.btcmap.db.table.note.Note
import org.btcmap.settings.Settings
import org.btcmap.util.toUrl
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class NotesTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private fun settings(db: Database) = Settings(dbProvider = { db }, legacyValues = { emptyMap() })

    private fun api() = Api(
        httpClient = HttpClient(CIO),
        baseUrl = { serverRule.server.url("/").toString().toUrl() },
    )

    private fun cached(
        id: Long,
        text: String = "cached",
        public: Boolean = false,
        icon: String = "notes",
    ) = Note(
        id = id,
        lat = 1.0,
        lon = 2.0,
        text = text,
        icon = icon,
        public = public,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
    )

    private fun noteJson(
        id: Long,
        text: String,
        public: Boolean = false,
        icon: String = "notes",
        updatedAt: String = "2026-10-07T12:00:00Z",
        deletedAt: String? = null,
    ): String {
        val deleted = if (deletedAt == null) "" else ",\"deleted_at\":\"$deletedAt\""
        return """
        {
            "id": $id,
            "lat": 1.0,
            "lon": 2.0,
            "text": "$text",
            "icon": "$icon",
            "public": $public,
            "author": { "id": 17, "name": "satoshi" },
            "created_at": "2026-10-07T12:00:00Z",
            "updated_at": "$updatedAt"$deleted
        }
        """.trimIndent()
    }

    private fun enqueue(body: String) {
        serverRule.server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    @Test
    fun sync_upsertsTheServerNotesWithoutDroppingOthers() = runTest {
        val db = org.btcmap.db.testDatabase()
        val settings = settings(db)
        settings.setAuthTokenForTesting("token-1")
        // A note the server does not return in this delta page stays cached.
        db.note.insert(listOf(cached(id = 99, text = "keep")))

        enqueue("[${noteJson(id = 7, text = "ATM inside", icon = "star")}]")

        val changed = Notes.sync(api(), db, settings)

        Assert.assertTrue(changed)
        val rows = db.note.selectAll()
        Assert.assertEquals(setOf(7L, 99L), rows.map { it.id }.toSet())
        Assert.assertEquals("ATM inside", rows.first { it.id == 7L }.text)
        Assert.assertEquals("star", rows.first { it.id == 7L }.icon)
    }

    @Test
    fun sync_appliesTombstones() = runTest {
        val db = org.btcmap.db.testDatabase()
        val settings = settings(db)
        settings.setAuthTokenForTesting("token-1")
        db.note.insert(
            listOf(
                Note(
                    id = 7,
                    lat = 1.0,
                    lon = 2.0,
                    text = "ATM inside",
                    icon = "notes",
                    public = false,
                    createdAt = Instant.parse("2026-10-07T12:00:00Z"),
                    updatedAt = Instant.parse("2026-10-07T12:00:00Z"),
                ),
            ),
        )

        enqueue(
            "[${noteJson(
                id = 7,
                text = "ATM inside",
                updatedAt = "2026-10-08T00:00:00Z",
                deletedAt = "2026-10-08T00:00:00Z",
            )}]",
        )

        val changed = Notes.sync(api(), db, settings)

        Assert.assertTrue(changed)
        // The deletion is a tombstone: hidden from reads, kept for the cursor.
        Assert.assertTrue(db.note.selectAll().isEmpty())
        Assert.assertEquals(0L, db.note.selectCount())
        Assert.assertEquals(1L, db.note.selectCount(includeDeleted = true))
    }

    @Test
    fun sync_returnsFalseWhenTheDeltaIsEmpty() = runTest {
        val db = org.btcmap.db.testDatabase()
        val settings = settings(db)
        settings.setAuthTokenForTesting("token-1")
        db.note.insert(
            listOf(
                Note(
                    id = 7,
                    lat = 1.0,
                    lon = 2.0,
                    text = "ATM inside",
                    icon = "notes",
                    public = false,
                    createdAt = Instant.parse("2026-10-07T12:00:00Z"),
                    updatedAt = Instant.parse("2026-10-07T12:00:00Z"),
                ),
            ),
        )

        enqueue("[]")

        Assert.assertFalse(Notes.sync(api(), db, settings))
        // The cursor is the newest cached note, so the server is only asked for
        // what changed after it.
        val request = serverRule.server.takeRequest()
        Assert.assertEquals(
            "2026-10-07T12:00:00Z",
            request.url.queryParameter("updated_since"),
        )
        Assert.assertEquals("true", request.url.queryParameter("include_deleted"))
    }

    @Test
    fun sync_isANoOpWhenSignedOut() = runTest {
        val db = org.btcmap.db.testDatabase()
        val settings = settings(db)
        db.note.insert(listOf(cached(id = 1, text = "keep")))

        Assert.assertFalse(Notes.sync(api(), db, settings))

        Assert.assertEquals(0, serverRule.server.requestCount)
        Assert.assertEquals(1L, db.note.selectCount())
    }

    @Test
    fun updateVisibility_updatesTheServerAndTheCache() = runTest {
        val db = org.btcmap.db.testDatabase()
        db.note.insert(listOf(cached(id = 7, public = false)))

        enqueue(noteJson(id = 7, text = "cached", public = true))

        Notes.updateVisibility(api(), db, id = 7L, public = true)

        Assert.assertTrue(db.note.selectAll().single().public)
    }

    @Test
    fun updateText_updatesTheServerAndTheCache() = runTest {
        val db = org.btcmap.db.testDatabase()
        db.note.insert(listOf(cached(id = 7, text = "old")))

        enqueue(noteJson(id = 7, text = "new"))

        Notes.updateText(api(), db, id = 7L, text = "new")

        Assert.assertEquals("new", db.note.selectAll().single().text)
        Assert.assertEquals(1, serverRule.server.requestCount)
    }

    @Test
    fun updateIcon_updatesTheServerAndTheCache() = runTest {
        val db = org.btcmap.db.testDatabase()
        db.note.insert(listOf(cached(id = 7, icon = "notes")))

        enqueue(noteJson(id = 7, text = "cached", icon = "star"))

        Notes.updateIcon(api(), db, id = 7L, icon = "star")

        Assert.assertEquals("star", db.note.selectAll().single().icon)
        Assert.assertEquals(1, serverRule.server.requestCount)
    }

    @Test
    fun delete_removesTheCachedNote() = runTest {
        val db = org.btcmap.db.testDatabase()
        db.note.insert(listOf(cached(id = 7)))

        enqueue(noteJson(id = 7, text = "cached"))

        Notes.delete(api(), db, id = 7L)

        Assert.assertEquals(0L, db.note.selectCount())
    }
}
