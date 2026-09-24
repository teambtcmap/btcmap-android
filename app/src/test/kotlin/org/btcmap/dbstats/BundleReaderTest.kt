package org.btcmap.dbstats

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

class BundleReaderTest {

    @Test
    fun read_countsVisibleAndDeletedRecords() {
        val bytes = """
            [
              {"id": 1, "deleted_at": "2024-01-01T00:00:00Z"},
              {"id": 2},
              {"id": 3, "deleted_at": null}
            ]
        """.trimIndent().toByteArray()

        val stats = BundleReader.read("assets/bundled-places.json") {
            ByteArrayInputStream(bytes)
        }

        Assert.assertNotNull(stats)
        stats!!
        Assert.assertEquals("assets/bundled-places.json", stats.location)
        Assert.assertEquals(bytes.size.toLong(), stats.sizeBytes)
        Assert.assertEquals(2L, stats.visibleCount)
        Assert.assertEquals(1L, stats.deletedCount)
    }

    @Test
    fun read_reportsTheNewestUpdatedAtChronologicallyNotAsText() {
        // ZonedDateTime.toString() drops a zero fraction, so as text the
        // earlier "2024-01-01T10:00Z" sorts after the later ".500Z".
        val bytes = """
            [
              {"id": 1, "updated_at": "2024-01-01T10:00Z"},
              {"id": 2, "updated_at": "2024-01-01T10:00:00.500Z"}
            ]
        """.trimIndent().toByteArray()

        val stats = BundleReader.read("assets/bundled-places.json") {
            ByteArrayInputStream(bytes)
        }

        Assert.assertNotNull(stats)
        Assert.assertEquals("2024-01-01T10:00:00.500Z", stats!!.maxUpdatedAt)
    }

    @Test
    fun read_countsTrailingBytesInTheSize() {
        // The parser stops at the closing bracket; the size must still cover
        // the whole asset, including the trailing newline.
        val bytes = "[{\"id\": 1}]\n".toByteArray()

        val stats = BundleReader.read("assets/bundled-places.json") {
            ByteArrayInputStream(bytes)
        }

        Assert.assertNotNull(stats)
        Assert.assertEquals(bytes.size.toLong(), stats!!.sizeBytes)
    }

    @Test
    fun read_reportsNullMaxUpdatedAtWhenNoRecordHasOne() {
        val bytes = """[{"id": 1}]""".toByteArray()

        val stats = BundleReader.read("assets/bundled-places.json") {
            ByteArrayInputStream(bytes)
        }

        Assert.assertNotNull(stats)
        Assert.assertNull(stats!!.maxUpdatedAt)
    }

    @Test
    fun read_returnsNullWhenTheAssetIsMissing() {
        val stats = BundleReader.read("assets/bundled-places.json") {
            throw FileNotFoundException()
        }

        Assert.assertNull(stats)
    }
}
