package org.btcmap.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.btcmap.search.SearchAdapterItem
import org.btcmap.ui.MaterialSymbol

/**
 * The map search field and its results, ported from the `SearchView` in
 * `map_fragment.xml`. The results are the local matches from
 * [rememberSearchResults].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SearchAdapterItem>,
    onResultClick: (SearchAdapterItem) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Places, events, areas",
    actions: SearchActions? = null,
    addLocationLabels: AddLocationLabels = AddLocationLabels(
        addPlace = "Add a place",
        addEvent = "Add an event",
        addNote = "Add a note",
    ),
    loading: Boolean = false,
    active: Boolean = false,
    emptyMessage: String = "No results",
    /** The boosted marker colour from settings, tinting boosted results. */
    boostedMarkerColor: Color? = null,
    /** The rows of the OpenStreetMap group, shown under the local [results]. */
    nominatimResults: List<SearchAdapterItem> = emptyList(),
    /** Whether the OpenStreetMap group is still being fetched. */
    nominatimLoading: Boolean = false,
    /** The heading above the OpenStreetMap group. */
    nominatimHeader: String = "OpenStreetMap",
    /**
     * The heading above the local group, shown only when the OpenStreetMap
     * group sits under it. The app's own name, so it is not translated.
     */
    localHeader: String = "BTC Map",
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    // On a large screen the place sheet's ModalBottomSheet keeps to this width
    // and centres itself, so the search field and its results do the same; the
    // host aligns this column to the top centre.
    Column(modifier = modifier.widthIn(max = BottomSheetDefaults.SheetMaxWidth)) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            interactionSource = interactionSource,
            leadingIcon = { MaterialSymbol(glyph = "search", contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // While focused the X cancels the search when there is no
                    // input, and clears the query when there is; it is also
                    // shown unfocused so a stale query can be cleared.
                    if (isFocused || query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (query.isNotEmpty()) {
                                    onQueryChange("")
                                } else {
                                    focusManager.clearFocus()
                                }
                            },
                        ) {
                            MaterialSymbol(glyph = "close", contentDescription = null)
                        }
                    }
                    // The map's actions live inside the field, as the SearchBar's
                    // menu did, rather than beside it. Hide them while the field
                    // is focused or a search is showing, so the query and its
                    // results have room; de-focusing with a live query must not
                    // bring them back over the results.
                    if (!isFocused && !active) {
                        actions?.let { actions ->
                            AddLocationAction(
                                onAddPlace = actions.onAddPlace,
                                onAddEvent = actions.onAddEvent,
                                onAddNote = actions.onAddNote,
                                labels = addLocationLabels,
                            )
                        }
                        actions?.onSettings?.let { onSettings ->
                            IconButton(onClick = onSettings) {
                                MaterialSymbol(glyph = "settings", contentDescription = null)
                            }
                        }
                    }
                }
            },
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(28.dp),
            // Material 3's search bar is a filled container with no outline or
            // indicator; the field floats over the map, so it also needs an
            // opaque surface behind it.
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // The panel stays up for the whole search: a spinner while it runs, the
        // matches when there are any, and a message when there are none. It is
        // only absent while the query is too short to have started a search, so
        // something is always shown once a search is under way.
        if (active) {
            Spacer(modifier = Modifier.size(8.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val hasResults = results.isNotEmpty() || nominatimResults.isNotEmpty()
                when {
                    // The local search runs first and is quick; until it has
                    // anything, only the spinner is worth showing.
                    loading && !hasResults -> CenteredRow {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }

                    // "No results" waits for the OpenStreetMap group too: a
                    // local miss with that group still loading is not final.
                    !hasResults && !nominatimLoading -> CenteredRow {
                        Text(
                            text = emptyMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> LazyColumn(modifier = Modifier.heightIn(max = SEARCH_RESULTS_MAX_HEIGHT)) {
                        // The local hits get their own heading only when the
                        // OpenStreetMap group sits under them, so a search with
                        // a single group stays unlabelled as before.
                        if (results.isNotEmpty() && nominatimResults.isNotEmpty()) {
                            item(key = LOCAL_HEADER_KEY) { SearchSectionHeader(localHeader) }
                        }
                        items(results) { result ->
                            SearchResultRow(
                                result = result,
                                boostedMarkerColor = boostedMarkerColor,
                                onClick = onResultClick,
                            )
                        }
                        if (nominatimResults.isNotEmpty()) {
                            item(key = NOMINATIM_HEADER_KEY) {
                                SearchSectionHeader(nominatimHeader)
                            }
                            items(nominatimResults) { result ->
                                SearchResultRow(
                                    result = result,
                                    boostedMarkerColor = boostedMarkerColor,
                                    onClick = onResultClick,
                                )
                            }
                        } else if (nominatimLoading) {
                            item(key = NOMINATIM_LOADING_KEY) { SearchGroupLoading() }
                        }
                    }
                }
            }
        }
    }
}

/** The height of one result row, so the spinner and message match the list. */
private val SEARCH_RESULT_ROW_HEIGHT = 56.dp

/** A one-row-high, centred container for the panel's spinner or message. */
@Composable
private fun CenteredRow(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(SEARCH_RESULT_ROW_HEIGHT),
    ) {
        content()
    }
}

/** The heading that separates the OpenStreetMap group from the local results. */
@Composable
private fun SearchSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/** A short, centred spinner shown in place of a group that is still loading. */
@Composable
private fun SearchGroupLoading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(SEARCH_GROUP_LOADING_HEIGHT),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
    }
}

/** Stable keys for the groups' header and placeholder rows. */
private const val LOCAL_HEADER_KEY = "local-header"
private const val NOMINATIM_HEADER_KEY = "nominatim-header"
private const val NOMINATIM_LOADING_KEY = "nominatim-loading"

/**
 * How tall the results panel may grow before it scrolls. Enough for the local
 * hits plus the OpenStreetMap group's heading and first rows to be visible
 * together, without covering the whole map.
 */
private val SEARCH_RESULTS_MAX_HEIGHT = 480.dp

/** The height of the placeholder shown while the OpenStreetMap group loads. */
private val SEARCH_GROUP_LOADING_HEIGHT = 48.dp

@Composable
private fun SearchResultRow(
    result: SearchAdapterItem,
    boostedMarkerColor: Color?,
    onClick: (SearchAdapterItem) -> Unit,
) {
    val boosted = (result as? SearchAdapterItem.Place)?.boosted == true
    val areaIconUrl = (result as? SearchAdapterItem.Area)?.iconUrl
    // A boosted row wears the boosted marker colour from settings, on both the
    // glyph and the name, matching its pin.
    val boostedColor = boostedMarkerColor ?: MaterialTheme.colorScheme.primary

    ListItem(
        headlineContent = {
            Text(
                text = result.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (boosted) boostedColor else MaterialTheme.colorScheme.onSurface,
            )
        },
        leadingContent = {
            if (areaIconUrl != null) {
                AsyncImage(
                    model = areaIconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                )
            } else {
                MaterialSymbol(
                    glyph = result.icon,
                    contentDescription = null,
                    tint = if (boosted) boostedColor else LocalContentColor.current,
                )
            }
        },
        trailingContent = {
            result.distanceToUser?.let { distance ->
                Text(text = distance, style = MaterialTheme.typography.bodySmall)
            }
        },
        // The row shows the enclosing Surface's tonal colour rather than the
        // list item's own surface, so the panel stays one colour.
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick(result) },
    )
}

/**
 * The add-location action: a single icon that acts directly when only one kind
 * of location is offered, or opens a chooser between the place, event and note
 * kinds when more than one is.
 */
@Composable
private fun AddLocationAction(
    onAddPlace: (() -> Unit)?,
    onAddEvent: (() -> Unit)?,
    onAddNote: (() -> Unit)?,
    labels: AddLocationLabels,
) {
    val items = buildList {
        onAddPlace?.let { add(AddLocationItem(labels.addPlace, "place", it)) }
        onAddEvent?.let { add(AddLocationItem(labels.addEvent, "event", it)) }
        onAddNote?.let { add(AddLocationItem(labels.addNote, "notes", it)) }
    }
    if (items.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = {
                if (items.size == 1) items.single().onClick() else expanded = true
            },
        ) {
            MaterialSymbol(glyph = "add_location_alt", contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    leadingIcon = { MaterialSymbol(glyph = item.glyph, contentDescription = null) },
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                )
            }
        }
    }
}

/** One row of the add-location chooser: its label, its leading icon and its action. */
private data class AddLocationItem(
    val label: String,
    val glyph: String,
    val onClick: () -> Unit,
)
