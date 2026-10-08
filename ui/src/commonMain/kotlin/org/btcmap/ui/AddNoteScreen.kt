package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.btcmap.ui.map.LocationPickerMap
import org.btcmap.ui.map.LocationPickerPin
import org.btcmap.ui.map.MarkerPalette

/** The longest note body the API accepts, mirrored so the field cannot overflow it. */
const val MAX_NOTE_LENGTH = 2000

/** Test tags so a test can drive the add-note form. */
const val ADD_NOTE_TEXT_TAG = "add-note-text"
const val ADD_NOTE_PRIVATE_TAG = "add-note-private"
const val ADD_NOTE_PUBLIC_TAG = "add-note-public"
const val ADD_NOTE_SUBMIT_TAG = "add-note-submit"

/** Test tag prefix for an icon choice in the add-note picker, keyed by the icon. */
const val ADD_NOTE_ICON_TAG_PREFIX = "add-note-icon-"

/** The fields a new note is submitted with, positioned at the map centre. */
data class AddNoteDraft(
    val lat: Double,
    val lon: Double,
    val text: String,
    val icon: String,
    val public: Boolean,
)

/** The add-note screen's strings, so it stays resource-free. */
data class AddNoteLabels(
    val title: String,
    /** The back button's accessible name; the ligature is not announced. */
    val back: String,
    val text: String,
    val textPlaceholder: String,
    /** The label over the icon picker. */
    val icon: String,
    /** The private segment of the visibility control. */
    val private: String,
    /** The public segment of the visibility control. */
    val public: String,
    /** The caption under the visibility control while the note is private. */
    val privateDescription: String,
    /** The caption under the visibility control while the note is public. */
    val publicDescription: String,
    /** The helper caption over the positioning map. */
    val dragMap: String,
    val required: String,
    val submit: String,
    val submitted: String,
    val backToMap: String,
)

/**
 * The whole add-note screen: a Material 3 top app bar, a positioning map with
 * its drag hint, a note body, an icon picker and a public/private switch. The
 * location starts at the map centre the host handed over and can be adjusted by
 * panning under the pin, which previews the chosen icon. Notes are private unless
 * the switch is turned on.
 *
 * It mirrors [AddPlaceScreen] and [AddEventScreen]: the screen owns everything it
 * renders, the hosts cannot drift apart, [submit] is injected so it can be driven
 * without a server, and [map] so a test can render the form without a GPU.
 *
 * The body is capped at [CONTENT_MAX_WIDTH] and centred, so a wide desktop or
 * tablet window neither stretches the map and the fields edge to edge nor breaks
 * a phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    labels: AddNoteLabels,
    iconFont: FontFamily?,
    palette: MarkerPalette,
    submit: suspend (AddNoteDraft) -> Unit,
    onBack: () -> Unit,
    map: @Composable (icon: String, onCenterChanged: (Double, Double) -> Unit) -> Unit =
        { selectedIcon, onCenterChanged ->
            LocationPickerMap(
                lat = lat,
                lon = lon,
                styleUrl = styleUrl,
                styleJson = styleJson,
                palette = palette,
                onCenterChanged = onCenterChanged,
                pin = LocationPickerPin.Note,
                noteIcon = selectedIcon,
            )
        },
) {
    var center by remember { mutableStateOf(lat to lon) }
    var icon by rememberSaveable { mutableStateOf(DEFAULT_NOTE_ICON) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Submit failures surface through a snackbar; the field errors stay inline.
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            error = null
        }
    }

    AppTheme(iconFont = iconFont) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(labels.title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            MaterialSymbol(glyph = "arrow_back", contentDescription = labels.back)
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = CONTENT_MAX_WIDTH)
                        .fillMaxSize(),
                ) {
                    // The positioning map is a terminal confirmation's backdrop only
                    // while the form is still being filled in. It is inset to the
                    // form's margin and clipped to the theme's medium shape, so it
                    // reads as a contained M3 media block rather than a bare
                    // rectangle wider than the fields below it.
                    if (!submitted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                                .height(240.dp)
                                .clip(MaterialTheme.shapes.medium),
                        ) {
                            map(icon) { newLat, newLon -> center = newLat to newLon }
                            // The hint floats on the map so it reads as belonging to
                            // it, in the inverse-surface role M3 reserves for content
                            // over imagery.
                            Surface(
                                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                shape = RoundedCornerShape(percent = 50),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp),
                            ) {
                                Text(
                                    text = labels.dragMap,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                    AddNoteForm(
                        busy = busy,
                        submitted = submitted,
                        labels = labels,
                        icon = icon,
                        onIconChange = { icon = it },
                        onSubmit = { text, public ->
                            busy = true
                            val draft = AddNoteDraft(
                                lat = center.first,
                                lon = center.second,
                                text = text,
                                icon = icon,
                                public = public,
                            )
                            scope.launch {
                                try {
                                    submit(draft)
                                    submitted = true
                                } catch (t: Throwable) {
                                    error = t.message ?: t.toString()
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        onBack = onBack,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The add-note fields. The coordinates come from the map, not the form. Split
 * from the map so a test can drive the fields without a GPU.
 *
 * Pressing submit validates that the body is not blank and only then submits.
 * The body is capped at [MAX_NOTE_LENGTH] characters with a running counter, the
 * visibility is a private/public segmented control, and the primary action shows
 * a spinner while the submission is in flight.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddNoteForm(
    busy: Boolean,
    submitted: Boolean,
    labels: AddNoteLabels,
    icon: String,
    onIconChange: (String) -> Unit,
    onSubmit: (text: String, public: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var public by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val blank = attempted && text.isBlank()

    if (submitted) {
        AddNoteSubmitted(labels = labels, onBack = onBack, modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= MAX_NOTE_LENGTH) text = it },
            label = { Text(requiredLabel(labels.text)) },
            placeholder = { Text(labels.textPlaceholder) },
            isError = blank,
            supportingText = {
                when {
                    blank -> Text(labels.required)
                    text.isNotEmpty() -> Text("${text.length}/$MAX_NOTE_LENGTH")
                }
            },
            enabled = !busy,
            minLines = 4,
            maxLines = 8,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ADD_NOTE_TEXT_TAG),
        )
        // The pin glyph: a wrapping group of the icons the app offers, of which
        // exactly one is selected. The chosen icon is previewed on the map above
        // and stored with the note.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text(
                text = labels.icon,
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NOTE_ICONS.forEach { option ->
                    FilterChip(
                        selected = icon == option,
                        onClick = { onIconChange(option) },
                        enabled = !busy,
                        label = { MaterialSymbol(glyph = option, contentDescription = option) },
                        modifier = Modifier.testTag(ADD_NOTE_ICON_TAG_PREFIX + option),
                    )
                }
            }
        }
        // A two-segment control: the whole segment is the target, and the chosen
        // visibility is explained in the caption below.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !public,
                    onClick = { public = false },
                    enabled = !busy,
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag(ADD_NOTE_PRIVATE_TAG),
                ) {
                    Text(labels.private)
                }
                SegmentedButton(
                    selected = public,
                    onClick = { public = true },
                    enabled = !busy,
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag(ADD_NOTE_PUBLIC_TAG),
                ) {
                    Text(labels.public)
                }
            }
            Text(
                text = if (public) labels.publicDescription else labels.privateDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Button(
            onClick = {
                attempted = true
                if (text.isNotBlank()) {
                    focusManager.clearFocus()
                    onSubmit(text.trim(), public)
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .testTag(ADD_NOTE_SUBMIT_TAG),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(labels.submit)
            }
        }
    }
}

/** The confirmation shown once a note has been submitted. */
@Composable
private fun AddNoteSubmitted(
    labels: AddNoteLabels,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) {
            Text(labels.backToMap)
        }
    }
}

/**
 * Marks a required field's label. The screen's optional fields say so in their
 * own label, so a lone asterisk on the required ones keeps the two conventions
 * symmetric without a new translatable string.
 */
private fun requiredLabel(label: String): String = "$label *"
