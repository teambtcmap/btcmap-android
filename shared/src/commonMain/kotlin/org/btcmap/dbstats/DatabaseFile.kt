package org.btcmap.dbstats

import okio.Path.Companion.toPath
import org.btcmap.io.platformFileSystem

/** The app's database file on disk. */
data class DatabaseFile(
    val name: String,
    val sizeBytes: Long,
) {
    companion object {
        /**
         * Reads the database at [path], or returns null when there is no file
         * (for example an in-memory database used by tests, or a browser where
         * there is no file system).
         */
        fun read(path: String): DatabaseFile? {
            val fileSystem = platformFileSystem ?: return null
            val file = path.toPath()
            val metadata = fileSystem.metadataOrNull(file) ?: return null
            if (!metadata.isRegularFile) return null
            return DatabaseFile(file.name, metadata.size ?: 0L)
        }
    }
}
