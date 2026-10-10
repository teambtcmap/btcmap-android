package org.btcmap.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext
import org.btcmap.api.deleteEvent
import org.btcmap.api.deletePlaceImage
import org.btcmap.api.getDashboard
import org.btcmap.api.createNote
import org.btcmap.api.getPendingEvents
import org.btcmap.api.getRecentPlaceImages
import org.btcmap.api.placeImageUrl
import org.btcmap.api.revokeEvent
import org.btcmap.api.searchUsers
import org.btcmap.api.setEventStatus
import org.btcmap.api.updateUser
import org.btcmap.api.setAreaDescription
import org.btcmap.api.setAreaName
import org.btcmap.api.submitEvent
import org.btcmap.api.submitPlace
import org.btcmap.api.toEvent
import org.btcmap.api.verifyArea
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.event.Event
import org.btcmap.dbstats.BundleStats
import org.btcmap.i18n.getLocalizedName
import org.btcmap.map.toEventGeoJson
import org.btcmap.place.submitReport
import org.btcmap.platform.ioDispatcher
import org.btcmap.settings.authorized
import org.btcmap.settings.isDark
import org.btcmap.settings.mapStyle
import org.btcmap.sync.SyncState
import org.btcmap.ui.map.AreaPreviewMap
import org.btcmap.ui.map.EventMiniMap
import org.btcmap.ui.map.MarkerKind

/**
 * The shared application root: the app's navigation, owned by `:ui` so it no
 * longer depends on Android's `FragmentManager`.
 *
 * It holds a [NavController] of [AppRoute] and renders the screen for the
 * current route, wiring each to the host's [AppServices], [AppLabels] and
 * [AppPlatform]. A host embeds it in a single Compose view; the back affordance
 * pops the stack and, once the start route is reached, calls [onExit] so the
 * host can close the screen or the window.
 *
 * Screens migrate here one cluster at a time; the routes that have not moved yet
 * stay with the host.
 */
@Composable
fun AppRoot(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    startRoute: AppRoute,
    onExit: () -> Unit,
    /** A place a deep link or feed row wants the map to open. */
    openPlaceId: Long? = null,
    onOpenPlaceConsumed: () -> Unit = {},
    /** An event a deep link wants the root to open. */
    pendingEvent: Event? = null,
    onEventConsumed: () -> Unit = {},
    /** Gives the host a way to pop the root's stack for a system back press. */
    registerBack: ((() -> Boolean)) -> Unit = {},
    /**
     * Called when the settings screen changes the UI language, so the host can
     * rebuild its [labels] against the new one and the whole root re-renders in
     * it without a restart.
     */
    onLanguageChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppTheme(iconFont = services.iconFont) {
        val nav = rememberNavController(startRoute)
        LaunchedEffect(Unit) { registerBack { nav.pop() } }
        // The auth dialog is shown by the root, so any screen can ask for a
        // session; a success bumps a key the account screens re-read.
        var showAuth by remember { mutableStateOf(false) }
        var authReload by remember { mutableStateOf(0) }
        // A coordinate a screen asked the map to centre on (a note's banner),
        // passed to the map and cleared once it has moved there.
        var mapFocus by remember { mutableStateOf<Pair<Double, Double>?>(null) }
        // The marker kind the map shows, kept here so leaving the map for
        // another screen and returning restores the user's choice. It is saved
        // across configuration changes but not persisted, so a fresh app start
        // begins on merchants.
        var markerKind by rememberSaveable { mutableStateOf(MarkerKind.Merchants) }
        // The place whose sheet is open on the map, kept here so leaving the map
        // for another screen — the report form, say — and returning reopens the
        // sheet rather than dropping the selection. Saved across configuration
        // changes but not persisted, so a fresh app start begins with none.
        var selectedPlaceId by rememberSaveable { mutableStateOf<Long?>(null) }
        val back = {
            // At the start route there is nothing left on the stack to pop, so
            // the host closes the screen instead.
            if (!nav.pop()) onExit()
        }

        LaunchedEffect(pendingEvent) {
            val event = pendingEvent ?: return@LaunchedEffect
            nav.push(AppRoute.EventDetails(event))
            onEventConsumed()
        }

        // The keyboard inset is applied to every screen except the map: the map
        // is full-bleed and moving its top edge would shift the camera.
        val route = nav.current
        // Resolved from the current setting on every recomposition of the root,
        // so returning from the settings screen after a style change rebuilds
        // the maps with the newly picked style.
        val mapStyle = services.rememberMapStyle()

        // The system bars overlay the current screen. The map is full-bleed and
        // can be a light style in a dark app, so its own style decides the icon
        // tint there; every other screen paints the theme background, which
        // already matches the platform's choice.
        val systemDark = isSystemInDarkTheme()
        PlatformSystemBarIcons(
            light = if (route == AppRoute.Map) {
                services.settings.mapStyle.isDark(systemDark)
            } else {
                systemDark
            },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (route == AppRoute.Map) Modifier else Modifier.imePadding()),
        ) {
            when (route) {
            AppRoute.Map -> MapRoute(
                services = services,
                platform = platform,
                labels = labels,
                openPlaceId = openPlaceId,
                onOpenPlaceConsumed = onOpenPlaceConsumed,
                onNavigate = { nav.push(it) },
                onShowAuth = { showAuth = true },
                markerKind = markerKind,
                onMarkerKindChange = { markerKind = it },
                selectedPlaceId = selectedPlaceId,
                onSelectedPlaceIdChange = { selectedPlaceId = it },
                modifier = modifier,
                focusTarget = mapFocus,
                onFocusTargetConsumed = { mapFocus = null },
            )
            AppRoute.InfraDashboard -> InfraDashboardRoute(
                services = services,
                labels = labels,
                onBack = back,
                modifier = modifier,
            )

            AppRoute.EventReview -> EventReviewScreen(
                labels = labels.eventReview,
                load = {
                    services.api.getPendingEvents().map { event ->
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
                approve = { services.api.setEventStatus(it.id, "live") },
                reject = { services.api.setEventStatus(it.id, "rejected") },
                onOpenUrl = platform::openUrl,
                onBack = back,
                title = labels.eventReviewTitle,
                modifier = modifier,
                iconFont = services.iconFont,
                map = { event, mapModifier ->
                    EventMiniMap(
                        lat = event.lat,
                        lon = event.lon,
                        styleUrl = mapStyle.url,
                        styleJson = mapStyle.json,
                        palette = markerPalette(services.settings),
                        modifier = mapModifier,
                    )
                },
            )

            is AppRoute.Boost -> ScreenPage(
                title = route.placeName.ifBlank { labels.boostTitle },
                onBack = back,
                backContentDescription = labels.back,
            ) {
                BoostScreen(
                    api = services.api,
                    placeId = route.placeId,
                    labels = labels.boost,
                    onPay = platform::openLightningWallet,
                    onCopy = { platform.copyBolt11(labels.boostPaymentRequest, it) },
                    onBack = back,
                    onPosted = {
                        platform.showMessage(labels.boost.active)
                        back()
                    },
                )
            }

            is AppRoute.AddComment -> ScreenPage(
                title = route.placeName.ifBlank { labels.addCommentTitle },
                onBack = back,
                backContentDescription = labels.back,
            ) {
                CommentScreen(
                    api = services.api,
                    placeId = route.placeId,
                    labels = labels.addComment,
                    onPay = platform::openLightningWallet,
                    onCopy = { platform.copyBolt11(labels.commentPaymentRequest, it) },
                    onBack = back,
                    onPosted = {
                        platform.showMessage(labels.addComment.posted)
                        back()
                    },
                )
            }

            is AppRoute.EventDetails -> EventDetailsRoute(
                services = services,
                platform = platform,
                labels = labels,
                route = route,
                mapStyle = mapStyle,
                onBack = back,
            )

            is AppRoute.AddPlace -> AddPlaceScreen(
                lat = route.lat,
                lon = route.lon,
                styleUrl = mapStyle.url,
                styleJson = mapStyle.json,
                labels = labels.addPlace,
                iconFont = services.iconFont,
                palette = markerPalette(services.settings),
                submit = { draft ->
                    services.api.submitPlace(
                        lat = draft.lat,
                        lon = draft.lon,
                        category = draft.category,
                        name = draft.name,
                        address = draft.address.takeIf { it.isNotEmpty() },
                        website = draft.website.takeIf { it.isNotEmpty() },
                        description = draft.description.takeIf { it.isNotEmpty() },
                    )
                },
                onBack = back,
                initialName = route.name,
                initialCategory = route.category,
            )

            is AppRoute.AddNote -> AddNoteScreen(
                lat = route.lat,
                lon = route.lon,
                styleUrl = mapStyle.url,
                styleJson = mapStyle.json,
                labels = labels.addNote,
                iconFont = services.iconFont,
                palette = markerPalette(services.settings),
                submit = { draft ->
                    services.api.createNote(
                        lat = draft.lat,
                        lon = draft.lon,
                        text = draft.text,
                        icon = draft.icon,
                        public = draft.public,
                    )
                },
                onBack = back,
                initialText = route.text,
            )

            is AppRoute.AddEvent -> AddEventScreen(
                lat = route.lat,
                lon = route.lon,
                styleUrl = mapStyle.url,
                styleJson = mapStyle.json,
                labels = labels.addEvent,
                iconFont = services.iconFont,
                palette = markerPalette(services.settings),
                submit = { draft ->
                    services.api.submitEvent(
                        lat = draft.lat,
                        lon = draft.lon,
                        name = draft.name,
                        website = draft.website,
                        startsAt = draft.startsAt,
                        endsAt = draft.endsAt,
                    )
                },
                onBack = back,
                initialName = route.name,
                initialWebsite = route.website,
                initialStartsAt = route.startsAt,
                initialEndsAt = route.endsAt,
            )

            is AppRoute.Report -> ScreenPage(
                title = route.placeName,
                onBack = back,
                backContentDescription = labels.back,
            ) {
                ReportPlaceScreen(
                    initialType = route.defaultType,
                    labels = labels.report,
                    submit = { draft ->
                        services.api.submitReport(placeId = route.placeId, draft = draft)
                    },
                    onBack = back,
                    pickPhotos = platform::pickPhotos,
                )
            }

            AppRoute.Colors -> ScreenPage(
                title = labels.colorsTitle,
                onBack = back,
                backContentDescription = labels.back,
            ) {
                ColorsPage(settings = services.settings, labels = labels.colors)
            }

            AppRoute.DbStats -> DbStatsRoute(
                services = services,
                platform = platform,
                labels = labels,
                onBack = back,
            )

            AppRoute.ImageStats -> ImageStatsRoute(
                services = services,
                platform = platform,
                labels = labels,
                onBack = back,
            )

            is AppRoute.Feed -> FeedRoute(
                services = services,
                platform = platform,
                labels = labels,
                route = route,
                onBack = back,
                onOpenPlace = { placeId -> nav.push(AppRoute.Place(placeId)) },
            )

            is AppRoute.Area -> AreaRoute(
                services = services,
                platform = platform,
                labels = labels,
                route = route,
                onBack = back,
                onOpenEvent = { event -> nav.push(AppRoute.EventDetails(event.toEvent())) },
                onShowAuth = { showAuth = true },
            )

            AppRoute.Settings -> ScreenPage(
                title = labels.settingsTitle,
                onBack = back,
                backContentDescription = labels.back,
            ) {
                SettingsPage(
                    settings = services.settings,
                    db = services.db,
                    labels = labels.settings,
                    includeImageStats = true,
                    onOpenAccount = {
                        if (services.settings.authorized) {
                            nav.push(AppRoute.UserProfile)
                        } else {
                            showAuth = true
                        }
                    },
                    onOpenColors = { nav.push(AppRoute.Colors) },
                    onOpenDbStats = { nav.push(AppRoute.DbStats) },
                    onOpenImageStats = { nav.push(AppRoute.ImageStats) },
                    onOpenManageUsers = { nav.push(AppRoute.ManageUsers) },
                    onOpenManageAreas = { nav.push(AppRoute.ManageAreas) },
                    onOpenManagePlaceImages = { nav.push(AppRoute.ManagePlaceImages) },
                    onLanguageChanged = onLanguageChanged,
                    reloadKey = authReload,
                )
            }

            AppRoute.ManageUsers -> ManageUsersRoute(
                services = services,
                labels = labels,
                onBack = back,
                onOpenUser = { user -> nav.push(AppRoute.UserAdmin(user)) },
            )

            is AppRoute.UserAdmin -> UserAdminRoute(
                services = services,
                labels = labels,
                route = route,
                onBack = back,
            )

            AppRoute.ManageAreas -> ManageAreasRoute(
                services = services,
                labels = labels,
                onBack = back,
                onOpenArea = { area -> nav.push(AppRoute.AreaAdmin(area.id)) },
            )

            AppRoute.ManagePlaceImages -> ManagePlaceImagesRoute(
                services = services,
                labels = labels,
                onBack = back,
            )

            is AppRoute.AreaAdmin -> AreaAdminRoute(
                services = services,
                platform = platform,
                labels = labels,
                route = route,
                onBack = back,
            )

            AppRoute.UserProfile -> UserProfileRoute(
                services = services,
                labels = labels,
                onBack = back,
                onDuplicateEvent = { event ->
                    nav.push(
                        AppRoute.AddEvent(
                            lat = event.lat,
                            lon = event.lon,
                            name = event.name,
                            website = event.website,
                            startsAt = event.startsAtLocal.toLocalDateTimeOrNull(),
                            endsAt = event.endsAtLocal?.toLocalDateTimeOrNull(),
                        ),
                    )
                },
                onOpenNoteOnMap = { note ->
                    // Focusing a note supersedes any place selection, so the
                    // remembered sheet does not reopen over the note's banner.
                    selectedPlaceId = null
                    mapFocus = note.lat to note.lon
                    nav.reset(AppRoute.Map)
                },
            )

            is AppRoute.Place -> PlaceRoute(
                services = services,
                platform = platform,
                labels = labels,
                route = route,
                onBack = back,
                onNavigate = { nav.push(it) },
                onShowAuth = { showAuth = true },
            )
            }
        }

        if (showAuth) {
            AuthDialog(
                api = services.api,
                db = services.db,
                settings = services.settings,
                tokenLabel = services.authTokenLabel,
                labels = labels.account,
                onDismiss = { showAuth = false },
                onAuthenticated = {
                    showAuth = false
                    authReload++
                },
            )
        }
    }
}

/**
 * The event route: the shared [EventScreen] under a bar with the event's name,
 * the directions action and a back affordance. The delete action is offered only
 * when the signed-in user may remove the event — an event manager, admin or root
 * through the `delete_event` RPC, or the submitter of a still-pending event
 * through the REST revoke — so anyone else sees no delete affordance at all.
 */
@Composable
private fun EventDetailsRoute(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    route: AppRoute.EventDetails,
    mapStyle: MapStyleSpec,
    onBack: () -> Unit,
) {
    // Whether the current user may delete this event, and whether they do so
    // with the privileged RPC (any event) or the submitter's own revoke (their
    // pending event only).
    val delete = rememberEventDeleteState(services.db, services.api, route.event.id)

    ScreenPage(
        title = route.event.name,
        onBack = onBack,
        backContentDescription = labels.back,
        actions = {
            IconButton(
                onClick = { platform.openDirections(route.event.lat, route.event.lon) },
            ) {
                MaterialSymbol(
                    glyph = "directions",
                    contentDescription = labels.directions,
                )
            }
            EventDeleteAction(
                labels = labels.eventScreen,
                onDelete = if (delete.canDelete) {
                    {
                        if (delete.withRpc) {
                            services.api.deleteEvent(route.event.id)
                        } else {
                            services.api.revokeEvent(route.event.id)
                        }
                    }
                } else {
                    null
                },
                onDeleted = onBack,
            )
        },
    ) {
        EventScreen(
            event = route.event,
            geoJson = listOf(route.event).toEventGeoJson(),
            styleUrl = mapStyle.url,
            styleJson = mapStyle.json,
            palette = markerPalette(services.settings),
            iconFont = services.iconFont,
            usingOpenFreeMap = services.usingOpenFreeMap,
            labels = labels.eventScreen,
            onOpenWebsite = route.event.website?.let { url ->
                { platform.openUrl(url.toString()) }
            },
        )
    }
}

/**
 * The account route: the shared [ProfileScreen] under a bar whose title follows
 * the profile's current sub-screen (uploaded images, my events or my notes), so
 * the back affordance returns to the profile before leaving the screen.
 */
@Composable
private fun UserProfileRoute(
    services: AppServices,
    labels: AppLabels,
    onBack: () -> Unit,
    onDuplicateEvent: (MyEventUi) -> Unit,
    onOpenNoteOnMap: (MyNoteUi) -> Unit,
) {
    var showUploadedImages by remember { mutableStateOf(false) }
    var showMyEvents by remember { mutableStateOf(false) }
    var showMyNotes by remember { mutableStateOf(false) }
    val mapStyle = services.rememberMapStyle()

    ScreenPage(
        title = when {
            showUploadedImages -> labels.uploadedImagesTitle
            showMyEvents -> labels.myEventsTitle
            showMyNotes -> labels.myNotesTitle
            else -> labels.profileTitle
        },
        onBack = {
            when {
                showUploadedImages -> showUploadedImages = false
                showMyEvents -> showMyEvents = false
                showMyNotes -> showMyNotes = false
                else -> onBack()
            }
        },
        backContentDescription = labels.back,
    ) {
        ProfileScreen(
            api = services.api,
            db = services.db,
            settings = services.settings,
            profileLabels = labels.userProfile,
            formLabels = labels.profileForm,
            imagesLabels = labels.uploadedImages,
            eventsLabels = labels.myEvents,
            notesLabels = labels.myNotes,
            mapStyleUrl = mapStyle.url,
            mapStyleJson = mapStyle.json,
            showUploadedImages = showUploadedImages,
            onShowUploadedImagesChange = { showUploadedImages = it },
            showMyEvents = showMyEvents,
            onShowMyEventsChange = { showMyEvents = it },
            showMyNotes = showMyNotes,
            onShowMyNotesChange = { showMyNotes = it },
            onDuplicateEvent = onDuplicateEvent,
            onOpenNoteOnMap = onOpenNoteOnMap,
            onLoggedOut = onBack,
        )
    }
}

/**
 * The database stats route: the shared [DbStatsPage] under the standard top bar,
 * whose refresh action starts the app-scoped sync and disables itself while one
 * is running. The bundled snapshot stats are read once, off the main thread.
 */
@Composable
private fun DbStatsRoute(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    onBack: () -> Unit,
) {
    val syncState by services.syncController.state.collectAsState()
    var bundles by remember { mutableStateOf(emptyMap<String, BundleStats>()) }

    LaunchedEffect(Unit) {
        bundles = platform.loadBundles()
    }

    ScreenPage(
        title = labels.dbStatsTitle,
        onBack = onBack,
        backContentDescription = labels.back,
        actions = {
            IconButton(
                onClick = { services.syncController.start() },
                enabled = syncState == SyncState.Idle,
            ) {
                MaterialSymbol(
                    glyph = "sync",
                    contentDescription = labels.dbStats.syncNow,
                )
            }
        },
    ) {
        DbStatsPage(
            db = services.db,
            settings = services.settings,
            syncState = syncState,
            labels = labels.dbStats,
            onSync = { services.syncController.start() },
            bundles = bundles,
            showSyncButton = false,
        )
    }
}

/**
 * The manage-users route: the shared [ManageUsersScreen] under the standard top
 * bar, searching the server for users by name. Only the settings row (itself
 * hidden from non-admins) opens it, and the server enforces the role.
 */
@Composable
private fun ManageUsersRoute(
    services: AppServices,
    labels: AppLabels,
    onBack: () -> Unit,
    onOpenUser: (ManageUserUi) -> Unit,
) {
    ScreenPage(
        title = labels.manageUsersTitle,
        onBack = onBack,
        backContentDescription = labels.back,
    ) {
        ManageUsersScreen(
            labels = labels.manageUsers,
            search = { query ->
                withContext(ioDispatcher) {
                    services.api.searchUsers(query).map { it.toManageUserUi() }
                }
            },
            onUserClick = onOpenUser,
        )
    }
}

/**
 * The user admin route: the shared [UserAdminPage] under the standard top bar,
 * showing one user's record with its edit-roles action. Opened from the
 * manage-users search.
 */
@Composable
private fun UserAdminRoute(
    services: AppServices,
    labels: AppLabels,
    route: AppRoute.UserAdmin,
    onBack: () -> Unit,
) {
    UserAdminPage(
        user = route.user,
        labels = labels,
        loadCaller = {
            withContext(ioDispatcher) {
                services.db.user.select()?.let { CallerRoles(it.id, it.roles) }
                    ?: CallerRoles(id = -1L, roles = emptyList())
            }
        },
        loadAreaName = { id ->
            withContext(ioDispatcher) {
                services.db.area.selectById(id)?.getLocalizedName()?.ifBlank { null }
            }
        },
        loadAreas = {
            withContext(ioDispatcher) {
                services.db.area.selectAll().map {
                    GeofenceAreaUi(
                        id = it.id,
                        name = it.getLocalizedName().ifBlank { EARTH_NAME },
                        type = it.type,
                    )
                }
            }
        },
        updateRoles = { userId, roles ->
            withContext(ioDispatcher) {
                services.api.updateUser(userId, roles = roles).toManageUserUi()
            }
        },
        updateGeofence = { userId, geofence ->
            withContext(ioDispatcher) {
                services.api.updateUser(userId, geofence = geofence).toManageUserUi()
            }
        },
        onBack = onBack,
    )
}

/**
 * The manage-areas route: the shared [ManageAreasScreen] under the standard top
 * bar, listing every cached area. Only the settings row (itself hidden from
 * non-area-managers) opens it. A geofenced account may only edit the areas in
 * its geofence, so the screen is told to hide its search and list just those.
 */
@Composable
private fun ManageAreasRoute(
    services: AppServices,
    labels: AppLabels,
    onBack: () -> Unit,
    onOpenArea: (Area) -> Unit,
) {
    // Read once per visit: the cached profile's geofence is a tiny preference
    // lookup, so blocking here is cheaper than a state that would briefly render
    // the search before the geofence arrives.
    val geofence = remember {
        runDbBlocking { services.db.user.select()?.geofence ?: emptyList() }
    }

    ScreenPage(
        title = labels.manageAreasTitle,
        onBack = onBack,
        backContentDescription = labels.back,
    ) {
        ManageAreasScreen(
            labels = labels.manageAreas,
            load = { withContext(ioDispatcher) { services.db.area.selectAll() } },
            onAreaClick = onOpenArea,
            geofence = geofence,
        )
    }
}

/**
 * The manage-place-images route: the shared [ManagePlaceImagesScreen] under the
 * standard top bar, listing the newest uploads across every place for
 * moderation. Only the settings row (itself hidden from non-admins) opens it.
 */
@Composable
private fun ManagePlaceImagesRoute(
    services: AppServices,
    labels: AppLabels,
    onBack: () -> Unit,
) {
    val imagesLabels = labels.managePlaceImages

    ScreenPage(
        title = labels.managePlaceImagesTitle,
        onBack = onBack,
        backContentDescription = labels.back,
    ) {
        ManagePlaceImagesScreen(
            labels = imagesLabels,
            load = {
                withContext(ioDispatcher) {
                    services.api.getRecentPlaceImages().map { image ->
                        ManagePlaceImageUi(
                            placeId = image.placeId,
                            imageId = image.id,
                            thumbnailUrl = services.api.placeImageUrl(
                                placeId = image.placeId,
                                imageId = image.id,
                                width = PLACE_PHOTO_THUMBNAIL_SIZE,
                                height = PLACE_PHOTO_THUMBNAIL_SIZE,
                            ),
                            fullUrl = services.api.placeImageUrl(
                                placeId = image.placeId,
                                imageId = image.id,
                                width = PLACE_PHOTO_FULL_SIZE,
                                height = PLACE_PHOTO_FULL_SIZE,
                            ),
                            placeName = services.db.place.selectById(image.placeId)
                                ?.getLocalizedName()
                                ?: imagesLabels.unknownPlace(image.placeId),
                            uploaderName = image.authorName,
                            createdAt = image.createdAt,
                        )
                    }
                }
            },
            delete = { image ->
                services.api.deletePlaceImage(image.placeId, image.imageId)
            },
        )
    }
}

/**
 * The area admin route: the shared [AreaAdminPage] with the host's map preview
 * and a verify action that stamps today's date on the server and in the cache.
 */
@Composable
private fun AreaAdminRoute(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    route: AppRoute.AreaAdmin,
    onBack: () -> Unit,
) {
    val mapStyle = services.rememberMapStyle()

    AreaAdminPage(
        areaId = route.areaId,
        labels = labels,
        onOpenUrl = platform::openUrl,
        load = { withContext(ioDispatcher) { services.db.area.selectById(route.areaId) } },
        verify = { id, date ->
            services.api.verifyArea(id, date)
            withContext(ioDispatcher) {
                services.db.area.selectById(id)?.let {
                    services.db.area.insert(listOf(it.copy(verifiedAt = date)))
                }
            }
        },
        rename = { name ->
            services.api.setAreaName(route.areaId, name)
            withContext(ioDispatcher) {
                services.db.area.selectById(route.areaId)?.let {
                    services.db.area.insert(listOf(it.copy(name = name)))
                }
            }
        },
        updateDescription = { description ->
            services.api.setAreaDescription(route.areaId, description)
            withContext(ioDispatcher) {
                services.db.area.selectById(route.areaId)?.let {
                    services.db.area.insert(listOf(it.copy(description = description)))
                }
            }
        },
        onBack = onBack,
        map = { area ->
            AreaPreviewMap(
                area = area,
                styleUrl = mapStyle.url,
                styleJson = mapStyle.json,
                borderColor = markerPalette(services.settings).markerBackground,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

/**
 * The image stats route: the shared [ImageStatsPage] under the standard top bar,
 * with a refresh action that re-reads the cache snapshot.
 */
@Composable
private fun ImageStatsRoute(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    onBack: () -> Unit,
) {
    var refreshKey by remember { mutableStateOf(0) }

    ScreenPage(
        title = labels.imageStatsTitle,
        onBack = onBack,
        backContentDescription = labels.back,
        actions = {
            IconButton(onClick = { refreshKey++ }) {
                MaterialSymbol(glyph = "refresh", contentDescription = labels.refresh)
            }
        },
    ) {
        ImageStatsPage(
            imageLoader = services.imageLoader,
            homeDirectory = services.imageHomeDirectory,
            labels = labels.imageStats,
            refreshKey = refreshKey,
            onError = platform::showError,
        )
    }
}

/**
 * The infrastructure dashboard route: the shared [InfraDashboardScreen] under
 * the standard [ScreenPage] bar, whose refresh action swaps to a spinner while a
 * load is in flight.
 */
@Composable
private fun InfraDashboardRoute(
    services: AppServices,
    labels: AppLabels,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var refreshKey by remember { mutableStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    ScreenPage(
        title = labels.infraTitle,
        onBack = onBack,
        backContentDescription = labels.back,
        actions = {
            if (refreshing) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                IconButton(onClick = { refreshKey++ }) {
                    MaterialSymbol(glyph = "refresh", contentDescription = labels.refresh)
                }
            }
        },
    ) {
        InfraDashboardScreen(
            load = { services.api.getDashboard() },
            refreshKey = refreshKey,
            onLoadingChange = { refreshing = it },
            modifier = modifier,
        )
    }
}
