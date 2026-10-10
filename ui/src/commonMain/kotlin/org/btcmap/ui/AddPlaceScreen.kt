package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.btcmap.ui.map.LocationPickerMap
import org.btcmap.ui.map.MarkerPalette

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
    /** The dialog a submission failure is shown in. */
    val error: ErrorDialogLabels,
)

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
 *
 * The optional initial values pre-fill the form for a place submitted from a
 * basemap POI: [initialName] and [initialCategory] seed the fields, so the user
 * only has to confirm the details of a feature the map already draws.
 *
 * The body is capped at [CONTENT_MAX_WIDTH] and centred, so a wide desktop or
 * tablet window neither stretches the map and the fields edge to edge nor breaks
 * a phone.
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
    /** Pre-fills the form when the place is submitted from a basemap POI. */
    initialName: String = "",
    initialCategory: String = "",
    map: @Composable ((Double, Double) -> Unit) -> Unit = { onCenterChanged ->
        LocationPickerMap(
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
                    // The positioning map is a terminal confirmation's backdrop
                    // only while the form is still being filled in. It is inset
                    // to the form's margin and clipped to the theme's medium
                    // shape, so it reads as a contained M3 media block rather
                    // than a bare rectangle wider than the fields below it.
                    if (!submitted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                                .height(240.dp)
                                .clip(MaterialTheme.shapes.medium),
                        ) {
                            map { newLat, newLon -> center = newLat to newLon }
                            // The hint floats on the map so it reads as
                            // belonging to it, in the inverse-surface role M3
                            // reserves for content over imagery.
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
                        initialName = initialName,
                        initialCategory = initialCategory,
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

        error?.let {
            ErrorDialog(labels = labels.error, message = it) { error = null }
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
    initialName: String = "",
    initialCategory: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var category by rememberSaveable { mutableStateOf(initialCategory) }
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

