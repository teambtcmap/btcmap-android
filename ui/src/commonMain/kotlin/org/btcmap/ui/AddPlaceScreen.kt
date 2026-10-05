package org.btcmap.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.btcmap.ui.map.MarkerBitmapFactory
import org.btcmap.ui.map.MarkerPalette
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/** Test tags so a test can drive the add-place form. */
const val ADD_PLACE_NAME_TAG = "add-place-name"
const val ADD_PLACE_CATEGORY_TAG = "add-place-category"
const val ADD_PLACE_ADDRESS_TAG = "add-place-address"
const val ADD_PLACE_SUBMIT_TAG = "add-place-submit"

/** The fields a new place is submitted with, positioned at the map centre. */
data class AddPlaceDraft(
    val lat: Double,
    val lon: Double,
    val name: String,
    val category: String,
    val address: String,
    val website: String,
    val description: String,
)

/** The add-place screen's strings, so it stays resource-free. */
data class AddPlaceLabels(
    val title: String,
    /** The back button's accessible name; the ligature is not announced. */
    val back: String,
    val name: String,
    val namePlaceholder: String,
    val category: String,
    val categoryPlaceholder: String,
    val address: String,
    val addressPlaceholder: String,
    val website: String,
    val websitePlaceholder: String,
    val description: String,
    val descriptionPlaceholder: String,
    /** The helper caption over the positioning map. */
    val dragMap: String,
    val required: String,
    val submit: String,
    val submitted: String,
    val backToMap: String,
)

private const val ADD_PLACE_ZOOM = 16.0

/**
 * The whole add-place screen: a Material 3 top app bar, a positioning map with
 * its drag hint, and the fields a new place is submitted with. The location
 * starts at the map centre the host handed over and can be adjusted by panning
 * under the pin. Name, category and address are required; the rest is optional.
 *
 * The screen is shared by Android and the desktop app and owns everything it
 * renders, so the two hosts cannot drift apart: [iconFont] is the host's
 * Material Symbols typeface, applied through [AppTheme]; [palette] is the user's
 * marker colour, so the positioning pin is the same merchant pin the map draws.
 * [submit] is injected so the screen can be driven without a server, and [map] so
 * a test can render the form without a GPU.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceScreen(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    labels: AddPlaceLabels,
    iconFont: FontFamily?,
    palette: MarkerPalette,
    submit: suspend (AddPlaceDraft) -> Unit,
    onBack: () -> Unit,
    map: @Composable ((Double, Double) -> Unit) -> Unit = { onCenterChanged ->
        AddPlaceMap(
            lat = lat,
            lon = lon,
            styleUrl = styleUrl,
            styleJson = styleJson,
            palette = palette,
            onCenterChanged = onCenterChanged,
        )
    },
) {
    var center by remember { mutableStateOf(lat to lon) }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // The positioning map is a terminal confirmation's backdrop only
                // while the form is still being filled in.
                if (!submitted) {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        map { newLat, newLon -> center = newLat to newLon }
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
                AddPlaceForm(
                    busy = busy,
                    submitted = submitted,
                    labels = labels,
                    onSubmit = { name, category, address, website, description ->
                        busy = true
                        val draft = AddPlaceDraft(
                            lat = center.first,
                            lon = center.second,
                            name = name,
                            category = category,
                            address = address,
                            website = website,
                            description = description,
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

/**
 * The add-place fields. The coordinates come from the map, not the form. Split
 * from the map so a test can drive the fields without a GPU.
 *
 * Pressing submit validates the fields, scrolls to the first invalid one and
 * only then submits. The Next key walks the fields, and the primary action shows
 * a spinner while the submission is in flight.
 */
@Composable
fun AddPlaceForm(
    busy: Boolean,
    submitted: Boolean,
    labels: AddPlaceLabels,
    onSubmit: (name: String, category: String, address: String, website: String, description: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val nameRequester = remember { BringIntoViewRequester() }
    val categoryRequester = remember { BringIntoViewRequester() }
    val addressRequester = remember { BringIntoViewRequester() }

    if (submitted) {
        AddPlaceSubmitted(labels = labels, onBack = onBack, modifier = modifier)
        return
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
                .testTag(ADD_PLACE_NAME_TAG),
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text(requiredLabel(labels.category)) },
            placeholder = { Text(labels.categoryPlaceholder) },
            isError = attempted && category.isBlank(),
            supportingText = if (attempted && category.isBlank()) {
                { Text(labels.required) }
            } else {
                null
            },
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .bringIntoViewRequester(categoryRequester)
                .testTag(ADD_PLACE_CATEGORY_TAG),
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(requiredLabel(labels.address)) },
            placeholder = { Text(labels.addressPlaceholder) },
            isError = attempted && address.isBlank(),
            supportingText = if (attempted && address.isBlank()) {
                { Text(labels.required) }
            } else {
                null
            },
            enabled = !busy,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .bringIntoViewRequester(addressRequester)
                .testTag(ADD_PLACE_ADDRESS_TAG),
        )
        OutlinedTextField(
            value = website,
            onValueChange = { website = it },
            label = { Text(labels.website) },
            placeholder = { Text(labels.websitePlaceholder) },
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(labels.description) },
            placeholder = { Text(labels.descriptionPlaceholder) },
            enabled = !busy,
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        Button(
            onClick = {
                attempted = true
                val firstInvalid = when {
                    name.isBlank() -> nameRequester
                    category.isBlank() -> categoryRequester
                    address.isBlank() -> addressRequester
                    else -> null
                }
                if (firstInvalid == null) {
                    onSubmit(
                        name.trim(),
                        category.trim(),
                        address.trim(),
                        website.trim(),
                        description.trim(),
                    )
                } else {
                    scope.launch { firstInvalid.bringIntoView() }
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .testTag(ADD_PLACE_SUBMIT_TAG),
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

/** The confirmation shown once a place has been submitted for review. */
@Composable
private fun AddPlaceSubmitted(
    labels: AddPlaceLabels,
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

/** The add-place map: pan and zoom to move the centre the pin marks. */
@Composable
private fun AddPlaceMap(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    palette: MarkerPalette,
    onCenterChanged: (Double, Double) -> Unit,
) {
    val state = rememberMapState(
        baseStyle = if (styleJson != null) BaseStyle.Json(styleJson) else BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(target = Position(lon, lat), zoom = ADD_PLACE_ZOOM),
    ) { }

    LaunchedEffect(state, onCenterChanged) {
        state.events.filterIsInstance<MapEvent.CameraMoveEnded>().collect {
            state.cameraPosition?.target?.let { onCenterChanged(it.latitude, it.longitude) }
        }
    }

    // The positioning pin is the standard merchant pin the map draws, built with
    // the icon font AppTheme provides and the user's marker colours.
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val iconFont = LocalIconFont.current
    val factory = remember(textMeasurer, iconFont, density, palette) {
        MarkerBitmapFactory(
            textMeasurer = textMeasurer,
            iconFont = iconFont,
            density = density,
            palette = palette,
        )
    }
    val pin = remember(factory) { factory.merchantPin() }
    // The pin's tip, not its centre, marks the position, so the bitmap is lifted
    // by half its height to sit on the map centre.
    val pinHeight = with(density) { pin.height.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        // The positioning map is a temporary overlay, not a place map, so the
        // MapLibre logo and the expanding attribution pill are dropped. Its
        // `overlay = {}` is what removes them; `MapUiOptions` does not control
        // the overlay.
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            state = state,
            overlay = {},
        )
        Image(
            bitmap = pin,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -(pinHeight / 2)),
        )
    }
}
