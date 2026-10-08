package org.btcmap.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.btcmap.place.MAX_REPORT_PHOTOS
import org.btcmap.place.ReportDraft
import org.btcmap.place.ReportType

/** Test tags for the report form's controls. */
internal const val REPORT_ADD_PHOTO_TAG = "report-add-photo"
internal const val REPORT_PHOTO_TAG_PREFIX = "report-photo-"
internal const val REPORT_PHOTO_REMOVE_TAG_PREFIX = "report-photo-remove-"
internal const val REPORT_NOTE_TAG = "report-note"
internal const val REPORT_SUBMIT_TAG = "report-submit"

/** The report form's strings, so the screen stays resource-free. */
data class ReportPlaceLabels(
    val intro: String,
    val reasonLabel: (ReportType) -> String,
    val reasonDescription: (ReportType) -> String,
    val noteHint: String,
    val addPhoto: (taken: Int, max: Int) -> String,
    val removePhoto: String,
    val submit: String,
    val submitted: String,
    val backToMap: String,
)

/**
 * The report form for a place: the three reasons the API accepts, an optional
 * note, up to [MAX_REPORT_PHOTOS] evidence photos, and the injected [submit].
 *
 * [pickPhotos] is supplied by the host (a file picker on the desktop, a photo
 * picker on Android) and returns already-encoded photos. The host supplies its
 * own top bar and back affordance.
 *
 * The body is a [ContentColumn], so a tablet or desktop window centres the form
 * at a readable width instead of stretching it edge to edge.
 */
@Composable
fun ReportPlaceScreen(
    initialType: String?,
    labels: ReportPlaceLabels,
    submit: suspend (ReportDraft) -> Unit,
    onBack: () -> Unit,
    pickPhotos: suspend () -> List<ByteArray> = { emptyList() },
    // Hoisted so a host that survives a configuration change (the Android
    // ViewModel) can keep the attached photos across it.
    photos: SnapshotStateList<ByteArray> = remember { mutableStateListOf() },
) {
    var type by remember { mutableStateOf(ReportType.fromValue(initialType)) }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // The confirmation is a short, centred body rather than a form field, so it
    // is centred vertically in the available space.
    if (submitted) {
        ContentColumn(centerVertically = true, margin = true) {
            ReportSubmitted(labels = labels, onBack = onBack)
        }
        return
    }

    ContentColumn(
        modifier = Modifier.padding(vertical = 16.dp),
        scroll = true,
        margin = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = labels.intro,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The reasons are a single-choice group: the whole row is one target,
        // with the label and description part of its semantics, so tapping the
        // text selects the option and a screen reader announces it with it.
        Column(modifier = Modifier.selectableGroup()) {
            ReportType.entries.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = type == option,
                            enabled = !busy,
                            role = Role.RadioButton,
                            onClick = { type = option },
                        )
                        .padding(vertical = 8.dp),
                ) {
                    RadioButton(selected = type == option, onClick = null, enabled = !busy)
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = labels.reasonLabel(option))
                        Text(
                            text = labels.reasonDescription(option),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(text = labels.noteHint) },
            enabled = !busy,
            minLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(REPORT_NOTE_TAG),
        )

        OutlinedButton(
            enabled = !busy && photos.size < MAX_REPORT_PHOTOS,
            onClick = {
                scope.launch {
                    val remaining = MAX_REPORT_PHOTOS - photos.size
                    if (remaining > 0) photos += pickPhotos().take(remaining)
                }
            },
            modifier = Modifier.testTag(REPORT_ADD_PHOTO_TAG),
        ) {
            Text(text = labels.addPhoto(photos.size, MAX_REPORT_PHOTOS))
        }

        if (photos.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                photos.forEachIndexed { index, bytes ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .testTag(REPORT_PHOTO_TAG_PREFIX + index),
                    ) {
                        Image(
                            bitmap = remember(bytes) { decodeImageBitmap(bytes) },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        // The button keeps its 48dp touch target; the smaller
                        // scrimmed glyph sits centred inside it for contrast.
                        IconButton(
                            onClick = { photos.removeAt(index) },
                            enabled = !busy,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .testTag(REPORT_PHOTO_REMOVE_TAG_PREFIX + index),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                MaterialSymbol(
                                    glyph = "close",
                                    contentDescription = labels.removePhoto,
                                    size = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            enabled = type != null && !busy,
            onClick = {
                val reason = type ?: return@Button
                busy = true
                error = null
                scope.launch {
                    try {
                        submit(ReportDraft(type = reason, note = note, photos = photos.toList()))
                        submitted = true
                    } catch (t: Throwable) {
                        error = t.message ?: t.toString()
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(REPORT_SUBMIT_TAG),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(text = labels.submit)
            }
        }
    }
}

/** The centred confirmation shown once a report has been submitted. */
@Composable
private fun ReportSubmitted(
    labels: ReportPlaceLabels,
    onBack: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        MaterialSymbol(
            glyph = "check_circle",
            contentDescription = null,
            size = 48.sp,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = labels.submitted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(text = labels.backToMap)
        }
    }
}
