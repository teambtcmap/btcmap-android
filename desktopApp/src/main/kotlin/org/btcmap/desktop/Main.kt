package org.btcmap.desktop

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.db.Database
import org.btcmap.settings.KEY_AUTH_TOKEN
import org.btcmap.settings.Settings
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.ui.AppTheme
import org.btcmap.ui.StatsScreen
import java.io.File

/**
 * The desktop entry point. This stage opens the app database and settings in a
 * per-user data directory and renders the shared stats screen with the counts it
 * reads back, so the shared data layer runs on the JVM; navigation and the map
 * follow.
 */
fun main() = application {
    val home = DesktopHome()
    val db = home.database()
    val settings = home.settings(db).apply { preload() }

    val iconFont = loadIconFont()

    Window(
        onCloseRequest = ::exitApplication,
        title = "BTC Map",
        state = rememberWindowState(size = DpSize(480.dp, 720.dp)),
    ) {
        AppTheme(iconFont = iconFont) {
            StatsScreen(sections = statsSections(db, settings))
        }
    }
}

/** The per-user data directory the desktop app keeps its database in. */
private class DesktopHome {
    private val dir: File = File(System.getenv("BTCMAP_HOME") ?: "${System.getProperty("user.home")}/.btcmap")
        .apply { mkdirs() }

    fun database(): Database = Database(
        driver = BundledSQLiteDriver(),
        path = File(dir, "btcmap.db").absolutePath,
    )

    fun settings(db: Database): Settings = Settings(
        dbProvider = { db },
        // The desktop app has no SharedPreferences to import from.
        legacyValues = { emptyMap() },
    )
}

/**
 * The Material Symbols typeface the icon ligatures need, or null when it is not
 * on disk. The font is the app asset the Android build bundles; packaging it
 * into the desktop distribution is still to do, so for now it is looked up
 * beside the sources (or at `BTCMAP_ICON_FONT`).
 */
private fun loadIconFont(): FontFamily? {
    val assetDir = File(
        System.getenv("BTCMAP_ICON_FONT")
            ?: System.getProperty("btcmap.iconFontDir")
            ?: "app/src/main/assets",
    )
    val font = assetDir.listFiles { file -> file.name.startsWith("material-symbols") }
        ?.maxByOrNull { it.name }
    println("desktop: icon font ${font?.absolutePath ?: "NOT FOUND (icons will show as text)"}")
    return font?.let { FontFamily(Font(it)) }
}

private fun statsSections(db: Database, settings: Settings): List<StatsSection> = listOf(
    StatsSection(
        key = "cache",
        title = "Local cache",
        icon = "storefront",
        entries = listOf(
            StatsEntry("Places", db.place.selectCount().toString()),
            StatsEntry("Areas", db.area.selectCount().toString()),
            StatsEntry("Comments", db.comment.selectCount().toString()),
            StatsEntry("Events", db.event.selectCount().toString()),
        ),
    ),
    StatsSection(
        key = "session",
        title = "Session",
        icon = "person",
        entries = listOf(
            StatsEntry("Signed in", (settings.getString(KEY_AUTH_TOKEN, null) != null).toString()),
        ),
    ),
)
