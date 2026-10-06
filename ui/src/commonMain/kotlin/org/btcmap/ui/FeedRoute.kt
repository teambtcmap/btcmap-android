package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.api.ActivityFeedItem
import org.btcmap.api.getActivity
import org.btcmap.settings.ActivityInterval
import org.btcmap.settings.activityIntervalDays
import org.btcmap.settings.authorized

/**
 * The activity feed route: a Local and a Saved tab over the shared
 * [ActivityFeedPage], with the area/interval filter in the top bar.
 *
 * The Local tab queries the API for the areas around the map; the Saved tab
 * reads the signed-in user's saved areas and places from the local cache. The
 * page is keyed on the tab so switching reloads it, and the filter bumps
 * [reloadKey] in place.
 */
@Composable
internal fun FeedRoute(
    services: AppServices,
    platform: AppPlatform,
    labels: AppLabels,
    route: AppRoute.Feed,
    onBack: () -> Unit,
    onOpenPlace: (Long) -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var reloadKey by remember { mutableStateOf(0) }
    var showFilter by remember { mutableStateOf(false) }
    var selected by remember {
        mutableStateOf(
            route.areaIds.indices
                .filter { route.areaTypes.getOrNull(it) != "country" }
                .ifEmpty { route.areaIds.indices.toList() }
                .toSet(),
        )
    }
    var savedItemsEmpty by remember { mutableStateOf(false) }

    val load: suspend () -> List<ActivityFeedItem> = when (tab) {
        0 -> {
            {
                val ids = selected.toList().map { route.areaIds[it] }
                if (ids.isEmpty()) {
                    emptyList()
                } else {
                    services.api.getActivity(
                        areaIds = ids,
                        days = services.settings.activityIntervalDays,
                    )
                }
            }
        }

        else -> {
            {
                if (!services.settings.authorized) {
                    emptyList()
                } else {
                    val user = withContext(Dispatchers.IO) { services.db.user.select() }
                    savedItemsEmpty = user == null ||
                        (user.savedAreas.isEmpty() && user.savedPlaces.isEmpty())
                    if (user == null) {
                        emptyList()
                    } else {
                        services.api.getActivity(
                            areaIds = user.savedAreas.map { it.id.toString() },
                            placeIds = user.savedPlaces.map { it.id.toString() },
                            days = services.settings.activityIntervalDays,
                        )
                    }
                }
            }
        }
    }

    val emptyMessage: () -> String = when (tab) {
        0 -> {
            { labels.feedEmptyLocal }
        }

        else -> {
            {
                when {
                    !services.settings.authorized -> labels.feedEmptySavedSignedOut
                    savedItemsEmpty -> labels.feedEmptySavedNoItems
                    else -> labels.feedEmptySavedNoActivity
                }
            }
        }
    }

    ScreenPage(
        title = labels.feedTitle,
        onBack = onBack,
        backContentDescription = labels.back,
        actions = {
            IconButton(onClick = { showFilter = true }) {
                MaterialSymbol(glyph = "filter_list", contentDescription = labels.feedFilter)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(labels.feedLocalTab) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(labels.feedSavedTab) },
                )
            }

            key(tab) {
                ActivityFeedPage(
                    load = load,
                    toRow = labels.feedRow,
                    emptyMessage = emptyMessage,
                    errorMessage = labels.feedError,
                    onItemClick = { item ->
                        if (item.type == ActivityFeedItem.TYPE_PLACE_DELETED) {
                            platform.openFeedItem(item)
                        } else {
                            onOpenPlace(item.placeId)
                        }
                    },
                    reloadKey = reloadKey,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (showFilter) {
        AlertDialog(
            onDismissRequest = { showFilter = false },
            title = { Text(labels.feedFilter) },
            text = {
                ChipFilterContent(
                    areasLabel = labels.feedAreasLabel,
                    areaOptions = if (tab == 0) {
                        route.areaIds.indices.map { index ->
                            ChipOption(
                                key = route.areaIds[index],
                                label = route.areaNames.getOrNull(index) ?: route.areaIds[index],
                                selected = selected.contains(index),
                            )
                        }
                    } else {
                        emptyList()
                    },
                    intervalLabel = labels.feedIntervalLabel,
                    intervalOptions = ActivityInterval.entries.map { interval ->
                        ChipOption(
                            key = interval.days.toString(),
                            label = labels.feedIntervalName(interval),
                            selected = interval.days == services.settings.activityIntervalDays,
                        )
                    },
                    onAreaToggle = { key ->
                        val index = route.areaIds.indexOf(key)
                        if (index >= 0) {
                            selected = if (selected.contains(index)) {
                                selected - index
                            } else {
                                selected + index
                            }
                            reloadKey++
                        }
                    },
                    onIntervalSelect = { key ->
                        key.toIntOrNull()?.let { days ->
                            if (services.settings.activityIntervalDays != days) {
                                services.settings.activityIntervalDays = days
                                reloadKey++
                            }
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { showFilter = false }) {
                    Text(labels.ok)
                }
            },
        )
    }
}
