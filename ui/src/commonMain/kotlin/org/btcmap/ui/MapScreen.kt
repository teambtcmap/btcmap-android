package org.btcmap.ui

import kotlin.time.Clock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.filterIsInstance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.btcmap.comment.CommentsAdapterItem
import org.btcmap.comment.commentDateFormatter
import org.btcmap.comment.toAdapterItem
import org.btcmap.db.Database
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.place.Place
import org.btcmap.map.EMPTY_GEOJSON
import org.btcmap.map.EVENT_ICON
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.btcmap.map.exchangeMarkerIconImageName
import org.btcmap.util.isUpcoming
import org.btcmap.map.MapArea
import org.btcmap.map.markerImageName
import org.btcmap.search.SearchAdapterItem
import org.btcmap.map.toEventGeoJson
import org.btcmap.map.toMarkerGeoJson
import org.btcmap.map.toNoteGeoJson
import org.btcmap.place.isMerchant
import org.btcmap.ui.map.AREA_CHIP_SIZE
import org.btcmap.ui.map.AddLocationLabels
import org.btcmap.ui.map.AreaChipPalette
import org.btcmap.ui.map.AreaChips
import org.btcmap.ui.map.EventLayers
import org.btcmap.ui.map.ExchangeLayers
import org.btcmap.ui.map.FILTER_BUTTON_INSET
import org.btcmap.ui.map.MAP_BADGE_OFFSET
import org.btcmap.ui.map.MAP_CONTROLS_GAP
import org.btcmap.ui.map.MARKER_PIN_IMAGE_ID
import org.btcmap.ui.map.MapCameraState
import org.btcmap.ui.map.MarkerBitmapFactory
import org.btcmap.ui.map.MarkerClickHandler
import org.btcmap.ui.map.MarkerFilterButtons
import org.btcmap.ui.map.MarkerKind
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.MerchantLayers
import org.btcmap.ui.map.NoteLayers
import org.btcmap.ui.map.NOTE_MARKER_IMAGE_NAME
import org.btcmap.ui.map.SearchActions
import org.btcmap.ui.map.SearchOverlay
import org.btcmap.ui.map.platformMapUiOptions
import org.btcmap.ui.map.rememberSearchResults
import org.btcmap.ui.map.rememberMapAreas
import org.btcmap.ui.map.rememberReloadedPlace
import org.btcmap.ui.map.rememberViewportFeatures
import org.btcmap.ui.map.setBitmap
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.LocationIndicatorLayer
import org.maplibre.compose.location.LocationPermission
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.map.LocalMapState
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Instant

/**
 * The shared MapLibre Compose map: the app's base style, the merchant, event and
 * exchange layer pipelines, and the viewport-driven loading of their features.
 */
@Composable
fun MapScreen(
    db: Database,
    styleUrl: String,
    styleJson: String? = null,
    initialLat: Double,
    initialLon: Double,
    initialZoom: Double,
    /** The camera's rotation, in degrees clockwise from north. */
    initialBearing: Double = 0.0,
    /** The camera's pitch, in degrees from straight down. */
    initialTilt: Double = 0.0,
    minVerifiedAt: Instant?,
    palette: MarkerPalette,
    areaChipPalette: AreaChipPalette,
    apiUrl: String,
    usingOpenFreeMap: Boolean,
    /**
     * Whether user input may change the map's bearing. Programmatic camera
     * moves are unaffected, so the map can still be pointed at a place.
     */
    mapRotationEnabled: Boolean = false,
    /**
     * Whether user input may change the map's tilt. Programmatic camera moves
     * are unaffected, so the map can still be pitched at a place.
     */
    mapTiltEnabled: Boolean = false,
    iconFont: FontFamily?,
    placeSheetStrings: PlaceSheetStrings,
    searchActions: SearchActions? = null,
    /**
     * Creates a place at the map centre, if the host can. The map supplies the
     * coordinates because only it knows where the user is looking.
     */
    onAddPlace: ((Double, Double) -> Unit)? = null,
    /**
     * Creates an event at the map centre, if the host can. Offered alongside
     * [onAddPlace] in the add-location chooser.
     */
    onAddEvent: ((Double, Double) -> Unit)? = null,
    /**
     * Creates a note at the map centre, if the host can. Offered alongside
     * [onAddPlace] and [onAddEvent] in the add-location chooser.
     */
    onAddNote: ((Double, Double) -> Unit)? = null,
    /** The strings of the read-only dialog a note pin opens. */
    noteDialogLabels: NoteDialogLabels = NoteDialogLabels(title = "Note", ok = "OK"),
    /** The labels of the add-location chooser. */
    addLocationLabels: AddLocationLabels = AddLocationLabels(
        addPlace = "Add a place",
        addEvent = "Add an event",
        addNote = "Add a note",
    ),
    onOpenFeed: ((List<MapArea>) -> Unit)? = null,
    /**
     * Opens the infrastructure dashboard. Null unless the signed-in user holds
     * an admin or root role, so the button is only drawn for them.
     */
    onOpenInfra: (() -> Unit)? = null,
    /**
     * Opens the event review queue. Null unless the signed-in user holds an
     * event manager, admin or root role, so the button is only drawn for them.
     */
    onOpenEventReview: (() -> Unit)? = null,
    /** The number of events awaiting review, shown as the button's badge. */
    pendingEventCount: Int = 0,
    /**
     * Bumped by the host when the synced data changed, so the markers and the
     * area chips are queried again rather than left stale until the next camera
     * move.
     */
    reloadKey: Int = 0,
    /**
     * Whether the map draws its attribution. The app has a setting for this, so
     * it is the map's own overlay rather than a fixed part of it.
     */
    showAttribution: Boolean = true,
    /** Whether the sync spinner is shown above the marker filter buttons. */
    syncVisible: Boolean = false,
    /** Whether the update button is shown above the marker filter buttons. */
    updateVisible: Boolean = false,
    onUpdateClick: () -> Unit = {},
    /**
     * The attribution line, shown centred at the bottom of the map. The host
     * supplies it because the string is localized there.
     */
    attributionText: String = "© OpenStreetMap contributors",
    /** The colour of [attributionText], which the host resolves from its theme. */
    attributionTextColor: Color = Color.Black.copy(alpha = 0.8f),
    /** Reports where the camera came to rest, so the host can remember it. */
    onCameraIdle: ((MapCameraState) -> Unit)? = null,
    /**
     * Called once, after the first non-empty marker snapshot has been drawn, for
     * startup metrics. Android's default fully-drawn moment is the first frame,
     * which here is an empty map, so the host reports it from here instead.
     */
    onFeaturesDrawn: (() -> Unit)? = null,
    /**
     * Whether the map draws its own place sheet. A host with a place screen of
     * its own says no and handles [onPlaceSelected] itself.
     */
    placeSheet: Boolean = true,
    /**
     * A point the host wants the map to move to, as latitude to longitude, for
     * a place that is not in the local cache or a note tapped in the profile.
     */
    openTarget: Pair<Double, Double>? = null,
    /** Called once the move to [openTarget] has finished. */
    onOpenTargetConsumed: () -> Unit = {},
    /**
     * A place the host wants the map to open from outside it, such as a deep
     * link. The map selects it and moves to it, as a tap on its marker would.
     */
    openPlaceId: Long? = null,
    photos: List<PlacePhoto> = emptyList(),
    bookmarked: Boolean = false,
    /** Whether a photo upload is running for the shown place. */
    addingPhoto: Boolean = false,
    onPlaceSelected: (Place) -> Unit = {},
    /**
     * Called when the map closes its own place sheet, so a host that remembers
     * the selection (to reopen it after the view is recreated) can forget it.
     */
    onPlaceDismissed: () -> Unit = {},
    onPlaceAction: (Place, PlaceAction) -> Unit,
    /**
     * Deletes a photo the signed-in user is allowed to remove; null when the
     * host does not offer deletion. The map closes the viewer after calling it.
     */
    onDeletePhoto: ((PlacePhoto) -> Unit)? = null,
    onSelectEvent: (Event) -> Unit,
    onSelectArea: (Long) -> Unit,
    formatDistance: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // The part of the map the search bar and the place sheet leave visible, so a
    // selected place can be centred in it rather than under the sheet.
    var searchBarBottomPx by remember { mutableStateOf(0f) }
    var viewportTopPx by remember { mutableStateOf(0f) }
    var viewportHeightPx by remember { mutableStateOf(0f) }

    // The camera padding that centres a place between the search bar's bottom
    // and the sheet's top edge. The sheet opens at half the viewport, so its top
    // edge sits there; the horizontal padding stays zero, centring the place.
    fun placeSheetPadding(): DpPadding? {
        if (!placeSheet || searchBarBottomPx <= 0f || viewportHeightPx <= 0f) return null
        return with(density) {
            DpPadding(
                top = (searchBarBottomPx - viewportTopPx).toDp(),
                bottom = (viewportHeightPx / 2f).toDp(),
            )
        }
    }

    // Opening a place is a coordinate jump: the zoom the user is at is kept when
    // it is already close enough (15+, where the cluster layers stop clustering),
    // otherwise the map zooms in to 15 so the place is not lost inside a cluster.
    fun placeCameraUpdate(place: Place, currentZoom: Double?): CameraUpdate = CameraUpdate(
        target = Position(place.lon, place.lat),
        zoom = if (currentZoom != null && currentZoom < PLACE_MIN_ZOOM) PLACE_MIN_ZOOM else null,
        padding = placeSheetPadding(),
    )

    val scope = rememberCoroutineScope()
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    // Bumped on every marker tap, even for the place already selected, so the
    // recentring effect below runs again.
    var recenterKey by remember { mutableStateOf(0) }
    // The sheet shows the current row: a sync rewrites rows in place, so the
    // selected copy is re-read whenever the host bumps the reload key.
    val shownPlace = rememberReloadedPlace(selectedPlace, db, reloadKey)
    // A tap that no marker layer consumed (empty map) dismisses the sheet, as
    // the Views map did. The handler reads the live selection, so it is built
    // once instead of on every recomposition; the dismissal callback goes
    // through a live state so a changed host lambda is still the one called.
    val currentOnPlaceDismissed by rememberUpdatedState(onPlaceDismissed)
    val mapInteractions = remember(mapRotationEnabled, mapTiltEnabled) {
        MapInteractions {
            camera {
                // Bearing permission. A gesture binding cannot re-enable a
                // movement the camera disallows, so this alone stops user
                // rotation (two-finger twist, drag, keys).
                rotate { enabled = mapRotationEnabled }
                // Pitch permission, on the same terms as rotation.
                tilt { enabled = mapTiltEnabled }
            }
            callbacks {
                click {
                    onUnhandled {
                        selectedPlace = null
                        currentOnPlaceDismissed()
                        ClickResult.Consume
                    }
                }
            }
        }
    }
    var selectedComments by remember { mutableStateOf<List<CommentsAdapterItem>>(emptyList()) }

    LaunchedEffect(shownPlace) {
        val place = shownPlace
        selectedComments = if (place == null) {
            emptyList()
        } else {
            withContext(Dispatchers.Default) {
                val formatter = commentDateFormatter()
                db.comment.selectByPlaceId(place.id).map { it.toAdapterItem(formatter) }
            }
        }
    }

    val onMarkerClick: MarkerClickHandler = { features ->
        val properties = features.firstOrNull()?.properties
        val id = properties?.get("id")?.jsonPrimitive?.longOrNull
        if (id == null) {
            ClickResult.Pass
        } else {
            // Event features carry no iconId; place features do.
            val isEvent = properties["iconId"] == null
            scope.launch {
                if (isEvent) {
                    withContext(Dispatchers.Default) { db.event.selectById(id) }?.let(onSelectEvent)
                } else {
                    withContext(Dispatchers.Default) { db.place.selectById(id) }?.let {
                        selectedPlace = it
                        onPlaceSelected(it)
                        recenterKey++
                    }
                }
            }
            ClickResult.Consume
        }
    }

    // Tapping a note pin opens a read-only dialog with its text.
    var shownNoteText by remember { mutableStateOf<String?>(null) }
    val onNoteClick: MarkerClickHandler = { features ->
        val id = features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.longOrNull
        if (id == null) {
            ClickResult.Pass
        } else {
            scope.launch {
                withContext(Dispatchers.Default) { db.note.selectById(id) }?.let { shownNoteText = it.text }
            }
            ClickResult.Consume
        }
    }

    val factory = remember(textMeasurer, iconFont, density, palette) {
        MarkerBitmapFactory(
            textMeasurer = textMeasurer,
            iconFont = iconFont,
            density = density,
            palette = palette,
        )
    }

    // The marker layers may only be declared once their images exist in the
    // style: a layer declared earlier keeps the image it resolved as missing.
    var imagesReady by remember { mutableStateOf(false) }

    // The source data is filled in below, once the map state the loaders observe
    // exists. The layer content reads it through these states.
    val merchantsGeoJson = remember { mutableStateOf(EMPTY_GEOJSON) }
    val exchangesGeoJson = remember { mutableStateOf(EMPTY_GEOJSON) }
    val eventsGeoJson = remember { mutableStateOf(EMPTY_GEOJSON) }
    val noteGeoJson = remember { mutableStateOf(EMPTY_GEOJSON) }

    // The signed-in user's personal notes are always drawn, whatever marker kind
    // is selected. They are not viewport-bound, so every cached note is loaded;
    // the sync's NotesChanged event bumps reloadKey to refresh them.
    LaunchedEffect(reloadKey) {
        noteGeoJson.value = withContext(Dispatchers.Default) { db.note.selectAll().toNoteGeoJson() }
    }

    // The platform's own location provider: the module resolves the Android
    // framework or fused provider and the desktop portal, and reports an
    // unsupported backend through [locationState] rather than failing.
    val locationState = rememberLocationState()

    // Set when the user asked for their location but no fix has arrived yet:
    // the tracking effect below moves the camera as soon as one does.
    var recenterToLocation by remember { mutableStateOf(false) }

    // Which marker kind the map shows. One at a time, as the Views button group
    // was; merchants are the default.
    var markerKind by rememberSaveable { mutableStateOf(MarkerKind.Merchants) }

    val state = rememberMapState(
        // The bundled styles are handed over as JSON: the Compose map cannot
        // read the asset:// style the Android SDK uses.
        baseStyle = if (styleJson != null) BaseStyle.Json(styleJson) else BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(
            target = Position(initialLon, initialLat),
            zoom = initialZoom,
            bearing = initialBearing,
            tilt = initialTilt,
        ),
    ) {
        // Only the selected kind is declared: 0.19.0 fixed the 0.18.0 quirk that
        // made hiding a kind stop the others drawing (see the working notes).
        when (markerKind) {
            MarkerKind.Merchants -> MerchantLayers(
                geoJson = merchantsGeoJson.value,
                clusterBackgroundColor = palette.markerBackground,
                clusterTextColor = palette.markerIcon,
                usingOpenFreeMap = usingOpenFreeMap,
                showMarkers = imagesReady,
                onMarkerClick = onMarkerClick,
            )

            MarkerKind.Events -> EventLayers(
                geoJson = eventsGeoJson.value,
                clusterBackgroundColor = palette.markerBackground,
                clusterTextColor = palette.markerIcon,
                usingOpenFreeMap = usingOpenFreeMap,
                showMarkers = imagesReady,
                onMarkerClick = onMarkerClick,
            )

            MarkerKind.Exchanges -> ExchangeLayers(
                geoJson = exchangesGeoJson.value,
                clusterBackgroundColor = palette.markerBackground,
                clusterTextColor = palette.markerIcon,
                badgeBackgroundColor = palette.badgeBackground,
                badgeTextColor = palette.badgeText,
                usingOpenFreeMap = usingOpenFreeMap,
                showMarkers = imagesReady,
                onMarkerClick = onMarkerClick,
            )
        }

        // The signed-in user's own notes are always drawn, on top of whichever
        // marker kind is selected, and never clustered.
        NoteLayers(
            geoJson = noteGeoJson.value,
            showMarkers = imagesReady,
            onClick = onNoteClick,
        )

        // The puck draws the last known position. The tracking effect is the
        // only thing that moves the camera, and only when the location button
        // asked for it: following every fix would fight the user's gestures.
        val mapState = checkNotNull(LocalMapState.current)
        LocationIndicatorLayer(
            id = LOCATION_INDICATOR_LAYER_ID,
            locationState = locationState,
        )
        LocationTrackingEffect(locationState = locationState) {
            if (recenterToLocation) {
                recenterToLocation = false
                mapState.animateCamera(
                    CameraUpdate(target = currentLocation.position, zoom = LOCATION_ZOOM),
                )
            }
        }
    }

    val areas = rememberMapAreas(state, db, reloadKey)

    // A marker tap centres the place between the search bar and the sheet. The
    // padding stays when the sheet closes, so dismissing it does not shift the
    // map back.
    LaunchedEffect(recenterKey) {
        if (recenterKey == 0) return@LaunchedEffect
        selectedPlace?.let { state.animateCamera(placeCameraUpdate(it, state.cameraPosition?.zoom)) }
    }

    // The host may want to remember where the user left the map.
    LaunchedEffect(state, onCameraIdle) {
        val callback = onCameraIdle ?: return@LaunchedEffect
        state.events.filterIsInstance<MapEvent.CameraMoveEnded>().collect {
            val camera = state.cameraPosition ?: return@collect
            callback(
                MapCameraState(
                    lat = camera.target.latitude,
                    lon = camera.target.longitude,
                    zoom = camera.zoom,
                    bearing = camera.bearing,
                    tilt = camera.tilt,
                ),
            )
        }
    }

    LaunchedEffect(openTarget) {
        val target = openTarget ?: return@LaunchedEffect
        state.animateCamera(
            CameraUpdate(target = Position(target.second, target.first), zoom = OPEN_ZOOM),
        )
        onOpenTargetConsumed()
    }

    LaunchedEffect(openPlaceId) {
        val id = openPlaceId ?: return@LaunchedEffect
        val place = withContext(Dispatchers.Default) { db.place.selectById(id) }
            ?: return@LaunchedEffect
        selectedPlace = place
        // The place may be of the kind the filter is hiding, so show its kind
        // before moving to it, as the Views map did.
        markerKind = if (place.isMerchant()) MarkerKind.Merchants else MarkerKind.Exchanges
        onPlaceSelected(place)
        state.animateCamera(placeCameraUpdate(place, state.cameraPosition?.zoom))
    }

    // The host supplies what it can do, but only the map knows where it is
    // looking, so the add-location actions are handed the map centre.
    val mapSearchActions = if (
        searchActions == null && onAddPlace == null && onAddEvent == null && onAddNote == null
    ) {
        null
    } else {
        SearchActions(
            onAddPlace = onAddPlace?.let { createPlace ->
                {
                    state.cameraPosition?.target?.let { centre ->
                        createPlace(centre.latitude, centre.longitude)
                    }
                }
            },
            onAddEvent = onAddEvent?.let { createEvent ->
                {
                    state.cameraPosition?.target?.let { centre ->
                        createEvent(centre.latitude, centre.longitude)
                    }
                }
            },
            onAddNote = onAddNote?.let { createNote ->
                {
                    state.cameraPosition?.target?.let { centre ->
                        createNote(centre.latitude, centre.longitude)
                    }
                }
            },
            onSettings = searchActions?.onSettings,
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val searchFocusManager = LocalFocusManager.current
    val searchResults = rememberSearchResults(
        db = db,
        state = state,
        query = searchQuery,
        formatDistance = formatDistance,
    )
    val onSearchResultClick: (SearchAdapterItem) -> Unit = { result ->
        searchQuery = ""
        // Leave search mode: drop focus so the keyboard closes and the field's
        // resting actions come back.
        searchFocusManager.clearFocus()
        when (result) {
            is SearchAdapterItem.Place -> scope.launch {
                withContext(Dispatchers.Default) { db.place.selectById(result.placeId) }?.let { place ->
                    selectedPlace = place
                    markerKind = if (place.isMerchant()) MarkerKind.Merchants else MarkerKind.Exchanges
                    onPlaceSelected(place)
                    // A search result can be anywhere, so the map jumps straight
                    // to it rather than only opening its sheet; the current
                    // bearing and tilt are kept, and the zoom only moves when it
                    // is below the un-clustering threshold.
                    state.cameraPosition?.let { current ->
                        state.setCameraPosition(
                            current.copy(
                                target = Position(place.lon, place.lat),
                                zoom = maxOf(current.zoom, PLACE_MIN_ZOOM),
                                padding = placeSheetPadding() ?: DpPadding.Zero,
                            ),
                        )
                    }
                }
            }

            is SearchAdapterItem.Event -> scope.launch {
                withContext(Dispatchers.Default) { db.event.selectById(result.eventId) }
                    ?.let(onSelectEvent)
            }

            // A search result frames the area on the map, as the Views map did;
            // the area screen stays one tap away on the chip that appears. An
            // area without a bbox has nothing to frame, so it opens instead. A
            // result can be a continent away, so the camera jumps rather than
            // flying there.
            is SearchAdapterItem.Area -> {
                val bbox = result.bbox
                if (bbox != null && bbox.size == 4) {
                    scope.launch {
                        state.fitCameraToBounds(
                            BoundingBox(
                                west = bbox[0],
                                south = bbox[1],
                                east = bbox[2],
                                north = bbox[3],
                            ),
                        )
                    }
                } else {
                    onSelectArea(result.areaId)
                }
            }
        }
    }

    val merchants = rememberViewportFeatures(
        state = state,
        reloadKey = reloadKey,
        idOf = { it.id },
        locationOf = { it.lat to it.lon },
        toGeoJson = { it.toMarkerGeoJson() },
    ) { bounds ->
        bounds.longitudeRanges().flatMap { (minLon, maxLon) ->
            db.place.selectMerchantsByBounds(bounds.south, bounds.north, minLon, maxLon, minVerifiedAt)
        }
    }

    val exchanges = rememberViewportFeatures(
        state = state,
        reloadKey = reloadKey,
        idOf = { it.id },
        locationOf = { it.lat to it.lon },
        toGeoJson = { it.toMarkerGeoJson() },
    ) { bounds ->
        bounds.longitudeRanges().flatMap { (minLon, maxLon) ->
            db.place.selectExchangesByBounds(bounds.south, bounds.north, minLon, maxLon, minVerifiedAt)
        }
    }

    val events = rememberViewportFeatures(
        state = state,
        reloadKey = reloadKey,
        idOf = { it.id },
        locationOf = { it.lat to it.lon },
        toGeoJson = { it.toEventGeoJson() },
    ) { bounds ->
        val now = Clock.System.now()
        bounds.longitudeRanges().flatMap { (minLon, maxLon) ->
            db.event.selectByBounds(bounds.south, bounds.north, minLon, maxLon)
        }.filter { it.startsAt.isUpcoming(now) }
    }

    SideEffect {
        merchantsGeoJson.value = merchants.geoJson
        exchangesGeoJson.value = exchanges.geoJson
        eventsGeoJson.value = events.geoJson
    }

    val markersByName = remember(merchants.snapshot) {
        val now = Clock.System.now()
        merchants.snapshot.associateBy { it.markerImageName(now) }
    }
    val exchangeIcons = remember(exchanges.snapshot) { exchanges.snapshot.map { it.icon }.distinct() }
    val hasEvents = events.snapshot.isNotEmpty()

    // Reported once, on the frame after the first non-empty snapshot, so the
    // metric matches the map the user is waiting for rather than an empty one.
    var featuresReported by remember { mutableStateOf(false) }
    val hasFeatures = merchants.snapshot.isNotEmpty() ||
        exchanges.snapshot.isNotEmpty() ||
        events.snapshot.isNotEmpty()
    LaunchedEffect(imagesReady, hasFeatures, onFeaturesDrawn) {
        val callback = onFeaturesDrawn ?: return@LaunchedEffect
        if (!imagesReady || !hasFeatures || featuresReported) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        featuresReported = true
        callback()
    }

    val loadState = state.style.loadState
    // The marker image names already registered in the style. Building a bitmap
    // is not cheap (it parses the pin path, measures the glyph and paints), and
    // a marker's name fully determines its image, so only the names the loaded
    // features just added are built and registered. Redrawing them all on every
    // viewport change is what froze the UI once a session had accumulated many.
    val registeredImages = remember { mutableSetOf<String>() }
    var imagesCachedFor by remember { mutableStateOf<Any?>(null) }
    var imagesCachedFactory by remember { mutableStateOf<MarkerBitmapFactory?>(null) }

    LaunchedEffect(state, factory, markersByName, exchangeIcons, hasEvents, loadState) {
        if (loadState !is StyleLoadState.Ready) return@LaunchedEffect

        // A reloaded style drops the registered images, and a new palette
        // repaints them, so rebuild everything instead of trusting the cache.
        if (imagesCachedFor !== loadState || imagesCachedFactory !== factory) {
            registeredImages.clear()
            imagesCachedFor = loadState
            imagesCachedFactory = factory
        }

        // What the map currently needs, and how to draw each missing one.
        val wanted = LinkedHashMap<String, () -> ImageBitmap>()
        wanted[MARKER_PIN_IMAGE_ID] = { factory.pin(palette.markerBackground) }
        wanted[NOTE_MARKER_IMAGE_NAME] = { factory.notePin() }
        if (hasEvents) {
            wanted[EVENT_MARKER_ICON_NAME] = { factory.icon(EVENT_ICON, palette.markerIcon) }
        }
        markersByName.forEach { (name, marker) ->
            wanted[name] = { factory.merchantMarker(marker) }
        }
        exchangeIcons.forEach { icon ->
            wanted[exchangeMarkerIconImageName(icon)] = { factory.icon(icon, palette.markerIcon) }
        }

        val missing = wanted.keys.filterNot { it in registeredImages }
        if (missing.isEmpty()) return@LaunchedEffect

        // Take the marker layers out for a frame so they are declared again, and
        // so resolve the image names the loaded features added.
        imagesReady = false
        withFrameNanos { }

        val images = state.style.images
        missing.forEach { name ->
            images.setBitmap(name, wanted.getValue(name)())
            registeredImages.add(name)
        }

        imagesReady = true
    }

    AppTheme(iconFont = iconFont) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    viewportTopPx = it.positionInRoot().y
                    viewportHeightPx = it.size.height.toFloat()
                },
        ) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                state = state,
                interactions = mapInteractions,
                // Texture rendering on Android: the default Surface mode leaves
                // the map blank after the view is resized (maplibre-compose#1121).
                uiOptions = platformMapUiOptions(),
                // The map would otherwise include MapOverlay.Default, its
                // MapLibre logo and expanding attribution pill. The app draws
                // its own attribution line below, as the Views map did.
                overlay = {},
            )
            shownNoteText?.let { text ->
                NoteDialog(
                    text = text,
                    labels = noteDialogLabels,
                    onDismiss = { shownNoteText = null },
                )
            }
            if (showAttribution) {
                Text(
                    text = attributionText,
                    color = attributionTextColor,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ATTRIBUTION_BOTTOM),
                )
            }
            // The map's own controls wear the app's configurable button colours,
            // not Material's tonal container, so they keep the look the Views map
            // had (and the area chips already share it).
            val controlColors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = areaChipPalette.buttonBackground,
                contentColor = areaChipPalette.buttonIcon,
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(MAP_CONTROLS_GAP),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = MAP_CONTROLS_BOTTOM),
            ) {
                AreaChips(
                    areas = areas,
                    apiUrl = apiUrl,
                    palette = areaChipPalette,
                    onAreaClick = { onSelectArea(it.id) },
                )
                onOpenFeed?.let { openFeed ->
                    // The feed is about the areas the map is showing, and the
                    // map is the only one that knows them.
                    FilledTonalIconButton(
                        onClick = { openFeed(areas) },
                        colors = controlColors,
                        modifier = Modifier.size(AREA_CHIP_SIZE),
                    ) {
                        MaterialSymbol(glyph = "vital_signs", contentDescription = null)
                    }
                }
                onOpenInfra?.let { openInfra ->
                    // Shown only to admins and roots; the host decides whether to
                    // supply the callback.
                    FilledTonalIconButton(
                        onClick = openInfra,
                        colors = controlColors,
                        modifier = Modifier.size(AREA_CHIP_SIZE),
                    ) {
                        MaterialSymbol(glyph = "host", contentDescription = "Infra dashboard")
                    }
                }
                onOpenEventReview?.let { openReview ->
                    // Shown only to event managers/admins/roots; the badge counts
                    // the submissions waiting for review.
                    BadgedBox(
                        badge = {
                            if (pendingEventCount > 0) {
                                Badge(
                                    // The same colours and corner-inset the area
                                    // chip badges use, so every count on the map
                                    // reads alike.
                                    containerColor = areaChipPalette.badgeBackground,
                                    contentColor = areaChipPalette.badgeText,
                                    modifier = Modifier.offset(MAP_BADGE_OFFSET.x, MAP_BADGE_OFFSET.y),
                                ) {
                                    Text(
                                        text = if (pendingEventCount > 99) {
                                            "99+"
                                        } else {
                                            pendingEventCount.toString()
                                        },
                                    )
                                }
                            }
                        },
                    ) {
                        FilledTonalIconButton(
                            onClick = openReview,
                            colors = controlColors,
                            modifier = Modifier.size(AREA_CHIP_SIZE),
                        ) {
                            MaterialSymbol(glyph = "event_upcoming", contentDescription = "Review events")
                        }
                    }
                }
                FilledTonalIconButton(
                    onClick = {
                        if (locationState.permission is LocationPermission.Granted) {
                            val last = locationState.lastLocation
                            if (last == null) {
                                // Tracking is on, but no fix has arrived yet.
                                recenterToLocation = true
                            } else {
                                scope.launch {
                                    state.animateCamera(
                                        CameraUpdate(
                                            target = last.position,
                                            zoom = LOCATION_ZOOM,
                                        ),
                                    )
                                }
                            }
                        } else {
                            // The button doubles as the prompt, the way the
                            // Android map's own location button does.
                            recenterToLocation = true
                            locationState.requestPermission()
                        }
                    },
                    colors = controlColors,
                    modifier = Modifier.size(AREA_CHIP_SIZE),
                ) {
                    MaterialSymbol(glyph = "my_location", contentDescription = null)
                }
            }
            if (placeSheet) shownPlace?.let { place ->
                PlaceSheet(
                    place = place,
                    comments = selectedComments,
                    photos = photos,
                    bookmarked = bookmarked,
                    strings = placeSheetStrings,
                    onAction = { onPlaceAction(place, it) },
                    onDeletePhoto = onDeletePhoto,
                    addingPhoto = addingPhoto,
                    onDismiss = {
                        selectedPlace = null
                        onPlaceDismissed()
                    },
                )
            }
            // The sync and update indicators sit above the marker filter, as the
            // Views button group did before the map swap, so they share a column
            // with it and leave the filter's position untouched.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Like the filter buttons, the sync and update indicators are
                // 40dp circles whose 48dp touch targets pad them by 4dp, so 8dp
                // here reads as MAP_CONTROLS_GAP on screen.
                verticalArrangement = Arrangement.spacedBy(MAP_CONTROLS_GAP - 8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    // The filter buttons' touch targets extend 4dp below their
                    // circles, so lift the group by that much to line their
                    // visual bottoms up with the round buttons on the right.
                    .padding(start = 24.dp, bottom = MAP_CONTROLS_BOTTOM - FILTER_BUTTON_INSET),
            ) {
                if (syncVisible) {
                    val transition = rememberInfiniteTransition()
                    val angle by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                        ),
                    )
                    FilledTonalIconButton(
                        onClick = {},
                        colors = controlColors,
                        // The sync indicator is dimmed, as the Views button was,
                        // so it reads as a status rather than a control.
                        modifier = Modifier.alpha(0.3f),
                    ) {
                        MaterialSymbol(
                            glyph = "sync",
                            contentDescription = null,
                            modifier = Modifier.rotate(angle),
                        )
                    }
                }
                if (updateVisible) {
                    FilledTonalIconButton(
                        onClick = onUpdateClick,
                        colors = controlColors,
                    ) {
                        MaterialSymbol(
                            glyph = "warning",
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                MarkerFilterButtons(
                    selected = markerKind,
                    onSelect = { markerKind = it },
                    palette = areaChipPalette,
                )
            }
            SearchOverlay(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                results = searchResults.items,
                loading = searchResults.loading,
                active = searchResults.active,
                boostedMarkerColor = palette.boostedMarkerBackground,
                onResultClick = onSearchResultClick,
                actions = mapSearchActions,
                addLocationLabels = addLocationLabels,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // The Views SearchBar sat below the status bar with its own
                    // 16dp margin; the map is edge-to-edge, so the inset is drawn
                    // here rather than by the window.
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    // Its bottom edge, with the map's origin as the reference,
                    // bounds the top of the gap a selected place is centred in.
                    .onGloballyPositioned {
                        searchBarBottomPx = it.positionInRoot().y + it.size.height
                    },
            )
        }
    }
}

/** How far the attribution line sits above the bottom of the map. */
private val ATTRIBUTION_BOTTOM = 32.dp

/**
 * How far the side panels sit above the bottom: just clear of the attribution
 * line, as the Views button groups did before the map swap.
 */
private val MAP_CONTROLS_BOTTOM = ATTRIBUTION_BOTTOM + 20.dp

/** The zoom a host-supplied map target is shown at. */
private const val OPEN_ZOOM = 16.0

/**
 * The zoom at which markers stop clustering: the map sources cap clustering at
 * zoom 14, so this is the first zoom where a place jumped to is shown
 * un-clustered.
 */
private const val PLACE_MIN_ZOOM = 15.0

/** The zoom the location button moves to, matching the Android map's own. */
private const val LOCATION_ZOOM = 14.0

/** The id of the user-location layer the puck draws through. */
private const val LOCATION_INDICATOR_LAYER_ID = "user-location"
