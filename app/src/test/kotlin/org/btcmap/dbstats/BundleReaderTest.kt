package org.btcmap.dbstats

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.util.concurrent.CancellationException

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

    @Test
    fun readBundles_readsEverySnapshot() {
        val bundles = mapOf(
            "place" to "bundled-places.json",
            "comment" to "bundled-comments.json",
        )
        val assets = mapOf(
            "bundled-places.json" to """[{"id": 1}]""".toByteArray(),
            "bundled-comments.json" to
                """[{"id": 2}, {"id": 3, "deleted_at": "2024-01-01T00:00:00Z"}]"""
                    .toByteArray(),
        )

        val reads = readBundles(bundles) { fileName ->
            ByteArrayInputStream(assets.getValue(fileName))
        }

        Assert.assertTrue(reads.failures.isEmpty())
        Assert.assertEquals(setOf("place", "comment"), reads.stats.keys)
        Assert.assertEquals("assets/bundled-places.json", reads.stats.getValue("place").location)
        Assert.assertEquals(1L, reads.stats.getValue("place").visibleCount)
        Assert.assertEquals(1L, reads.stats.getValue("comment").visibleCount)
        Assert.assertEquals(1L, reads.stats.getValue("comment").deletedCount)
    }

    @Test
    fun readBundles_skipsAMissingSnapshotWithoutFailing() {
        val reads = readBundles(mapOf("place" to "bundled-places.json")) {
            throw FileNotFoundException()
        }

        Assert.assertTrue(reads.stats.isEmpty())
        Assert.assertTrue(reads.failures.isEmpty())
    }

    @Test
    fun readBundles_collectsAFailureAndKeepsTheOthers() {
        val bundles = mapOf(
            "place" to "bundled-places.json",
            "comment" to "bundled-comments.json",
        )

        val reads = readBundles(bundles) { fileName ->
            if (fileName == "bundled-comments.json") throw IllegalStateException("bad snapshot")
            ByteArrayInputStream("""[{"id": 1}]""".toByteArray())
        }

        Assert.assertEquals(setOf("place"), reads.stats.keys)
        Assert.assertEquals(1, reads.failures.size)
        Assert.assertTrue(reads.failures.single() is IllegalStateException)
    }

    @Test(expected = CancellationException::class)
    fun readBundles_rethrowsCancellation() {
        readBundles(mapOf("place" to "bundled-places.json")) {
            throw CancellationException()
        }
    }
}
