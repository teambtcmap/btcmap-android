package org.btcmap.db

import okio.FileSystem
import okio.Path

/**
 * Removes the databases left behind by earlier versions of the app.
 *
 * The app used to store its data under a new name for every schema change,
 * first `btcmap-vN.db` and later `btcmap-<date>.db`, and never cleaned the old
 * files up. Now that the database is a fixed `btcmap.db` created fresh rather
 * than migrated, those files are dead weight: they are deleted here, together
 * with their `-wal`/`-shm`/`-journal` sidecars. The current file is left alone.
 */
object LegacyDatabases {
    private const val LEGACY_PREFIX = "btcmap-"
    private const val DB_SUFFIX = ".db"

    private val SIDECAR_SUFFIXES = listOf("-wal", "-shm", "-journal")

    /** Deletes every legacy `btcmap-*.db` in [directory], keeping [current]. */
    fun delete(fileSystem: FileSystem, directory: Path, current: Path) {
        val files = runCatching { fileSystem.list(directory) }.getOrNull() ?: return

        for (file in files) {
            val name = file.name
            if (fileSystem.metadataOrNull(file)?.isRegularFile != true) continue
            if (name == current.name) continue
            if (!name.startsWith(LEGACY_PREFIX)) continue
            if (!name.endsWith(DB_SUFFIX)) continue

            fileSystem.delete(file)
            SIDECAR_SUFFIXES.forEach { suffix ->
                runCatching { fileSystem.delete(directory / (name + suffix)) }
            }
        }
    }
}
