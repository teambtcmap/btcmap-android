package org.btcmap.db.table.comment

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class CommentQueriesTest {

    private fun createDatabase(): Database {
        return Database(BundledSQLiteDriver(), ":memory:")
    }

    @Test
    fun insert_and_selectByPlaceId() {
        val db = createDatabase()
        val comment = Comment(
            id = 1L,
            placeId = 100L,
            comment = "Great place!",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )

        db.comment.insert(listOf(comment))
        val results = db.comment.selectByPlaceId(100L)

        Assert.assertEquals(1, results.size)
        Assert.assertEquals(1L, results[0].id)
        Assert.assertEquals(100L, results[0].placeId)
        Assert.assertEquals("Great place!", results[0].comment)
    }

    @Test
    fun insert_replacesAnExistingComment() {
        val db = createDatabase()
        val original = Comment(
            id = 1L,
            placeId = 100L,
            comment = "First",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )
        val edited = Comment(
            id = 1L,
            placeId = 100L,
            comment = "Edited",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-02T10:00:00Z"),
        )

        db.comment.insert(listOf(original))
        db.comment.insert(listOf(edited))

        val results = db.comment.selectByPlaceId(100L)
        Assert.assertEquals(1, results.size)
        Assert.assertEquals("Edited", results[0].comment)
        Assert.assertEquals(ZonedDateTime.parse("2024-01-02T10:00:00Z"), results[0].updatedAt)
    }

    @Test
    fun insert_isIdempotentWhenARowIsFetchedTwice() {
        // The delta sync can hand out the same comment again, for example when
        // it was hidden on the first fetch and published on the next.
        val db = createDatabase()
        val comment = Comment(
            id = 5L,
            placeId = 100L,
            comment = "gm",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-02T10:00:00Z"),
        )

        db.comment.insert(listOf(comment))
        db.comment.insert(listOf(comment))

        val results = db.comment.selectByPlaceId(100L)
        Assert.assertEquals(1, results.size)
        Assert.assertEquals("gm", results[0].comment)
    }

    @Test
    fun selectByPlaceId_returnsEmptyListWhenNoComments() {
        val db = createDatabase()

        val results = db.comment.selectByPlaceId(999L)

        Assert.assertTrue(results.isEmpty())
    }

    @Test
    fun selectByPlaceId_ordersByCreatedAtDesc() {
        val db = createDatabase()
        val comment1 = Comment(
            id = 1L,
            placeId = 1L,
            comment = "First",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )
        val comment2 = Comment(
            id = 2L,
            placeId = 1L,
            comment = "Second",
            createdAt = ZonedDateTime.parse("2024-01-02T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-02T10:00:00Z"),
        )

        db.comment.insert(listOf(comment1, comment2))
        val results = db.comment.selectByPlaceId(1L)

        Assert.assertEquals("Second", results[0].comment)
        Assert.assertEquals("First", results[1].comment)
    }

    @Test
    fun selectByPlaceId_breaksCreatedAtTiesByIdDesc() {
        val db = createDatabase()
        val sameTime = ZonedDateTime.parse("2024-01-01T10:00:00Z")
        val lowerId = Comment(
            id = 1L,
            placeId = 1L,
            comment = "Lower id",
            createdAt = sameTime,
            updatedAt = sameTime,
        )
        val higherId = Comment(
            id = 2L,
            placeId = 1L,
            comment = "Higher id",
            createdAt = sameTime,
            updatedAt = sameTime,
        )

        // Inserted ascending: without the id tie-break the query would return
        // them in insertion order and this would come back 1, 2.
        db.comment.insert(listOf(lowerId, higherId))
        val results = db.comment.selectByPlaceId(1L)

        Assert.assertEquals(listOf("Higher id", "Lower id"), results.map { it.comment })
    }

    @Test
    fun selectMaxUpdatedAt_returnsNullWhenEmpty() {
        val db = createDatabase()

        val result = db.comment.selectMaxUpdatedAt()

        Assert.assertNull(result)
    }

    @Test
    fun selectMaxUpdatedAt_returnsMaxDate() {
        val db = createDatabase()
        val comment1 = Comment(
            id = 1L,
            placeId = 1L,
            comment = "First",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )
        val comment2 = Comment(
            id = 2L,
            placeId = 1L,
            comment = "Second",
            createdAt = ZonedDateTime.parse("2024-01-02T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-03T10:00:00Z"),
        )

        db.comment.insert(listOf(comment1, comment2))
        val result = db.comment.selectMaxUpdatedAt()

        Assert.assertEquals(ZonedDateTime.parse("2024-01-03T10:00:00Z"), result)
    }

    @Test
    fun selectMaxUpdatedAt_comparesByInstantNotByText() {
        val db = createDatabase()
        // ZonedDateTime.toString() drops a zero fraction, so the earlier
        // "2024-01-01T10:00Z" sorts after "2024-01-01T10:00:00.500Z" as text.
        val earlier = Comment(
            id = 1L,
            placeId = 1L,
            comment = "Earlier",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )
        val later = Comment(
            id = 2L,
            placeId = 1L,
            comment = "Later",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00.500Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00.500Z"),
        )

        db.comment.insert(listOf(earlier, later))

        Assert.assertEquals(
            ZonedDateTime.parse("2024-01-01T10:00:00.500Z"),
            db.comment.selectMaxUpdatedAt(),
        )
    }

    @Test
    fun selectByPlaceId_ordersByInstantNotByText() {
        val db = createDatabase()
        val earlier = Comment(
            id = 1L,
            placeId = 1L,
            comment = "Earlier",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )
        val later = Comment(
            id = 2L,
            placeId = 1L,
            comment = "Later",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00.500Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00.500Z"),
        )

        db.comment.insert(listOf(earlier, later))
        val results = db.comment.selectByPlaceId(1L)

        Assert.assertEquals("Later", results[0].comment)
        Assert.assertEquals("Earlier", results[1].comment)
    }

    @Test
    fun deleteById_removesComment() {
        val db = createDatabase()
        val comment = Comment(
            id = 1L,
            placeId = 100L,
            comment = "To delete",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )

        db.comment.insert(listOf(comment))
        Assert.assertEquals(1, db.comment.selectByPlaceId(100L).size)

        db.comment.deleteById(1L)
        Assert.assertTrue(db.comment.selectByPlaceId(100L).isEmpty())
    }

    @Test
    fun deleteById_doesNothingWhenIdNotFound() {
        val db = createDatabase()
        val comment = Comment(
            id = 1L,
            placeId = 100L,
            comment = "Test",
            createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
            updatedAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
        )

        db.comment.insert(listOf(comment))
        db.comment.deleteById(999L)

        Assert.assertEquals(1, db.comment.selectByPlaceId(100L).size)
    }

    @Test
    fun insert_keepsTombstoneButHidesItFromSelects() {
        val db = createDatabase()
        val deletedAt = ZonedDateTime.parse("2024-01-02T10:00:00Z")
        db.comment.insert(
            listOf(
                Comment(
                    id = 1L,
                    placeId = 100L,
                    comment = "gone",
                    createdAt = ZonedDateTime.parse("2024-01-01T10:00:00Z"),
                    updatedAt = deletedAt,
                    deletedAt = deletedAt,
                )
            )
        )

        Assert.assertTrue(db.comment.selectByPlaceId(100L).isEmpty())
        // The tombstone must still advance the delta cursor and stay on disk.
        Assert.assertEquals(deletedAt, db.comment.selectMaxUpdatedAt())
        Assert.assertEquals(1L, physicalRowCount(db, "comment"))
    }

    private fun physicalRowCount(db: Database, table: String): Long {
        db.conn.prepare("SELECT count(*) FROM $table;").use {
            it.step()
            return it.getLong(0)
        }
    }
}