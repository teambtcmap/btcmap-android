package org.btcmap.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.account.canManageEvents
import org.btcmap.account.isAdmin
import org.btcmap.api.addPlaceImage
import org.btcmap.api.deleteEvent
import org.btcmap.api.deletePlaceImage
import org.btcmap.api.getPendingEvents
import org.btcmap.api.getPlaceCoordinates
import org.btcmap.api.getPlaceImages
import org.btcmap.api.revokeEvent
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.i18n.getLocalizedName
import org.btcmap.map.MapArea
import org.btcmap.map.toEventGeoJson
import org.btcmap.place.btcmapUrl
import org.btcmap.place.canDeletePlaceImage
import org.btcmap.place.osmEditUrl
import org.btcmap.place.osmUrl
import org.btcmap.platform.ioDispatcher
import org.btcmap.saved.SavedItems
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.isDark
import org.btcmap.settings.mapBearing
import org.btcmap.settings.mapCenterLat
import org.btcmap.settings.mapCenterLon
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.mapStyle
import org.btcmap.settings.mapTilt
import org.btcmap.settings.mapTiltEnabled
import org.btcmap.settings.mapZoom
import org.btcmap.settings.showAttribution
import org.btcmap.settings.verifiedFilterMinVerifiedAt
import org.btcmap.sync.SyncEvent
import org.btcmap.sync.SyncState
import org.btcmap.ui.map.MarkerKind
import org.btcmap.ui.map.SearchActions
import org.btcmap.util.rethrowIfCancellation

/**
 * The map route, the app's root screen: the shared [MapScreen] wired to the
 * app services, the sync, the admin-only buttons and the sheets' actions.
 *
 * A place deep link or a feed row sets [openPlaceId], which the map selects and
 * moves to (falling back to the coordinates when the row is not cached yet). A
 * selected marker opens the map's own sheet — the place sheet, or the event
 * sheet — rather than pushing a screen.
 */
@Composable
internal fun MapRoute(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    openPlaceId: Long?,
    onOpenPlaceConsumed: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onShowAuth: () -> Unit,
    /** The marker kind the map shows, hoisted so it survives leaving the map. */
    markerKind: MarkerKind,
    onMarkerKindChange: (MarkerKind) -> Unit,
    /**
     * The place whose sheet is open, hoisted so leaving the map for another
     * screen and returning reopens it. [onSelectedPlaceIdChange] reports the
     * map's selection, and a null reports that the sheet was dismissed.
     */
    selectedPlaceId: Long?,
    onSelectedPlaceIdChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    /** A coordinate a screen (e.g. a note's banner) wants the map centred on. */
    focusTarget: Pair<Double, Double>? = null,
    onFocusTargetConsumed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val syncState by services.syncController.state.collectAsState()
    val updateAvailable by platform.updateAvailable.collectAsState()

    var currentOpenPlaceId by remember { mutableStateOf(openPlaceId) }
    var openTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // The filter to select when moving to [openTarget]. Only a note focus sets
    // it; a place deep link keeps the current filter.
    var openTargetMarkerKind by remember { mutableStateOf<MarkerKind?>(null) }
    // The event whose sheet is open over the map. Opening an event from a
    // marker or a search result shows this sheet instead of pushing a screen,
    // as a selected place shows the map's place sheet.
    var sheetEvent by remember { mutableStateOf<Event?>(null) }
    var photos by remember { mutableStateOf(emptyList<PlacePhoto>()) }
    var bookmarked by remember { mutableStateOf(false) }
    var addingPhoto by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    // Bumped when the sync reports a profile change, so the role-gated buttons
    // below re-derive after a promotion without waiting for the screen to be
    // re-entered.
    var userVersion by remember { mutableStateOf(0) }
    var isAdmin by remember { mutableStateOf(false) }
    var canManageEvents by remember { mutableStateOf(false) }
    var pendingEventCount by remember { mutableStateOf(0) }

    // The sync is app-scoped: starting it here only kicks it off; it keeps
    // running after the screen is left.
    LaunchedEffect(Unit) { services.syncController.start() }

    // A finished sync asks the map to re-query its features rather than leaving
    // it stale until the user moves it. A profile change additionally re-derives
    // the role-gated buttons.
    LaunchedEffect(Unit) {
        services.syncController.events.collect { event ->
            reloadKey++
            if (event == SyncEvent.UserChanged) userVersion++
        }
    }

    // The admin and event-manager buttons are role-gated and re-derived whenever
    // the map is (re)entered, a sync reports a profile change, or a sync
    // finishes (its last step re-reads the profile), so a sign-in, a sign-out or
    // a promotion made elsewhere is reflected.
    LaunchedEffect(userVersion, syncState) {
        val user = withContext(ioDispatcher) { services.db.user.select() }
        isAdmin = user?.isAdmin() == true
        canManageEvents = user?.canManageEvents() == true
    }

    // The pending-event badge is a network read, so it is refreshed only when
    // the event-manager role changes rather than on every sync state change.
    LaunchedEffect(canManageEvents) {
        pendingEventCount = if (canManageEvents) {
            runCatching { services.api.getPendingEvents().size }.getOrDefault(0)
        } else {
            0
        }
    }

    LaunchedEffect(openPlaceId) {
        val id = openPlaceId ?: return@LaunchedEffect
        val cached = withContext(ioDispatcher) { services.db.place.selectById(id) }
        if (cached != null) {
            currentOpenPlaceId = id
        } else {
            val coordinates = try {
                services.api.getPlaceCoordinates(id)
            } catch (t: Throwable) {
                t.rethrowIfCancellation()
                null
            }
            if (coordinates != null) openTarget = coordinates.lat to coordinates.lon
        }
        onOpenPlaceConsumed()
    }

    // A screen asked the map to centre on a coordinate (a note's banner, say):
    // hand it to the same camera move the deep-link fallback uses, and select
    // the notes filter so the note is actually drawn, then tell the host it was
    // consumed.
    LaunchedEffect(focusTarget) {
        val target = focusTarget ?: return@LaunchedEffect
        openTarget = target
        openTargetMarkerKind = MarkerKind.Notes
        onFocusTargetConsumed()
    }

    fun refreshSheet(place: Place) {
        scope.launch {
            bookmarked = SavedItems.isPlaceSaved(services.db, place.id)
            photos = loadPhotos(services, place.id)
        }
    }

    fun toggleBookmark(place: Place) {
        if (!services.settings.authorized) {
            onShowAuth()
            return
        }
        scope.launch {
            SavedItems.togglePlace(services.api, services.db, place.id, place.name.orEmpty())
            bookmarked = SavedItems.isPlaceSaved(services.db, place.id)
        }
    }

    fun addPhoto(place: Place) {
        if (!services.settings.authorized) {
            onShowAuth()
            return
        }
        scope.launch {
            addingPhoto = true
            try {
                platform.pickPhotos().firstOrNull()?.let { bytes ->
                    try {
                        services.api.addPlaceImage(place.id, bytes)
                    } catch (t: Throwable) {
                        t.rethrowIfCancellation()
                        platform.showError(t)
                    }
                    photos = loadPhotos(services, place.id)
                }
            } finally {
                addingPhoto = false
            }
        }
    }

    val mapStyle = services.rememberMapStyle()
    // The attribution sits on the map itself, so its colour follows the style's
    // brightness rather than the app theme: a light style in a dark app still
    // needs dark text.
    val mapIsDark = services.settings.mapStyle.isDark(isSystemInDarkTheme())

    MapScreen(
        db = services.db,
        styleUrl = mapStyle.url,
        styleJson = mapStyle.json,
        initialLat = services.settings.mapCenterLat,
        initialLon = services.settings.mapCenterLon,
        initialZoom = services.settings.mapZoom,
        initialBearing = services.settings.mapBearing,
        initialTilt = services.settings.mapTilt,
        minVerifiedAt = services.settings.verifiedFilterMinVerifiedAt(),
        palette = markerPalette(services.settings),
        areaChipPalette = areaChipPalette(services.settings),
        apiUrl = services.settings.apiUrl.toString(),
        usingOpenFreeMap = services.usingOpenFreeMap,
        markerKind = markerKind,
        onMarkerKindChange = onMarkerKindChange,
        mapRotationEnabled = services.settings.mapRotationEnabled,
        mapTiltEnabled = services.settings.mapTiltEnabled,
        iconFont = services.iconFont,
        placeSheetStrings = labels.placeStrings,
        searchActions = SearchActions(onSettings = { onNavigate(AppRoute.Settings) }),
        onAddPlace = { lat, lon ->
            if (services.settings.authorized) {
                onNavigate(AppRoute.AddPlace(lat, lon))
            } else {
                onShowAuth()
            }
        },
        onAddEvent = { lat, lon ->
            if (services.settings.authorized) {
                onNavigate(AppRoute.AddEvent(lat, lon))
            } else {
                onShowAuth()
            }
        },
        onAddNote = { lat, lon ->
            if (services.settings.authorized) {
                onNavigate(AppRoute.AddNote(lat, lon))
            } else {
                onShowAuth()
            }
        },
        addLocationLabels = labels.addLocation,
        noteSheetLabels = labels.noteSheet,
        onOpenFeed = { areas -> onNavigate(areas.toFeedRoute()) },
        onOpenInfra = if (isAdmin) {
            { onNavigate(AppRoute.InfraDashboard) }
        } else {
            null
        },
        onOpenEventReview = if (canManageEvents) {
            { onNavigate(AppRoute.EventReview) }
        } else {
            null
        },
        pendingEventCount = pendingEventCount,
        reloadKey = reloadKey,
        showAttribution = services.settings.showAttribution,
        syncVisible = syncState != SyncState.Idle,
        updateVisible = updateAvailable,
        onUpdateClick = { platform.showUpdateDialog() },
        attributionText = labels.osmAttribution,
        attributionTextColor = if (mapIsDark) Color.White else Color.Black.copy(alpha = 0.8f),
        onCameraIdle = { camera ->
            services.settings.mapCenterLat = camera.lat
            services.settings.mapCenterLon = camera.lon
            services.settings.mapZoom = camera.zoom
            services.settings.mapBearing = camera.bearing
            services.settings.mapTilt = camera.tilt
        },
        onFeaturesDrawn = { platform.reportFullyDrawn() },
        placeSheet = true,
        openTarget = openTarget,
        openTargetMarkerKind = openTargetMarkerKind,
        onOpenTargetConsumed = {
            openTarget = null
            openTargetMarkerKind = null
        },
        openPlaceId = currentOpenPlaceId,
        initialPlaceId = selectedPlaceId,
        photos = photos,
        bookmarked = bookmarked,
        addingPhoto = addingPhoto,
        onPlaceSelected = { place ->
            onSelectedPlaceIdChange(place.id)
            refreshSheet(place)
        },
        onPlaceDismissed = {
            onSelectedPlaceIdChange(null)
            currentOpenPlaceId = null
        },
        onPlaceAction = { place, action ->
            when (action) {
                PlaceAction.Directions -> platform.openDirections(place.lat, place.lon)
                PlaceAction.Share -> platform.shareText(place.btcmapUrl())
                PlaceAction.ViewOnBtcmap -> platform.openOnBtcmap(place.btcmapUrl())
                PlaceAction.ViewOnOsm -> place.osmUrl()?.let { platform.openUrl(it) }
                PlaceAction.EditOnOsm -> place.osmEditUrl()?.let { platform.openUrl(it) }
                PlaceAction.Phone -> platform.openDialer(place.phone)
                PlaceAction.Website -> place.website?.let { platform.openUrl(it.toString()) }
                PlaceAction.Email -> platform.openEmail(place.email)
                PlaceAction.Telegram -> place.telegram?.let { platform.openUrl(it.toString()) }
                PlaceAction.Line -> place.line?.let { platform.openUrl(it.toString()) }
                PlaceAction.Twitter -> place.twitter?.let { platform.openUrl(it.toString()) }
                PlaceAction.Facebook -> place.facebook?.let { platform.openUrl(it.toString()) }
                PlaceAction.Instagram -> place.instagram?.let { platform.openUrl(it.toString()) }
                PlaceAction.ToggleBookmark -> toggleBookmark(place)
                PlaceAction.AddPhoto -> addPhoto(place)
                PlaceAction.Verify -> {
                    if (services.settings.authorized) {
                        onNavigate(AppRoute.Report(place.id, place.getLocalizedName(), "verified"))
                    } else {
                        onShowAuth()
                    }
                }

                PlaceAction.Report -> {
                    if (services.settings.authorized) {
                        onNavigate(AppRoute.Report(place.id, place.getLocalizedName(), null))
                    } else {
                        onShowAuth()
                    }
                }

                PlaceAction.Boost ->
                    onNavigate(AppRoute.Boost(place.id, place.getLocalizedName()))

                PlaceAction.AddComment ->
                    onNavigate(AppRoute.AddComment(place.id, place.getLocalizedName()))

                else -> {}
            }
        },
        onDeletePhoto = { photo ->
            scope.launch {
                try {
                    services.api.deletePlaceImage(photo.placeId, photo.imageId)
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    platform.showError(t)
                }
                selectedPlaceId?.let { photos = loadPhotos(services, it) }
            }
        },
        onSelectEvent = { sheetEvent = it },
        onSelectArea = { areaId -> onNavigate(AppRoute.Area(areaId)) },
        formatDistance = labels.formatDistance,
        modifier = modifier,
    )

    sheetEvent?.let { event ->
        val delete = rememberEventDeleteState(services.db, services.api, event.id)
        EventSheet(
            event = event,
            geoJson = listOf(event).toEventGeoJson(),
            styleUrl = mapStyle.url,
            styleJson = mapStyle.json,
            palette = markerPalette(services.settings),
            iconFont = services.iconFont,
            usingOpenFreeMap = services.usingOpenFreeMap,
            labels = labels.eventScreen,
            onDismiss = { sheetEvent = null },
            onOpenWebsite = event.website?.let { url -> { platform.openUrl(url.toString()) } },
            onDirections = { platform.openDirections(event.lat, event.lon) },
            onDelete = if (delete.canDelete) {
                {
                    if (delete.withRpc) {
                        services.api.deleteEvent(event.id)
                    } else {
                        services.api.revokeEvent(event.id)
                    }
                }
            } else {
                null
            },
            onDeleted = { sheetEvent = null },
        )
    }
}

/** The feed route for the areas around the map. */
private fun List<MapArea>.toFeedRoute(): AppRoute = AppRoute.Feed(
    areaIds = map { it.urlAlias },
    areaNames = map { it.name },
    areaTypes = map { it.type },
)

/** Loads a place's photos, leaving the strip empty when the fetch fails. */
private suspend fun loadPhotos(services: AppServices, placeId: Long): List<PlacePhoto> = try {
    withContext(ioDispatcher) {
        val user = services.db.user.select()
        services.api.getPlaceImages(placeId).map {
            services.api.toPlacePhoto(it, canDelete = user?.canDeletePlaceImage(it) ?: false)
        }
    }
} catch (t: Throwable) {
    t.rethrowIfCancellation()
    emptyList()
}
