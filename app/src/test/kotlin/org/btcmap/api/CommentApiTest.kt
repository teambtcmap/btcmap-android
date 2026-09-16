package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CommentApiTest : ApiTestBase() {
    @Test
    fun getComments_sendsParametersAndParsesAllFields() = runTest {
        enqueueJson(COMMENTS)

        val updatedSince = ZonedDateTime.parse("2026-01-01T00:00:00Z")
        val comments = api().getComments(updatedSince = updatedSince, limit = 100)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/place-comments", request.url.encodedPath)
        Assert.assertEquals("100", request.url.queryParameter("limit"))
        Assert.assertEquals("true", request.url.queryParameter("include_deleted"))
        Assert.assertEquals(
            updatedSince.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            request.url.queryParameter("updated_since"),
        )

        Assert.assertEquals(2, comments.size)
        val comment = comments[0]
        Assert.assertEquals(1L, comment.id)
        Assert.assertEquals(100L, comment.elementId)
        Assert.assertEquals("Great coffee!", comment.comment)
        Assert.assertEquals("2026-01-01T10:00:00Z", comment.createdAt)
        Assert.assertEquals("2026-01-02T10:00:00Z", comment.updatedAt)
        Assert.assertEquals("2026-01-03T10:00:00Z", comment.deletedAt)
    }

    @Test
    fun getComments_omitsUpdatedSinceAndParsesBlankFieldsAsNull() = runTest {
        enqueueJson(
            """
            [
                {
                    "id": 1,
                    "place_id": null,
                    "text": "  ",
                    "created_at": null,
                    "updated_at": "2026-01-02T10:00:00Z",
                    "deleted_at": null
                }
            ]
            """.trimIndent()
        )

        val comment = api().getComments(updatedSince = null, limit = 10).single()

        val request = takeRequest()
        Assert.assertNull(request.url.queryParameter("updated_since"))
        Assert.assertNull(comment.elementId)
        Assert.assertNull(comment.comment)
        Assert.assertNull(comment.createdAt)
        Assert.assertNull(comment.deletedAt)
    }

    @Test
    fun getCommentQuote_parsesQuote() = runTest {
        enqueueJson("""{"quote_sat":500}""")

        val quote = api().getCommentQuote()

        val request = takeRequest()
        Assert.assertEquals("/v4/place-comments/quote", request.url.encodedPath)
        Assert.assertEquals(500L, quote.quoteSat)
    }

    @Test
    fun addComment_postsAndParsesInvoice() = runTest {
        enqueueJson("""{"invoice_id":"inv-2","invoice":"lnbc2..."}""")

        val response = api().addComment(placeId = 12345, comment = "Amazing view!")

        val request = takeRequest()
        Assert.assertEquals("POST", request.method)
        Assert.assertEquals("/v4/place-comments", request.url.encodedPath)
        Assert.assertEquals(
            """{"place_id":"12345","comment":"Amazing view!"}""",
            request.jsonBody(),
        )
        Assert.assertEquals("inv-2", response.invoiceId)
        Assert.assertEquals("lnbc2...", response.invoice)
    }

    private companion object {
        const val COMMENTS = """
            [
                {
                    "id": 1,
                    "place_id": 100,
                    "text": "Great coffee!",
                    "created_at": "2026-01-01T10:00:00Z",
                    "updated_at": "2026-01-02T10:00:00Z",
                    "deleted_at": "2026-01-03T10:00:00Z"
                },
                {
                    "id": 2,
                    "place_id": 100,
                    "text": "Updated",
                    "created_at": "2026-01-01T10:00:00Z",
                    "updated_at": "2026-01-02T10:00:00Z",
                    "deleted_at": null
                }
            ]
        """
    }
}
