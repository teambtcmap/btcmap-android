package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.api.addPlaceImage
import org.btcmap.api.deletePlaceImage
import org.btcmap.api.getPlaceImages
import org.btcmap.comment.CommentsAdapterItem
import org.btcmap.comment.commentDateFormatter
import org.btcmap.comment.toAdapterItem
import org.btcmap.db.table.place.Place
import org.btcmap.db.table.place.toMarker
import org.btcmap.i18n.getLocalizedName
import org.btcmap.platform.ioDispatcher
import org.btcmap.place.canDeletePlaceImage
import org.btcmap.place.osmEditUrl
import org.btcmap.place.osmUrl
import org.btcmap.saved.SavedItems
import org.btcmap.settings.authorized
import org.btcmap.sync.SyncEvent
import org.btcmap.ui.map.PlacePreviewMap
import org.btcmap.util.rethrowIfCancellation

/** Test tag on the place screen's overflow action menu. */
const val PLACE_MENU_TAG = "place-menu"

/** The height of the place's preview map. */
private val PLACE_PREVIEW_MAP_HEIGHT = 192.dp

/**
 * The standalone place screen route: the shared [PlaceDetails] body under the
 * standard top bar, whose overflow menu carries directions, save, share and the
 * OpenStreetMap links.
 *
 * The place, its comments and its photos come from the local cache and the API,
 * and are re-read when a background sync changes the place table.
 */
@Composable
internal fun PlaceRoute(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    route: AppRoute.Place,
    onBack: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onShowAuth: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var place by remember { mutableStateOf<Place?>(null) }
    var comments by remember { mutableStateOf(emptyList<CommentsAdapterItem>()) }
    var photos by remember { mutableStateOf(emptyList<PlacePhoto>()) }
    var bookmarked by remember { mutableStateOf(false) }
    var addingPhoto by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(route.placeId, reloadKey) {
        val loaded = services.db.place.selectById(route.placeId) ?: return@LaunchedEffect
        place = loaded
        comments = withContext(ioDispatcher) {
            val formatter = commentDateFormatter()
            services.db.comment.selectByPlaceId(loaded.id).map { it.toAdapterItem(formatter) }
        }
        photos = loadPhotos(services, loaded.id)
        bookmarked = SavedItems.isPlaceSaved(services.db, loaded.id)
    }

    // A background sync can rewrite the cached row this screen is showing.
    LaunchedEffect(route.placeId) {
        services.syncController.events.collect { event ->
            if (event == SyncEvent.PlacesChanged) reloadKey++
        }
    }

    val loaded = place ?: return Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator()
    }

    val name = loaded.getLocalizedName()
    val osmUrl = loaded.osmUrl()
    val osmEditUrl = loaded.osmEditUrl()
    val mapStyle = services.rememberMapStyle()

    fun toggleSaved() {
        if (!services.settings.authorized) {
            onShowAuth()
            return
        }
        scope.launch {
            SavedItems.togglePlace(services.api, services.db, loaded.id, name)
            bookmarked = SavedItems.isPlaceSaved(services.db, loaded.id)
        }
    }

    fun addPhoto() {
        if (!services.settings.authorized) {
            onShowAuth()
            return
        }
        scope.launch {
            addingPhoto = true
            try {
                val picked = platform.pickPhotos()
                picked.firstOrNull()?.let { bytes ->
                    try {
                        services.api.addPlaceImage(loaded.id, bytes)
                    } catch (t: Throwable) {
                        t.rethrowIfCancellation()
                        platform.showError(t)
                    }
                    photos = loadPhotos(services, loaded.id)
                }
            } finally {
                addingPhoto = false
            }
        }
    }

    ScreenPage(
        title = name,
        onBack = onBack,
        backContentDescription = labels.back,
        actions = {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag(PLACE_MENU_TAG),
                ) {
                    MaterialSymbol(glyph = "more_vert", contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(labels.placeStrings.directions) },
                        onClick = {
                            menuOpen = false
                            platform.openDirections(loaded.lat, loaded.lon)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(labels.save) },
                        onClick = {
                            menuOpen = false
                            toggleSaved()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(labels.placeStrings.share) },
                        onClick = {
                            menuOpen = false
                            platform.shareText("https://btcmap.org/merchant/${loaded.id}")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(labels.placeStrings.viewOnBtcmap) },
                        onClick = {
                            menuOpen = false
                            platform.openOnBtcmap("https://btcmap.org/merchant/${loaded.id}")
                        },
                    )
                    if (osmUrl != null) {
                        DropdownMenuItem(
                            text = { Text(labels.placeStrings.viewOnOsm) },
                            onClick = {
                                menuOpen = false
                                platform.openUrl(osmUrl)
                            },
                        )
                    }
                    if (osmEditUrl != null) {
                        DropdownMenuItem(
                            text = { Text(labels.placeStrings.editOnOsm) },
                            onClick = {
                                menuOpen = false
                                platform.openUrl(osmEditUrl)
                            },
                        )
                    }
                }
            }
        },
    ) {
        PlaceDetails(
            place = loaded,
            comments = comments,
            photos = photos,
            bookmarked = bookmarked,
            strings = labels.placeStrings,
            onAction = { action ->
                when (action) {
                    PlaceAction.Directions -> platform.openDirections(loaded.lat, loaded.lon)

                    PlaceAction.Verify -> {
                        if (services.settings.authorized) {
                            onNavigate(AppRoute.Report(loaded.id, name, "verified"))
                        } else {
                            onShowAuth()
                        }
                    }

                    PlaceAction.Report -> {
                        if (services.settings.authorized) {
                            onNavigate(AppRoute.Report(loaded.id, name, null))
                        } else {
                            onShowAuth()
                        }
                    }

                    PlaceAction.Boost -> onNavigate(AppRoute.Boost(loaded.id, name))
                    PlaceAction.AddComment -> onNavigate(AppRoute.AddComment(loaded.id, name))
                    PlaceAction.AddPhoto -> addPhoto()
                    PlaceAction.ToggleBookmark -> toggleSaved()
                    PlaceAction.Phone -> platform.openDialer(loaded.phone)
                    PlaceAction.Website -> loaded.website?.let { platform.openUrl(it.toString()) }
                    PlaceAction.Email -> platform.openEmail(loaded.email)
                    PlaceAction.Telegram -> loaded.telegram?.let { platform.openUrl(it.toString()) }
                    PlaceAction.Line -> loaded.line?.let { platform.openUrl(it.toString()) }
                    PlaceAction.Twitter -> loaded.twitter?.let { platform.openUrl(it.toString()) }
                    PlaceAction.Facebook -> loaded.facebook?.let { platform.openUrl(it.toString()) }
                    PlaceAction.Instagram -> loaded.instagram?.let { platform.openUrl(it.toString()) }
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
                    photos = loadPhotos(services, loaded.id)
                }
            },
            showHeader = false,
            addingPhoto = addingPhoto,
            previewMap = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PLACE_PREVIEW_MAP_HEIGHT)
                        .clickable { platform.openPlace(loaded.id) },
                ) {
                    PlacePreviewMap(
                        lat = loaded.lat,
                        lon = loaded.lon,
                        marker = loaded.toMarker(),
                        styleUrl = mapStyle.url,
                        styleJson = mapStyle.json,
                        palette = markerPalette(services.settings),
                        usingOpenFreeMap = services.usingOpenFreeMap,
                        iconFont = services.iconFont,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
    }
}

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
