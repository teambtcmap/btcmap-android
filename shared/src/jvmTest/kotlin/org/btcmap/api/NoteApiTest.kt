package org.btcmap.api

import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Assert
import org.junit.Test

class NoteApiTest : ApiTestBase() {

    private fun noteJson(
        id: Long = 7,
        public: Boolean = true,
        icon: String = "notes",
        deletedAt: String? = null,
    ): String {
        val deleted = if (deletedAt == null) "" else ",\"deleted_at\":\"$deletedAt\""
        return """
        {
            "id": $id,
            "lat": 53.55,
            "lon": 9.99,
            "text": "ATM is inside",
            "icon": "$icon",
            "public": $public,
            "author": { "id": 17, "name": "satoshi" },
            "created_at": "2026-10-07T12:00:00Z",
            "updated_at": "2026-10-07T12:00:00Z"$deleted
        }
        """.trimIndent()
    }

    @Test
    fun createNote_postsCoordinatesTextIconAndVisibility() = runTest {
        enqueueJson(noteJson(icon = "star"))

        val note = api().createNote(
            lat = 53.55,
            lon = 9.99,
            text = "ATM is inside",
            icon = "star",
            public = true,
        )

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/notes", request.url.encodedPath)
        Assert.assertEquals(
            """{"lat":53.55,"lon":9.99,"text":"ATM is inside","icon":"star","public":true}""",
            request.jsonBody(),
        )
        Assert.assertEquals(7L, note.id)
        Assert.assertEquals("star", note.icon)
        Assert.assertTrue(note.public)
        Assert.assertEquals(17L, note.authorId)
        Assert.assertEquals("satoshi", note.authorName)
    }

    @Test
    fun getMyNotes_getsTheOwnerScopedList() = runTest {
        enqueueJson(
            "[${noteJson(id = 1, public = false, icon = "star")}," +
                "${noteJson(id = 2, public = true)}]",
        )

        val notes = api().getMyNotes(updatedSince = null, includeDeleted = true, limit = 500)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/users/me/notes", request.url.encodedPath)
        Assert.assertEquals("500", request.url.queryParameter("limit"))
        Assert.assertEquals("true", request.url.queryParameter("include_deleted"))
        Assert.assertNull(request.url.queryParameter("updated_since"))
        Assert.assertEquals(listOf(1L, 2L), notes.map { it.id })
        Assert.assertEquals(listOf("star", "notes"), notes.map { it.icon })
        Assert.assertFalse(notes.first().public)
        Assert.assertTrue(notes.last().public)
    }

    @Test
    fun getMyNotes_passesTheDeltaCursorAndParsesTombstones() = runTest {
        enqueueJson("[${noteJson(id = 3, deletedAt = "2026-10-08T00:00:00Z")}]")

        val notes = api().getMyNotes(
            updatedSince = Instant.parse("2026-10-07T12:00:00Z"),
            includeDeleted = false,
            limit = 100,
        )

        val request = takeRequest()
        Assert.assertEquals(
            "2026-10-07T12:00:00Z",
            request.url.queryParameter("updated_since"),
        )
        Assert.assertNull(request.url.queryParameter("include_deleted"))
        Assert.assertEquals("2026-10-08T00:00:00Z", notes.single().deletedAt)
    }

    @Test
    fun updateNote_patchesVisibility() = runTest {
        enqueueJson(noteJson(public = true))

        val note = api().updateNote(7L, public = true)

        val request = takeRequest()
        Assert.assertEquals("PATCH", request.method)
        Assert.assertEquals("/v4/notes/7", request.url.encodedPath)
        Assert.assertEquals("""{"public":true}""", request.jsonBody())
        Assert.assertTrue(note.public)
    }

    @Test
    fun updateNoteText_patchesTheBody() = runTest {
        enqueueJson(noteJson())

        val note = api().updateNoteText(7L, text = "ATM moved to the back")

        val request = takeRequest()
        Assert.assertEquals("PATCH", request.method)
        Assert.assertEquals("/v4/notes/7", request.url.encodedPath)
        Assert.assertEquals("""{"text":"ATM moved to the back"}""", request.jsonBody())
        Assert.assertEquals(7L, note.id)
    }

    @Test
    fun updateNoteIcon_patchesTheIcon() = runTest {
        enqueueJson(noteJson(icon = "star"))

        val note = api().updateNoteIcon(7L, icon = "star")

        val request = takeRequest()
        Assert.assertEquals("PATCH", request.method)
        Assert.assertEquals("/v4/notes/7", request.url.encodedPath)
        Assert.assertEquals("""{"icon":"star"}""", request.jsonBody())
        Assert.assertEquals("star", note.icon)
    }

    @Test
    fun deleteNote_deletesById() = runTest {
        enqueueJson(noteJson())

        api().deleteNote(7L)

        val request = takeRequest()
        Assert.assertEquals("DELETE", request.method)
        Assert.assertEquals("/v4/notes/7", request.url.encodedPath)
    }
}
