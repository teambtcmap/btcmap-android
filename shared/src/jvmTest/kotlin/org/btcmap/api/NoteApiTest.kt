package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class NoteApiTest : ApiTestBase() {

    private fun noteJson(id: Long = 7, public: Boolean = true): String =
        """
        {
            "id": $id,
            "lat": 53.55,
            "lon": 9.99,
            "text": "ATM is inside",
            "public": $public,
            "author": { "id": 17, "name": "satoshi" },
            "created_at": "2026-10-07T12:00:00Z",
            "updated_at": "2026-10-07T12:00:00Z"
        }
        """.trimIndent()

    @Test
    fun createNote_postsCoordinatesTextAndVisibility() = runTest {
        enqueueJson(noteJson())

        val note = api().createNote(lat = 53.55, lon = 9.99, text = "ATM is inside", public = true)

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/notes", request.url.encodedPath)
        Assert.assertEquals(
            """{"lat":53.55,"lon":9.99,"text":"ATM is inside","public":true}""",
            request.jsonBody(),
        )
        Assert.assertEquals(7L, note.id)
        Assert.assertTrue(note.public)
        Assert.assertEquals(17L, note.authorId)
        Assert.assertEquals("satoshi", note.authorName)
    }

    @Test
    fun getMyNotes_getsTheOwnerScopedList() = runTest {
        enqueueJson("[${noteJson(id = 1, public = false)},${noteJson(id = 2, public = true)}]")

        val notes = api().getMyNotes()

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/users/me/notes", request.url.encodedPath)
        Assert.assertEquals(listOf(1L, 2L), notes.map { it.id })
        Assert.assertFalse(notes.first().public)
        Assert.assertTrue(notes.last().public)
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
    fun deleteNote_deletesById() = runTest {
        enqueueJson(noteJson())

        api().deleteNote(7L)

        val request = takeRequest()
        Assert.assertEquals("DELETE", request.method)
        Assert.assertEquals("/v4/notes/7", request.url.encodedPath)
    }
}
