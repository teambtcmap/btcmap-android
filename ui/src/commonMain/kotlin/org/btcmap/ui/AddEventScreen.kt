package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.btcmap.search.NominatimSearch
import org.btcmap.ui.map.LocationMove
import org.btcmap.ui.map.LocationPickerMap
import org.btcmap.ui.map.LocationPickerPin
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.util.rethrowIfCancellation
import java.time.Instant as JavaInstant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Test tags so a test can drive the add-event form. */
const val ADD_EVENT_NAME_TAG = "add-event-name"
const val ADD_EVENT_OSM_ID_TAG = "add-event-osm-id"
const val ADD_EVENT_WEBSITE_TAG = "add-event-website"
const val ADD_EVENT_STARTS_TAG = "add-event-starts"
const val ADD_EVENT_ENDS_TAG = "add-event-ends"
const val ADD_EVENT_END_CLEAR_TAG = "add-event-end-clear"
const val ADD_EVENT_SUBMIT_TAG = "add-event-submit"

/** The shortest query the OpenStreetMap ID field sends to the search service. */
private const val OSM_SEARCH_MIN_CHARS = 3

/** How long typing settles before the OpenStreetMap ID field searches. */
private const val OSM_SEARCH_DEBOUNCE_MS = 300L

/**
 * An OpenStreetMap element the ID field's search offers. [osmId] is already in
 * the `<type>/<id>` form the event endpoint takes.
 */
data class OsmPlace(
    val osmId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)

/** The OSM reference form the event endpoint takes: `<type>/<positive id>`. */
private val OSM_ID_PATTERN = Regex("^(node|way|relation)/[1-9][0-9]*$")

/**
 * [value] in the `<type>/<id>` form the endpoint takes, or null when it is not
 * a valid OpenStreetMap reference.
 */
internal fun normalizedOsmId(value: String): String? =
    value.trim().lowercase().takeIf { OSM_ID_PATTERN.matches(it) }

/**
 * Adapts the map's [NominatimSearch] to the add-event form's OpenStreetMap ID
 * field, keeping only the hits Nominatim can name by element.
 */
fun NominatimSearch.toOsmPlaceSearch(): suspend (String) -> List<OsmPlace> = { query ->
    search(query).mapNotNull { place ->
        val osmId = place.osmId ?: return@mapNotNull null
        OsmPlace(osmId = osmId, name = place.name, lat = place.lat, lon = place.lon)
    }
}

/**
 * The fields a new event is submitted with, positioned at the map centre.
 *
 * [startsAt] and [endsAt] are floating local wall-clock times (ISO 8601 with no
 * offset), because the event's time is the local time at its location — the API
 * resolves the zone from the coordinates. [placeOsmId] is the optional
 * OpenStreetMap place link, already in `<type>/<id>` form.
 */
data class AddEventDraft(
    val lat: Double,
    val lon: Double,
    val name: String,
    val website: String,
    val startsAt: String,
    val endsAt: String?,
    val placeOsmId: String? = null,
)

/** The add-event screen's strings, so it stays resource-free. */
data class AddEventLabels(
    val title: String,
    /** The back button's accessible name; the ligature is not announced. */
    val back: String,
    val name: String,
    val namePlaceholder: String,
    /** The optional OpenStreetMap place link field's label. */
    val osmId: String,
    val osmIdPlaceholder: String,
    /** Shown when the typed OpenStreetMap reference is malformed. */
    val osmIdInvalid: String,
    val website: String,
    val websitePlaceholder: String,
    /** The required start date and time field's label. */
    val startsAt: String,
    /** The optional end date and time field's label. */
    val endsAt: String,
    /** The placeholder shown before a date and time has been picked. */
    val selectDateTime: String,
    /** The content description of the button that clears the end time. */
    val clearEnd: String,
    /** The helper caption over the positioning map. */
    val dragMap: String,
    val required: String,
    val submit: String,
    val submitted: String,
    val backToMap: String,
    val ok: String,
    val cancel: String,
    /** The dialog a submission failure is shown in. */
    val error: ErrorDialogLabels,
)

/**
 * Renders the date and time picker for one event field: a calendar, then a
 * clock, confirming the combined local date and time. Injected so a test can
 * set a value without driving the Material dialogs.
 */
typealias EventDateTimePicker = @Composable (
    title: String,
    initial: LocalDateTime,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
) -> Unit

/** The event field a date and time is being picked for. */
private enum class EventField { Start, End }

private val EVENT_DISPLAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/**
 * The floating local date and time the API takes, always carrying seconds so the
 * value is unambiguous. The server infers the zone from the event's coordinates.
 */
private val API_LOCAL_DATE_TIME: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

/** Saves a picked wall-clock time across a configuration change as ISO text. */
private val LOCAL_DATE_TIME_SAVER: Saver<LocalDateTime?, String> = Saver(
    save = { it?.format(API_LOCAL_DATE_TIME) },
    restore = { text -> text.takeIf { it.isNotEmpty() }?.let(LocalDateTime::parse) },
)

/**
 * The whole add-event screen: a Material 3 top app bar, a positioning map with
 * its drag hint, and the fields a new event is submitted with. The location
 * starts at the map centre the host handed over and can be adjusted by panning
 * under the pin. Name, website and starts-at are required; the end time is
 * optional.
 *
 * It mirrors [AddPlaceScreen]: the screen owns everything it renders, the hosts
 * cannot drift apart, [submit] is injected so it can be driven without a server,
 * [map] so a test can render the form without a GPU and [dateTimePicker] so a
 * test can set the timestamps without the Material dialogs.
 *
 * The optional initial values pre-fill the form for a duplicate: [initialName],
 * [initialWebsite], [initialStartsAt] and [initialEndsAt] seed the fields, so the
 * user only has to adjust the date to repeat a regular event. The same form edits
 * an existing event, seeded from its stored values; [initialOsmId] carries its
 * current OpenStreetMap place link.
 *
 * [osmSearch] powers the optional OpenStreetMap ID field: typing a query finds
 * places, and picking one sets the reference and moves the pin to its
 * coordinates. A null searcher hides the field.
 *
 * The body is capped at [CONTENT_MAX_WIDTH] and centred, so a wide desktop or
 * tablet window neither stretches the map and the fields edge to edge nor breaks
 * a phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    labels: AddEventLabels,
    iconFont: FontFamily?,
    palette: MarkerPalette,
    submit: suspend (AddEventDraft) -> Unit,
    onBack: () -> Unit,
    initialName: String = "",
    initialWebsite: String = "",
    initialStartsAt: LocalDateTime? = null,
    initialEndsAt: LocalDateTime? = null,
    /** The event's current OpenStreetMap place link when editing. */
    initialOsmId: String = "",
    /** Searches OpenStreetMap for the optional place link field. */
    osmSearch: (suspend (query: String) -> List<OsmPlace>)? = null,
    map: @Composable (
        moveTo: LocationMove?,
        onCenterChanged: (Double, Double) -> Unit,
    ) -> Unit = { moveTo, onCenterChanged ->
        LocationPickerMap(
            lat = lat,
            lon = lon,
            styleUrl = styleUrl,
            styleJson = styleJson,
            palette = palette,
            onCenterChanged = onCenterChanged,
            pin = LocationPickerPin.Event,
            moveTo = moveTo,
        )
    },
    dateTimePicker: EventDateTimePicker = { title, initial, onConfirm, onDismiss ->
        EventDateTimePickerDialog(
            title = title,
            initial = initial,
            ok = labels.ok,
            cancel = labels.cancel,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    },
) {
    var center by remember { mutableStateOf(lat to lon) }
    var moveTo by remember { mutableStateOf<LocationMove?>(null) }
    // Bumped for every pick, so choosing the same place twice still recentres a
    // map the user has since panned away.
    var moveSequence by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Submit failures surface through a dialog; the field errors stay inline.
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
                            map(moveTo) { newLat, newLon -> center = newLat to newLon }
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
                    AddEventForm(
                        busy = busy,
                        submitted = submitted,
                        labels = labels,
                        initialName = initialName,
                        initialWebsite = initialWebsite,
                        initialStartsAt = initialStartsAt,
                        initialEndsAt = initialEndsAt,
                        initialOsmId = initialOsmId,
                        osmSearch = osmSearch,
                        onOsmPlaceSelected = { place ->
                            center = place.lat to place.lon
                            moveSequence += 1
                            moveTo = LocationMove(
                                lat = place.lat,
                                lon = place.lon,
                                sequence = moveSequence,
                            )
                        },
                        onSubmit = { name, website, startsAt, endsAt, placeOsmId ->
                            busy = true
                            val draft = AddEventDraft(
                                lat = center.first,
                                lon = center.second,
                                name = name,
                                website = website,
                                startsAt = startsAt.format(API_LOCAL_DATE_TIME),
                                endsAt = endsAt?.format(API_LOCAL_DATE_TIME),
                                placeOsmId = placeOsmId,
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
                        dateTimePicker = dateTimePicker,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        error?.let {
            ErrorDialog(labels = labels.error, message = it) { error = null }
        }
    }
}

/**
 * The add-event fields. The coordinates come from the map, not the form. Split
 * from the map so a test can drive the fields without a GPU.
 *
 * Pressing submit validates the fields, scrolls to the first invalid one and
 * only then submits. The Next key walks the fields, and the primary action shows
 * a spinner while the submission is in flight.
 */
@Composable
fun AddEventForm(
    busy: Boolean,
    submitted: Boolean,
    labels: AddEventLabels,
    onSubmit: (
        name: String,
        website: String,
        startsAt: LocalDateTime,
        endsAt: LocalDateTime?,
        placeOsmId: String?,
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialWebsite: String = "",
    initialStartsAt: LocalDateTime? = null,
    initialEndsAt: LocalDateTime? = null,
    /** The event's current OpenStreetMap place link when editing. */
    initialOsmId: String = "",
    /** Searches OpenStreetMap for the optional place link field, or null to hide it. */
    osmSearch: (suspend (query: String) -> List<OsmPlace>)? = null,
    /** Called with the picked place so the screen can move the pin to it. */
    onOsmPlaceSelected: (OsmPlace) -> Unit = {},
    dateTimePicker: EventDateTimePicker = { title, initial, onConfirm, onDismiss ->
        EventDateTimePickerDialog(
            title = title,
            initial = initial,
            ok = labels.ok,
            cancel = labels.cancel,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    },
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var osmId by rememberSaveable { mutableStateOf(initialOsmId) }
    var osmResults by remember { mutableStateOf(emptyList<OsmPlace>()) }
    var osmSearching by remember { mutableStateOf(false) }
    var website by rememberSaveable { mutableStateOf(initialWebsite) }
    // The wall-clock times the user picked, saved across a configuration change.
    // They are not bound to a zone: the API places them in the event's zone.
    var startsAt by rememberSaveable(stateSaver = LOCAL_DATE_TIME_SAVER) {
        mutableStateOf(initialStartsAt)
    }
    var endsAt by rememberSaveable(stateSaver = LOCAL_DATE_TIME_SAVER) {
        mutableStateOf(initialEndsAt)
    }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var picking by remember { mutableStateOf<EventField?>(null) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val nameRequester = remember { BringIntoViewRequester() }
    val osmRequester = remember { BringIntoViewRequester() }
    val startsRequester = remember { BringIntoViewRequester() }
    val websiteRequester = remember { BringIntoViewRequester() }
    val latestOsmSearch by rememberUpdatedState(osmSearch)

    // The optional place link is searched as it is typed, but a complete
    // reference is not a search term: it is what picking a result (or typing
    // the reference by hand) produces. A short query stays quiet, as the map's
    // own search does.
    LaunchedEffect(osmId, osmSearch != null) {
        val search = latestOsmSearch
        if (search == null) return@LaunchedEffect
        val query = osmId.trim()
        if (normalizedOsmId(query) != null || query.length < OSM_SEARCH_MIN_CHARS) {
            osmResults = emptyList()
            osmSearching = false
            return@LaunchedEffect
        }
        osmSearching = true
        delay(OSM_SEARCH_DEBOUNCE_MS)
        val results = try {
            search(query)
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            emptyList()
        }
        osmResults = results
        osmSearching = false
    }

    if (submitted) {
        AddEventSubmitted(labels = labels, onBack = onBack, modifier = modifier)
        return
    }

    when (picking) {
        EventField.Start -> dateTimePicker(
            labels.startsAt,
            startsAt ?: LocalDateTime.now(),
            { startsAt = it; picking = null },
            { picking = null },
        )

        EventField.End -> dateTimePicker(
            labels.endsAt,
            endsAt ?: startsAt ?: LocalDateTime.now(),
            { endsAt = it; picking = null },
            { picking = null },
        )

        null -> {}
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(requiredLabel(labels.name)) },
            placeholder = { Text(labels.namePlaceholder) },
            isError = attempted && name.isBlank(),
            supportingText = if (attempted && name.isBlank()) {
                { Text(labels.required) }
            } else {
                null
            },
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(nameRequester)
                .testTag(ADD_EVENT_NAME_TAG),
        )
        if (osmSearch != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                OutlinedTextField(
                    value = osmId,
                    onValueChange = { osmId = it },
                    label = { Text(labels.osmId) },
                    placeholder = { Text(labels.osmIdPlaceholder) },
                    isError = attempted && osmId.isNotBlank() && normalizedOsmId(osmId) == null,
                    supportingText = if (
                        attempted && osmId.isNotBlank() && normalizedOsmId(osmId) == null
                    ) {
                        { Text(labels.osmIdInvalid) }
                    } else {
                        null
                    },
                    enabled = !busy,
                    singleLine = true,
                    trailingIcon = if (osmSearching) {
                        {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(osmRequester)
                        .testTag(ADD_EVENT_OSM_ID_TAG),
                )
                // The hits sit directly under the field, like the map's own
                // OpenStreetMap group. Picking one writes the element's
                // reference into the field, which ends the search.
                if (osmResults.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Column {
                            osmResults.forEach { place ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !busy) {
                                            osmId = place.osmId
                                            osmResults = emptyList()
                                            onOsmPlaceSelected(place)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = place.osmId,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    MaterialSymbol(
                                        glyph = "travel_explore",
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // The start and end fields open the date and time pickers; they are
        // buttons rather than text fields because the value is never typed.
        DateTimeField(
            label = labels.startsAt,
            required = true,
            value = startsAt,
            placeholder = labels.selectDateTime,
            enabled = !busy,
            onClick = { picking = EventField.Start },
            onClear = null,
            clearDescription = labels.clearEnd,
            buttonTag = ADD_EVENT_STARTS_TAG,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .bringIntoViewRequester(startsRequester),
        )
        DateTimeField(
            label = labels.endsAt,
            required = false,
            value = endsAt,
            placeholder = labels.selectDateTime,
            enabled = !busy,
            onClick = { picking = EventField.End },
            onClear = { endsAt = null },
            clearDescription = labels.clearEnd,
            buttonTag = ADD_EVENT_ENDS_TAG,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        OutlinedTextField(
            value = website,
            onValueChange = { website = it },
            label = { Text(requiredLabel(labels.website)) },
            placeholder = { Text(labels.websitePlaceholder) },
            isError = attempted && website.isBlank(),
            supportingText = if (attempted && website.isBlank()) {
                { Text(labels.required) }
            } else {
                null
            },
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .bringIntoViewRequester(websiteRequester)
                .testTag(ADD_EVENT_WEBSITE_TAG),
        )
        Button(
            onClick = {
                attempted = true
                val placeOsmId = normalizedOsmId(osmId)
                val invalidOsm = osmSearch != null && osmId.isNotBlank() && placeOsmId == null
                val firstInvalid = when {
                    name.isBlank() -> nameRequester
                    invalidOsm -> osmRequester
                    startsAt == null -> startsRequester
                    website.isBlank() -> websiteRequester
                    else -> null
                }
                if (firstInvalid == null) {
                    onSubmit(
                        name.trim(),
                        website.trim(),
                        requireNotNull(startsAt),
                        endsAt,
                        placeOsmId,
                    )
                } else {
                    scope.launch { firstInvalid.bringIntoView() }
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .testTag(ADD_EVENT_SUBMIT_TAG),
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

/**
 * A date and time field styled like the form's outlined text fields but opening
 * the pickers instead of a keyboard. The optional end field carries a clear
 * action.
 */
@Composable
private fun DateTimeField(
    label: String,
    required: Boolean,
    value: LocalDateTime?,
    placeholder: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    clearDescription: String,
    buttonTag: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .testTag(buttonTag),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (required) requiredLabel(label) else label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value?.format(EVENT_DISPLAY_FORMATTER.withAppLocale()) ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                )
            }
            MaterialSymbol(glyph = "schedule", contentDescription = null)
        }
        if (value != null && onClear != null) {
            IconButton(
                onClick = onClear,
                enabled = enabled,
                modifier = Modifier.testTag(ADD_EVENT_END_CLEAR_TAG),
            ) {
                MaterialSymbol(glyph = "close", contentDescription = clearDescription)
            }
        }
    }
}

/** The confirmation shown once an event has been submitted. */
@Composable
private fun AddEventSubmitted(
    labels: AddEventLabels,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
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

private fun LocalDateTime.toUtcDateMillis(): Long =
    toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** The two stages of the combined date and time picker. */
private enum class PickerStage { Date, Time }

/**
 * The default date and time picker: the Material 3 calendar, then the Material 3
 * clock, confirming the combined local date and time. The date is converted
 * through UTC because that is the zone the date picker works in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDateTimePickerDialog(
    title: String,
    initial: LocalDateTime,
    ok: String,
    cancel: String,
    onConfirm: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var stage by remember { mutableStateOf(PickerStage.Date) }
    var dateMillis by remember { mutableStateOf(initial.toUtcDateMillis()) }

    when (stage) {
        PickerStage.Date -> {
            val dateState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            dateState.selectedDateMillis?.let {
                                dateMillis = it
                                stage = PickerStage.Time
                            } ?: onDismiss()
                        },
                    ) { Text(ok) }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(cancel) }
                },
            ) {
                DatePicker(state = dateState)
            }
        }

        PickerStage.Time -> {
            val timeState = rememberTimePickerState(
                initialHour = initial.hour,
                initialMinute = initial.minute,
            )
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = { TimePicker(state = timeState) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val date = JavaInstant.ofEpochMilli(dateMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onConfirm(
                                LocalDateTime.of(
                                    date,
                                    LocalTime.of(timeState.hour, timeState.minute),
                                ),
                            )
                        },
                    ) { Text(ok) }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(cancel) }
                },
            )
        }
    }
}
