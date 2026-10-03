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
import java.awt.Color as AwtColor
import androidx.compose.ui.graphics.Color
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.btcmap.api.ActivityFeedItem
import org.btcmap.api.getActivity
import org.btcmap.feed.feedKey
import org.btcmap.feed.iconGlyph
import org.btcmap.map.DEFAULT_MAP_CENTER_LAT
import org.btcmap.map.DEFAULT_MAP_CENTER_LON
import org.btcmap.map.DEFAULT_MAP_ZOOM
import org.btcmap.map.MapAreasController
import org.btcmap.ui.ActivityFeedRow
import org.btcmap.ui.ActivityFeedScreen
import org.btcmap.ui.ActivityFeedState
import org.btcmap.ui.MaterialSymbol
import org.btcmap.ui.map.SearchActions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
import org.btcmap.api.signIn
import org.btcmap.api.submitPlace
import org.btcmap.ui.PlaceAction
import org.btcmap.api.apiHttpClient
import org.btcmap.saved.SavedItems
import org.btcmap.bundle.BundledAreas
import org.btcmap.bundle.BundledComments
import org.btcmap.bundle.BundledEvents
import org.btcmap.bundle.BundledPlaces
import org.btcmap.boost.BoostPlan
import org.btcmap.db.Database
import org.btcmap.dbstats.DbStatsLabels
import org.btcmap.payment.PaymentInvoice
import org.btcmap.place.ReportType
import org.btcmap.place.submitReport
import org.btcmap.sync.Sync
import org.btcmap.sync.SyncManager
import org.btcmap.settings.KEY_AUTH_TOKEN
import org.btcmap.settings.MapColor
import org.btcmap.settings.MapStyle
import org.btcmap.settings.Settings
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.bundledStyleAsset
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.mapStyle
import org.maplibre.compose.resource.MapResourceProvider
import org.btcmap.settings.showAttribution
import org.btcmap.settings.verifiedFilterMinVerifiedAt
import org.btcmap.sync.SyncState
import org.btcmap.ui.AccountLabels
import org.btcmap.ui.AccountScreen
import org.btcmap.ui.AddPlaceForm
import org.btcmap.ui.AddPlaceLabels
import org.btcmap.ui.AddPlaceScreen
import org.btcmap.ui.AddCommentLabels
import org.btcmap.ui.AppTheme
import org.btcmap.ui.BoostScreen
import org.btcmap.ui.BoostScreenLabels
import org.btcmap.ui.ColorsPage
import org.btcmap.ui.ColorsPageLabels
import org.btcmap.ui.CommentScreen
import org.btcmap.ui.CommentScreenLabels
import org.btcmap.ui.DbStatsPage
import org.btcmap.ui.DbStatsPageLabels
import org.btcmap.ui.InvoicePaymentLabels
import org.btcmap.ui.InvoicePaymentSection
import org.btcmap.ui.InvoicePaymentSectionLabels
import org.btcmap.ui.MapScreen
import org.btcmap.ui.ProfileFormLabels
import org.btcmap.ui.ProfileScreen
import org.btcmap.ui.ReportPlaceLabels
import org.btcmap.ui.ReportPlaceScreen
import org.btcmap.ui.SettingsPage
import org.btcmap.ui.SettingsPageLabels
import org.btcmap.ui.StatsScreen
import org.btcmap.ui.UserProfileLabels
import org.btcmap.ui.areaChipPalette
import org.btcmap.ui.markerPalette
import java.io.File
import java.io.InputStream
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

    // The bundled snapshots are on the classpath (see the build file), so the
    // desktop seeds offline like Android and the first sync only fetches the
    // delta since the snapshot was generated.
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
        seedPlaces = { onBatch ->
            BundledPlaces.import(db, onBatch) { bundledSnapshot(BundledPlaces.FILE_NAME) }.placesImported
        },
        seedEvents = { BundledEvents.import(db) { bundledSnapshot(BundledEvents.FILE_NAME) }.eventsImported },
        seedComments = {
            BundledComments.import(db) { bundledSnapshot(BundledComments.FILE_NAME) }.commentsImported
        },
        seedAreas = { BundledAreas.import(db) { bundledSnapshot(BundledAreas.FILE_NAME) }.areasImported },
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
                        bookmarked = selectedPlaceId?.let { SavedItems.isPlaceSaved(db, it) } ?: false
                    }
                    // The place a feed row asked the map to show. Leaving the
                    // map disposes it and coming back rebuilds it, so opening a
                    // row always lands on the map with that place selected.
                    var feedPlaceId by remember { mutableStateOf<Long?>(null) }
                    // Where the map is looking, tracked so the feed lists the
                    // areas the user is actually around rather than a fixed
                    // point. Starts at the shared default view.
                    var mapCenterLat by remember { mutableStateOf(DEFAULT_MAP_CENTER_LAT) }
                    var mapCenterLon by remember { mutableStateOf(DEFAULT_MAP_CENTER_LON) }
                    // Where the add-place screen was opened from the map.
                    var addPlace by remember { mutableStateOf<Pair<Double, Double>?>(null) }
                    // The place a boost or comment payment screen is for.
                    var paymentPlace by remember { mutableStateOf<Pair<Long, String>?>(null) }

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
                            initialLat = DEFAULT_MAP_CENTER_LAT,
                            initialLon = DEFAULT_MAP_CENTER_LON,
                            initialZoom = DEFAULT_MAP_ZOOM,
                            // The user's verification window and colour choices
                            // come from the shared settings, so the desktop map
                            // honours the same screen Android does.
                            minVerifiedAt = settings.verifiedFilterMinVerifiedAt(),
                            palette = markerPalette(settings),
                            areaChipPalette = areaChipPalette(settings),
                            apiUrl = API_URL,
                            usingOpenFreeMap = true,
                            mapRotationEnabled = settings.mapRotationEnabled,
                            showAttribution = settings.showAttribution,
                            iconFont = iconFont,
                            placeSheetStrings = PLACE_SHEET_STRINGS,
                            attributionText = "© OpenStreetMap contributors",
                            attributionTextColor = if (darkTheme) {
                                Color.White
                            } else {
                                Color.Black.copy(alpha = 0.8f)
                            },
                            searchActions = SearchActions(onSettings = { route = Route.Settings }),
                            onAddPlace = { lat, lon ->
                                addPlace = lat to lon
                                route = if (settings.authorized) Route.AddPlace else Route.Account
                            },
                            onOpenFeed = { route = Route.Feed },
                            bookmarked = bookmarked,
                            onPlaceSelected = { selectedPlaceId = it.id },
                            onPlaceDismissed = {
                                selectedPlaceId = null
                                // The feed row that opened this place has been
                                // seen; do not reopen it when the map returns.
                                feedPlaceId = null
                            },
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
                                                SavedItems.togglePlace(api, db, place.id, place.name.orEmpty())
                                                bookmarked = SavedItems.isPlaceSaved(db, place.id)
                                            }
                                        } else {
                                            route = Route.Account
                                        }
                                    }

                                    // The sats-paid actions open their own
                                    // payment screens; no session is needed, the
                                    // Lightning payment is the gate.
                                    PlaceAction.Boost -> {
                                        paymentPlace = place.id to place.name.orEmpty()
                                        route = Route.Boost
                                    }

                                    PlaceAction.AddComment -> {
                                        paymentPlace = place.id to place.name.orEmpty()
                                        route = Route.AddComment
                                    }

                                    else -> handlePlaceAction(place, action)
                                }
                            },
                            onSelectEvent = {},
                            onSelectArea = {},
                            onCameraIdle = { lat, lon, _ ->
                                mapCenterLat = lat
                                mapCenterLon = lon
                            },
                            formatDistance = { meters -> "%.1f km".format(meters / 1000) },
                        )

                        Route.Report -> ScreenPage(
                            title = reportPlace?.second?.ifBlank { "Report a place" } ?: "Report a place",
                            onBack = { route = Route.Map },
                        ) {
                            ReportPlaceScreen(
                                initialType = reportType,
                                labels = REPORT_LABELS,
                                submit = { draft ->
                                    api.submitReport(placeId = reportPlace?.first ?: 0L, draft = draft)
                                },
                                pickPhotos = reportPhotoPicker(window),
                                onBack = { route = Route.Map },
                            )
                        }

                        Route.AddPlace -> ScreenPage(
                            title = "Add a place",
                            onBack = { route = Route.Map },
                        ) {
                            AddPlaceScreen(
                                lat = addPlace?.first ?: 0.0,
                                lon = addPlace?.second ?: 0.0,
                                styleUrl = HOSTED_STYLE_URL,
                                styleJson = styleJson,
                                labels = ADD_PLACE_LABELS,
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
                        }

                        Route.AddComment -> ScreenPage(
                            title = paymentPlace?.second?.ifBlank { "Add comment" } ?: "Add comment",
                            onBack = { route = Route.Map },
                        ) {
                            CommentScreen(
                                api = api,
                                placeId = paymentPlace?.first ?: 0L,
                                labels = COMMENT_LABELS,
                                onPay = { openUrl("lightning:$it") },
                                onCopy = { copyToClipboard(it) },
                                onBack = { route = Route.Map },
                            )
                        }

                        Route.Boost -> ScreenPage(
                            title = paymentPlace?.second?.ifBlank { "Boost merchant" } ?: "Boost merchant",
                            onBack = { route = Route.Map },
                        ) {
                            BoostScreen(
                                api = api,
                                placeId = paymentPlace?.first ?: 0L,
                                labels = BOOST_LABELS,
                                onPay = { openUrl("lightning:$it") },
                                onCopy = { copyToClipboard(it) },
                                onBack = { route = Route.Map },
                            )
                        }

                        Route.Settings -> ScreenPage(
                            title = "Settings",
                            onBack = { route = Route.Map },
                        ) {
                            SettingsPage(
                                settings = settings,
                                db = db,
                                labels = SETTINGS_PAGE_LABELS,
                                includeImageStats = false,
                                onOpenAccount = { route = Route.Account },
                                onOpenColors = { route = Route.Colors },
                                onOpenDbStats = { route = Route.DbStats },
                            )
                        }

                        Route.Colors -> ScreenPage(
                            title = "Customize colors",
                            onBack = { route = Route.Settings },
                        ) {
                            ColorsPage(settings = settings, labels = COLORS_PAGE_LABELS)
                        }

                        Route.DbStats -> ScreenPage(
                            title = "Database",
                            onBack = { route = Route.Settings },
                        ) {
                            DbStatsPage(
                                db = db,
                                settings = settings,
                                syncState = syncState,
                                labels = DB_STATS_PAGE_LABELS,
                                onSync = { syncManager.start() },
                            )
                        }

                        Route.Account -> ScreenPage(
                            title = "Account",
                            onBack = { route = Route.Settings },
                        ) {
                            AccountScreen(
                                api = api,
                                db = db,
                                settings = settings,
                                tokenLabel = DESKTOP_TOKEN_LABEL,
                                labels = ACCOUNT_LABELS,
                                profile = { onLoggedOut ->
                                    ProfileScreen(
                                        api = api,
                                        db = db,
                                        settings = settings,
                                        profileLabels = PROFILE_LABELS,
                                        formLabels = PROFILE_FORM_LABELS,
                                        onLoggedOut = onLoggedOut,
                                    )
                                },
                            )
                        }

                        Route.Feed -> ScreenPage(
                            title = "Activity",
                            onBack = { route = Route.Map },
                        ) {
                            DesktopFeedScreen(
                                api = api,
                                db = db,
                                lat = mapCenterLat,
                                lon = mapCenterLon,
                            ) { placeId ->
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
private enum class Route { Map, Feed, Settings, Colors, DbStats, Account, Report, AddPlace, AddComment, Boost }

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
                    "settings" -> SettingsPage(
                        settings = settings,
                        db = db,
                        labels = SETTINGS_PAGE_LABELS,
                        includeImageStats = false,
                        onOpenAccount = {},
                        onOpenColors = {},
                        onOpenDbStats = {},
                    )

                    "colors" -> ColorsPage(settings = settings, labels = COLORS_PAGE_LABELS)
                    "dbstats" -> DbStatsPage(
                        db = db,
                        settings = settings,
                        syncState = SyncState.Idle,
                        labels = DB_STATS_PAGE_LABELS,
                        onSync = {},
                    )

                    "report" -> ReportPlaceScreen(
                        initialType = "verified",
                        labels = REPORT_LABELS,
                        submit = {},
                        onBack = {},
                    )

                    "account" -> {
                        val api = Api(
                            httpClient = apiHttpClient(
                                userAgent = USER_AGENT,
                                token = { settings.getString(KEY_AUTH_TOKEN, null) },
                                apiUrl = { API_URL.toHttpUrl() },
                            ),
                            baseUrl = { API_URL.toHttpUrl() },
                            userAgent = USER_AGENT,
                        )
                        AccountScreen(
                            api = api,
                            db = db,
                            settings = settings,
                            tokenLabel = DESKTOP_TOKEN_LABEL,
                            labels = ACCOUNT_LABELS,
                            profile = { onLoggedOut ->
                                ProfileScreen(
                                    api = api,
                                    db = db,
                                    settings = settings,
                                    profileLabels = PROFILE_LABELS,
                                    formLabels = PROFILE_FORM_LABELS,
                                    onLoggedOut = onLoggedOut,
                                )
                            },
                        )
                    }

                    "payment" -> InvoicePaymentSection(
                        invoice = PaymentInvoice(id = "demo", bolt11 = "lnbc1u1p3exampleinvoice"),
                        labels = INVOICE_SECTION_LABELS,
                        onPay = {},
                        onCopy = {},
                        onStartOver = {},
                    )

                    "addplace" -> AddPlaceForm(
                        busy = false,
                        error = null,
                        submitted = false,
                        labels = ADD_PLACE_LABELS,
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
 * The shared activity feed for the areas around the map at [lat]/[lon]. A row
 * is about a place, so opening one hands its id back to the map.
 */
@androidx.compose.runtime.Composable
private fun DesktopFeedScreen(
    api: Api,
    db: Database,
    lat: Double,
    lon: Double,
    onOpenPlace: (Long) -> Unit,
) {
    var attempt by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<ActivityFeedState>(ActivityFeedState.Loading) }

    LaunchedEffect(attempt, lat, lon) {
        state = ActivityFeedState.Loading
        state = loadFeed(api, db, lat, lon)
    }

    ActivityFeedScreen(
        state = state,
        onItemClick = { key -> placeIdOf(key)?.let(onOpenPlace) },
        onRetry = { attempt++ },
    )
}

/** The feed's row keys are "type:placeId:date", so a row names the place it is about. */
private fun placeIdOf(key: String): Long? = key.split(':').getOrNull(1)?.toLongOrNull()

private suspend fun loadFeed(api: Api, db: Database, lat: Double, lon: Double): ActivityFeedState {
    val aliases = areaAliases(db, lat, lon)
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

private suspend fun areaAliases(db: Database, lat: Double, lon: Double): List<String> {
    val controller = MapAreasController(db)
    return try {
        controller.load(lat, lon)
        withTimeoutOrNull(5_000) { controller.areas.first { it.isNotEmpty() } }
            ?.map { it.urlAlias }
            ?: emptyList()
    } finally {
        controller.dispose()
    }
}

/** A feed item as a row, with the labels spelled out for the desktop. */
private fun ActivityFeedItem.toRow(): ActivityFeedRow {
    val subtitle = when (type) {
        ActivityFeedItem.TYPE_PLACE_BOOSTED -> durationDays?.let { "Boosted for $it days" }.orEmpty()
        ActivityFeedItem.TYPE_PLACE_COMMENTED -> comment.orEmpty()
        ActivityFeedItem.TYPE_PLACE_ADDED -> osmUserName?.let { "Added by $it" }.orEmpty()
        ActivityFeedItem.TYPE_PLACE_UPDATED -> osmUserName?.let { "Updated by $it" }.orEmpty()
        ActivityFeedItem.TYPE_PLACE_DELETED -> osmUserName?.let { "Deleted by $it" }.orEmpty()
        else -> osmUserName?.let { "by $it" }.orEmpty()
    }

    return ActivityFeedRow(
        key = feedKey(),
        icon = iconGlyph(),
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

/**
 * Opens a bundled snapshot resource. A missing resource throws
 * [java.io.FileNotFoundException], the same signal a missing Android asset
 * gives, which the shared seeders treat as an absent optional snapshot rather
 * than an error.
 */
private fun bundledSnapshot(fileName: String): InputStream =
    Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)
        ?: throw java.io.FileNotFoundException(fileName)

private val INVOICE_LABELS = InvoicePaymentLabels(
    qrDescription = "Lightning invoice QR code",
    pay = "Pay",
    copy = "Copy",
    startOver = "Start over",
)

private val INVOICE_SECTION_LABELS = InvoicePaymentSectionLabels(
    invoice = INVOICE_LABELS,
    discardMessage = "Discard this invoice? It stays payable until it expires, so " +
        "starting over and paying the old one too would charge you twice.",
    discard = "Discard",
    cancel = "Cancel",
)

private val BOOST_LABELS = BoostScreenLabels(
    active = "Your boost is active.",
    backToMap = "Back to the map",
    planLabel = {
        when (it) {
            BoostPlan.ONE_MONTH -> "1 month"
            BoostPlan.THREE_MONTHS -> "3 months"
            BoostPlan.TWELVE_MONTHS -> "12 months"
        }
    },
    description = "Boosting a place keeps it at the top of search results and " +
        "highlights it on the map.",
    durationTitle = "For how long?",
    continueLabel = "Continue",
    invoice = INVOICE_SECTION_LABELS,
)

private val COMMENT_LABELS = CommentScreenLabels(
    posted = "Your comment has been posted.",
    backToMap = "Back to the map",
    form = AddCommentLabels(
        disclosure = "A comment carries a small anti-spam fee, paid in sats over the " +
            "Lightning Network.",
        currentFee = "Current fee",
        comment = "Comment",
        placeholder = "Your comment",
        continueLabel = "Continue",
        emptyComment = "The comment cannot be empty.",
        failedToLoad = "The fee could not be loaded.",
        tapToRetry = "Tap to retry",
    ),
    invoice = INVOICE_SECTION_LABELS,
)

private val VERIFIED_FILTER_YEARS = listOf(1, 2, 3)

private fun mapStyleName(style: MapStyle): String = when (style) {
    MapStyle.Auto -> "Auto"
    MapStyle.Liberty -> "OpenFreeMap Liberty"
    MapStyle.Positron -> "OpenFreeMap Positron"
    MapStyle.Bright -> "OpenFreeMap Bright"
    MapStyle.Dark -> "OpenFreeMap Dark"
    MapStyle.DarkMatter -> "OpenFreeMap Dark Matter"
}

private fun verifiedFilterName(years: Int): String = when (years) {
    1 -> "Verified within 1 year"
    2 -> "Verified within 2 years"
    3 -> "Verified within 3 years"
    else -> ""
}

private fun mapColorTitle(color: MapColor): String = when (color) {
    MapColor.MarkerBackground -> "Marker background"
    MapColor.MarkerIcon -> "Marker icon"
    MapColor.BoostedMarkerBackground -> "Boosted marker background"
    MapColor.BoostedMarkerIcon -> "Boosted marker icon"
    MapColor.BadgeBackground -> "Badge background"
    MapColor.BadgeText -> "Badge text"
    MapColor.ButtonBackground -> "Button background"
    MapColor.ButtonIcon -> "Button icon"
    MapColor.ButtonBorder -> "Button border"
}

private fun syncStateLabel(state: SyncState): String = when (state) {
    SyncState.Idle -> "Idle"
    SyncState.UnbundlingPlaces -> "Unbundling places"
    SyncState.SyncingPlaces -> "Syncing places"
    SyncState.UnbundlingEvents -> "Unbundling events"
    SyncState.SyncingEvents -> "Syncing events"
    SyncState.UnbundlingComments -> "Unbundling comments"
    SyncState.SyncingComments -> "Syncing comments"
    SyncState.UnbundlingAreas -> "Unbundling areas"
    SyncState.SyncingAreas -> "Syncing areas"
}

private val DESKTOP_DB_STATS_LABELS = DbStatsLabels(
    database = "Database",
    file = "File",
    version = "Version",
    size = "Size",
    table = { "Table $it" },
    bundle = { "Bundle $it" },
    location = "Location",
    visibleRows = "Visible rows",
    deletedRows = "Deleted rows",
    futureRows = "Future rows",
    rows = "Rows",
    newestUpdate = "Newest update",
)

private val SETTINGS_PAGE_LABELS = SettingsPageLabels(
    account = "Account",
    logIn = "Log in",
    loggedInAs = { "Logged in as $it" },
    openProfile = "Click to see your profile",
    mapStyle = "Map style",
    mapStyleValue = ::mapStyleName,
    customizeColors = "Customize colors",
    customizeColorsSecondary = "Marker, badge and button colors",
    verifiedFilter = "Only show places",
    verifiedFilterValue = ::verifiedFilterName,
    verifiedFilterYears = VERIFIED_FILTER_YEARS,
    showAttribution = "Show attribution",
    showAttributionSecondary = "Visible by default per OSM policy",
    mapRotation = "Allow map rotation",
    mapRotationSecondary = "Compass appears when not facing north",
    dbStats = "Database",
    dbStatsSecondary = "Database metadata and table management",
    imageStats = "Image cache",
    imageStatsSecondary = "Memory and disk caches metadata plus load stats",
    mapStyleDialogTitle = "Map style",
    verifiedFilterDialogTitle = "Only show places",
    close = "Close",
)

private val COLORS_PAGE_LABELS = ColorsPageLabels(
    colorTitle = ::mapColorTitle,
    red = "Red",
    green = "Green",
    blue = "Blue",
    alpha = "Alpha",
    ok = "OK",
    reset = "Reset",
    cancel = "Cancel",
)

private val DB_STATS_PAGE_LABELS = DbStatsPageLabels(
    dbStats = DESKTOP_DB_STATS_LABELS,
    sync = "Sync",
    source = "Source",
    state = "State",
    syncNow = "Sync now",
    syncStateLabel = ::syncStateLabel,
)

private const val DESKTOP_TOKEN_LABEL = "BTC Map desktop"

private val PROFILE_LABELS = UserProfileLabels(
    username = "Username",
    password = "Password",
    savedPlaces = "Saved places",
    savedAreas = "Saved areas",
    noSavedPlaces = "No saved places.",
    noSavedAreas = "No saved areas.",
    logOut = "Log out",
    editUsername = "Change username",
    editPassword = "Change password",
    delete = "Delete",
)

private val PROFILE_FORM_LABELS = ProfileFormLabels(
    passwordMask = "••••••••",
    required = "Required",
    changeUsernameTitle = "Change username",
    changePasswordTitle = "Change password",
    username = "Username",
    currentPassword = "Current password",
    newPassword = "New password",
    confirmPassword = "Confirm password",
    passwordsDoNotMatch = "Passwords do not match",
    passwordTooShort = { "At least $it characters" },
    save = "Save",
    cancel = "Cancel",
    usernameChanged = "Username changed.",
    passwordChanged = "Password changed.",
)

private val ACCOUNT_LABELS = AccountLabels(
    username = "Username",
    password = "Password",
    confirmPassword = "Confirm password",
    required = "Required",
    passwordTooShort = { "At least $it characters" },
    passwordsDoNotMatch = "Passwords do not match",
    signIn = "Sign in",
    createAccount = "Create account",
    alreadyHaveAccount = "I already have an account",
    createAnAccount = "Create an account",
    signInHint = "Accounts can also be created in the mobile app or on btcmap.org.",
    accountCreated = "Account created. Please sign in.",
)

private val REPORT_LABELS = ReportPlaceLabels(
    intro = "Let editors know the current state of this place. Your report will be " +
        "reviewed by the BTC Map community.",
    reasonLabel = {
        when (it) {
            ReportType.Verified -> "Verified - still accepts Bitcoin"
            ReportType.RefusedSats -> "Refused Bitcoin payment"
            ReportType.OutOfBusiness -> "Out of business"
        }
    },
    reasonDescription = {
        when (it) {
            ReportType.Verified ->
                "You confirmed this place exists and currently accepts Bitcoin payments."

            ReportType.RefusedSats ->
                "An attempt to pay with Bitcoin on-site was refused by the merchant."

            ReportType.OutOfBusiness ->
                "The place has been permanently closed or is no longer operating."
        }
    },
    noteHint = "Additional notes (optional)",
    addPhoto = { taken, max -> "Add photo ($taken/$max)" },
    removePhoto = "Remove photo",
    submit = "Submit",
    submitted = "Report submitted.",
    backToMap = "Back to the map",
)

private val ADD_PLACE_LABELS = AddPlaceLabels(
    name = "Name",
    category = "Category",
    address = "Address",
    website = "Website (optional)",
    description = "Description (optional)",
    required = "Required",
    submit = "Submit place",
    submitted = "Place submitted for review.",
    backToMap = "Back to the map",
)

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
    commentsTitle = { "Comments ($it)" },
    addComment = "Add comment",
    watch = "Watch",
    unwatch = "Unwatch",
    addPhoto = "Add photo",
    openingHoursClosed = "Closed",
    openingHoursOpen24_7 = "Open 24/7",
)
