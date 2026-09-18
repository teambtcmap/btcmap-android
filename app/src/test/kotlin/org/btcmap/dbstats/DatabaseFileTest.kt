package org.btcmap.dbstats

import org.junit.Assert
import org.junit.Test
import java.nio.file.Files

class DatabaseFileTest {

    @Test
    fun read_returnsTheFileNameAndSize() {
        val file = Files.createTempFile("btcmap-file", ".db").toFile()
        file.deleteOnExit()
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        val result = DatabaseFile.read(file.absolutePath)

        Assert.assertEquals(file.name, result?.name)
        Assert.assertEquals(5L, result?.sizeBytes)
    }

    @Test
    fun read_returnsNullForAMissingFile() {
        Assert.assertNull(DatabaseFile.read("/tmp/btcmap-missing-${System.nanoTime()}.db"))
    }

    @Test
    fun read_returnsNullForAnInMemoryDatabase() {
        Assert.assertNull(DatabaseFile.read(":memory:"))
    }
}
