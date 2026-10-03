package org.btcmap.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.btcmap.db.Database
import org.btcmap.dbstats.DatabaseFile
import org.btcmap.dbstats.DbStatsLabels
import org.btcmap.dbstats.DbStatsReader
import org.btcmap.dbstats.dbStatsSections
import org.btcmap.settings.MapColor
import org.btcmap.settings.MapStyle
import org.btcmap.settings.Settings
import org.btcmap.settings.apiUrl
import org.btcmap.settings.authorized
import org.btcmap.settings.mapColor
import org.btcmap.settings.mapRotationEnabled
import org.btcmap.settings.mapStyle
import org.btcmap.settings.setMapColor
import org.btcmap.settings.showAttribution
import org.btcmap.settings.verifiedFilterYears
import org.btcmap.stats.StatsEntry
import org.btcmap.stats.StatsSection
import org.btcmap.sync.SyncState
import org.btcmap.ui.ColorSettingsScreen
import org.btcmap.ui.RadioOption
import org.btcmap.ui.RadioPickerContent
import org.btcmap.ui.SettingsScreen
import org.btcmap.ui.SettingsStrings
import org.btcmap.ui.StatsScreen
import org.btcmap.ui.map.AreaChipPalette
import org.btcmap.ui.map.MarkerPalette
import org.btcmap.ui.mapColorItems
import org.btcmap.ui.settingsItems

/** The marker colours the user configured, or their defaults. */
internal fun markerPalette(settings: Settings): MarkerPalette = MarkerPalette(
    markerBackground = Color(settings.mapColor(MapColor.MarkerBackground)),
    markerIcon = Color(settings.mapColor(MapColor.MarkerIcon)),
    boostedMarkerBackground = Color(settings.mapColor(MapColor.BoostedMarkerBackground)),
    boostedMarkerIcon = Color(settings.mapColor(MapColor.BoostedMarkerIcon)),
    badgeBackground = Color(settings.mapColor(MapColor.BadgeBackground)),
    badgeText = Color(settings.mapColor(MapColor.BadgeText)),
)

/** The area-chip and map-control colours the user configured, or their defaults. */
internal fun areaChipPalette(settings: Settings): AreaChipPalette = AreaChipPalette(
    buttonBackground = Color(settings.mapColor(MapColor.ButtonBackground)),
    buttonIcon = Color(settings.mapColor(MapColor.ButtonIcon)),
    buttonBorder = Color(settings.mapColor(MapColor.ButtonBorder)),
    badgeBackground = Color(settings.mapColor(MapColor.BadgeBackground)),
    badgeText = Color(settings.mapColor(MapColor.BadgeText)),
)

/** The dialog the settings list can raise. */
private enum class SettingsDialog { MapStyle, VerifiedFilter }

/**
 * The desktop settings screen. It renders the same rows Android does through the
 * shared [settingsItems], and opens the colour and database screens the desktop
 * hosts. The strings and the pickers are the desktop's own; everything the
 * screen reads and writes goes through `:shared`, so both apps see one set of
 * settings.
 */
@Composable
internal fun DesktopSettingsScreen(
    settings: Settings,
    db: Database,
    onOpenAccount: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenDbStats: () -> Unit,
) {
    var attribution by remember { mutableStateOf(settings.showAttribution) }
    var rotation by remember { mutableStateOf(settings.mapRotationEnabled) }
    var mapStyle by remember { mutableStateOf(settings.mapStyle) }
    var verifiedYears by remember { mutableStateOf(settings.verifiedFilterYears) }

    // The account row names the cached user, which is read off the main thread.
    var accountTitle by remember { mutableStateOf(NOT_LOGGED_IN) }
    var accountSecondary by remember { mutableStateOf(LOG_IN) }
    LaunchedEffect(Unit) {
        val username = if (settings.authorized) {
            withContext(Dispatchers.IO) { db.user.select()?.name }
        } else {
            null
        }
        if (username != null) {
            accountTitle = "Logged in as $username"
            accountSecondary = "Click to see your profile"
        }
    }

    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    SettingsScreen(
        items = settingsItems(
            strings = SettingsStrings(
                accountTitle = accountTitle,
                accountSecondary = accountSecondary,
                mapStyle = "Map style",
                mapStyleValue = mapStyleName(mapStyle),
                customizeColors = "Customize colors",
                customizeColorsSecondary = "Marker, badge and button colors",
                verifiedFilter = "Only show places",
                verifiedFilterValue = verifiedFilterName(verifiedYears),
                showAttribution = "Show attribution",
                showAttributionSecondary = "Visible by default per OSM policy",
                mapRotation = "Allow map rotation",
                mapRotationSecondary = "Compass appears when not facing north",
                dbStats = "Database",
                dbStatsSecondary = "Database metadata and table management",
                imageStats = "Image cache",
                imageStatsSecondary = "Memory and disk caches metadata plus load stats",
            ),
            showAttribution = attribution,
            mapRotationEnabled = rotation,
            includeImageStats = false,
        ),
        onItemClick = { key ->
            when (key) {
                "account" -> onOpenAccount()
                "mapStyle" -> dialog = SettingsDialog.MapStyle
                "customizeColors" -> onOpenColors()
                "verifiedFilter" -> dialog = SettingsDialog.VerifiedFilter
                "dbStats" -> onOpenDbStats()
            }
        },
        onItemCheckedChange = { key, checked ->
            when (key) {
                "showAttribution" -> {
                    attribution = checked
                    settings.showAttribution = checked
                }

                "mapRotation" -> {
                    rotation = checked
                    settings.mapRotationEnabled = checked
                }
            }
        },
    )

    when (dialog) {
        SettingsDialog.MapStyle -> RadioPickerDialog(
            title = "Map style",
            options = MapStyle.entries.map { RadioOption(it.name, mapStyleName(it)) },
            selectedKey = mapStyle.name,
            onSelect = { key ->
                MapStyle.entries.firstOrNull { it.name == key }?.let {
                    mapStyle = it
                    settings.mapStyle = it
                }
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        SettingsDialog.VerifiedFilter -> RadioPickerDialog(
            title = "Only show places",
            options = VERIFIED_FILTER_YEARS.map { years ->
                RadioOption(years.toString(), verifiedFilterName(years))
            },
            selectedKey = verifiedYears.toString(),
            onSelect = { key ->
                key.toIntOrNull()?.let {
                    verifiedYears = it
                    settings.verifiedFilterYears = it
                }
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

/**
 * The colour screen: the shared list plus the desktop colour picker. A chosen
 * colour is written straight to the shared settings and the list re-read, so the
 * map picks it up when it returns.
 */
@Composable
internal fun DesktopColorsScreen(
    settings: Settings,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf(mapColorItems(settings, ::mapColorTitle)) }
    var picking by remember { mutableStateOf<MapColor?>(null) }

    ScreenPage(title = "Customize colors", onBack = onBack) {
        ColorSettingsScreen(
            items = items,
            onItemClick = { key -> picking = MapColor.fromKey(key) },
        )
    }

    picking?.let { color ->
        ColorPickerDialog(
            title = mapColorTitle(color),
            initial = settings.mapColor(color),
            resettable = color.resettable,
            onPick = { argb ->
                settings.setMapColor(color, argb)
                items = mapColorItems(settings, ::mapColorTitle)
                picking = null
            },
            onReset = {
                settings.setMapColor(color, null)
                items = mapColorItems(settings, ::mapColorTitle)
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

/**
 * The database stats screen, the desktop counterpart of Android's `DbStatsFragment`.
 * The desktop bundles no data snapshots, so the bundle cards Android shows have
 * no desktop equivalent and are omitted.
 */
@Composable
internal fun DesktopDbStatsScreen(
    db: Database,
    settings: Settings,
    syncState: SyncState,
    onSync: () -> Unit,
    onBack: () -> Unit,
) {
    var sections by remember { mutableStateOf<List<StatsSection>>(emptyList()) }
    LaunchedEffect(syncState) {
        sections = withContext(Dispatchers.IO) { loadDbStatsSections(db, settings.apiUrl.toString(), syncState) }
    }

    ScreenPage(title = "Database", onBack = onBack) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Button(
                onClick = onSync,
                enabled = syncState == SyncState.Idle,
            ) {
                Text(text = "Sync now")
            }
        }
        StatsScreen(sections = sections)
    }
}

/** A titled radio list in a dialog, the desktop shape of Android's picker dialogs. */
@Composable
private fun RadioPickerDialog(
    title: String,
    options: List<RadioOption>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            RadioPickerContent(
                options = options,
                selectedKey = selectedKey,
                onSelect = onSelect,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Close") }
        },
    )
}

/**
 * A minimal RGBA colour picker: one slider per channel, a live preview and the
 * resulting hex. The Android app uses a Views pop-up instead; only the shared
 * colour keys and defaults are common.
 */
@Composable
private fun ColorPickerDialog(
    title: String,
    initial: Int,
    resettable: Boolean,
    onPick: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var red by remember { mutableFloatStateOf(((initial shr 16) and 0xFF).toFloat()) }
    var green by remember { mutableFloatStateOf(((initial shr 8) and 0xFF).toFloat()) }
    var blue by remember { mutableFloatStateOf((initial and 0xFF).toFloat()) }
    var alpha by remember { mutableFloatStateOf(((initial ushr 24) and 0xFF).toFloat()) }

    val argb = (alpha.toInt() shl 24) or
        (red.toInt() shl 16) or
        (green.toInt() shl 8) or
        blue.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(argb)),
                )
                ChannelSlider(label = "Red", value = red) { red = it }
                ChannelSlider(label = "Green", value = green) { green = it }
                ChannelSlider(label = "Blue", value = blue) { blue = it }
                ChannelSlider(label = "Alpha", value = alpha) { alpha = it }
                Text(
                    text = "#" + argb.toUInt().toString(16).uppercase().padStart(8, '0'),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(argb) }) { Text(text = "OK") }
        },
        dismissButton = {
            Row {
                if (resettable) {
                    TextButton(onClick = onReset) { Text(text = "Reset") }
                }
                TextButton(onClick = onDismiss) { Text(text = "Cancel") }
            }
        },
    )
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(text = label, modifier = Modifier.width(56.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.toInt().toString(),
            modifier = Modifier
                .width(40.dp)
                .padding(start = 8.dp),
        )
    }
}

/** A human-readable byte size, the desktop stand-in for Android's file formatter. */
private fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes ${units[unit]}" else "%.1f %s".format(value, units[unit])
}

private fun loadDbStatsSections(
    db: Database,
    apiUrl: String,
    syncState: SyncState,
): List<StatsSection> {
    val reader = DbStatsReader(db.conn)
    val file = DatabaseFile.read(db.path)
    val version = reader.readUserVersion()
    val tables = reader.readTables()

    return buildList {
        addAll(
            dbStatsSections(
                file = file,
                version = version,
                tables = tables,
                bundles = emptyMap(),
                labels = DESKTOP_DB_STATS_LABELS,
                formatBytes = ::formatBytes,
            ),
        )
        add(
            StatsSection(
                key = "sync",
                title = "Sync",
                icon = "sync",
                entries = listOf(
                    StatsEntry("Source", apiUrl),
                    StatsEntry("State", syncStateLabel(syncState)),
                ),
            )
        )
    }
}

/** The desktop's labels for the shared database stats cards. */
private val DESKTOP_DB_STATS_LABELS = DbStatsLabels(
    database = "Database",
    file = "File",
    version = "Version",
    size = "Size",
    table = { "Table $it" },
    bundle = { "Bundle $it" },
    location = "Location",
    visibleRows = "Visible rows",
    deletedRows = "Deleted rows",
    futureRows = "Future rows",
    rows = "Rows",
    newestUpdate = "Newest update",
)

private fun syncStateLabel(state: SyncState): String = when (state) {
    SyncState.Idle -> "Idle"
    SyncState.UnbundlingPlaces -> "Unbundling places"
    SyncState.SyncingPlaces -> "Syncing places"
    SyncState.UnbundlingEvents -> "Unbundling events"
    SyncState.SyncingEvents -> "Syncing events"
    SyncState.UnbundlingComments -> "Unbundling comments"
    SyncState.SyncingComments -> "Syncing comments"
    SyncState.UnbundlingAreas -> "Unbundling areas"
    SyncState.SyncingAreas -> "Syncing areas"
}

private fun mapStyleName(style: MapStyle): String = when (style) {
    MapStyle.Auto -> "Auto"
    MapStyle.Liberty -> "OpenFreeMap Liberty"
    MapStyle.Positron -> "OpenFreeMap Positron"
    MapStyle.Bright -> "OpenFreeMap Bright"
    MapStyle.Dark -> "OpenFreeMap Dark"
    MapStyle.DarkMatter -> "OpenFreeMap Dark Matter"
}

private fun mapColorTitle(color: MapColor): String = when (color) {
    MapColor.MarkerBackground -> "Marker background"
    MapColor.MarkerIcon -> "Marker icon"
    MapColor.BoostedMarkerBackground -> "Boosted marker background"
    MapColor.BoostedMarkerIcon -> "Boosted marker icon"
    MapColor.BadgeBackground -> "Badge background"
    MapColor.BadgeText -> "Badge text"
    MapColor.ButtonBackground -> "Button background"
    MapColor.ButtonIcon -> "Button icon"
    MapColor.ButtonBorder -> "Button border"
}

private fun verifiedFilterName(years: Int): String = when (years) {
    1 -> "Verified within 1 year"
    2 -> "Verified within 2 years"
    3 -> "Verified within 3 years"
    else -> ""
}

private const val NOT_LOGGED_IN = "Account"
private const val LOG_IN = "Log in"

private val VERIFIED_FILTER_YEARS = listOf(1, 2, 3)
