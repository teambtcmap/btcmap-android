package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.btcmap.db.table.area.Area
import org.btcmap.i18n.getLocalizedName
import org.btcmap.ui.map.areaIcon
import org.btcmap.util.rethrowIfCancellation

/** The test tag of the manage-areas search field. */
const val MANAGE_AREAS_SEARCH_TAG = "manage-areas-search"

/**
 * The label for the world area, which the API carries with an empty name. It is
 * shown (and sorted, and searched) as "Earth" so the row is not blank and does
 * not float to the top of the list.
 */
const val EARTH_NAME = "Earth"

/** The only area type the screen lists for an unrestricted account. */
const val COMMUNITY_AREA_TYPE = "community"

/** The manage-areas screen's strings, so the screen stays resource-free. */
data class ManageAreasLabels(
    val search: String,
    val clear: String,
    val empty: String,
    val noMatches: String,
    val failed: String,
    val retry: String,
    val notVerified: String,
)

/**
 * The "manage areas" screen: every community area in the local cache (countries
 * and cities are not listed), ordered so the ones that need attention come
 * first — never-verified areas (which wear a warning) ahead of the oldest
 * verification dates. The search field filters the list to the names that
 * contain the query, so an area manager can find one area among the
 * thousand-odd. Only users holding an area-manager role reach it (the settings
 * row is hidden for everyone else). The list is loaded through [load] so the
 * host owns the cache read and the screen stays testable.
 *
 * When [geofence] is non-empty the account may only edit those area ids (the
 * server rejects any other area), so the search field is hidden and the list
 * shows just the geofenced areas — of any type, since a geofence can name a
 * country the community-only list would otherwise drop.
 *
 * The body is a single column capped at [CONTENT_MAX_WIDTH] and centred, so a
 * desktop or tablet window does not stretch the rows and the search field edge
 * to edge. The search field stays pinned above the scrolling list, and the
 * loading, empty, no-match and failure states render as one centred state.
 *
 * The screen carries no title of its own: the host shows "Manage areas" in its
 * own bar, and its back affordance leaves this screen.
 */
@Composable
fun ManageAreasScreen(
    labels: ManageAreasLabels,
    load: suspend () -> List<Area>,
    onAreaClick: (Area) -> Unit = {},
    modifier: Modifier = Modifier,
    /**
     * The area ids the account is restricted to, or empty when unrestricted.
     * Non-empty hides the search and lists only these areas.
     */
    geofence: List<Long> = emptyList(),
) {
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var areas by remember { mutableStateOf(emptyList<Area>()) }
    var reloadKey by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }

    val restricted = geofence.isNotEmpty()

    LaunchedEffect(reloadKey) {
        failed = false
        try {
            // Only communities are managed here; countries and cities are
            // synced for the map but not editable area records — unless the
            // account is geofenced, which names the exact areas it may edit.
            //
            // Unverified areas first (they need attention), then the oldest
            // verification dates, so the stale ones surface at the top. The
            // name is a stable tiebreaker. `verified_at` is an ISO date, so a
            // plain string compare is chronological.
            areas = load()
                .filter { if (restricted) it.id in geofence else it.type == COMMUNITY_AREA_TYPE }
                .sortedWith(
                    compareBy<Area> { it.verifiedAt != null }
                        .thenBy { it.verifiedAt.orEmpty() }
                        .thenBy { it.displayName().lowercase() },
                )
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            failed = true
        } finally {
            loaded = true
        }
    }

    val matches = remember(areas, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            areas
        } else {
            areas.filter { it.displayName().contains(trimmed, ignoreCase = true) }
        }
    }

    ContentColumn(modifier = modifier) {
        when {
            !loaded -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                CircularProgressIndicator()
            }

            failed -> ManageAreasStateMessage(
                icon = "error",
                text = labels.failed,
                modifier = Modifier.weight(1f),
                action = {
                    TextButton(onClick = { reloadKey++ }) { Text(labels.retry) }
                },
            )

            else -> {
                // A geofenced account can only reach its own areas, so the
                // search would never narrow anything.
                if (!restricted) {
                    ManageAreasSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = labels.search,
                        clearDescription = labels.clear,
                    )
                }
                when {
                    areas.isEmpty() -> ManageAreasStateMessage(
                        icon = "travel_explore",
                        text = labels.empty,
                        modifier = Modifier.weight(1f),
                    )

                    matches.isEmpty() -> ManageAreasStateMessage(
                        icon = "search_off",
                        text = labels.noMatches,
                        modifier = Modifier.weight(1f),
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        items(matches, key = { it.id }) { area ->
                            ListItem(
                                headlineContent = { Text(area.displayName()) },
                                supportingContent = {
                                    Text(area.type.replaceFirstChar { it.uppercase() })
                                },
                                leadingContent = { ManageAreaIcon(area) },
                                trailingContent = {
                                    ManageAreaVerification(
                                        verifiedAt = area.verifiedAt,
                                        notVerifiedDescription = labels.notVerified,
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                // The name, type and verification are announced
                                // as one row, not three separate stops; tapping
                                // the row opens the area's admin detail.
                                modifier = Modifier
                                    .clickable { onAreaClick(area) }
                                    .semantics(mergeDescendants = true) {},
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The area's name for the list, with the nameless world area labelled [EARTH_NAME]. */
private fun Area.displayName(): String = getLocalizedName().ifBlank { EARTH_NAME }

/**
 * One centred state: a symbol over a message and, for a failure, a retry action.
 * Used for loading-adjacent states so the screen never shows bare top-left text.
 */
@Composable
private fun ManageAreasStateMessage(
    icon: String,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(24.dp),
    ) {
        MaterialSymbol(
            glyph = icon,
            contentDescription = null,
            size = 48.sp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.let {
            Spacer(modifier = Modifier.height(8.dp))
            it()
        }
    }
}

/**
 * A row's leading icon: the area's cached image when it has one, over the glyph
 * for its type. The glyph sits behind the image (as the map's area chips do), so
 * a row keeps an icon while the image loads or when it cannot.
 */
@Composable
private fun ManageAreaIcon(area: Area) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(MANAGE_AREA_ICON_SIZE),
    ) {
        MaterialSymbol(glyph = areaIcon(area.type), contentDescription = null)
        area.icon?.let { icon ->
            AsyncImage(
                model = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(MANAGE_AREA_ICON_SIZE)
                    .clip(CircleShape),
            )
        }
    }
}

private val MANAGE_AREA_ICON_SIZE = 40.dp

/**
 * A row's trailing verification state: the last verification date with a check,
 * or a red warning when the area has never been verified.
 */
@Composable
private fun ManageAreaVerification(
    verifiedAt: String?,
    notVerifiedDescription: String,
) {
    if (verifiedAt == null) {
        MaterialSymbol(
            glyph = "warning",
            contentDescription = notVerifiedDescription,
            tint = MaterialTheme.colorScheme.error,
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MaterialSymbol(
                glyph = "verified",
                contentDescription = null,
                size = 18.sp,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = verifiedAt,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The rounded search field pinned above the list, matching the map's field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageAreasSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
) {
    val focusManager = LocalFocusManager.current

    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        leadingIcon = { MaterialSymbol(glyph = "search", contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    MaterialSymbol(glyph = "close", contentDescription = clearDescription)
                }
            }
        },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(MANAGE_AREAS_SEARCH_TAG),
    )
}
