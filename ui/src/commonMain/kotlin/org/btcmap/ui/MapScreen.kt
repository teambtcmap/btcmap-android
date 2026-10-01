package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
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
import org.btcmap.db.table.place.toMarker
import org.btcmap.map.EMPTY_GEOJSON
import org.btcmap.map.EVENT_ICON
import org.btcmap.map.EVENT_MARKER_ICON_NAME
import org.btcmap.map.exchangeMarkerIconImageName
import org.btcmap.util.isUpcoming
import org.btcmap.map.markerImageName
import org.btcmap.search.SearchAdapterItem
import org.btcmap.map.toEventGeoJson
import org.btcmap.map.toMarkerGeoJson
import org.btcmap.ui.map.AreaChipPalette
import org.btcmap.ui.map.AreaChips
import org.btcmap.ui.map.EventLayers
import org.btcmap.ui.map.ExchangeLayers
import org.btcmap.ui.map.MARKER_PIN_IMAGE_ID
import org.btcmap.ui.map.MarkerBitmapFactory
import org.btcmap.ui.map.MarkerClickHandler
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.map.MerchantLayers
import org.btcmap.ui.map.PlacePreviewMap
import org.btcmap.ui.map.SearchActions
import org.btcmap.ui.map.SearchOverlay
import org.btcmap.ui.map.rememberSearchResults
import org.btcmap.ui.map.rememberMapAreas
import org.btcmap.ui.map.rememberViewportFeatures
import androidx.compose.material3.FilledTonalIconButton
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.StyleLoadState
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
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
    iconFont: FontFamily?,
    placeSheetStrings: PlaceSheetStrings,
    searchActions: SearchActions? = null,
    onOpenFeed: (() -> Unit)? = null,
    photos: List<String> = emptyList(),
    bookmarked: Boolean = false,
    onPlaceSelected: (Place) -> Unit = {},
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
    var selectedComments by remember { mutableStateOf<List<CommentsAdapterItem>>(emptyList()) }

    LaunchedEffect(selectedPlace) {
        val place = selectedPlace
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

    val state = rememberMapState(
        // The bundled styles are handed over as JSON: the Compose map cannot
        // read the asset:// style the Android SDK uses.
        baseStyle = if (styleJson != null) BaseStyle.Json(styleJson) else BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(
            target = Position(initialLon, initialLat),
            zoom = initialZoom,
        ),
    ) {
        MerchantLayers(
            geoJson = merchantsGeoJson.value,
            clusterBackgroundColor = palette.markerBackground,
            clusterTextColor = palette.markerIcon,
            usingOpenFreeMap = usingOpenFreeMap,
            showMarkers = imagesReady,
            onMarkerClick = onMarkerClick,
        )
        EventLayers(
            geoJson = eventsGeoJson.value,
            clusterBackgroundColor = palette.markerBackground,
            clusterTextColor = palette.markerIcon,
            usingOpenFreeMap = usingOpenFreeMap,
            showMarkers = imagesReady,
            onMarkerClick = onMarkerClick,
        )
        ExchangeLayers(
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

    val areas = rememberMapAreas(state, db)

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
                withContext(Dispatchers.Default) { db.place.selectById(result.placeId) }?.let {
                    selectedPlace = it
                    onPlaceSelected(it)
                }
            }

            is SearchAdapterItem.Event -> scope.launch {
                withContext(Dispatchers.Default) { db.event.selectById(result.eventId) }
                    ?.let(onSelectEvent)
            }

            is SearchAdapterItem.Area -> onSelectArea(result.areaId)
        }
    }

    val merchants = rememberViewportFeatures(
        state = state,
        idOf = { it.id },
        toGeoJson = { it.toMarkerGeoJson() },
    ) { bounds ->
        bounds.longitudeRanges().flatMap { (minLon, maxLon) ->
            db.place.selectMerchantsByBounds(bounds.south, bounds.north, minLon, maxLon, minVerifiedAt)
        }
    }

    val exchanges = rememberViewportFeatures(
        state = state,
        idOf = { it.id },
        toGeoJson = { it.toMarkerGeoJson() },
    ) { bounds ->
        bounds.longitudeRanges().flatMap { (minLon, maxLon) ->
            db.place.selectExchangesByBounds(bounds.south, bounds.north, minLon, maxLon, minVerifiedAt)
        }
    }

    val events = rememberViewportFeatures(
        state = state,
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

    val loadState = state.style.loadState
    LaunchedEffect(state, factory, markersByName, exchangeIcons, hasEvents, loadState) {
        if (loadState !is StyleLoadState.Ready) return@LaunchedEffect

        // Take the marker layers out for a frame so they are declared again, and
        // so resolve the image names the loaded features added.
        imagesReady = false
        withFrameNanos { }

        val images = state.style.images
        images.set(MARKER_PIN_IMAGE_ID, factory.pin(palette.markerBackground))
        if (hasEvents) {
            images.set(EVENT_MARKER_ICON_NAME, factory.icon(EVENT_ICON, palette.markerIcon))
        }
        markersByName.forEach { (name, marker) ->
            images.set(name, factory.merchantMarker(marker))
        }
        exchangeIcons.forEach { icon ->
            images.set(exchangeMarkerIconImageName(icon), factory.icon(icon, palette.markerIcon))
        }

        imagesReady = true
    }

    AppTheme(iconFont = iconFont) {
        Box(modifier = modifier.fillMaxSize()) {
            MaplibreMap(modifier = Modifier.fillMaxSize(), state = state)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 112.dp),
            ) {
                AreaChips(
                    areas = areas,
                    apiUrl = apiUrl,
                    palette = areaChipPalette,
                    onAreaClick = { onSelectArea(it.id) },
                )
                onOpenFeed?.let { openFeed ->
                    FilledTonalIconButton(onClick = openFeed) {
                        MaterialSymbol(glyph = "monitor_heart", contentDescription = null)
                    }
                }
            }
            selectedPlace?.let { place ->
                PlaceSheet(
                    place = place,
                    comments = selectedComments,
                    photos = photos,
                    bookmarked = bookmarked,
                    strings = placeSheetStrings,
                    onAction = { onPlaceAction(place, it) },
                    onDismiss = { selectedPlace = null },
                    previewMap = {
                        PlacePreviewMap(
                            lat = place.lat,
                            lon = place.lon,
                            marker = place.toMarker(),
                            styleUrl = styleUrl,
                            styleJson = styleJson,
                            palette = palette,
                            usingOpenFreeMap = usingOpenFreeMap,
                            iconFont = iconFont,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                        )
                    },
                )
            }
            SearchOverlay(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                results = searchResults,
                onResultClick = onSearchResultClick,
                actions = searchActions,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 16.dp),
            )
        }
    }
}
