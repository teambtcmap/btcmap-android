package org.btcmap.db

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.junit.Assert
import org.junit.Test
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

        LegacyDatabases.delete(FileSystem.SYSTEM, directory, current)

        Assert.assertTrue(FileSystem.SYSTEM.exists(current))
        Assert.assertFalse(FileSystem.SYSTEM.exists(dated))
        Assert.assertFalse(FileSystem.SYSTEM.exists(versioned))
        Assert.assertFalse(FileSystem.SYSTEM.exists(directory / "btcmap-2025-11-06.db-wal"))
        Assert.assertFalse(FileSystem.SYSTEM.exists(directory / "btcmap-v2.db-journal"))
    }

    @Test
    fun delete_keepsTheCurrentDatabaseAndItsSidecars() {
        val directory = tempDirectory()
        val current = file(directory, "btcmap.db")
        val wal = file(directory, "btcmap.db-wal")
        val shm = file(directory, "btcmap.db-shm")

        LegacyDatabases.delete(FileSystem.SYSTEM, directory, current)

        Assert.assertTrue(FileSystem.SYSTEM.exists(current))
        Assert.assertTrue(FileSystem.SYSTEM.exists(wal))
        Assert.assertTrue(FileSystem.SYSTEM.exists(shm))
    }

    @Test
    fun delete_leavesUnrelatedFilesAlone() {
        val directory = tempDirectory()
        val current = file(directory, "btcmap.db")
        val unrelated = file(directory, "notes.txt")
        val otherDatabase = file(directory, "coins.db")

        LegacyDatabases.delete(FileSystem.SYSTEM, directory, current)

        Assert.assertTrue(FileSystem.SYSTEM.exists(unrelated))
        Assert.assertTrue(FileSystem.SYSTEM.exists(otherDatabase))
    }

    @Test
    fun delete_isANoOpWhenTheDirectoryDoesNotExist() {
        val directory = "/tmp/btcmap-missing-legacy".toPath()

        LegacyDatabases.delete(FileSystem.SYSTEM, directory, directory / "btcmap.db")
    }

    private fun tempDirectory(): Path =
        Files.createTempDirectory("btcmap-legacy").toAbsolutePath().toString().toPath()

    private fun file(directory: Path, name: String): Path =
        (directory / name).also { FileSystem.SYSTEM.write(it) { writeByte(1) } }
}
