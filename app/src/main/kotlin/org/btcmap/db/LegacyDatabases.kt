package org.btcmap.db

import java.io.File

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
    fun delete(directory: File, current: File) {
        val files = directory.listFiles() ?: return

        for (file in files) {
            if (!file.isFile) continue
            if (file.name == current.name) continue
            if (!file.name.startsWith(LEGACY_PREFIX)) continue
            if (!file.name.endsWith(DB_SUFFIX)) continue

            file.delete()
            SIDECAR_SUFFIXES.forEach { suffix ->
                File(directory, file.name + suffix).delete()
            }
        }
    }
}
