package org.btcmap.dbstats

import java.io.File

/** The app's database file on disk. */
data class DatabaseFile(
    val name: String,
    val sizeBytes: Long,
) {
    companion object {
        /**
         * Reads the database at [path], or returns null when there is no file
         * (for example an in-memory database used by tests).
         */
        fun read(path: String): DatabaseFile? {
            val file = File(path)
            return if (file.isFile) DatabaseFile(file.name, file.length()) else null
        }
    }
}
