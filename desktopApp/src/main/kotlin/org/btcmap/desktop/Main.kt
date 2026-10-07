package org.btcmap.desktop

import kotlinx.coroutines.runBlocking
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.pollSystemTheme
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.btcmap.account.canManageEvents
import org.btcmap.account.isAdmin
import org.btcmap.api.ActivityFeedItem
import org.btcmap.api.getActivity
import org.btcmap.api.getDashboard
import org.btcmap.api.getPendingEvents
import org.btcmap.api.setEventStatus
import org.btcmap.api.deletePlaceImage
import org.btcmap.api.getPlaceImages
import org.btcmap.feed.feedKey
import org.btcmap.feed.iconGlyph
import org.btcmap.map.DEFAULT_MAP_CENTER_LAT
import org.btcmap.map.DEFAULT_MAP_CENTER_LON
import org.btcmap.map.DEFAULT_MAP_ZOOM
import org.btcmap.map.MapAreasController
import org.btcmap.place.canDeletePlaceImage
import org.btcmap.ui.ActivityFeedRow
import org.btcmap.ui.ActivityFeedScreen
import org.btcmap.ui.ActivityFeedState
import org.btcmap.ui.MaterialSymbol
import org.btcmap.ui.map.AddLocationLabels
import org.btcmap.ui.map.EventMiniMap
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
import org.btcmap.util.toUrl
import org.btcmap.api.Api
import org.btcmap.api.signIn
import org.btcmap.api.submitEvent
import org.btcmap.api.submitPlace
import org.btcmap.ui.PlaceAction
import org.btcmap.ui.PendingEventUi
import org.btcmap.ui.PlacePhoto
import org.btcmap.ui.toPlacePhoto
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
import org.btcmap.util.rethrowIfCancellation
import org.btcmap.settings.KEY_AUTH_TOKEN
import org.btcmap.settings.MapColor
import org.btcmap.settings.MapStyle
import org.btcmap.settings.Settings
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.bundledStyleAsset
import org.btcmap.settings.isDark
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.mapStyle
import org.maplibre.compose.resource.MapResourceProvider
import org.btcmap.settings.showAttribution
import org.btcmap.settings.verifiedFilterMinVerifiedAt
import org.btcmap.sync.SyncState
import org.btcmap.ui.AccountLabels
import org.btcmap.ui.AccountScreen
import org.btcmap.ui.AddEventForm
import org.btcmap.ui.AddEventLabels
import org.btcmap.ui.AddEventScreen
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
import org.btcmap.ui.EventReviewLabels
import org.btcmap.ui.EventReviewScreen
import org.btcmap.ui.InfraDashboardScreen
import org.btcmap.ui.InvoicePaymentLabels
import org.btcmap.ui.InvoicePaymentSection
import org.btcmap.ui.InvoicePaymentSectionLabels
import org.btcmap.ui.MapScreen
import org.btcmap.ui.ProfileFormLabels
import org.btcmap.ui.MyEventsLabels
import org.btcmap.ui.ProfileScreen
import org.btcmap.ui.UploadedImagesLabels
import org.btcmap.ui.ReportPlaceLabels
import org.btcmap.ui.ReportPlaceScreen
import org.btcmap.ui.ScreenPage
import org.btcmap.ui.SettingsPage
import org.btcmap.ui.SettingsPageLabels
import org.btcmap.ui.StatsScreen
import org.btcmap.ui.UserProfileLabels
import org.btcmap.ui.AreaScreen
import org.btcmap.ui.AreaStrings
import org.btcmap.ui.EventScreen
import org.btcmap.ui.EventScreenLabels
import org.btcmap.ui.areaChipPalette
import org.btcmap.ui.markerPalette
import org.btcmap.ui.rememberNavController
import org.btcmap.api.GetEventsItem
import org.btcmap.area.AreaIssues
import org.btcmap.area.AreaPlaceIssue
import org.btcmap.area.AreaSections
import org.btcmap.area.websiteDisplayText
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.i18n.getLocalizedDescription
import org.btcmap.i18n.getLocalizedName
import org.btcmap.map.toEventGeoJson
import java.io.File
import java.net.URLDecoder
import okio.Source
import okio.source

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

@OptIn(ExperimentalComposeUiApi::class)
private fun runApp() = application {
    // Compose Desktop only polls the OS theme for changes on Windows and macOS;
    // on Linux the poll is off by default, so the window would keep its
    // launch-time theme until restarted. Turn it on here so all three desktop
    // platforms follow a runtime switch; the shared screens read
    // isSystemInDarkTheme(), which the poll updates. It is a no-op where the
    // poll is already on (Windows and macOS).
    ComposeUiFlags.pollSystemTheme = true

    val home = DesktopHome()
    val db = home.database()
    val settings = runBlocking { home.settings(db).apply { preload() } }

    // The bundled snapshots are on the classpath (see the build file), so the
    // desktop seeds offline like Android and the first sync only fetches the
    // delta since the snapshot was generated.
    val api = Api(
        httpClient = apiHttpClient(USER_AGENT),
        baseUrl = { API_URL.toUrl() },
        token = { settings.getString(KEY_AUTH_TOKEN, null) },
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
        state = rememberWindowState(placement = WindowPlacement.Maximized),
    ) {
        val mapHost = rememberAwtComposeMapPresentationHost(window)
        ProvideMapPresentationHost(mapHost) {
            AppTheme(iconFont = iconFont) {
                // The window's own background is white, which shows through the
                // transparent rows of screens like the settings list.
                Surface(modifier = Modifier.fillMaxSize()) {
                    val syncState by syncManager.state.collectAsState()

                    // The map screen owns its own affordances (the search bar's
                    // settings, the chips and the pulse button), so the desktop
                    // keeps no navigation of its own: the settings and the feed
                    // are full-window pages the map opens. The shared
                    // NavController is the back stack those pages push and pop.
                    val nav = rememberNavController(Route.Map)
                    val route = nav.current
                    val scope = rememberCoroutineScope()
                    // The place the sheet is showing, and whether the account has
                    // it saved, which is what the sheet's Save row renders.
                    var selectedPlaceId by remember { mutableStateOf<Long?>(null) }
                    // The place and reason a report was opened with, if any.
                    var reportPlace by remember { mutableStateOf<Pair<Long, String>?>(null) }
                    var reportType by remember { mutableStateOf<String?>(null) }
                    var bookmarked by remember { mutableStateOf(false) }
                    // The selected place's photo thumbnails, loaded from the image
                    // endpoint the way the Android map does. Empty until they
                    // arrive, and for a place without photos.
                    var selectedPhotos by remember { mutableStateOf<List<PlacePhoto>>(emptyList()) }
                    // Bumped after a photo is deleted to reload the strip.
                    var photosReloadKey by remember { mutableStateOf(0) }
                    LaunchedEffect(selectedPlaceId, photosReloadKey) {
                        val placeId = selectedPlaceId
                        bookmarked = placeId?.let { SavedItems.isPlaceSaved(db, it) } ?: false
                        selectedPhotos = placeId?.let { id ->
                            try {
                                val user = db.user.select()
                                api.getPlaceImages(id).map { image ->
                                    api.toPlacePhoto(
                                        image,
                                        canDelete = user?.canDeletePlaceImage(image) ?: false,
                                    )
                                }
                            } catch (t: Throwable) {
                                t.rethrowIfCancellation()
                                emptyList()
                            }
                        } ?: emptyList()
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
                    // Where the add-event screen was opened from, and what it is
                    // pre-filled with: blank from the map, an event to duplicate
                    // from the profile. Its back target is the stack entry below
                    // it, so the map and the profile both return correctly.
                    var addEvent by remember { mutableStateOf<AddEventPrefill?>(null) }
                    // The place a boost or comment payment screen is for.
                    var paymentPlace by remember { mutableStateOf<Pair<Long, String>?>(null) }
                    // The area a chip (or an area search result) opened, and the
                    // event an area row or search result opened. An opened event
                    // returns to the stack entry below it, so the area that listed
                    // it and the map both return correctly.
                    var selectedAreaId by remember { mutableStateOf<Long?>(null) }
                    var selectedEvent by remember { mutableStateOf<Event?>(null) }

                    // Whether the signed-in account may open the infrastructure
                    // dashboard. Re-derived whenever the map is (re)entered, so a
                    // sign-in or sign-out is reflected.
                    var isAdmin by remember { mutableStateOf(false) }
                    var canManageEvents by remember { mutableStateOf(false) }
                    var pendingEventCount by remember { mutableStateOf(0) }
                    // Bumped after a sync changes the data, so the map re-queries
                    // its features rather than leaving them stale until the next
                    // camera move, as Android's MapRoute does.
                    var reloadKey by remember { mutableStateOf(0) }
                    LaunchedEffect(route) {
                        if (route == Route.Map) {
                            val user = db.user.select()
                            isAdmin = user?.isAdmin() == true
                            canManageEvents = user?.canManageEvents() == true
                            pendingEventCount = if (canManageEvents) {
                                runCatching { api.getPendingEvents().size }.getOrDefault(0)
                            } else {
                                0
                            }
                        }
                    }

                    // Started every time the map is (re)entered, as Android's
                    // MapRoute does with its own LaunchedEffect, so returning
                    // from a sub-screen refreshes the data and shows the sync
                    // indicator. The manager ignores a start while one is
                    // already running, and the job lives on its own scope. The
                    // sync's events bump reloadKey so the map re-queries.
                    LaunchedEffect(route) {
                        if (route == Route.Map) {
                            syncManager.start()
                            syncManager.events.collect { reloadKey++ }
                        }
                    }

                    // Bumped by the infrastructure dashboard's toolbar refresh.
                    var infraRefreshKey by remember { mutableStateOf(0) }
                    var infraRefreshing by remember { mutableStateOf(false) }

                    // The bundled style, shared by the map and the add-place map.
                    // Its sprite and glyph URLs are served from the app's
                    // resources (the style itself references them with Android's
                    // asset:// scheme, which the Compose map cannot read).
                    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                    val styleAsset = settings.mapStyle.bundledStyleAsset(darkSystemTheme = darkTheme)
                    val styleJson = remember(styleAsset) { bundledStyleJson(styleAsset) }
                    // The attribution colour follows the map style's light/dark
                    // tone, shared by the main map and the add-place map.
                    val attributionColor = if (settings.mapStyle.isDark(darkTheme)) {
                        Color.White
                    } else {
                        Color.Black.copy(alpha = 0.8f)
                    }

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
                            syncVisible = syncState != SyncState.Idle,
                            reloadKey = reloadKey,
                            iconFont = iconFont,
                            placeSheetStrings = PLACE_SHEET_STRINGS,
                            attributionText = "© OpenStreetMap contributors",
                            attributionTextColor = attributionColor,
                            searchActions = SearchActions(onSettings = { nav.push(Route.Settings) }),
                            onAddPlace = { lat, lon ->
                                addPlace = lat to lon
                                nav.push(if (settings.authorized) Route.AddPlace else Route.Account)
                            },
                            onAddEvent = { lat, lon ->
                                addEvent = AddEventPrefill(lat = lat, lon = lon)
                                nav.push(if (settings.authorized) Route.AddEvent else Route.Account)
                            },
                            addLocationLabels = ADD_LOCATION_LABELS,
                            onOpenFeed = { nav.push(Route.Feed) },
                            onOpenInfra = if (isAdmin) {
                                { nav.push(Route.Infra) }
                            } else {
                                null
                            },
                            onOpenEventReview = if (canManageEvents) {
                                { nav.push(Route.EventReview) }
                            } else {
                                null
                            },
                            pendingEventCount = pendingEventCount,
                            bookmarked = bookmarked,
                            photos = selectedPhotos,
                            onDeletePhoto = { photo ->
                                scope.launch {
                                    try {
                                        api.deletePlaceImage(photo.placeId, photo.imageId)
                                    } catch (t: Throwable) {
                                        t.rethrowIfCancellation()
                                    }
                                    photosReloadKey++
                                }
                            },
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
                                            nav.push(Route.Report)
                                        } else {
                                            nav.push(Route.Account)
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
                                            nav.push(Route.Account)
                                        }
                                    }

                                    // The sats-paid actions open their own
                                    // payment screens; no session is needed, the
                                    // Lightning payment is the gate.
                                    PlaceAction.Boost -> {
                                        paymentPlace = place.id to place.name.orEmpty()
                                        nav.push(Route.Boost)
                                    }

                                    PlaceAction.AddComment -> {
                                        paymentPlace = place.id to place.name.orEmpty()
                                        nav.push(Route.AddComment)
                                    }

                                    else -> handlePlaceAction(place, action)
                                }
                            },
                            onSelectEvent = { event ->
                                selectedEvent = event
                                nav.push(Route.Event)
                            },
                            onSelectArea = { areaId ->
                                selectedAreaId = areaId
                                nav.push(Route.Area)
                            },
                            onCameraIdle = { lat, lon, _ ->
                                mapCenterLat = lat
                                mapCenterLon = lon
                            },
                            formatDistance = { meters ->
                                val km = meters / 1_000
                                // Beyond 10 km the fraction is noise.
                                if (km > 10) "%.0f km".format(km) else "%.1f km".format(km)
                            },
                        )

                        Route.Report -> ScreenPage(
                            title = reportPlace?.second?.ifBlank { "Report a place" } ?: "Report a place",
                            onBack = { nav.pop() },
                        ) {
                            ReportPlaceScreen(
                                initialType = reportType,
                                labels = REPORT_LABELS,
                                submit = { draft ->
                                    api.submitReport(placeId = reportPlace?.first ?: 0L, draft = draft)
                                },
                                pickPhotos = reportPhotoPicker(window),
                                onBack = { nav.pop() },
                            )
                        }

                        // The shared screen draws its own top bar and back
                        // affordance, so the route is the screen itself, not a
                        // ScreenPage wrapping it.
                        Route.AddPlace -> AddPlaceScreen(
                            lat = addPlace?.first ?: 0.0,
                            lon = addPlace?.second ?: 0.0,
                            styleUrl = HOSTED_STYLE_URL,
                            styleJson = styleJson,
                            labels = ADD_PLACE_LABELS,
                            iconFont = iconFont,
                            palette = markerPalette(settings),
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
                            onBack = { nav.pop() },
                        )

                        Route.AddEvent -> AddEventScreen(
                            lat = addEvent?.lat ?: 0.0,
                            lon = addEvent?.lon ?: 0.0,
                            styleUrl = HOSTED_STYLE_URL,
                            styleJson = styleJson,
                            labels = ADD_EVENT_LABELS,
                            iconFont = iconFont,
                            palette = markerPalette(settings),
                            initialName = addEvent?.name.orEmpty(),
                            initialWebsite = addEvent?.website.orEmpty(),
                            initialStartsAt = addEvent?.startsAt?.toLocalDateTimeOrNull(),
                            initialEndsAt = addEvent?.endsAt?.toLocalDateTimeOrNull(),
                            submit = { draft ->
                                api.submitEvent(
                                    lat = draft.lat,
                                    lon = draft.lon,
                                    name = draft.name,
                                    website = draft.website,
                                    startsAt = draft.startsAt,
                                    endsAt = draft.endsAt,
                                )
                            },
                            onBack = { nav.pop() },
                        )

                        Route.AddComment -> ScreenPage(
                            title = paymentPlace?.second?.ifBlank { "Add comment" } ?: "Add comment",
                            onBack = { nav.pop() },
                        ) {
                            CommentScreen(
                                api = api,
                                placeId = paymentPlace?.first ?: 0L,
                                labels = COMMENT_LABELS,
                                onPay = { openUrl("lightning:$it") },
                                onCopy = { copyToClipboard(it) },
                                onBack = { nav.pop() },
                            )
                        }

                        Route.Boost -> ScreenPage(
                            title = paymentPlace?.second?.ifBlank { "Boost merchant" } ?: "Boost merchant",
                            onBack = { nav.pop() },
                        ) {
                            BoostScreen(
                                api = api,
                                placeId = paymentPlace?.first ?: 0L,
                                labels = BOOST_LABELS,
                                onPay = { openUrl("lightning:$it") },
                                onCopy = { copyToClipboard(it) },
                                onBack = { nav.pop() },
                            )
                        }

                        Route.Infra -> ScreenPage(
                            title = "Infra dashboard",
                            onBack = { nav.pop() },
                            actions = {
                                if (infraRefreshing) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(24.dp),
                                    )
                                } else {
                                    IconButton(onClick = { infraRefreshKey++ }) {
                                        MaterialSymbol(
                                            glyph = "refresh",
                                            contentDescription = "Refresh",
                                        )
                                    }
                                }
                            },
                        ) {
                            InfraDashboardScreen(
                                load = { api.getDashboard() },
                                refreshKey = infraRefreshKey,
                                onLoadingChange = { infraRefreshing = it },
                            )
                        }

                        Route.EventReview -> EventReviewScreen(
                            labels = EVENT_REVIEW_LABELS,
                            load = {
                                api.getPendingEvents().map { event ->
                                    PendingEventUi(
                                        id = event.id,
                                        lat = event.lat,
                                        lon = event.lon,
                                        name = event.name,
                                        website = event.website?.toString().orEmpty(),
                                        startsAt = event.startsAt,
                                        endsAt = event.endsAt,
                                    )
                                }
                            },
                            approve = { api.setEventStatus(it.id, "live") },
                            reject = { api.setEventStatus(it.id, "rejected") },
                            onOpenUrl = { openUrl(it) },
                            onBack = { nav.pop() },
                            title = "Review events",
                            iconFont = iconFont,
                            map = { event, mapModifier ->
                                EventMiniMap(
                                    lat = event.lat,
                                    lon = event.lon,
                                    styleUrl = HOSTED_STYLE_URL,
                                    styleJson = styleJson,
                                    palette = markerPalette(settings),
                                    modifier = mapModifier,
                                )
                            },
                        )

                        Route.Settings -> ScreenPage(
                            title = "Settings",
                            onBack = { nav.pop() },
                        ) {
                            SettingsPage(
                                settings = settings,
                                db = db,
                                labels = SETTINGS_PAGE_LABELS,
                                includeImageStats = false,
                                onOpenAccount = { nav.push(Route.Account) },
                                onOpenColors = { nav.push(Route.Colors) },
                                onOpenDbStats = { nav.push(Route.DbStats) },
                            )
                        }

                        Route.Colors -> ScreenPage(
                            title = "Customize colors",
                            onBack = { nav.pop() },
                        ) {
                            ColorsPage(settings = settings, labels = COLORS_PAGE_LABELS)
                        }

                        Route.DbStats -> ScreenPage(
                            title = "Database",
                            onBack = { nav.pop() },
                        ) {
                            DbStatsPage(
                                db = db,
                                settings = settings,
                                syncState = syncState,
                                labels = DB_STATS_PAGE_LABELS,
                                onSync = { syncManager.start() },
                            )
                        }

                        Route.Account -> {
                            // Uploaded images and my events are inside the
                            // profile, so the screen's back arrow returns to the
                            // profile page before leaving the account screen.
                            var showUploadedImages by remember { mutableStateOf(false) }
                            var showMyEvents by remember { mutableStateOf(false) }
                            ScreenPage(
                                title = when {
                                    showUploadedImages -> PROFILE_LABELS.uploadedImages
                                    showMyEvents -> PROFILE_LABELS.myEvents
                                    else -> "Account"
                                },
                                onBack = {
                                    when {
                                        showUploadedImages -> showUploadedImages = false
                                        showMyEvents -> showMyEvents = false
                                        else -> nav.pop()
                                    }
                                },
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
                                            imagesLabels = UPLOADED_IMAGES_LABELS,
                                            eventsLabels = MY_EVENTS_LABELS,
                                            mapStyleUrl = HOSTED_STYLE_URL,
                                            mapStyleJson = styleJson,
                                            showUploadedImages = showUploadedImages,
                                            onShowUploadedImagesChange = {
                                                showUploadedImages = it
                                            },
                                            showMyEvents = showMyEvents,
                                            onShowMyEventsChange = { showMyEvents = it },
                                            onDuplicateEvent = { event ->
                                                addEvent = AddEventPrefill(
                                                    lat = event.lat,
                                                    lon = event.lon,
                                                    name = event.name,
                                                    website = event.website,
                                                    startsAt = event.startsAtLocal,
                                                    endsAt = event.endsAtLocal,
                                                )
                                                nav.push(Route.AddEvent)
                                            },
                                            onLoggedOut = {
                                                showUploadedImages = false
                                                showMyEvents = false
                                                onLoggedOut()
                                            },
                                        )
                                    },
                                )
                            }
                        }

                        // An area opened from a map chip or an area search
                        // result: the shared body under its own header.
                        Route.Area -> {
                            val areaId = selectedAreaId
                            if (areaId == null) {
                                LaunchedEffect(Unit) { nav.pop() }
                            } else {
                                DesktopAreaScreen(
                                    areaId = areaId,
                                    db = db,
                                    api = api,
                                    boostedMarkerColor = markerPalette(settings).boostedMarkerBackground,
                                    onBack = { nav.pop() },
                                    onOpenPlace = { placeId ->
                                        feedPlaceId = placeId
                                        nav.reset(Route.Map)
                                    },
                                    onOpenEvent = { event ->
                                        selectedEvent = event
                                        nav.push(Route.Event)
                                    },
                                )
                            }
                        }

                        // An event opened from an area row or an event search
                        // result: its own map, dates and website.
                        Route.Event -> {
                            val event = selectedEvent
                            if (event == null) {
                                LaunchedEffect(Unit) { nav.pop() }
                            } else {
                                ScreenPage(
                                    title = event.name,
                                    onBack = { nav.pop() },
                                ) {
                                    EventScreen(
                                        event = event,
                                        geoJson = listOf(event).toEventGeoJson(),
                                        styleUrl = HOSTED_STYLE_URL,
                                        styleJson = styleJson,
                                        palette = markerPalette(settings),
                                        iconFont = iconFont,
                                        usingOpenFreeMap = true,
                                        labels = EVENT_SCREEN_LABELS,
                                    )
                                }
                            }
                        }

                        Route.Feed -> ScreenPage(
                            title = "Activity",
                            onBack = { nav.pop() },
                        ) {
                            DesktopFeedScreen(
                                api = api,
                                db = db,
                                lat = mapCenterLat,
                                lon = mapCenterLon,
                            ) { placeId ->
                                feedPlaceId = placeId
                                nav.reset(Route.Map)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The area screen: the shared [AreaScreen] body under a back affordance, with
 * the area and its boosted merchant, event and issue sections loaded from the
 * shared cache and API. The desktop has no offline packs, so the offline panel
 * is not shown.
 */
@androidx.compose.runtime.Composable
private fun DesktopAreaScreen(
    areaId: Long,
    db: Database,
    api: Api,
    boostedMarkerColor: Color,
    onBack: () -> Unit,
    onOpenPlace: (Long) -> Unit,
    onOpenEvent: (Event) -> Unit,
) {
    var area by remember(areaId) { mutableStateOf<Area?>(null) }
    var missing by remember(areaId) { mutableStateOf(false) }
    var boostedMerchants by remember(areaId) { mutableStateOf(emptyList<Place>()) }
    var events by remember(areaId) { mutableStateOf(emptyList<GetEventsItem>()) }
    var issues by remember(areaId) { mutableStateOf<AreaIssues?>(null) }

    LaunchedEffect(areaId) {
        val loaded = db.area.selectById(areaId)
        if (loaded == null) {
            missing = true
            return@LaunchedEffect
        }
        area = loaded
        boostedMerchants = loadSection { AreaSections.boostedMerchants(db, loaded) } ?: emptyList()
        events = loadSection { AreaSections.events(db, loaded) } ?: emptyList()
        issues = loadSection { AreaSections.placeIssues(api, db, areaId) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            IconButton(onClick = onBack) {
                MaterialSymbol(glyph = "arrow_back", contentDescription = "Back")
            }
            Text(
                text = area?.getLocalizedName() ?: "Area",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        val loaded = area
        when {
            loaded != null -> AreaScreen(
                headerImageUrl = loaded.iconWide ?: loaded.icon,
                description = loaded.getLocalizedDescription(),
                websiteText = loaded.websiteUrl.takeIf { it.isNotBlank() }?.let(::websiteDisplayText),
                onOpenWebsite = loaded.websiteUrl.takeIf { it.isNotBlank() }?.let { url ->
                    { openUrl(url) }
                },
                boostedMerchants = boostedMerchants,
                events = events,
                issues = issues,
                offlineState = null,
                offlineStyleMatches = { true },
                strings = DESKTOP_AREA_STRINGS,
                onOpenPlace = onOpenPlace,
                onOpenEvent = { onOpenEvent(it.toEvent()) },
                onOpenIssue = { openUrl(it.osmEditUrl()) },
                onJoinUs = { openUrl(JOIN_US_URL) },
                onDownload = {},
                onDelete = {},
                boostedMarkerColor = boostedMarkerColor,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )

            missing -> Text(
                text = "This area is not available.",
                modifier = Modifier.padding(16.dp),
            )

            else -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/** Reads a secondary section, hiding it when the read or fetch fails. */
private suspend fun <T> loadSection(block: suspend () -> T): T? = try {
    block()
} catch (t: Throwable) {
    t.rethrowIfCancellation()
    null
}

private fun GetEventsItem.toEvent(): Event = Event(
    id = id,
    lat = lat,
    lon = lon,
    name = name,
    website = website,
    startsAt = startsAt,
    endsAt = endsAt,
)

private fun AreaPlaceIssue.osmEditUrl(): String =
    "https://www.openstreetmap.org/edit?$elementOsmType=$elementOsmId"

private const val JOIN_US_URL = "https://btcmap.org/join-us"

/** The desktop app's full-window pages. */
private enum class Route { Map, Area, Event, Feed, Settings, Colors, DbStats, Account, Report, AddPlace, AddEvent, AddComment, Boost, Infra, EventReview }

/**
 * What the add-event screen opens with. From the map it is just the centre; from
 * the profile it is a submitted event to repeat, with its wall-clock times so the
 * user only has to adjust the date.
 */
private data class AddEventPrefill(
    val lat: Double,
    val lon: Double,
    val name: String = "",
    val website: String = "",
    val startsAt: String? = null,
    val endsAt: String? = null,
)

/** Parses a floating local date-time pre-fill, or null when absent/malformed. */
private fun String.toLocalDateTimeOrNull(): java.time.LocalDateTime? =
    runCatching { java.time.LocalDateTime.parse(this) }.getOrNull()

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
    val settings = runBlocking { home.settings(db).apply { preload() } }

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
                            httpClient = apiHttpClient(USER_AGENT),
                            baseUrl = { API_URL.toUrl() },
                            token = { settings.getString(KEY_AUTH_TOKEN, null) },
                            userAgent = USER_AGENT,
                        )
                        var showUploadedImages by remember { mutableStateOf(false) }
                        var showMyEvents by remember { mutableStateOf(false) }
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
                                    imagesLabels = UPLOADED_IMAGES_LABELS,
                                    eventsLabels = MY_EVENTS_LABELS,
                                    mapStyleUrl = HOSTED_STYLE_URL,
                                    mapStyleJson = null,
                                    showUploadedImages = showUploadedImages,
                                    onShowUploadedImagesChange = {
                                        showUploadedImages = it
                                    },
                                    showMyEvents = showMyEvents,
                                    onShowMyEventsChange = { showMyEvents = it },
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
                        submitted = false,
                        labels = ADD_PLACE_LABELS,
                        onSubmit = { _, _, _, _, _ -> },
                        onBack = {},
                    )

                    "addevent" -> AddEventForm(
                        busy = false,
                        submitted = false,
                        labels = ADD_EVENT_LABELS,
                        onSubmit = { _, _, _, _ -> },
                        onBack = {},
                    )

                    "infra" -> {
                        val api = Api(
                            httpClient = apiHttpClient(USER_AGENT),
                            baseUrl = { API_URL.toUrl() },
                            token = { settings.getString(KEY_AUTH_TOKEN, null) },
                            userAgent = USER_AGENT,
                        )
                        InfraDashboardScreen(load = { api.getDashboard() })
                    }

                    else -> Text(text = "Unknown screen: $name")
                }
            }
        }
    }

    // Render once so effects start, then again after a moment: a screen that
    // loads its data off the main thread (the profile) is blank on the first
    // frame and settled by the second. The infra dashboard fetches over the
    // network, which takes a few seconds.
    scene.render()
    Thread.sleep(if (name == "infra") 8000 else 500)
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

    fun database(): Database = runBlocking {
        Database(
            driver = BundledSQLiteDriver(),
            path = File(dir, "btcmap.db").absolutePath,
        ).apply { connect() }
    }

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
 * Opens a bundled snapshot resource, or returns null when it is absent. The
 * shared seeders treat a missing optional snapshot as absent rather than an
 * error.
 */
private fun bundledSnapshot(fileName: String): Source? =
    Thread.currentThread().contextClassLoader.getResourceAsStream(fileName)?.source()

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
    MapStyle.Auto -> "Auto (system)"
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
    uploadedImages = "Uploaded images",
    myEvents = "My events",
)

private val UPLOADED_IMAGES_LABELS = UploadedImagesLabels(
    empty = "You haven't uploaded any images yet.",
    delete = "Delete",
    failed = "Couldn't delete the image.",
    retry = "Retry",
    unknownPlace = { "Place #$it" },
)

private val MY_EVENTS_LABELS = MyEventsLabels(
    empty = "You haven't submitted any events yet.",
    failed = "Couldn't load your events",
    retry = "Retry",
    duplicate = "Duplicate",
    revoke = "Revoke",
    revokeFailed = "Couldn't revoke the event",
    statusPending = "Pending review",
    statusLive = "Live",
    statusRejected = "Rejected",
    dateRange = { date, start, end -> "$date, $start - $end" },
)

private val EVENT_REVIEW_LABELS = EventReviewLabels(
    back = "Navigate up",
    empty = "No events are waiting for review",
    failed = "Couldn't load the events",
    retry = "Retry",
    approve = "Approve",
    reject = "Reject",
    actionFailed = "Couldn't update the event",
    dateRange = { date, start, end -> "$date, $start - $end" },
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
    accountCreated = "Account created. Please sign in.",
    showPassword = "Show password",
    hidePassword = "Hide password",
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
    title = "Add a place",
    back = "Navigate up",
    name = "Name",
    namePlaceholder = "Satoshi Cafe",
    category = "Category",
    categoryPlaceholder = "cafe",
    address = "Address",
    addressPlaceholder = "1 HODL Lane, Block 21",
    website = "Website (optional)",
    websitePlaceholder = "example.com",
    description = "Description (optional)",
    descriptionPlaceholder = "Accepts Lightning payments via Blink wallet, ATM in the back room",
    dragMap = "Drag the map to set the exact location",
    required = "Required",
    submit = "Submit place",
    submitted = "Place submitted for review.",
    backToMap = "Back to the map",
)

private val ADD_EVENT_LABELS = AddEventLabels(
    title = "Add an event",
    back = "Navigate up",
    name = "Name",
    namePlaceholder = "Phuket Bitcoin Meetup",
    website = "Website",
    websitePlaceholder = "example.com",
    startsAt = "Starts at",
    endsAt = "Ends (optional)",
    selectDateTime = "Select date and time",
    clearEnd = "Remove end time",
    dragMap = "Drag the map to set the exact location",
    required = "Required",
    submit = "Submit event",
    submitted = "Event submitted.",
    backToMap = "Back to the map",
    ok = "OK",
    cancel = "Cancel",
)

private val ADD_LOCATION_LABELS = AddLocationLabels(
    addPlace = "Add a place",
    addEvent = "Add an event",
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
    uploadedBy = { "Uploaded by $it" },
    deletePhoto = "Delete photo",
    openingHoursClosed = "Closed",
    openingHoursOpen24_7 = "Open 24/7",
)

private val DESKTOP_AREA_STRINGS = AreaStrings(
    readMore = "Read more",
    collapse = "Collapse",
    boostedMerchants = "Boosted merchants",
    events = "Events",
    howToHelp = "How to help?",
    offlineMap = "Offline map",
    offlineDownload = "Download map",
    offlineDownloadAgain = "Download again",
    offlineDelete = "Delete",
    cancel = "Cancel",
    boosted = "Boosted",
    boostedUntil = { date -> "Boosted until $date" },
    issues = { shown, total ->
        if (shown < total) "Issues ($shown of $total)" else "Issues ($total)"
    },
    issueDescription = ::desktopIssueDescription,
    offlineStatusDownloading = { size -> "Downloading… $size" },
    offlineStatusProgress = { percent, size -> "Downloading… $percent% ($size)" },
    offlineStatusDownloaded = { size, minZoom, maxZoom ->
        "Downloaded · $size · zoom $minZoom–$maxZoom"
    },
    offlineStyleMismatch = "Downloaded for a different map style. Download again to " +
        "use it with the current style.",
    offlineStatusFailed = { message -> "Download failed: $message" },
    offlineDialogDescription = { areaName ->
        "Download the map of $areaName for offline use, so it stays available " +
            "without a connection."
    },
    offlineDialogStyle = { styleName -> "Map style: $styleName" },
    offlineDialogMaxZoom = { zoom -> "Maximum zoom: $zoom" },
    offlineDialogEstimatedSize = { size -> "Estimated size: ~$size" },
    offlineDialogTooLarge = { size -> "Too large to download offline (estimated $size)." },
    offlineDialogEstimateNote = "This is only an estimate. The actual size depends " +
        "on how much map data the area contains.",
    formatBytes = ::formatBytes,
)

private val EVENT_SCREEN_LABELS = EventScreenLabels(
    dateRange = { date, start, end -> "$date, $start - $end" },
)

/** The issue codes the API returns, matching the Android resource strings. */
private fun desktopIssueDescription(code: String): String = when {
    code == "outdated" -> "Outdated, needs verification"
    code == "outdated_soon" -> "Will be outdated soon"
    code == "not_verified" -> "Not verified"
    code == "missing_icon" -> "Missing icon"
    code.startsWith("invalid_tag_value:") ->
        "Invalid value for ${code.substringAfter("invalid_tag_value:")}"

    code.startsWith("misspelled_tag_name:") ->
        "Misspelled tag name: ${code.substringAfter("misspelled_tag_name:")}"

    else -> "Unknown issue"
}

/** A human-readable byte size for the offline estimate. */
private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f kB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
}
