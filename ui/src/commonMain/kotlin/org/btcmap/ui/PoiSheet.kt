package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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

/** Test tags for the OpenStreetMap feature sheet. */
const val POI_SHEET_NAME_TAG = "poi-sheet-name"
const val POI_SHEET_LOCAL_NAME_TAG = "poi-sheet-local-name"
const val POI_SHEET_CATEGORY_TAG = "poi-sheet-category"
const val POI_SHEET_COORDINATES_TAG = "poi-sheet-coordinates"
const val POI_SHEET_COPIED_TAG = "poi-sheet-copied"

/** The OpenStreetMap feature sheet's strings, so the map stays resource-free. */
data class PoiSheetLabels(val copied: String)

/**
 * A small sheet for a non-BTC-Map OpenStreetMap feature tapped on the basemap,
 * such as a bar or a hotel drawn by the style's POI layers.
 *
 * It is read-only for now: the name, its OSM category and the coordinates. The
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
                        .padding(top = 8.dp)
                        .testTag(POI_SHEET_CATEGORY_TAG),
                ) {
                    MaterialSymbol(
                        glyph = poi.categoryGlyph(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
        }
    }
}
