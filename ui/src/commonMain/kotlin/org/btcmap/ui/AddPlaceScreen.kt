package org.btcmap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MapEvent
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/** Test tags so a test can drive the add-place form. */
internal const val ADD_PLACE_NAME_TAG = "add-place-name"
internal const val ADD_PLACE_CATEGORY_TAG = "add-place-category"
internal const val ADD_PLACE_ADDRESS_TAG = "add-place-address"
internal const val ADD_PLACE_SUBMIT_TAG = "add-place-submit"

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

/** The add-place form's strings, so the screen stays resource-free. */
data class AddPlaceLabels(
    val name: String,
    val category: String,
    val address: String,
    val website: String,
    val description: String,
    val required: String,
    val submit: String,
    val submitted: String,
    val backToMap: String,
)

private const val ADD_PLACE_ZOOM = 16.0

/**
 * The add-place screen: a positioning map over the fields a new place is
 * submitted with. The location starts at the map centre the host handed over
 * and can be adjusted by panning under the pin. Name, category and address are
 * required; the rest is optional.
 *
 * [submit] is injected so the screen can be driven without a server, and [map]
 * so a test can render the form without a GPU. The host supplies its own top
 * bar and back affordance.
 */
@Composable
fun AddPlaceScreen(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
    labels: AddPlaceLabels,
    submit: suspend (AddPlaceDraft) -> Unit,
    onBack: () -> Unit,
    map: @Composable ((Double, Double) -> Unit) -> Unit = { onCenterChanged ->
        AddPlaceMap(
            lat = lat,
            lon = lon,
            styleUrl = styleUrl,
            styleJson = styleJson,
            onCenterChanged = onCenterChanged,
        )
    },
) {
    var center by remember { mutableStateOf(lat to lon) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            map { newLat, newLon -> center = newLat to newLon }
        }
        AddPlaceForm(
            busy = busy,
            error = error,
            submitted = submitted,
            labels = labels,
            onSubmit = { name, category, address, website, description ->
                busy = true
                error = null
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

/**
 * The add-place fields. The coordinates come from the map, not the form. Split
 * from the map so a test can drive the fields without a GPU.
 */
@Composable
fun AddPlaceForm(
    busy: Boolean,
    error: String?,
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

    if (submitted) {
        Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
            Text(labels.submitted)
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text(labels.backToMap)
            }
        }
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
            label = { Text(labels.name) },
            isError = attempted && name.isBlank(),
            supportingText = if (attempted && name.isBlank()) {
                { Text(labels.required) }
            } else {
                null
            },
            enabled = !busy,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ADD_PLACE_NAME_TAG),
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text(labels.category) },
            isError = attempted && category.isBlank(),
            supportingText = if (attempted && category.isBlank()) {
                { Text(labels.required) }
            } else {
                null
            },
            enabled = !busy,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(ADD_PLACE_CATEGORY_TAG),
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text(labels.address) },
            isError = attempted && address.isBlank(),
            supportingText = if (attempted && address.isBlank()) {
                { Text(labels.required) }
            } else {
                null
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag(ADD_PLACE_ADDRESS_TAG),
        )
        OutlinedTextField(
            value = website,
            onValueChange = { website = it },
            label = { Text(labels.website) },
            enabled = !busy,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(labels.description) },
            enabled = !busy,
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Button(
            onClick = {
                attempted = true
                if (name.isNotBlank() && category.isNotBlank() && address.isNotBlank()) {
                    onSubmit(
                        name.trim(),
                        category.trim(),
                        address.trim(),
                        website.trim(),
                        description.trim(),
                    )
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .testTag(ADD_PLACE_SUBMIT_TAG),
        ) {
            Text(labels.submit)
        }
    }
}

/** The add-place map: pan and zoom to move the centre the pin marks. */
@Composable
private fun AddPlaceMap(
    lat: Double,
    lon: Double,
    styleUrl: String,
    styleJson: String?,
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

    Box(modifier = Modifier.fillMaxSize()) {
        // The logo and attribution are hidden, as the Android add-place map hides
        // them: the positioning map is a temporary overlay, not a place map.
        MaplibreMap(modifier = Modifier.fillMaxSize(), state = state, uiOptions = MapUiOptions.None)
        MaterialSymbol(
            glyph = "location_on",
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
