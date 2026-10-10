package org.btcmap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.btcmap.api.GetEventsItem
import org.btcmap.area.AreaIssues
import org.btcmap.area.AreaSections
import org.btcmap.area.osmEditUrl
import org.btcmap.area.websiteDisplayText
import org.btcmap.db.table.area.Area
import org.btcmap.db.table.place.Place
import org.btcmap.i18n.getLocalizedDescription
import org.btcmap.i18n.getLocalizedName
import org.btcmap.offline.OfflineAreaState
import org.btcmap.offline.OfflineBounds
import org.btcmap.platform.ioDispatcher
import org.btcmap.saved.SavedItems
import org.btcmap.settings.authorized
import org.btcmap.settings.mapStyle
import org.btcmap.ui.map.CITY_AREA_TYPE

private const val JOIN_US_URL = "https://btcmap.org/join-us"

/**
 * The area route: the shared [AreaScreen] body under the standard top bar, with
 * the save and offline-download actions.
 *
 * The area and its sections are read from the local cache, so the screen works
 * offline; the offline pack state comes from the app-scoped `OfflinePacks`. The
 * header image is drawn by [AreaScreen] itself, so the Android collapsing
 * toolbar is not reproduced.
 */
@Composable
internal fun AreaRoute(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    route: AppRoute.Area,    onBack: () -> Unit,
    onOpenEvent: (GetEventsItem) -> Unit,
    onShowAuth: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var area by remember { mutableStateOf<Area?>(null) }
    var missing by remember { mutableStateOf(false) }
    var boostedMerchants by remember { mutableStateOf(emptyList<Place>()) }
    var events by remember { mutableStateOf(emptyList<GetEventsItem>()) }
    var issues by remember { mutableStateOf<AreaIssues?>(null) }
    var saved by remember { mutableStateOf(false) }
    var packed by remember { mutableStateOf<OfflineAreaState?>(null) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(route.areaId) {
        val loaded = withContext(ioDispatcher) { services.db.area.selectById(route.areaId) }
        if (loaded == null) {
            missing = true
            return@LaunchedEffect
        }
        area = loaded
        saved = SavedItems.isAreaSaved(services.db, loaded.id)
        boostedMerchants = loadSection { AreaSections.boostedMerchants(services.db, loaded) }
            ?: emptyList()
        events = loadSection { AreaSections.events(services.db, loaded) } ?: emptyList()
        issues = loadSection { AreaSections.placeIssues(services.api, services.db, loaded.id) }
    }

    LaunchedEffect(route.areaId) {
        services.offlinePacks.states.collect { states -> packed = states[route.areaId] }
    }

    if (missing) {
        // The screen is only opened from a synced area row, so a miss means the
        // row is gone; report it and leave.
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text(labels.errorTitle) },
            text = { Text(labels.errorMessage) },
            confirmButton = {
                TextButton(onClick = onBack) { Text(labels.ok) }
            },
        )
        return
    }

    val loaded = area ?: return Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator()
    }

    val name = loaded.getLocalizedName()
    val bounds = loaded.offlineBounds()
    // Resolved from the current setting, so a pack downloaded after the user
    // changes the map style uses that style's hosted URL, and a pack from an
    // earlier style is reported as a mismatch.
    val offlineStyleUrl = services.offlineStyleUrlFor(services.settings.mapStyle)
    // A city's website is its generated btcmap.org page, which adds nothing to
    // the screen, so the row is hidden for cities only.
    val showWebsite = loaded.type != CITY_AREA_TYPE

    ScreenPage(
        title = name,
        onBack = onBack,
        backContentDescription = labels.back,
        actions = {
            IconButton(
                onClick = {
                    if (!services.settings.authorized) {
                        onShowAuth()
                    } else {
                        scope.launch {
                            SavedItems.toggleArea(services.api, services.db, loaded.id, name)
                            saved = SavedItems.isAreaSaved(services.db, loaded.id)
                        }
                    }
                },
            ) {
                MaterialSymbol(
                    glyph = if (saved) "bookmark" else "bookmark_border",
                    contentDescription = labels.save,
                    tint = if (saved) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }

            if (bounds != null) {
                IconButton(
                    onClick = { showOfflineDialog = true },
                    enabled = packed !is OfflineAreaState.Downloading,
                ) {
                    MaterialSymbol(glyph = "download", contentDescription = labels.area.offlineMap)
                }
            }
        },
    ) {
        AreaScreen(
            description = loaded.getLocalizedDescription(),
            websiteText = if (showWebsite) websiteDisplayText(loaded.websiteUrl) else null,
            onOpenWebsite = if (showWebsite && loaded.websiteUrl.isNotBlank()) {
                { platform.openUrl(loaded.websiteUrl) }
            } else {
                null
            },
            boostedMerchants = boostedMerchants,
            events = events,
            issues = issues,
            offlineState = packed ?: OfflineAreaState.None,
            offlineStyleMatches = { downloaded ->
                services.offlineStyleMatches(offlineStyleUrl, downloaded)
            },
            strings = labels.area,
            onOpenPlace = platform::openPlace,
            onOpenEvent = onOpenEvent,
            onOpenIssue = { issue -> platform.openUrl(issue.osmEditUrl()) },
            onJoinUs = { platform.openUrl(JOIN_US_URL) },
            onDownload = { showOfflineDialog = true },
            onDelete = { showDeleteConfirm = true },
            headerImageUrl = loaded.iconWide ?: loaded.icon,
            offlineDialog = if (showOfflineDialog && bounds != null) {
                AreaOfflineDialog(
                    areaName = name,
                    styleName = labels.offlineStyleName,
                    bounds = bounds,
                )
            } else {
                null
            },
            onDismissOfflineDialog = { showOfflineDialog = false },
            onConfirmOfflineDownload = { maxZoom ->
                showOfflineDialog = false
                bounds?.let { region ->
                    services.offlinePacks.download(
                        areaId = loaded.id,
                        areaName = name,
                        bounds = region,
                        styleUrl = offlineStyleUrl,
                        maxZoom = maxZoom,
                    )
                }
            },
            boostedMarkerColor = markerPalette(services.settings).boostedMarkerBackground,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(labels.offlineDeleteTitle) },
            text = { Text(labels.offlineDeleteMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        services.offlinePacks.delete(loaded.id)
                    },
                ) {
                    Text(labels.area.offlineDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(labels.area.cancel)
                }
            },
        )
    }
}

/**
 * The area's bounding box as [OfflineBounds], or null when any side is missing:
 * an area without a bbox has no offline region to download.
 */
fun Area.offlineBounds(): OfflineBounds? {
    val west = bboxWest ?: return null
    val south = bboxSouth ?: return null
    val east = bboxEast ?: return null
    val north = bboxNorth ?: return null
    return OfflineBounds(west = west, south = south, east = east, north = north)
}
