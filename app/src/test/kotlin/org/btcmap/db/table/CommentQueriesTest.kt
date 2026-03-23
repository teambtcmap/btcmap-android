package org.btcmap.db.table

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
}