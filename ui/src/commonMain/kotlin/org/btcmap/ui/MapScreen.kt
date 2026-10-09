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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
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
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.btcmap.comment.CommentsAdapterItem
import org.btcmap.comment.commentDateFormatter
import org.btcmap.comment.toAdapterItem
import org.btcmap.db.Database
import org.btcmap.platform.currentLanguage
import org.btcmap.db.table.event.Event
import org.btcmap.db.table.note.Note as DbNote
import org.btcmap.db.table.place.Place
import org.btcmap.map.EMPTY_GEOJSON
import org.btcmap.map.EVENT_ICON
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.btcmap.map.exchangeMarkerIconImageName
import org.btcmap.util.isUpcoming
import org.btcmap.map.MapArea
import org.btcmap.map.markerImageName
import org.btcmap.search.NominatimSearch
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
import org.btcmap.ui.map.noteMarkerImageName
import org.btcmap.ui.map.POI_LAYER_IDS
import org.btcmap.ui.map.PoiInfo
import org.btcmap.ui.map.matchesName
import org.btcmap.ui.map.toPoiInfo
import org.btcmap.ui.map.SearchActions
import org.btcmap.ui.map.SearchOverlay
import org.btcmap.ui.map.platformMapUiOptions
import org.btcmap.ui.map.rememberSearchResults
import org.btcmap.ui.map.rememberLocationProvider
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
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.CompassButtonStyle
import org.maplibre.compose.overlay.DisappearingCompassButton
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Instant

/**
 * The shared MapLibre Compose map: the app's base style, the merchant, event,
 * exchange and note layer pipelines, and the viewport-driven loading of their
 * features.
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
     * Which marker kind the map shows, one at a time. Hoisted so a host can keep
     * the user's choice while it leaves and returns to the map; [onMarkerKindChange]
     * reports a choice made on the map. Merchants are the default.
     */
    markerKind: MarkerKind = MarkerKind.Merchants,
    onMarkerKindChange: (MarkerKind) -> Unit = {},
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
    /**
     * Creates a place from a tapped basemap POI, pre-filled with the feature's
     * name, category and position, if the host can. Null hides the POI sheet's
     * add-as-merchant action, as when the host cannot create places.
     */
    onAddPlaceFromPoi: ((PoiInfo) -> Unit)? = null,
    /**
     * Creates a note from a tapped basemap POI, pre-filled with the feature's
     * name and position, if the host can. Null hides the POI sheet's add-as-note
     * action, as when the host cannot create notes.
     */
    onAddNoteFromPoi: ((PoiInfo) -> Unit)? = null,
    /**
     * Opens a URL, so the POI sheet can link to the feature on openstreetmap.org.
     * Null hides those links, as when the host has no browser.
     */
    onOpenUrl: ((String) -> Unit)? = null,
    /** The strings of the sheet a note pin opens. */
    noteSheetLabels: NoteSheetLabels = NoteSheetLabels(
        title = "Note",
        public = "Public",
        private = "Private",
        created = { "Created $it" },
        edit = "Edit note",
        editTitle = "Edit note",
        editFailed = "Couldn't save the note",
        save = "Save",
        changeIcon = "Change icon",
        iconSearchHint = "Search icons",
        iconFailed = "Couldn't change the icon",
        delete = "Delete",
        deleteConfirmTitle = "Delete this note?",
        deleteConfirmMessage = "This note will be permanently removed.",
        deleteFailed = "Couldn't delete the note",
        cancel = "Cancel",
    ),
    /** The strings of the sheet a tapped basemap POI opens. */
    poiSheetLabels: PoiSheetLabels = PoiSheetLabels(
        copied = "Copied to clipboard",
        addAsMerchant = "It accepts bitcoins",
        addAsNote = "Save as note",
        viewOnOsm = "View on openstreetmap.org",
        editOnOsm = "Edit on openstreetmap.org",
    ),
    /**
     * Edits the body of the note the sheet is showing, given its id. Null hides
     * the sheet's edit action, as when the host cannot edit notes.
     */
    onEditNote: (suspend (Long, String) -> Unit)? = null,
    /**
     * Changes the icon of the note the sheet is showing, given its id. Null
     * makes the sheet's header glyph inert, as when the host cannot edit notes.
     */
    onEditNoteIcon: (suspend (Long, String) -> Unit)? = null,
    /**
     * Deletes the note the sheet is showing, given its id. Null hides the sheet's
     * delete action, as when the host cannot delete notes.
     */
    onDeleteNote: (suspend (Long) -> Unit)? = null,
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
     * A transient confirmation shown under the place sheet's header right after
     * the share action fires (the desktop copies the link, so it confirms it).
     * Null shows nothing, which is the case on Android, where share opens the
     * chooser instead.
     */
    placeShareConfirmation: String? = null,
    /**
     * Whether the place sheet lays its overflow actions out inline instead of in
     * a dropdown. The desktop needs this because a popup inside the sheet's
     * dialog layer is positioned against the window, not the sheet, so the menu
     * opens at the sheet's edge.
     */
    placeOverflowInline: Boolean = false,
    /**
     * A point the host wants the map to move to, as latitude to longitude, for
     * a place that is not in the local cache or a note tapped in the profile.
     */
    openTarget: Pair<Double, Double>? = null,
    /**
     * The marker kind to select when moving to [openTarget], so a host that
     * opens the map at a note shows the notes filter. Null leaves the current
     * filter in place.
     */
    openTargetMarkerKind: MarkerKind? = null,
    /** Called once the move to [openTarget] has finished. */
    onOpenTargetConsumed: () -> Unit = {},
    /**
     * A place the host wants the map to open from outside it, such as a deep
     * link. The map selects it and moves to it, as a tap on its marker would.
     */
    openPlaceId: Long? = null,
    /**
     * A place whose sheet the host remembers was open when the map was last
     * left, reopened on entry. One-shot: a later selection reports back through
     * [onPlaceSelected] without this param changing, so it never re-triggers.
     */
    initialPlaceId: Long? = null,
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
    /**
     * The OpenStreetMap search service, whose hits are shown as a second group
     * under the local results. Null leaves the local results on their own, as
     * when the host has no network client.
     */
    nominatimSearch: NominatimSearch? = null,
    /** The heading above the OpenStreetMap group. */
    nominatimHeader: String = "OpenStreetMap",
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
    // A non-BTC-Map OpenStreetMap feature the user tapped on the basemap (a POI
    // the style draws), shown in its own sheet. Independent of the place sheet,
    // which only the app's own marker layers open.
    var shownPoi by remember { mutableStateOf<PoiInfo?>(null) }
    // Bumped on every marker tap, even for the place already selected, so the
    // recentring effect below runs again.
    var recenterKey by remember { mutableStateOf(0) }
    // The sheet shows the current row: a sync rewrites rows in place, so the
    // selected copy is re-read whenever the host bumps the reload key.
    val shownPlace = rememberReloadedPlace(selectedPlace, db, reloadKey)
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
                // A place or event selection replaces any basemap POI sheet.
                shownPoi = null
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

    // Tapping a note pin opens a sheet with its details and icon.
    var shownNote by remember { mutableStateOf<DbNote?>(null) }
    val onNoteClick: MarkerClickHandler = { features ->
        val id = features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.longOrNull
        if (id == null) {
            ClickResult.Pass
        } else {
            scope.launch {
                shownPoi = null
                withContext(Dispatchers.Default) { db.note.selectById(id) }?.let { shownNote = it }
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

    // The distinct icons the cached notes carry, so the map registers one pin
    // image per icon in use instead of one image for every note.
    val noteIcons = remember { mutableStateOf(emptyList<String>()) }

    // The signed-in user's personal notes are drawn when the notes marker kind
    // is selected. They are not viewport-bound, so every cached note is loaded;
    // the sync's NotesChanged event bumps reloadKey to refresh them.
    LaunchedEffect(reloadKey) {
        withContext(Dispatchers.Default) {
            val notes = db.note.selectAll()
            noteGeoJson.value = notes.toNoteGeoJson()
            noteIcons.value = notes.map { it.icon }.distinct()
        }
    }

    // The platform's own location provider: it resolves the Android framework or
    // fused provider, or the desktop portal, and reports an unsupported backend
    // through [locationState] rather than failing. Linux desktop's portal
    // provider is wrapped so the button can actually obtain a fix.
    val locationState = rememberLocationState(provider = rememberLocationProvider())

    // Set when the user asked for their location but no fix has arrived yet:
    // the tracking effect below moves the camera as soon as one does.
    var recenterToLocation by remember { mutableStateOf(false) }

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

            // The signed-in user's own notes, shown like any other kind and
            // never clustered.
            MarkerKind.Notes -> NoteLayers(
                geoJson = noteGeoJson.value,
                showMarkers = imagesReady,
                onClick = onNoteClick,
            )
        }

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

    // A tap that no marker layer consumed is either on a basemap POI the style
    // draws or on empty map. Query the style's POI layers at the tap so a bar or
    // hotel opens its own sheet; anything else dismisses the place sheet, as the
    // Views map did. The query is suspending, so the handler returns at once and
    // the result arrives on the map scope. The dismissal callback goes through a
    // live state so a changed host lambda is still the one called.
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
                    onUnhandled { event ->
                        // A small box around the tap, so it need not land exactly
                        // on the icon; the front-most POI under it wins.
                        val offset = event.screenOffset
                        val hitBox = DpRect(
                            offset.x - POI_HIT_PADDING,
                            offset.y - POI_HIT_PADDING,
                            offset.x + POI_HIT_PADDING,
                            offset.y + POI_HIT_PADDING,
                        )
                        scope.launch {
                            val poi = state.queryRenderedFeatures(
                                rect = hitBox,
                                layerIds = POI_LAYER_IDS,
                            ).firstNotNullOfOrNull { it.toPoiInfo(currentLanguage()) }
                            if (poi != null) {
                                shownPoi = poi
                            } else {
                                selectedPlace = null
                                currentOnPlaceDismissed()
                            }
                        }
                        ClickResult.Consume
                    }
                }
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

    // The host may want to remember where the user left the map. The callback is
    // read through a live state so a recomposition that hands over a new lambda
    // never restarts the collector: `state.events` is replay-less, so a restart
    // would drop a CameraMoveEnded emitted in the gap and the last camera would
    // never be saved.
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
    LaunchedEffect(state) {
        state.events.filterIsInstance<MapEvent.CameraMoveEnded>().collect {
            val callback = currentOnCameraIdle ?: return@collect
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
        openTargetMarkerKind?.let(onMarkerKindChange)
        state.animateCamera(
            CameraUpdate(target = Position(target.second, target.first), zoom = OPEN_ZOOM),
        )
        onOpenTargetConsumed()
    }

    LaunchedEffect(openPlaceId) {
        val id = openPlaceId ?: return@LaunchedEffect
        val place = withContext(Dispatchers.Default) { db.place.selectById(id) }
            ?: return@LaunchedEffect
        // A deep link replaces any basemap POI sheet the map was showing.
        shownPoi = null
        selectedPlace = place
        // The place may be of the kind the filter is hiding, so show its kind
        // before moving to it, as the Views map did.
        onMarkerKindChange(if (place.isMerchant()) MarkerKind.Merchants else MarkerKind.Exchanges)
        onPlaceSelected(place)
        state.animateCamera(placeCameraUpdate(place, state.cameraPosition?.zoom))
    }

    // Reopens the place a host remembered when the map was last left, so
    // returning from a sub-screen — the report form, say — keeps the selection
    // instead of dropping the sheet. A one-shot on entry: a later selection only
    // reports back through [onPlaceSelected] and does not feed back in here.
    LaunchedEffect(Unit) {
        val id = initialPlaceId ?: return@LaunchedEffect
        val place = withContext(Dispatchers.Default) { db.place.selectById(id) }
            ?: return@LaunchedEffect
        shownPoi = null
        selectedPlace = place
        onPlaceSelected(place)
        // MapLibre keeps the sheet padding apart from the camera target, so the
        // restored camera would leave the place at the viewport centre, behind
        // the reopened sheet. Wait for the search bar and viewport to be
        // measured, then apply the same padded camera a marker tap does.
        snapshotFlow { searchBarBottomPx > 0f && viewportHeightPx > 0f }.first { it }
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
        nominatimSearch = nominatimSearch,
    )
    val onSearchResultClick: (SearchAdapterItem) -> Unit = { result ->
        searchQuery = ""
        // Leave search mode: drop focus so the keyboard closes and the field's
        // resting actions come back.
        searchFocusManager.clearFocus()
        when (result) {
            is SearchAdapterItem.Place -> scope.launch {
                withContext(Dispatchers.Default) { db.place.selectById(result.placeId) }?.let { place ->
                    shownPoi = null
                    selectedPlace = place
                    onMarkerKindChange(
                        if (place.isMerchant()) MarkerKind.Merchants else MarkerKind.Exchanges,
                    )
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

            // A typed coordinate has no row behind it: the map simply moves to
            // the point, as a host-supplied target would.
            is SearchAdapterItem.Coordinate -> scope.launch {
                state.animateCamera(
                    CameraUpdate(target = Position(result.lon, result.lat), zoom = OPEN_ZOOM),
                )
            }

            // An OpenStreetMap hit has no row behind it either, so the map
            // simply moves to its coordinates. The user picked this feature by
            // name, so once the destination's tiles have rendered, the basemap
            // POI matching that name opens its own sheet: otherwise a hotel
            // would need a second tap on the icon the map just drew.
            is SearchAdapterItem.Nominatim -> scope.launch {
                state.animateCamera(
                    CameraUpdate(target = Position(result.lon, result.lat), zoom = OSM_RESULT_ZOOM),
                )
                val language = currentLanguage()
                var poi = state.poiMatchingName(result.lat, result.lon, result.name, language)
                if (poi == null) {
                    // The POIs render a frame or two after the camera stops; wait
                    // for the map to go idle, then look again. (The timeout covers
                    // an idle that fired before this subscription.)
                    withTimeoutOrNull(POI_LOOKUP_TIMEOUT_MS) {
                        state.events.filterIsInstance<MapEvent.Idle>().first()
                    }
                    poi = state.poiMatchingName(result.lat, result.lon, result.name, language)
                }
                if (poi != null) {
                    selectedPlace = null
                    currentOnPlaceDismissed()
                    shownPoi = poi
                }
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

    LaunchedEffect(state, factory, markersByName, exchangeIcons, hasEvents, loadState, noteIcons.value) {
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
        noteIcons.value.forEach { icon ->
            wanted[noteMarkerImageName(icon)] = { factory.notePin(icon) }
        }
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
                // its own attribution line below, as the Views map did. The
                // compass is kept from that default overlay, drawn by itself so
                // the logo and attribution are left out.
                overlay = {
                    // Both pixels are measured against the root, so their
                    // difference is the search bar's bottom within the map: the
                    // compass sits centred just under the search bar. It is only
                    // drawn once the bar has been measured, so a map reopened
                    // already rotated does not flash the compass at the top edge
                    // for a frame.
                    if (searchBarBottomPx > 0f) {
                        val compassTop = with(density) {
                            (searchBarBottomPx - viewportTopPx).toDp()
                        } + MAP_CONTROLS_GAP
                        DisappearingCompassButton(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = compassTop),
                            style = CompassButtonStyle(
                                containerColor = areaChipPalette.buttonBackground,
                            ),
                            // The compass points north, so only the bearing
                            // defines home: a tap straightens the map without
                            // flattening a tilt the user chose. It appears
                            // whenever the camera is turned off north, even with
                            // rotation disabled, since a stored bearing can
                            // reopen the map rotated.
                            getHomeUpdate = { CameraUpdate(bearing = 0.0) },
                        )
                    }
                },
            )
            shownNote?.let { note ->
                NoteSheet(
                    text = note.text,
                    icon = note.icon,
                    public = note.public,
                    createdAt = note.createdAt,
                    labels = noteSheetLabels,
                    onDismiss = { shownNote = null },
                    onEditText = onEditNote?.let { edit ->
                        { text ->
                            edit(note.id, text)
                            // The sheet reads its text from the selection, so
                            // carry the edit into it rather than leaving the old
                            // body on screen.
                            shownNote = note.copy(text = text)
                        }
                    },
                    onEditIcon = onEditNoteIcon?.let { edit ->
                        { icon ->
                            edit(note.id, icon)
                            // The map draws one pin image per icon, so an icon
                            // change also has to reach the selection the sheet
                            // reads (and the host's reload refreshes the pin).
                            shownNote = note.copy(icon = icon)
                        }
                    },
                    onDelete = onDeleteNote?.let { delete -> { delete(note.id) } },
                    onDeleted = { shownNote = null },
                )
            }
            shownPoi?.let { poi ->
                PoiSheet(
                    poi = poi,
                    labels = poiSheetLabels,
                    onDismiss = { shownPoi = null },
                    onAddAsMerchant = onAddPlaceFromPoi?.let { add ->
                        {
                            shownPoi = null
                            add(poi)
                        }
                    },
                    onAddAsNote = onAddNoteFromPoi?.let { add ->
                        {
                            shownPoi = null
                            add(poi)
                        }
                    },
                    onOpenUrl = onOpenUrl,
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
                    shareConfirmation = placeShareConfirmation,
                    overflowInline = placeOverflowInline,
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
                    onSelect = onMarkerKindChange,
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
                nominatimResults = searchResults.nominatim,
                nominatimLoading = searchResults.nominatimLoading,
                nominatimHeader = nominatimHeader,
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
 * The zoom an OpenStreetMap search hit is shown at: closer than [OPEN_ZOOM],
 * because the user picked a specific feature from the results list.
 */
private const val OSM_RESULT_ZOOM = 18.0

/**
 * How far around a map tap a basemap POI is looked for, so a touch need not land
 * exactly on the icon the style draws.
 */
private val POI_HIT_PADDING = 16.dp

/**
 * How long to wait for the destination's POIs to render after an OpenStreetMap
 * search jump before giving up on auto-opening one.
 */
private const val POI_LOOKUP_TIMEOUT_MS = 1_500L

/**
 * Finds the basemap POI drawn at (or within [POI_HIT_PADDING] of) [lat]/[lon]
 * whose name matches [name], or null. Used after an OpenStreetMap search jump,
 * so the feature the user picked by name opens its own sheet without a second
 * tap on the icon the map just drew.
 */
private suspend fun MapState.poiMatchingName(
    lat: Double,
    lon: Double,
    name: String,
    language: String,
): PoiInfo? {
    val point = screenLocationFromPosition(Position(lon, lat)) ?: return null
    val hitBox = DpRect(
        point.x - POI_HIT_PADDING,
        point.y - POI_HIT_PADDING,
        point.x + POI_HIT_PADDING,
        point.y + POI_HIT_PADDING,
    )
    return queryRenderedFeatures(rect = hitBox, layerIds = POI_LAYER_IDS)
        .mapNotNull { it.toPoiInfo(language) }
        .firstOrNull { it.matchesName(name) }
}

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
