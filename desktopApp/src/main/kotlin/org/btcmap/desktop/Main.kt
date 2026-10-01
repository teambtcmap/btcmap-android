package org.btcmap.desktop

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.io.files.Path
import org.maplibre.compose.desktop.rememberAwtComposeMapPresentationHost
import org.maplibre.compose.desktop.ProvideMapPresentationHost
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MapRuntimeOptions
import org.btcmap.ui.map.AreaChipPalette
import org.btcmap.ui.map.MarkerPalette
import java.awt.Color as AwtColor
import androidx.compose.ui.graphics.Color
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import org.btcmap.ui.MaterialSymbol
import org.btcmap.ui.SettingsItem
import org.btcmap.ui.SettingsScreen
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.db.Database
import org.btcmap.sync.Sync
import org.btcmap.sync.SyncManager
import org.btcmap.settings.KEY_AUTH_TOKEN
import org.btcmap.settings.Settings
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.showAttribution
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.ui.AppTheme
import org.btcmap.ui.MapScreen
import org.btcmap.ui.StatsScreen
import java.io.File

/**
 * The desktop entry point. This stage opens the app database and settings in a
 * per-user data directory and renders the shared stats screen with the counts it
 * reads back, so the shared data layer runs on the JVM; navigation and the map
 * follow.
 */
fun main(args: Array<String>) {
    val screenshot = args.firstOrNull { it.startsWith(SCREENSHOT_ARG) }
    if (screenshot != null) {
        renderScreen(screenshot.removePrefix(SCREENSHOT_ARG))
        return
    }
    runApp()
}

private fun runApp() = application {
    val home = DesktopHome()
    val db = home.database()
    val settings = home.settings(db).apply { preload() }

    // The desktop app has no bundled snapshot to seed from, so the sync pulls
    // the whole delta history on first run.
    val api = Api(
        httpClient = apiHttpClient(
            userAgent = USER_AGENT,
            token = { settings.getString(KEY_AUTH_TOKEN, null) },
            apiUrl = { API_URL.toHttpUrl() },
        ),
        baseUrl = { API_URL.toHttpUrl() },
        userAgent = USER_AGENT,
    )
    val syncManager = SyncManager(
        sync = { Sync(api, db) },
        seedPlaces = { 0L },
        seedEvents = { 0L },
        seedComments = { 0L },
        seedAreas = { 0L },
    )

    val iconFont = loadIconFont()

    // The map needs an app-wide cache directory and, per window, its GPU
    // context, before any map is created.
    DefaultMapRuntime.configure(
        MapRuntimeOptions(cacheFile = Path(home.cacheFile().absolutePath)),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "BTC Map",
        state = rememberWindowState(size = DpSize(480.dp, 720.dp)),
    ) {
        val mapHost = rememberAwtComposeMapPresentationHost(window)
        ProvideMapPresentationHost(mapHost) {
            AppTheme(iconFont = iconFont) {
                // The window's own background is white, which shows through the
                // transparent rows of screens like the settings list.
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                val syncState by syncManager.state.collectAsState()
                LaunchedEffect(Unit) { syncManager.start() }

                var screen by remember { mutableStateOf(Screen.Map) }

                Row(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    NavigationRail {
                        NavigationRailItem(
                            selected = screen == Screen.Map,
                            onClick = { screen = Screen.Map },
                            icon = { MaterialSymbol(glyph = "map", contentDescription = null) },
                            label = { Text("Map") },
                        )
                        NavigationRailItem(
                            selected = screen == Screen.Stats,
                            onClick = { screen = Screen.Stats },
                            icon = { MaterialSymbol(glyph = "insights", contentDescription = null) },
                            label = { Text("Cache") },
                        )
                        NavigationRailItem(
                            selected = screen == Screen.Settings,
                            onClick = { screen = Screen.Settings },
                            icon = { MaterialSymbol(glyph = "settings", contentDescription = null) },
                            label = { Text("Settings") },
                        )
                    }

                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                    ) {
                if (screen == Screen.Map) {
                MapScreen(
                    db = db,
                    styleUrl = "https://tiles.openfreemap.org/styles/liberty",
                    initialLat = 52.2333742,
                    initialLon = 21.0711489,
                    initialZoom = 13.0,
                    minVerifiedAt = null,
                    palette = MarkerPalette(
                        markerBackground = Color(0xFFF7931A),
                        markerIcon = Color.White,
                        boostedMarkerBackground = Color(0xFF7B3FE4),
                        boostedMarkerIcon = Color.White,
                        badgeBackground = Color(0xFFE53935),
                        badgeText = Color.White,
                    ),
                    areaChipPalette = AreaChipPalette(
                        buttonBackground = Color(0xFF1B1B1B),
                        buttonIcon = Color.White,
                        badgeBackground = Color(0xFFE53935),
                        badgeText = Color.White,
                    ),
                    apiUrl = "https://api.btcmap.org",
                    usingOpenFreeMap = true,
                    iconFont = iconFont,
                    placeSheetStrings = PLACE_SHEET_STRINGS,
                    onPlaceAction = { _, _ -> },
                    onSelectEvent = {},
                    onSelectArea = {},
                    formatDistance = { meters -> "%.1f km".format(meters / 1000) },
                )
                } else if (screen == Screen.Settings) {
                    DesktopSettingsScreen(db, settings)
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = androidx.compose.ui.Modifier.padding(24.dp),
                    ) {
                        Text(text = "Sync: $syncState")
                        Button(onClick = { syncManager.start() }) { Text("Sync now") }
                    }
                    val sections = remember(syncState) { statsSections(db, settings) }
                    StatsScreen(sections = sections)
                }
                }
                }
                }
            }
        }
    }
}

/** The desktop app's screens. */
private enum class Screen { Map, Stats, Settings }

private const val SCREENSHOT_ARG = "--screenshot="

/**
 * Renders one screen to a PNG without opening a window, so the desktop UI can be
 * checked from a headless/CI run: `--screenshot=<screen>:<path>`. The map needs a
 * real window and GPU context, so it is not one of the screens this can draw.
 */
private fun renderScreen(spec: String) {
    val parts = spec.split(':')
    val name = parts[0]
    val path = parts[1]
    val darkTheme = when (parts.getOrNull(2)) {
        "dark" -> true
        "light" -> false
        else -> null
    }

    val home = DesktopHome()
    val db = home.database()
    val settings = home.settings(db).apply { preload() }

    val scene = androidx.compose.ui.ImageComposeScene(
        width = 900,
        height = 1000,
        density = androidx.compose.ui.unit.Density(2f),
    ) {
        AppTheme(iconFont = loadIconFont(), darkTheme = darkTheme) {
            // Paint the theme's background, as the window does, so the render
            // shows what the screen actually sits on.
            Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                when (name) {
                    "settings" -> DesktopSettingsScreen(db, settings)
                    "cache" -> StatsScreen(sections = statsSections(db, settings))
                    else -> Text(text = "Unknown screen: $name")
                }
            }
        }
    }

    val bytes = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes
    scene.close()
    requireNotNull(bytes) { "could not encode the screenshot" }
    File(path).writeBytes(bytes)
    println("desktop: wrote $path")
}

/** The shared settings list, used by both the window and the screenshot mode. */
@androidx.compose.runtime.Composable
private fun DesktopSettingsScreen(
    db: Database,
    settings: Settings,
) {
    var attribution by remember { mutableStateOf(settings.showAttribution) }
    var rotation by remember { mutableStateOf(settings.mapRotationEnabled) }

    SettingsScreen(
        items = listOf(
            SettingsItem.Action(key = "api", title = "API", secondary = settings.apiUrl.toString()),
            SettingsItem.Toggle(key = "attribution", title = "Show attribution", checked = attribution),
            SettingsItem.Toggle(key = "rotation", title = "Map rotation", checked = rotation),
            SettingsItem.Action(
                key = "account",
                title = "Account",
                secondary = if (settings.authorized) "Signed in" else "Not signed in",
            ),
        ),
        onItemClick = {},
        onItemCheckedChange = { key, checked ->
            when (key) {
                "attribution" -> {
                    attribution = checked
                    settings.showAttribution = checked
                }

                "rotation" -> {
                    rotation = checked
                    settings.mapRotationEnabled = checked
                }
            }
        },
    )
}

private const val API_URL = "https://api.btcmap.org"
private const val USER_AGENT = "btcmap-desktop"

/** The per-user data directory the desktop app keeps its database in. */
private class DesktopHome {
    private val dir: File = File(System.getenv("BTCMAP_HOME") ?: "${System.getProperty("user.home")}/.btcmap")
        .apply { mkdirs() }

    fun database(): Database = Database(
        driver = BundledSQLiteDriver(),
        path = File(dir, "btcmap.db").absolutePath,
    )

    fun cacheFile(): File = File(dir, "map-cache").apply { mkdirs() }.let { File(it, "cache.db") }

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

private val PLACE_SHEET_STRINGS = org.btcmap.ui.PlaceSheetStrings(
    directions = "Directions",
    share = "Share",
    viewOnBtcmap = "View on btcmap.org",
    viewOnOsm = "View on openstreetmap.org",
    editOnOsm = "Edit on openstreetmap.org",
    notVerified = "Not verified",
    companionWarning = { "This place needs a companion app ($it)." },
    verificationWarningTitle = "Verification needed",
    verificationWarningOutdated = "This place was last verified more than a year ago.",
    verificationWarningNotVerified = "This place has not been verified yet.",
    ok = "OK",
    verify = "Verify",
    report = "Report",
    boost = "Boost",
    comments = { "Comments ($it)" },
    commentsTitle = { "Comments ($it)" },
    addComment = "Add comment",
    save = "Save",
    addPhoto = "Add photo",
)

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
