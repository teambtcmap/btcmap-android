package org.btcmap.bundle

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.test.runTest
import org.btcmap.db.Database
import org.btcmap.db.table.comment.Comment
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.StringReader
import java.time.ZonedDateTime

class BundledCommentsTest {
    private fun reader(json: String) = JsonReader(StringReader(json))

    private fun createDatabase(): Database = Database(BundledSQLiteDriver(), ":memory:")

    private fun comment(id: Long, deletedAt: ZonedDateTime? = null) = Comment(
        id = id,
        placeId = 1L,
        comment = "Comment $id",
        createdAt = ZonedDateTime.parse("2026-01-01T00:00:00Z"),
        updatedAt = ZonedDateTime.parse("2026-01-02T00:00:00Z"),
        deletedAt = deletedAt,
    )

    /** A snapshot with [count] minimal but complete comments, ids 1..count. */
    private fun snapshotJson(count: Int) = buildString {
        append('[')
        for (id in 1..count) {
            if (id > 1) append(',')
            append(
                """{"id":$id,"place_id":1,"text":"Comment $id",""" +
                    """"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-03-01T12:00:00Z"}""",
            )
        }
        append(']')
    }

    // --- parser -----------------------------------------------------------------

    @Test
    fun readBundledComment_parsesSeededFields() {
        val json = """
            {
              "id": 42,
              "place_id": 7,
              "text": "Bitcoin accepted here",
              "created_at": "2026-01-01T00:00:00Z",
              "updated_at": "2026-03-01T12:00:00Z",
              "unknown": "ignored"
            }
        """.trimIndent()

        val comment = reader(json).readBundledComment()

        Assert.assertEquals(42L, comment.id)
        Assert.assertEquals(7L, comment.placeId)
        Assert.assertEquals("Bitcoin accepted here", comment.comment)
        Assert.assertEquals(ZonedDateTime.parse("2026-01-01T00:00:00Z"), comment.createdAt)
        Assert.assertEquals(ZonedDateTime.parse("2026-03-01T12:00:00Z"), comment.updatedAt)
        Assert.assertNull(comment.deletedAt)
    }

    @Test
    fun readBundledComment_rejectsMissingRequiredFields() {
        val full = mapOf(
            "place_id" to """"place_id":7""",
            "text" to """"text":"Bitcoin accepted here"""",
            "created_at" to """"created_at":"2026-01-01T00:00:00Z"""",
            "updated_at" to """"updated_at":"2026-03-01T12:00:00Z"""",
        )
        val cases = buildMap {
            put("id", full.values.joinToString(separator = ",", prefix = "{", postfix = "}"))
            full.forEach { (omitted, _) ->
                put(
                    omitted,
                    full.filterKeys { it != omitted }
                        .values.joinToString(separator = ",", prefix = "{\"id\":1,", postfix = "}"),
                )
            }
        }

        cases.forEach { (field, json) ->
            try {
                reader(json).readBundledComment()
                Assert.fail("expected missing '$field' to be rejected")
            } catch (e: IllegalArgumentException) {
                Assert.assertTrue(e.message.orEmpty().contains(field))
            }
        }
    }

    @Test
    fun readBundledComment_rejectsUnparseableUpdatedAt() {
        val json = """
            {
              "id": 1,
              "place_id": 7,
              "text": "Bitcoin accepted here",
              "created_at": "2026-01-01T00:00:00Z",
              "updated_at": "not-a-date"
            }
        """.trimIndent()

        try {
            reader(json).readBundledComment()
            Assert.fail("expected an unparseable 'updated_at' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("updated_at"))
        }
    }

    @Test
    fun readBundledComment_rejectsEmptyText() {
        val json = """
            {
              "id": 1,
              "place_id": 7,
              "text": "",
              "created_at": "2026-01-01T00:00:00Z",
              "updated_at": "2026-03-01T12:00:00Z"
            }
        """.trimIndent()

        try {
            reader(json).readBundledComment()
            Assert.fail("expected an empty 'text' to be rejected")
        } catch (e: IllegalArgumentException) {
            Assert.assertTrue(e.message.orEmpty().contains("text"))
        }
    }

    // --- seeding ----------------------------------------------------------------

    @Test
    fun importFrom_seedsEmptyDatabaseWithBundledRows() = runTest {
        val db = createDatabase()
        val json = """
            [
              {"id":1,"place_id":7,"text":"One","created_at":"2026-01-01T00:00:00Z","updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"place_id":7,"text":"Two","created_at":"2026-02-01T00:00:00Z","updated_at":"2026-04-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledComments.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(2L, result.commentsImported)
        Assert.assertFalse(result.duration.isNegative)
        Assert.assertEquals(2L, db.comment.selectCount())

        val comments = db.comment.selectByPlaceId(7L)
        Assert.assertEquals(2, comments.size)
        Assert.assertEquals("Two", comments[0].comment)
        Assert.assertEquals(ZonedDateTime.parse("2026-04-01T12:00:00Z"), comments[0].updatedAt)
        Assert.assertEquals("One", comments[1].comment)
        Assert.assertNull(comments[1].deletedAt)
    }

    @Test
    fun importFrom_skipsWhenDatabaseAlreadyHasComments() = runTest {
        val db = createDatabase()
        db.comment.insert(listOf(comment(id = 99L)))

        var opened = false
        val result = BundledComments.importFrom(db) {
            opened = true
            snapshotJson(1).byteInputStream()
        }

        Assert.assertEquals(0L, result.commentsImported)
        Assert.assertFalse("snapshot must not be read once the seed is done", opened)
        Assert.assertEquals(1L, db.comment.selectCount())
    }

    @Test
    fun importFrom_skipsWhenDatabaseHoldsOnlyTombstones() = runTest {
        val db = createDatabase()
        db.comment.insert(
            listOf(comment(id = 99L, deletedAt = ZonedDateTime.parse("2026-05-01T00:00:00Z"))),
        )

        val result = BundledComments.importFrom(db) { snapshotJson(2).byteInputStream() }

        Assert.assertEquals(0L, result.commentsImported)
        Assert.assertEquals(1L, db.comment.selectCount(includeDeleted = true))
        Assert.assertTrue(db.comment.selectByPlaceId(1L).isEmpty())
    }

    @Test
    fun importFrom_isIdempotentAcrossRepeatedCalls() = runTest {
        val db = createDatabase()
        BundledComments.importFrom(db) { snapshotJson(2).byteInputStream() }

        val second = BundledComments.importFrom(db) { snapshotJson(5).byteInputStream() }

        Assert.assertEquals(0L, second.commentsImported)
        Assert.assertEquals(2L, db.comment.selectCount())
    }

    @Test
    fun importFrom_emptySnapshotSeedsNothing() = runTest {
        val db = createDatabase()

        val result = BundledComments.importFrom(db) { "[]".byteInputStream() }

        Assert.assertEquals(0L, result.commentsImported)
        Assert.assertEquals(0L, db.comment.selectCount())
    }

    @Test
    fun importFrom_importsEveryRowAcrossBatchBoundaries() = runTest {
        val db = createDatabase()
        val count = BundledComments.BATCH_SIZE + 1

        val result = BundledComments.importFrom(db) { snapshotJson(count).byteInputStream() }

        Assert.assertEquals(count.toLong(), result.commentsImported)
        Assert.assertEquals(count.toLong(), db.comment.selectCount())
        Assert.assertEquals(count, db.comment.selectByPlaceId(1L).size)
    }

    @Test
    fun importFrom_closesTheStream() = runTest {
        val db = createDatabase()
        var closed = false
        val stream = object : ByteArrayInputStream(snapshotJson(1).toByteArray()) {
            override fun close() {
                closed = true
                super.close()
            }
        }

        BundledComments.importFrom(db) { stream }

        Assert.assertTrue("the snapshot stream must be closed", closed)
    }

    // --- failure handling -------------------------------------------------------

    @Test
    fun importFrom_missingAssetIsNotAnError() = runTest {
        val db = createDatabase()

        val result = BundledComments.importFrom(db) { throw FileNotFoundException("no asset") }

        Assert.assertEquals(0L, result.commentsImported)
        Assert.assertEquals(0L, db.comment.selectCount())
    }

    @Test
    fun importFrom_malformedAssetRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        // The first entry is valid, the second is missing its required text.
        val json = """
            [
              {"id":1,"place_id":7,"text":"One","created_at":"2026-01-01T00:00:00Z","updated_at":"2026-03-01T12:00:00Z"},
              {"id":2,"place_id":7,"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-03-01T12:00:00Z"}
            ]
        """.trimIndent()

        val result = BundledComments.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(0L, result.commentsImported)
        Assert.assertEquals("a partial seed must not survive a parse failure", 0L, db.comment.selectCount())
    }

    @Test
    fun importFrom_invalidJsonRollsBackTheWholeSeed() = runTest {
        val db = createDatabase()
        val json = """[{"id":1,"place_id":7,"text":"One"},"""

        val result = BundledComments.importFrom(db) { json.byteInputStream() }

        Assert.assertEquals(0L, result.commentsImported)
        Assert.assertEquals(0L, db.comment.selectCount())
    }

    @Test
    fun importFrom_canBeRetriedAfterAFailedImport() = runTest {
        val db = createDatabase()
        BundledComments.importFrom(db) {
            """[{"id":1,"place_id":7,"text":"One"}]""".byteInputStream()
        }
        Assert.assertEquals(0L, db.comment.selectCount())

        val result = BundledComments.importFrom(db) { snapshotJson(2).byteInputStream() }

        Assert.assertEquals(2L, result.commentsImported)
        Assert.assertEquals(2L, db.comment.selectCount())
    }
}
