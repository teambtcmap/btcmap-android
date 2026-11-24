package org.btcmap.desktop

import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.update.AvailableUpdate
import org.btcmap.update.UpdateCheck
import org.btcmap.util.rethrowIfCancellation
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties

/**
 * The version and update channel baked into this build, written by
 * :desktopApp's `writeDesktopBuildInfo`. Local builds carry channel `none`,
 * which disables update checks; only a beta AppImage deployed by
 * `devtools deploy --linux` checks its manifest.
 */
data class DesktopBuildInfo(
    val versionName: String,
    val versionCode: Int,
    val buildTime: Long,
    val channel: String,
) {
    val versionLabel: String get() = "$versionName ($versionCode)"

    val supportsUpdates: Boolean get() = channel == "beta"

    companion object {
        private const val RESOURCE = "desktop-build.properties"

        fun load(): DesktopBuildInfo {
            val properties = Properties()
            DesktopBuildInfo::class.java.classLoader
                .getResourceAsStream(RESOURCE)
                ?.use(properties::load)
            return DesktopBuildInfo(
                versionName = properties.getProperty("versionName") ?: "dev",
                versionCode = properties.getProperty("versionCode")?.toIntOrNull() ?: 0,
                buildTime = properties.getProperty("buildTime")?.toLongOrNull() ?: 0L,
                channel = properties.getProperty("channel") ?: "none",
            )
        }
    }
}

/**
 * The Linux AppImage's self-update. [check] uses the shared [UpdateCheck]
 * against the beta manifest; [install] downloads the newer AppImage beside the
 * running one, verifies its SHA-256, and atomically replaces the file. The
 * caller then restarts the app with [restart], since Linux keeps the old inode
 * running until the process exits.
 *
 * The AppImage runtime exposes the file it mounted as `APPIMAGE`, which is the
 * only reliable way back to the installed file: the running process itself is
 * the extracted AppRun.
 */
class DesktopUpdater(
    private val buildInfo: DesktopBuildInfo,
    private val httpClient: HttpClient,
    private val appImage: File? = runningAppImage(),
    private val manifestUrl: String = UpdateCheck.METADATA_URL,
    private val updateId: String = UpdateCheck.APPIMAGE_BETA_ID,
    private val userAgent: String = "btcmap-desktop",
    private val toCdnUrl: (String) -> String = UpdateCheck::cdnUrl,
) {
    val versionLabel: String get() = buildInfo.versionLabel

    /** The published build newer than this one, or null when up to date. */
    suspend fun check(): AvailableUpdate? = UpdateCheck.fetch(
        httpClient = httpClient,
        manifestUrl = manifestUrl,
        currentVersionCode = buildInfo.versionCode,
        isDebugBuild = false,
        currentBuildTime = buildInfo.buildTime,
        id = updateId,
    )

    /**
     * Downloads [update] and replaces the running AppImage. A failure leaves the
     * installed file untouched: the new download only lands there after it
     * passes the manifest's checksum and is executable.
     */
    suspend fun install(
        update: AvailableUpdate,
        onProgress: (Double?) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val target = appImage?.toPath()?.toRealPath() ?: error("not running from an AppImage")
            if (!Files.isRegularFile(target)) error("the AppImage is gone: $target")
            val directory = target.parent ?: error("the AppImage has no directory")
            if (!Files.isWritable(directory)) {
                error("the AppImage's directory is not writable: $directory")
            }

            val partial = Files.createTempFile(directory, ".btcmap-update-", ".part")
            try {
                download(update, partial, onProgress)

                // An AppImage has to stay executable; keeping the old file's
                // mode also preserves anything the user set on it.
                val permissions = runCatching { Files.getPosixFilePermissions(target) }.getOrNull()
                if (permissions != null) Files.setPosixFilePermissions(partial, permissions)
                partial.toFile().setExecutable(true, false)

                try {
                    Files.move(
                        partial,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    // A filesystem without atomic renames still gets the file;
                    // the window between delete and move is the only risk.
                    Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING)
                }
                Result.success(Unit)
            } catch (t: Throwable) {
                Files.deleteIfExists(partial)
                throw t
            }
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            Result.failure(t)
        }
    }

    /**
     * Starts the replaced AppImage once this process has exited. Handing the
     * launch to a small shell that waits for our PID avoids the new process
     * opening the database while the old one still has it.
     */
    fun restart(): Boolean = runCatching {
        val file = appImage ?: error("not running from an AppImage")
        val pid = ProcessHandle.current().pid()
        ProcessBuilder(
            "sh",
            "-c",
            "while kill -0 $pid 2>/dev/null; do sleep 0.2; done; exec \"$1\"",
            "btcmap-restart",
            file.absolutePath,
        ).start()
    }.isSuccess

    private fun download(
        update: AvailableUpdate,
        dest: Path,
        onProgress: (Double?) -> Unit,
    ) {
        var failure: Throwable? = null
        // The manifest names the origin URL, so the CDN can be down without
        // taking the download with it. Try the CDN first, then the origin.
        for (url in listOf(toCdnUrl(update.url), update.url).distinct()) {
            try {
                downloadFrom(url, update.sha256, dest, onProgress)
                return
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                failure = t
            }
        }
        throw failure ?: error("the update has no download URL")
    }

    private fun downloadFrom(
        url: String,
        expectedSha256: String?,
        dest: Path,
        onProgress: (Double?) -> Unit,
    ) {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", userAgent)
        }
        try {
            val total = connection.contentLengthLong.takeIf { it > 0 }
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            connection.inputStream.use { input ->
                FileOutputStream(dest.toFile()).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        read += count
                        onProgress(total?.let { read.toDouble() / it })
                    }
                }
            }

            if (expectedSha256 != null) {
                val actual = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
                check(actual.equals(expectedSha256, ignoreCase = true)) {
                    "downloaded AppImage does not match the manifest's checksum"
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** The AppImage file the runtime mounted, or null outside an AppImage. */
        fun runningAppImage(): File? = System.getenv("APPIMAGE")
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.isFile }
    }
}

private val DESKTOP_BUILD_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** A beta's build time for the update dialog, or its version when unstamped. */
internal fun desktopBuildStamp(
    buildTime: Long,
    versionName: String,
    zone: ZoneId = ZoneId.systemDefault(),
): String =
    if (buildTime > 0) {
        DESKTOP_BUILD_TIME_FORMAT.withZone(zone).format(Instant.ofEpochMilli(buildTime))
    } else {
        versionName
    }
