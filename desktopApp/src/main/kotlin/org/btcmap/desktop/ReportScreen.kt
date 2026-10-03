package org.btcmap.desktop

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.btcmap.api.Api
import org.btcmap.place.MAX_REPORT_PHOTOS
import org.btcmap.place.ReportDraft
import org.btcmap.place.ReportType
import org.btcmap.place.submitReport
import org.btcmap.ui.MaterialSymbol

/** Test tags for the report form's photo controls. */
internal const val REPORT_ADD_PHOTO_TAG = "report-add-photo"
internal const val REPORT_PHOTO_TAG_PREFIX = "report-photo-"
internal const val REPORT_PHOTO_REMOVE_TAG_PREFIX = "report-photo-remove-"

/** The desktop's label for a shared report reason. */
private fun ReportType.label(): String = when (this) {
    ReportType.Verified -> "Verified - still accepts Bitcoin"
    ReportType.RefusedSats -> "Refused Bitcoin payment"
    ReportType.OutOfBusiness -> "Out of business"
}

/** The desktop's description for a shared report reason. */
private fun ReportType.description(): String = when (this) {
    ReportType.Verified -> "You confirmed this place exists and currently accepts Bitcoin payments."
    ReportType.RefusedSats -> "An attempt to pay with Bitcoin on-site was refused by the merchant."
    ReportType.OutOfBusiness -> "The place has been permanently closed or is no longer operating."
}

/**
 * The desktop's report form for a place: the same three reasons the app offers,
 * an optional note, up to [MAX_REPORT_PHOTOS] evidence photos, and the shared
 * `reportPlace` call.
 *
 * [pickPhotos] is supplied by the host (a file picker on the desktop, empty in
 * the headless screenshot) and returns already-encoded photos.
 */
@Composable
internal fun DesktopReportScreen(
    api: Api,
    placeId: Long,
    placeName: String,
    initialType: String?,
    onBack: () -> Unit,
    pickPhotos: suspend () -> List<ByteArray> = { emptyList() },
) {
    var type by remember { mutableStateOf(ReportType.fromValue(initialType)) }
    var note by remember { mutableStateOf("") }
    val photos = remember { mutableStateListOf<ByteArray>() }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ScreenPage(title = placeName.ifBlank { "Report a place" }, onBack = onBack) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (submitted) {
                Text(text = "Report submitted.")
                Button(onClick = onBack) { Text(text = "Back to the map") }
                return@Column
            }

            Text(
                text = "Let editors know the current state of this place. Your " +
                    "report will be reviewed by the BTC Map community.",
                style = MaterialTheme.typography.bodyMedium,
            )

            ReportType.entries.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == option, onClick = { type = option })
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(text = option.label())
                        Text(
                            text = option.description(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(text = "Additional notes (optional)") },
                modifier = Modifier.padding(top = 8.dp),
            )

            OutlinedButton(
                enabled = !busy && photos.size < MAX_REPORT_PHOTOS,
                onClick = {
                    scope.launch {
                        val remaining = MAX_REPORT_PHOTOS - photos.size
                        if (remaining > 0) photos += pickPhotos().take(remaining)
                    }
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag(REPORT_ADD_PHOTO_TAG),
            ) {
                Text(text = "Add photo (${photos.size}/$MAX_REPORT_PHOTOS)")
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
                                bitmap = remember(bytes) { loadImageBitmap(bytes.inputStream()) },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            IconButton(
                                onClick = { photos.removeAt(index) },
                                enabled = !busy,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .testTag(REPORT_PHOTO_REMOVE_TAG_PREFIX + index),
                            ) {
                                MaterialSymbol(glyph = "close", contentDescription = "Remove photo")
                            }
                        }
                    }
                }
            }

            error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            Button(
                enabled = type != null && !busy,
                onClick = {
                    val reason = type ?: return@Button
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            api.submitReport(
                                placeId = placeId,
                                draft = ReportDraft(type = reason, note = note, photos = photos.toList()),
                            )
                            submitted = true
                        } catch (t: Throwable) {
                            error = t.message ?: t.toString()
                        } finally {
                            busy = false
                        }
                    }
                },
            ) {
                Text(text = "Submit")
            }
        }
    }
}
