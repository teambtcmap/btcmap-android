package org.btcmap.desktop

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okio.Buffer
import org.btcmap.api.apiHttpClient
import org.btcmap.update.AvailableUpdate
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/** The Linux AppImage's download-and-replace path. */
class DesktopUpdaterTest {
    @JvmField
    @Rule
    val serverRule = MockWebServerRule()

    private val buildInfo = DesktopBuildInfo(
        versionName = "1.2.0",
        versionCode = 315,
        buildTime = 1_000L,
        channel = "beta",
    )

    @Test
    fun check_returnsThePublishedBuild() = runBlocking<Unit> {
        serverRule.server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(
                    Buffer().writeUtf8(
                        """{"appimage-beta": {"code": 315, "name": "1.2.0", "buildTime": 2000, """ +
                            """"url": "https://example.com/btcmap-beta.AppImage", "sha256": "abc"}}""",
                    ),
                )
                .build(),
        )

        val update = updater().check()

        assertNotNull(update)
        assertEquals(2_000L, update!!.buildTime)
        assertEquals("abc", update.sha256)
    }

    @Test
    fun install_replacesTheAppImageAndKeepsItExecutable() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("btcmap-updater").toFile()
        try {
            val target = File(directory, "btcmap-beta.AppImage").apply {
                writeText("old")
                setExecutable(true)
            }
            val replacement = "new appimage".toByteArray()
            serverRule.server.enqueue(
                MockResponse.Builder().body(Buffer().write(replacement)).build(),
            )

            val update = availableUpdate(replacement)
            val result = updater(target).install(update)

            assertTrue(result.isSuccess)
            assertArrayEquals(replacement, target.readBytes())
            assertTrue(target.canExecute())
            assertTrue(temporaryFiles(directory).isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun install_leavesTheAppImageUntouchedWhenTheChecksumDoesNotMatch() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("btcmap-updater").toFile()
        try {
            val target = File(directory, "btcmap-beta.AppImage").apply {
                writeText("old")
                setExecutable(true)
            }
            serverRule.server.enqueue(
                MockResponse.Builder().body(Buffer().writeUtf8("new appimage")).build(),
            )

            val update = availableUpdate("new appimage".toByteArray()).copy(sha256 = "deadbeef")
            val result = updater(target).install(update)

            assertTrue(result.isFailure)
            assertEquals("old", target.readText())
            assertTrue(temporaryFiles(directory).isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun install_failsOutsideAnAppImage() = runBlocking<Unit> {
        val result = updater(appImage = null).install(availableUpdate("new".toByteArray()))

        assertTrue(result.isFailure)
    }

    @Test
    fun install_fallsBackToTheOriginWhenTheCdnFails() = runBlocking<Unit> {
        val directory = Files.createTempDirectory("btcmap-updater").toFile()
        try {
            val target = File(directory, "btcmap-beta.AppImage").apply {
                writeText("old")
                setExecutable(true)
            }
            val replacement = "new appimage".toByteArray()
            serverRule.server.enqueue(MockResponse.Builder().code(404).build())
            serverRule.server.enqueue(
                MockResponse.Builder().body(Buffer().write(replacement)).build(),
            )

            val update = availableUpdate(replacement)
            val toCdnUrl: (String) -> String = {
                serverRule.server.url("/cdn-missing.AppImage").toString()
            }
            val result = updater(target, toCdnUrl).install(update)

            assertTrue(result.isSuccess)
            assertArrayEquals(replacement, target.readBytes())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cdnUrlFor_addsThePrefixOnlyOnce() {
        assertEquals(
            "https://cdn.static.btcmap.org/builds/btcmap-beta.AppImage",
            cdnUrlFor("https://static.btcmap.org/builds/btcmap-beta.AppImage"),
        )
        assertEquals(
            "https://cdn.static.btcmap.org/builds/btcmap-beta.AppImage",
            cdnUrlFor("https://cdn.static.btcmap.org/builds/btcmap-beta.AppImage"),
        )
    }

    private fun updater(
        appImage: File? = null,
        toCdnUrl: (String) -> String = { it },
    ): DesktopUpdater = DesktopUpdater(
        buildInfo = buildInfo,
        httpClient = apiHttpClient("test-agent"),
        appImage = appImage,
        manifestUrl = serverRule.server.url("/metadata.json").toString(),
        toCdnUrl = toCdnUrl,
    )

    private fun availableUpdate(bytes: ByteArray): AvailableUpdate = AvailableUpdate(
        versionCode = 315,
        versionName = "1.2.0",
        url = serverRule.server.url("/btcmap-beta.AppImage").toString(),
        buildTime = 2_000L,
        sha256 = sha256(bytes),
    )

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { byte -> "%02x".format(byte) }

    private fun temporaryFiles(directory: File): List<File> =
        directory.listFiles().orEmpty().filter { it.name.endsWith(".part") }
}
