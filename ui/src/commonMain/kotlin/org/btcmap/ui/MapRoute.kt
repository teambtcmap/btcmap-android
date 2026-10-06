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
import org.btcmap.api.deletePlaceImage
import org.btcmap.api.getPendingEvents
import org.btcmap.api.getPlaceCoordinates
import org.btcmap.api.getPlaceImages
import org.btcmap.db.table.place.Place
import org.btcmap.i18n.getLocalizedName
import org.btcmap.map.MapArea
import org.btcmap.place.btcmapUrl
import org.btcmap.place.canDeletePlaceImage
import org.btcmap.place.osmEditUrl
import org.btcmap.place.osmUrl
import org.btcmap.platform.ioDispatcher
import org.btcmap.saved.SavedItems
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.isDark
import org.btcmap.settings.mapCenterLat
import org.btcmap.settings.mapCenterLon
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.mapStyle
import org.btcmap.settings.mapZoom
import org.btcmap.settings.showAttribution
import org.btcmap.settings.verifiedFilterMinVerifiedAt
import org.btcmap.sync.SyncState
import org.btcmap.ui.map.SearchActions
import org.btcmap.util.rethrowIfCancellation

/**
 * The map route, the app's root screen: the shared [MapScreen] wired to the
 * app services, the sync, the admin-only buttons and the place sheet's actions.
 *
 * A place deep link or a feed row sets [openPlaceId], which the map selects and
 * moves to (falling back to the coordinates when the row is not cached yet).
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
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val syncState by services.syncController.state.collectAsState()
    val updateAvailable by platform.updateAvailable.collectAsState()

    var currentOpenPlaceId by remember { mutableStateOf(openPlaceId) }
    var openTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var selectedPlaceId by remember { mutableStateOf<Long?>(null) }
    var photos by remember { mutableStateOf(emptyList<PlacePhoto>()) }
    var bookmarked by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var isAdmin by remember { mutableStateOf(false) }
    var canManageEvents by remember { mutableStateOf(false) }
    var pendingEventCount by remember { mutableStateOf(0) }

    // The sync is app-scoped: starting it here only kicks it off; it keeps
    // running after the screen is left.
    LaunchedEffect(Unit) { services.syncController.start() }

    // A finished sync asks the map to re-query its features rather than leaving
    // it stale until the user moves it.
    LaunchedEffect(Unit) {
        services.syncController.events.collect { reloadKey++ }
    }

    // The admin and event-manager buttons are role-gated and re-derived whenever
    // the map is (re)entered, so a sign-in or sign-out is reflected.
    LaunchedEffect(Unit) {
        val user = withContext(ioDispatcher) { services.db.user.select() }
        isAdmin = user?.isAdmin() == true
        canManageEvents = user?.canManageEvents() == true
        pendingEventCount = if (canManageEvents) {
            runCatching { services.api.getPendingEvents().size }.getOrDefault(0)
        } else {
            0
        }
    }

    LaunchedEffect(openPlaceId) {
        val id = openPlaceId ?: return@LaunchedEffect
        val cached = services.db.place.selectById(id)
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
            platform.pickPhotos().firstOrNull()?.let { bytes ->
                try {
                    services.api.addPlaceImage(place.id, bytes)
                } catch (t: Throwable) {
                    t.rethrowIfCancellation()
                    platform.showError(t)
                }
                photos = loadPhotos(services, place.id)
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
        minVerifiedAt = services.settings.verifiedFilterMinVerifiedAt(),
        palette = markerPalette(services.settings),
        areaChipPalette = areaChipPalette(services.settings),
        apiUrl = services.settings.apiUrl.toString(),
        usingOpenFreeMap = services.usingOpenFreeMap,
        mapRotationEnabled = services.settings.mapRotationEnabled,
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
        addLocationLabels = labels.addLocation,
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
        onCameraIdle = { lat, lon, zoom ->
            services.settings.mapCenterLat = lat
            services.settings.mapCenterLon = lon
            services.settings.mapZoom = zoom
        },
        onFeaturesDrawn = { platform.reportFullyDrawn() },
        placeSheet = true,
        openTarget = openTarget,
        openPlaceId = currentOpenPlaceId,
        photos = photos,
        bookmarked = bookmarked,
        onPlaceSelected = { place ->
            selectedPlaceId = place.id
            refreshSheet(place)
        },
        onPlaceDismissed = {
            selectedPlaceId = null
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
        onSelectEvent = { event -> onNavigate(AppRoute.EventDetails(event)) },
        onSelectArea = { areaId -> onNavigate(AppRoute.Area(areaId)) },
        formatDistance = labels.formatDistance,
        modifier = modifier,
    )
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
