package org.btcmap.db

import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files

class LegacyDatabasesTest {

    @Test
    fun delete_removesLegacyDatabasesAndTheirSidecars() {
        val directory = tempDirectory()
        val current = file(directory, "btcmap.db")
        val dated = file(directory, "btcmap-2025-11-06.db")
        val versioned = file(directory, "btcmap-v2.db")
        file(directory, "btcmap-2025-11-06.db-wal")
        file(directory, "btcmap-v2.db-journal")

        LegacyDatabases.delete(directory, current)

        Assert.assertTrue(current.exists())
        Assert.assertFalse(dated.exists())
        Assert.assertFalse(versioned.exists())
        Assert.assertFalse(File(directory, "btcmap-2025-11-06.db-wal").exists())
        Assert.assertFalse(File(directory, "btcmap-v2.db-journal").exists())
    }

    @Test
    fun delete_keepsTheCurrentDatabaseAndItsSidecars() {
        val directory = tempDirectory()
        val current = file(directory, "btcmap.db")
        val wal = file(directory, "btcmap.db-wal")
        val shm = file(directory, "btcmap.db-shm")

        LegacyDatabases.delete(directory, current)

        Assert.assertTrue(current.exists())
        Assert.assertTrue(wal.exists())
        Assert.assertTrue(shm.exists())
    }

    @Test
    fun delete_leavesUnrelatedFilesAlone() {
        val directory = tempDirectory()
        val current = file(directory, "btcmap.db")
        val unrelated = file(directory, "notes.txt")
        val otherDatabase = file(directory, "coins.db")

        LegacyDatabases.delete(directory, current)

        Assert.assertTrue(unrelated.exists())
        Assert.assertTrue(otherDatabase.exists())
    }

    @Test
    fun delete_isANoOpWhenTheDirectoryDoesNotExist() {
        val directory = File("/tmp/btcmap-missing-${System.nanoTime()}")

        LegacyDatabases.delete(directory, File(directory, "btcmap.db"))
    }

    private fun tempDirectory(): File =
        Files.createTempDirectory("btcmap-legacy").toFile().apply { deleteOnExit() }

    private fun file(directory: File, name: String): File =
        File(directory, name).apply { writeBytes(byteArrayOf(1)) }
}
