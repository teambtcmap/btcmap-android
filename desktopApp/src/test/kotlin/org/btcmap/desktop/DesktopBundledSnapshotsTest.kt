package org.btcmap.desktop

import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The bundled snapshots must be on the desktop classpath, because the desktop
 * seeds from them (see the build file). A missing resource would not fail the
 * sync — the shared seeders treat it as an optional snapshot — so it would
 * silently fall back to pulling the whole delta history on first run. Pin the
 * resources here instead.
 */
class DesktopBundledSnapshotsTest {

    private fun snapshotHead(name: String): String {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(name)
            ?: throw FileNotFoundException(name)
        return stream.use { it.readNBytes(64).decodeToString() }
    }

    @Test
    fun bundledSnapshotsAreOnTheClasspathAndAreJsonArrays() {
        for (name in listOf(
            "bundled-places.json",
            "bundled-areas.json",
            "bundled-comments.json",
            "bundled-events.json",
        )) {
            val head = snapshotHead(name).trimStart()
            assertEquals('[', head.first(), "$name is not a JSON array")
        }
    }
}
