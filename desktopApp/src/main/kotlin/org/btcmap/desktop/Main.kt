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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import org.btcmap.api.ActivityFeedItem
import org.btcmap.api.getActivity
import org.btcmap.map.MapAreasController
import org.btcmap.ui.ActivityFeedRow
import org.btcmap.ui.ActivityFeedScreen
import org.btcmap.ui.ActivityFeedState
import org.btcmap.ui.MaterialSymbol
import org.btcmap.ui.map.SearchActions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.btcmap.ui.SettingsItem
import org.btcmap.ui.SettingsScreen
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import org.btcmap.api.signIn
import org.btcmap.api.submitPlace
import org.btcmap.ui.PlaceAction
import org.btcmap.api.savePlace
import org.btcmap.api.removeSavedPlace
import org.btcmap.api.getUser
import org.btcmap.api.toDbUser
import org.btcmap.api.apiHttpClient
import org.btcmap.db.Database
import org.btcmap.sync.Sync
import org.btcmap.sync.SyncManager
import org.btcmap.settings.KEY_AUTH_TOKEN
import org.btcmap.settings.Settings
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.bundledStyleAsset
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.mapStyle
import org.maplibre.compose.resource.MapResourceProvider
import org.btcmap.settings.showAttribution
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.ui.AppTheme
import org.btcmap.ui.MapScreen
import org.btcmap.ui.StatsScreen
import java.io.File
import java.net.URLDecoder

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

    // The map needs an app-wide cache directory, its bundled resources and, per
    // window, its GPU context, before any map is created.
    DefaultMapRuntime.configure(
        MapRuntimeOptions(
            cacheFile = Path(home.cacheFile().absolutePath),
            resourceProvider = bundledMapResources(),
        ),
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
                Surface(modifier = Modifier.fillMaxSize()) {
                    val syncState by syncManager.state.collectAsState()
                    LaunchedEffect(Unit) { syncManager.start() }

                    // The map screen owns its own affordances (the search bar's
                    // settings, the chips and the pulse button), so the desktop
                    // keeps no navigation of its own: the settings and the feed
                    // are full-window pages the map opens.
                    var route by remember { mutableStateOf(Route.Map) }
                    val scope = rememberCoroutineScope()
                    // The place the sheet is showing, and whether the account has
                    // it saved, which is what the sheet's Save row renders.
                    var selectedPlaceId by remember { mutableStateOf<Long?>(null) }
                    // The place and reason a report was opened with, if any.
                    var reportPlace by remember { mutableStateOf<Pair<Long, String>?>(null) }
                    var reportType by remember { mutableStateOf<String?>(null) }
                    var bookmarked by remember { mutableStateOf(false) }
                    LaunchedEffect(selectedPlaceId) {
                        bookmarked = selectedPlaceId?.let { isPlaceSaved(db, it) } ?: false
                    }
                    // The place a feed row asked the map to show. Leaving the
                    // map disposes it and coming back rebuilds it, so opening a
                    // row always lands on the map with that place selected.
                    var feedPlaceId by remember { mutableStateOf<Long?>(null) }
                    // Where the add-place screen was opened from the map.
                    var addPlace by remember { mutableStateOf<Pair<Double, Double>?>(null) }

                    // The bundled style, shared by the map and the add-place map.
                    // Its sprite and glyph URLs are served from the app's
                    // resources (the style itself references them with Android's
                    // asset:// scheme, which the Compose map cannot read).
                    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                    val styleAsset = settings.mapStyle.bundledStyleAsset(darkSystemTheme = darkTheme)
                    val styleJson = remember(styleAsset) { bundledStyleJson(styleAsset) }

                    when (route) {
                        Route.Map -> MapScreen(
                            db = db,
                            openPlaceId = feedPlaceId,
                            styleUrl = HOSTED_STYLE_URL,
                            styleJson = styleJson,
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
                            apiUrl = API_URL,
                            usingOpenFreeMap = true,
                            iconFont = iconFont,
                            placeSheetStrings = PLACE_SHEET_STRINGS,
                            searchActions = SearchActions(onSettings = { route = Route.Settings }),
                            onAddPlace = { lat, lon ->
                                addPlace = lat to lon
                                route = if (settings.authorized) Route.AddPlace else Route.Account
                            },
                            onOpenFeed = { route = Route.Feed },
                            bookmarked = bookmarked,
                            onPlaceSelected = { selectedPlaceId = it.id },
                            onPlaceAction = { place, action ->
                                when (action) {
                                    // Verify and Report are the same form, one
                                    // with the reason already chosen.
                                    PlaceAction.Verify, PlaceAction.Report -> {
                                        if (settings.authorized) {
                                            reportPlace = place.id to place.name.orEmpty()
                                            reportType = if (action == PlaceAction.Verify) {
                                                "verified"
                                            } else {
                                                null
                                            }
                                            route = Route.Report
                                        } else {
                                            route = Route.Account
                                        }
                                    }

                                    // Saving is the one sheet action that needs a
                                    // session but no screen of its own.
                                    PlaceAction.ToggleBookmark -> {
                                        if (settings.authorized) {
                                            scope.launch {
                                                toggleSavedPlace(api, db, place.id)
                                                bookmarked = isPlaceSaved(db, place.id)
                                            }
                                        } else {
                                            route = Route.Account
                                        }
                                    }

                                    else -> handlePlaceAction(place, action)
                                }
                            },
                            onSelectEvent = {},
                            onSelectArea = {},
                            formatDistance = { meters -> "%.1f km".format(meters / 1000) },
                        )

                        Route.Report -> DesktopReportScreen(
                            api = api,
                            placeId = reportPlace?.first ?: 0L,
                            placeName = reportPlace?.second ?: "",
                            initialType = reportType,
                            pickPhotos = reportPhotoPicker(window),
                            onBack = { route = Route.Map },
                        )

                        Route.AddPlace -> DesktopAddPlaceScreen(
                            lat = addPlace?.first ?: 0.0,
                            lon = addPlace?.second ?: 0.0,
                            styleUrl = HOSTED_STYLE_URL,
                            styleJson = styleJson,
                            submit = { draft ->
                                api.submitPlace(
                                    lat = draft.lat,
                                    lon = draft.lon,
                                    category = draft.category,
                                    name = draft.name,
                                    address = draft.address.takeIf { it.isNotEmpty() },
                                    website = draft.website.takeIf { it.isNotEmpty() },
                                    description = draft.description.takeIf { it.isNotEmpty() },
                                )
                            },
                            onBack = { route = Route.Map },
                        )

                        Route.Settings -> ScreenPage(
                            title = "Settings",
                            onBack = { route = Route.Map },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(text = "Sync: $syncState")
                                Button(onClick = { syncManager.start() }) { Text("Sync now") }
                            }
                            DesktopSettingsScreen(
                                settings = settings,
                                onOpenAccount = { route = Route.Account },
                            )
                        }

                        Route.Account -> DesktopAccountScreen(
                            api = api,
                            db = db,
                            settings = settings,
                            onBack = { route = Route.Settings },
                        )

                        Route.Feed -> ScreenPage(
                            title = "Activity",
                            onBack = { route = Route.Map },
                        ) {
                            DesktopFeedScreen(api = api, db = db) { placeId ->
                                feedPlaceId = placeId
                                route = Route.Map
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A screen the map opens, with a back affordance of its own. */
@androidx.compose.runtime.Composable
internal fun ScreenPage(
    title: String,
    onBack: () -> Unit,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            IconButton(onClick = onBack) {
                MaterialSymbol(glyph = "arrow_back", contentDescription = null)
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        }
        content()
    }
}

/** The desktop app's full-window pages. */
private enum class Route { Map, Feed, Settings, Account, Report, AddPlace }

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
                    "settings" -> DesktopSettingsScreen(settings, onOpenAccount = {})
                    "report" -> DesktopReportScreen(
                        api = Api(
                            httpClient = apiHttpClient(
                                userAgent = USER_AGENT,
                                token = { settings.getString(KEY_AUTH_TOKEN, null) },
                                apiUrl = { API_URL.toHttpUrl() },
                            ),
                            baseUrl = { API_URL.toHttpUrl() },
                            userAgent = USER_AGENT,
                        ),
                        placeId = 0L,
                        placeName = "Report a place",
                        initialType = "verified",
                        onBack = {},
                    )

                    "account" -> DesktopAccountScreen(
                        api = Api(
                            httpClient = apiHttpClient(
                                userAgent = USER_AGENT,
                                token = { settings.getString(KEY_AUTH_TOKEN, null) },
                                apiUrl = { API_URL.toHttpUrl() },
                            ),
                            baseUrl = { API_URL.toHttpUrl() },
                            userAgent = USER_AGENT,
                        ),
                        db = db,
                        settings = settings,
                        onBack = {},
                    )

                    "addplace" -> AddPlaceForm(
                        busy = false,
                        error = null,
                        submitted = false,
                        onSubmit = { _, _, _, _, _ -> },
                        onBack = {},
                    )

                    else -> Text(text = "Unknown screen: $name")
                }
            }
        }
    }

    // Render once so effects start, then again after a moment: a screen that
    // loads its data off the main thread (the profile) is blank on the first
    // frame and settled by the second.
    scene.render()
    Thread.sleep(500)
    val bytes = scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)?.bytes
    scene.close()
    requireNotNull(bytes) { "could not encode the screenshot" }
    File(path).writeBytes(bytes)
    println("desktop: wrote $path")
}

/**
 * The shared activity feed for the areas containing the map's starting point.
 * The desktop has no selected place or area of its own yet, so it asks the
 * shared area lookup for the ones around [FEED_LAT]/[FEED_LON].
 */
@androidx.compose.runtime.Composable
/**
 * The shared activity feed for the areas around the map's starting point. A row
 * is about a place, so opening one hands its id back to the map.
 */
private fun DesktopFeedScreen(api: Api, db: Database, onOpenPlace: (Long) -> Unit) {
    var attempt by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<ActivityFeedState>(ActivityFeedState.Loading) }

    LaunchedEffect(attempt) {
        state = ActivityFeedState.Loading
        state = loadFeed(api, db)
    }

    ActivityFeedScreen(
        state = state,
        onItemClick = { key -> placeIdOf(key)?.let(onOpenPlace) },
        onRetry = { attempt++ },
    )
}

/** The feed's row keys are "type:placeId:date", so a row names the place it is about. */
private fun placeIdOf(key: String): Long? = key.split(':').getOrNull(1)?.toLongOrNull()

private suspend fun loadFeed(api: Api, db: Database): ActivityFeedState {
    val aliases = areaAliases(db)
    if (aliases.isEmpty()) {
        return ActivityFeedState.Empty("No area found for the feed.", retryable = true)
    }

    return try {
        val items = api.getActivity(areaIds = aliases, days = 7)
        if (items.isEmpty()) {
            ActivityFeedState.Empty("No recent activity.", retryable = true)
        } else {
            ActivityFeedState.Content(items.map { it.toRow() })
        }
    } catch (t: Throwable) {
        ActivityFeedState.Empty("Could not load the feed.", retryable = true)
    }
}

private suspend fun areaAliases(db: Database): List<String> {
    val controller = MapAreasController(db)
    return try {
        controller.load(FEED_LAT, FEED_LON)
        withTimeoutOrNull(5_000) { controller.areas.first { it.isNotEmpty() } }
            ?.map { it.urlAlias }
            ?: emptyList()
    } finally {
        controller.dispose()
    }
}

/** A feed item as a row, with the labels spelled out for the desktop. */
private fun ActivityFeedItem.toRow(): ActivityFeedRow {
    val icon = when (type) {
        ActivityFeedItem.TYPE_PLACE_ADDED -> "add_location"
        ActivityFeedItem.TYPE_PLACE_UPDATED -> "edit"
        ActivityFeedItem.TYPE_PLACE_BOOSTED -> "rocket_launch"
        ActivityFeedItem.TYPE_PLACE_COMMENTED -> "comment"
        ActivityFeedItem.TYPE_PLACE_DELETED -> "delete"
        else -> "place"
    }

    val subtitle = when (type) {
        ActivityFeedItem.TYPE_PLACE_BOOSTED -> durationDays?.let { "Boosted for $it days" }.orEmpty()
        ActivityFeedItem.TYPE_PLACE_COMMENTED -> comment.orEmpty()
        ActivityFeedItem.TYPE_PLACE_ADDED -> osmUserName?.let { "Added by $it" }.orEmpty()
        ActivityFeedItem.TYPE_PLACE_UPDATED -> osmUserName?.let { "Updated by $it" }.orEmpty()
        ActivityFeedItem.TYPE_PLACE_DELETED -> osmUserName?.let { "Deleted by $it" }.orEmpty()
        else -> osmUserName?.let { "by $it" }.orEmpty()
    }

    return ActivityFeedRow(
        key = "$type:$placeId:$date",
        icon = icon,
        placeName = placeName.orEmpty(),
        subtitle = subtitle,
        date = relativeDate(date),
    )
}

private fun relativeDate(date: String): String = runCatching {
    val then = java.time.OffsetDateTime.parse(date).atZoneSameInstant(java.time.ZoneId.systemDefault())
    val days = java.time.Duration.between(then, java.time.ZonedDateTime.now()).toDays()
    when {
        days <= 0L -> "today"
        days == 1L -> "yesterday"
        else -> "$days days ago"
    }
}.getOrDefault(date)

/**
 * The desktop's account page. It signs in with the same credentials the app
 * uses and stores the same session the rest of the app reads, so everything
 * that needs a signed-in user works once this succeeds. When signed in it shows
 * the shared profile (saved places and areas, change username and password, and
 * log out). Accounts are still created elsewhere (the app or btcmap.org).
 */
@androidx.compose.runtime.Composable
internal fun DesktopAccountScreen(
    api: Api,
    db: Database,
    settings: Settings,
    onBack: () -> Unit,
) {
    var authorized by remember { mutableStateOf(settings.authorized) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ScreenPage(title = "Account", onBack = onBack) {
        if (authorized) {
            DesktopProfile(
                api = api,
                db = db,
                settings = settings,
                onLoggedOut = { authorized = false },
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(text = "Username") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val response = api.signIn(
                                    username = username.trim(),
                                    password = password,
                                    label = DESKTOP_TOKEN_LABEL,
                                )
                                withContext(Dispatchers.IO) {
                                    settings.replaceSession(
                                        db = db,
                                        token = response.token,
                                        user = response.user.toDbUser(),
                                    )
                                }
                                password = ""
                                authorized = true
                            } catch (t: Throwable) {
                                error = t.message ?: t.toString()
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) {
                    Text(text = "Sign in")
                }
                Text(
                    text = "Accounts are created in the mobile app or on btcmap.org.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** The label this machine's session shows up under in the account's devices. */
private const val DESKTOP_TOKEN_LABEL = "BTC Map desktop"

private const val FEED_LAT = 52.2333742
private const val FEED_LON = 21.0711489

/**
 * Whether the account has the place saved. The saved places live in the cached
 * user row, which the sync and the save action both keep current.
 */
private suspend fun isPlaceSaved(db: Database, placeId: Long): Boolean =
    withContext(Dispatchers.IO) {
        db.user.select()?.savedPlaces?.any { it.id == placeId }
    } ?: false

/**
 * Saves or unsaves the place and caches the canonical list the endpoint
 * returns. The whole user is refetched, which is also what the app does when a
 * saved id is neither cached nor named.
 */
private suspend fun toggleSavedPlace(api: Api, db: Database, placeId: Long) {
    if (isPlaceSaved(db, placeId)) api.removeSavedPlace(placeId) else api.savePlace(placeId)

    val user = api.getUser().toDbUser()
    withContext(Dispatchers.IO) {
        db.transaction {
            db.user.delete()
            db.user.insert(user)
        }
    }
}

/** The reasons a report can give, in the order the form offers them. */
/** The shared settings list, used by both the window and the screenshot mode. */
@androidx.compose.runtime.Composable
internal fun DesktopSettingsScreen(
    settings: Settings,
    onOpenAccount: () -> Unit,
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
        onItemClick = { key -> if (key == "account") onOpenAccount() },
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
private const val HOSTED_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

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
    // The packaged app carries the font as a resource (see the build file); the
    // asset directory is the fallback when running from the sources.
    if (Thread.currentThread().contextClassLoader.getResource(ICON_FONT_RESOURCE) != null) {
        println("desktop: icon font $ICON_FONT_RESOURCE (bundled)")
        return FontFamily(Font(ICON_FONT_RESOURCE))
    }

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

private const val ICON_FONT_RESOURCE = "material-symbols.ttf"

private const val BUNDLED_MAP_SCHEME = "app"

/** Serves `app://map-styles/...` (sprites and glyphs) from the app resources. */
private fun bundledMapResources(): MapResourceProvider =
    MapResourceProvider(scheme = BUNDLED_MAP_SCHEME) { request ->
        // Glyph paths arrive percent-encoded ("Noto%20Sans%20Italic").
        val path = URLDecoder.decode(
            request.url.removePrefix("$BUNDLED_MAP_SCHEME://"),
            Charsets.UTF_8.name(),
        )
        resourceBytes(path) ?: throw java.io.FileNotFoundException(path)
    }

/** The bundled style at [assetPath], with its sprite and glyph URLs rewritten. */
private fun bundledStyleJson(assetPath: String): String {
    println("desktop: bundled map style $assetPath")
    return (
        resourceBytes(assetPath)?.decodeToString()
            ?: error("missing bundled map style $assetPath")
        ).replace("asset://map-styles/", "$BUNDLED_MAP_SCHEME://map-styles/")
}

private fun resourceBytes(path: String): ByteArray? =
    Thread.currentThread().contextClassLoader.getResourceAsStream(path)?.readBytes()


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
