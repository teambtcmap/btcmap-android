package org.btcmap.ui

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
import androidx.compose.ui.platform.LocalDensity
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
import org.btcmap.place.isMerchant
import org.btcmap.ui.map.AREA_CHIP_SIZE
import org.btcmap.ui.map.AreaChipPalette
import org.btcmap.ui.map.AreaChips
import org.btcmap.ui.map.EventLayers
import org.btcmap.ui.map.ExchangeLayers
import org.btcmap.ui.map.FILTER_BUTTON_INSET
import org.btcmap.ui.map.MAP_CONTROLS_GAP
import org.btcmap.ui.map.MARKER_PIN_IMAGE_ID
import org.btcmap.ui.map.MarkerBitmapFactory
import org.btcmap.ui.map.MarkerClickHandler
import org.btcmap.ui.map.MarkerFilterButtons
import org.btcmap.ui.map.MarkerKind
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.MerchantLayers
import org.btcmap.ui.map.SearchActions
import org.btcmap.ui.map.SearchOverlay
import org.btcmap.ui.map.rememberSearchResults
import org.btcmap.ui.map.rememberMapAreas
import org.btcmap.ui.map.rememberReloadedPlace
import org.btcmap.ui.map.rememberViewportFeatures
import org.btcmap.ui.map.setBitmap
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
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import java.time.ZonedDateTime

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
    minVerifiedAt: ZonedDateTime?,
    palette: MarkerPalette,
    areaChipPalette: AreaChipPalette,
    apiUrl: String,
    usingOpenFreeMap: Boolean,
    /**
     * Whether user input may change the map's bearing. Programmatic camera
     * moves are unaffected, so the map can still be pointed at a place.
     */
    mapRotationEnabled: Boolean = false,
    iconFont: FontFamily?,
    placeSheetStrings: PlaceSheetStrings,
    searchActions: SearchActions? = null,
    /**
     * Creates a place at the map centre, if the host can. The map supplies the
     * coordinates because only it knows where the user is looking.
     */
    onAddPlace: ((Double, Double) -> Unit)? = null,
    onOpenFeed: ((List<MapArea>) -> Unit)? = null,
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
    /** Whether the sync spinner is shown above the map controls. */
    syncVisible: Boolean = false,
    /** Whether the update button is shown above the map controls. */
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
    onCameraIdle: ((Double, Double, Double) -> Unit)? = null,
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
     * a place that is not in the local cache.
     */
    openTarget: Pair<Double, Double>? = null,
    /**
     * A place the host wants the map to open from outside it, such as a deep
     * link. The map selects it and moves to it, as a tap on its marker would.
     */
    openPlaceId: Long? = null,
    photos: List<String> = emptyList(),
    bookmarked: Boolean = false,
    onPlaceSelected: (Place) -> Unit = {},
    /**
     * Called when the map closes its own place sheet, so a host that remembers
     * the selection (to reopen it after the view is recreated) can forget it.
     */
    onPlaceDismissed: () -> Unit = {},
    onPlaceAction: (Place, PlaceAction) -> Unit,
    onSelectEvent: (Event) -> Unit,
    onSelectArea: (Long) -> Unit,
    formatDistance: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    // The sheet shows the current row: a sync rewrites rows in place, so the
    // selected copy is re-read whenever the host bumps the reload key.
    val shownPlace = rememberReloadedPlace(selectedPlace, db, reloadKey)
    // A tap that no marker layer consumed (empty map) dismisses the sheet, as
    // the Views map did. The handler reads the live selection, so it is built
    // once instead of on every recomposition; the dismissal callback goes
    // through a live state so a changed host lambda is still the one called.
    val currentOnPlaceDismissed by rememberUpdatedState(onPlaceDismissed)
    val mapInteractions = remember(mapRotationEnabled) {
        MapInteractions {
            camera {
                // Bearing permission. A gesture binding cannot re-enable a
                // movement the camera disallows, so this alone stops user
                // rotation (two-finger twist, drag, keys).
                rotate { enabled = mapRotationEnabled }
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
                    }
                }
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

    // The host may want to remember where the user left the map.
    LaunchedEffect(state, onCameraIdle) {
        val callback = onCameraIdle ?: return@LaunchedEffect
        state.events.filterIsInstance<MapEvent.CameraMoveEnded>().collect {
            val camera = state.cameraPosition ?: return@collect
            callback(camera.target.latitude, camera.target.longitude, camera.zoom)
        }
    }

    LaunchedEffect(openTarget) {
        val target = openTarget ?: return@LaunchedEffect
        state.animateCamera(
            CameraUpdate(target = Position(target.second, target.first), zoom = OPEN_ZOOM),
        )
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
        state.animateCamera(
            CameraUpdate(target = Position(place.lon, place.lat), zoom = OPEN_ZOOM),
        )
    }

    // The host supplies what it can do, but only the map knows where it is
    // looking, so the add-place action is handed the map centre.
    val mapSearchActions = if (searchActions == null && onAddPlace == null) {
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
            onSettings = searchActions?.onSettings,
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val searchResults = rememberSearchResults(
        db = db,
        state = state,
        query = searchQuery,
        formatDistance = formatDistance,
    )
    val onSearchResultClick: (SearchAdapterItem) -> Unit = { result ->
        searchQuery = ""
        when (result) {
            is SearchAdapterItem.Place -> scope.launch {
                withContext(Dispatchers.Default) { db.place.selectById(result.placeId) }?.let { place ->
                    selectedPlace = place
                    markerKind = if (place.isMerchant()) MarkerKind.Merchants else MarkerKind.Exchanges
                    onPlaceSelected(place)
                    // A search result can be anywhere, so the map moves to it
                    // rather than only opening its sheet.
                    state.animateCamera(
                        CameraUpdate(
                            target = Position(place.lon, place.lat),
                            zoom = OPEN_ZOOM,
                        ),
                    )
                }
            }

            is SearchAdapterItem.Event -> scope.launch {
                withContext(Dispatchers.Default) { db.event.selectById(result.eventId) }
                    ?.let(onSelectEvent)
            }

            // A search result frames the area on the map, as the Views map did;
            // the area screen stays one tap away on the chip that appears. An
            // area without a bbox has nothing to frame, so it opens instead.
            is SearchAdapterItem.Area -> {
                val bbox = result.bbox
                if (bbox != null && bbox.size == 4) {
                    scope.launch {
                        state.animateCameraToBounds(
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
        toGeoJson = { it.toEventGeoJson() },
    ) { bounds ->
        val now = ZonedDateTime.now()
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
        val now = ZonedDateTime.now()
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
    LaunchedEffect(state, factory, markersByName, exchangeIcons, hasEvents, loadState) {
        if (loadState !is StyleLoadState.Ready) return@LaunchedEffect

        // Take the marker layers out for a frame so they are declared again, and
        // so resolve the image names the loaded features added.
        imagesReady = false
        withFrameNanos { }

        val images = state.style.images
        images.setBitmap(MARKER_PIN_IMAGE_ID, factory.pin(palette.markerBackground))
        if (hasEvents) {
            images.setBitmap(EVENT_MARKER_ICON_NAME, factory.icon(EVENT_ICON, palette.markerIcon))
        }
        markersByName.forEach { (name, marker) ->
            images.setBitmap(name, factory.merchantMarker(marker))
        }
        exchangeIcons.forEach { icon ->
            images.setBitmap(exchangeMarkerIconImageName(icon), factory.icon(icon, palette.markerIcon))
        }

        imagesReady = true
    }

    AppTheme(iconFont = iconFont) {
        Box(modifier = modifier.fillMaxSize()) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                state = state,
                interactions = mapInteractions,
                // The map would otherwise include MapOverlay.Default, its
                // MapLibre logo and expanding attribution pill. The app draws
                // its own attribution line below, as the Views map did.
                overlay = {},
            )
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
                        modifier = Modifier.size(AREA_CHIP_SIZE).alpha(0.3f),
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
                        modifier = Modifier.size(AREA_CHIP_SIZE),
                    ) {
                        MaterialSymbol(
                            glyph = "warning",
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
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
                    onDismiss = {
                        selectedPlace = null
                        onPlaceDismissed()
                    },
                )
            }
            MarkerFilterButtons(
                selected = markerKind,
                onSelect = { markerKind = it },
                palette = areaChipPalette,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    // The filter buttons' touch targets extend 4dp below their
                    // circles, so lift the group by that much to line their
                    // visual bottoms up with the round buttons on the right.
                    .padding(start = 24.dp, bottom = MAP_CONTROLS_BOTTOM - FILTER_BUTTON_INSET),
            )
            SearchOverlay(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                results = searchResults,
                onResultClick = onSearchResultClick,
                actions = mapSearchActions,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // The Views SearchBar sat below the status bar with its own
                    // 16dp margin; the map is edge-to-edge, so the inset is drawn
                    // here rather than by the window.
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            )
        }
    }
}

/** How far the attribution line sits above the bottom of the map. */
private val ATTRIBUTION_BOTTOM = 32.dp

/**
 * How far the side panels sit above the bottom: just clear of the attribution
 * line, as the Views button groups did before the map swap. The sync indicator
 * in the Android host clears the filter panel from this same offset.
 */
private val MAP_CONTROLS_BOTTOM = ATTRIBUTION_BOTTOM + 20.dp

/** The zoom a place the map was asked to open is shown at. */
private const val OPEN_ZOOM = 16.0

/** The zoom the location button moves to, matching the Android map's own. */
private const val LOCATION_ZOOM = 14.0

/** The id of the user-location layer the puck draws through. */
private const val LOCATION_INDICATOR_LAYER_ID = "user-location"
