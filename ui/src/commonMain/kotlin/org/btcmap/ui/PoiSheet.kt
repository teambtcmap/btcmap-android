package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.btcmap.ui.map.PoiInfo
import org.btcmap.ui.map.categoryGlyph
import org.btcmap.ui.map.categoryLabel
import org.btcmap.ui.map.coordinatesLabel
import org.btcmap.ui.map.osmEditUrl
import org.btcmap.ui.map.osmUrl

/** Test tags for the OpenStreetMap feature sheet. */
const val POI_SHEET_NAME_TAG = "poi-sheet-name"
const val POI_SHEET_LOCAL_NAME_TAG = "poi-sheet-local-name"
const val POI_SHEET_CATEGORY_TAG = "poi-sheet-category"
const val POI_SHEET_COORDINATES_TAG = "poi-sheet-coordinates"
const val POI_SHEET_COPIED_TAG = "poi-sheet-copied"
const val POI_SHEET_ADD_MERCHANT_TAG = "poi-sheet-add-merchant"
const val POI_SHEET_ADD_NOTE_TAG = "poi-sheet-add-note"
const val POI_SHEET_VIEW_OSM_TAG = "poi-sheet-view-osm"
const val POI_SHEET_EDIT_OSM_TAG = "poi-sheet-edit-osm"

/** The OpenStreetMap feature sheet's strings, so the map stays resource-free. */
data class PoiSheetLabels(
    val copied: String,
    /** The action that adds the feature as a Bitcoin-accepting place. */
    val addAsMerchant: String,
    /** The action that adds the feature as a note. */
    val addAsNote: String,
    /** The action that opens the feature's position on openstreetmap.org. */
    val viewOnOsm: String,
    /** The action that opens the iD editor at the feature's position. */
    val editOnOsm: String,
)

/**
 * A small sheet for a non-BTC-Map OpenStreetMap feature tapped on the basemap,
 * such as a bar or a hotel drawn by the style's POI layers.
 *
 * It shows the name, its OSM category and the coordinates, and offers shortcuts
 * into the app's own flows: adding the feature as a Bitcoin-accepting place or as
 * a note, each pre-filled with the feature's name, category and position, for a
 * place the basemap draws but BTC Map does not know yet. It also links to the
 * feature's position on openstreetmap.org and to the iD editor there; the tiles
 * carry no OSM element id, so those point at the coordinates rather than at the
 * element. Each action is hidden when the host does not offer it (or the user is
 * not signed in, which the host handles by opening its auth prompt instead). The
 * category glyph echoes the kind of place, so a tapped bar reads at a glance.
 *
 * Tapping either name copies it, as the plain text — ruby pinyin is not part of
 * the copied value — and flashes a confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun PoiSheet(
    poi: PoiInfo,
    labels: PoiSheetLabels,
    onDismiss: () -> Unit,
    /** Adds the feature as a place; null hides the action. */
    onAddAsMerchant: (() -> Unit)? = null,
    /** Adds the feature as a note; null hides the action. */
    onAddAsNote: (() -> Unit)? = null,
    /**
     * Opens a URL, used for the feature's openstreetmap.org links; null hides
     * them, as when the host has no browser.
     */
    onOpenUrl: ((String) -> Unit)? = null,
    /** Overrides how a tapped name is copied; tests inject a capture. */
    onCopy: ((String) -> Unit)? = null,
) {
    // The sheet holds only a few rows, so it opens straight to its content
    // height rather than at the half-expanded height the taller sheets use.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val copy: (String) -> Unit = { text ->
        if (onCopy != null) onCopy(text) else clipboard.setText(AnnotatedString(text))
        copied = true
    }

    // The confirmation is transient: show it, then drop it after a moment.
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // The map is the context the sheet is about, so keep it fully visible.
        scrimColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .testTag(POI_SHEET_NAME_TAG)
                    .clickable { copy(poi.name) },
            ) {
                RubyText(
                    text = poi.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = LocalContentColor.current,
                    rubyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    rubySize = 12.sp,
                )
            }
            poi.localName?.let { localName ->
                RubyText(
                    text = localName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    rubyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    rubySize = 9.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(POI_SHEET_LOCAL_NAME_TAG)
                        .clickable { copy(localName) },
                    leading = {
                        MaterialSymbol(
                            glyph = "translate",
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    },
                )
            }
            if (copied) {
                Text(
                    text = labels.copied,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag(POI_SHEET_COPIED_TAG),
                )
            }
            poi.categoryLabel()?.let { category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        // A touch more than the coordinates row below: the local
                        // name above centres its icon on the character line (its
                        // pinyin hangs above), so an equal padding would leave
                        // this icon closer to the one above than the one below.
                        .padding(top = 14.dp)
                        .testTag(POI_SHEET_CATEGORY_TAG),
                ) {
                    MaterialSymbol(
                        glyph = poi.categoryGlyph(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag(POI_SHEET_COORDINATES_TAG),
            ) {
                MaterialSymbol(
                    glyph = "location_on",
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = poi.coordinatesLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val hasAddActions = onAddAsMerchant != null || onAddAsNote != null
            if (hasAddActions) {
                // The two conversion actions share one row and match heights, so
                // a wrapped label on a narrow window does not stagger them.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(IntrinsicSize.Min),
                ) {
                    onAddAsMerchant?.let { add ->
                        Button(
                            onClick = add,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag(POI_SHEET_ADD_MERCHANT_TAG),
                        ) {
                            MaterialSymbol(glyph = "add_business", contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = labels.addAsMerchant)
                        }
                    }
                    onAddAsNote?.let { add ->
                        OutlinedButton(
                            onClick = add,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag(POI_SHEET_ADD_NOTE_TAG),
                        ) {
                            MaterialSymbol(glyph = "note_add", contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = labels.addAsNote)
                        }
                    }
                }
            }
            onOpenUrl?.let { open ->
                // The links sit on the row below the conversions, laid out inline
                // rather than in a dropdown: the popup would be positioned against
                // the window on the desktop, not the sheet (the same reason
                // PlaceSheet lays its overflow out inline).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    TextButton(
                        onClick = { open(poi.osmUrl()) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(POI_SHEET_VIEW_OSM_TAG),
                    ) {
                        Text(text = labels.viewOnOsm)
                    }
                    TextButton(
                        onClick = { open(poi.osmEditUrl()) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(POI_SHEET_EDIT_OSM_TAG),
                    ) {
                        Text(text = labels.editOnOsm)
                    }
                }
            }
        }
    }
}
