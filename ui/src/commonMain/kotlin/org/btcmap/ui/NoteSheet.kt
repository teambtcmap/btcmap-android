package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant

/** Test tag for the note sheet's body text. */
const val NOTE_SHEET_TEXT_TAG = "note-sheet-text"

/** Test tag for the note sheet's visibility row. */
const val NOTE_SHEET_VISIBILITY_TAG = "note-sheet-visibility"

/** Test tag for the note sheet's creation row. */
const val NOTE_SHEET_CREATED_TAG = "note-sheet-created"

/** The note sheet's strings, so the map stays resource-free. */
data class NoteSheetLabels(
    val title: String,
    /** The visibility row's value while the note is public. */
    val public: String,
    /** The visibility row's value while the note is private. */
    val private: String,
    /** Formats the note's creation date, e.g. "Created Jan 1, 2025". */
    val created: (String) -> String,
)

/**
 * The note details shown when a note pin is selected, the map's note sheet: the
 * pin's glyph and title, the note's body, and its visibility and creation date.
 *
 * It mirrors the place and event sheets, so a tapped note reads as a panel over
 * the map (rather than the modal dialog it used to be) and the map stays visible
 * behind it. [icon] is the glyph the pin carries, echoed in the header so the
 * sheet matches the pin that opened it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSheet(
    text: String,
    icon: String,
    public: Boolean,
    createdAt: Instant,
    labels: NoteSheetLabels,
    onDismiss: () -> Unit,
) {
    // Open at the half-expanded height the place and event sheets use, so the
    // map stays visible behind it; the user can drag it up to full screen.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

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
                .padding(bottom = 32.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
            ) {
                MaterialSymbol(
                    glyph = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = labels.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag(NOTE_SHEET_TEXT_TAG),
            )

            NoteInfoRow(
                glyph = if (public) "public" else "lock",
                text = if (public) labels.public else labels.private,
                modifier = Modifier.testTag(NOTE_SHEET_VISIBILITY_TAG),
            )
            NoteInfoRow(
                glyph = "schedule",
                text = labels.created(createdAt.format(dateFormatter)),
                modifier = Modifier.testTag(NOTE_SHEET_CREATED_TAG),
            )
        }
    }
}

/** A glyph followed by a note detail, laid out like the place sheet's info rows. */
@Composable
private fun NoteInfoRow(
    glyph: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        MaterialSymbol(
            glyph = glyph,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
