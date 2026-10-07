package org.btcmap.ui

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
import org.btcmap.dbstats.BundleStats
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
import org.btcmap.ui.map.AreaChipPalette
import org.btcmap.ui.map.MarkerPalette

/** The marker colours the user configured, or their defaults. */
fun markerPalette(settings: Settings): MarkerPalette = MarkerPalette(
    markerBackground = Color(settings.mapColor(MapColor.MarkerBackground)),
    markerIcon = Color(settings.mapColor(MapColor.MarkerIcon)),
    boostedMarkerBackground = Color(settings.mapColor(MapColor.BoostedMarkerBackground)),
    boostedMarkerIcon = Color(settings.mapColor(MapColor.BoostedMarkerIcon)),
    badgeBackground = Color(settings.mapColor(MapColor.BadgeBackground)),
    badgeText = Color(settings.mapColor(MapColor.BadgeText)),
)

/** The area-chip and map-control colours the user configured, or their defaults. */
fun areaChipPalette(settings: Settings): AreaChipPalette = AreaChipPalette(
    buttonBackground = Color(settings.mapColor(MapColor.ButtonBackground)),
    buttonIcon = Color(settings.mapColor(MapColor.ButtonIcon)),
    buttonBorder = Color(settings.mapColor(MapColor.ButtonBorder)),
    badgeBackground = Color(settings.mapColor(MapColor.BadgeBackground)),
    badgeText = Color(settings.mapColor(MapColor.BadgeText)),
)

/** The settings page's strings beyond what [settingsItems] already names. */
data class SettingsPageLabels(
    val account: String,
    val logIn: String,
    val loggedInAs: (username: String) -> String,
    val openProfile: String,
    val mapStyle: String,
    val mapStyleValue: (MapStyle) -> String,
    val customizeColors: String,
    val customizeColorsSecondary: String,
    val verifiedFilter: String,
    val verifiedFilterValue: (years: Int) -> String,
    val verifiedFilterYears: List<Int>,
    val showAttribution: String,
    val showAttributionSecondary: String,
    val mapRotation: String,
    val mapRotationSecondary: String,
    val dbStats: String,
    val dbStatsSecondary: String,
    val imageStats: String,
    val imageStatsSecondary: String,
    val mapStyleDialogTitle: String,
    val verifiedFilterDialogTitle: String,
    val close: String,
)

/** The dialog the settings list can raise. */
private enum class SettingsDialog { MapStyle, VerifiedFilter }

/**
 * The settings page: the shared [settingsItems] rows, the account row read from
 * the cached user, and the host's dialogs. The colour and database screens are
 * the host's to open.
 */
@Composable
fun SettingsPage(
    settings: Settings,
    db: Database,
    labels: SettingsPageLabels,
    includeImageStats: Boolean = true,
    onOpenAccount: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenDbStats: () -> Unit,
    onOpenImageStats: () -> Unit = {},
    reloadKey: Int = 0,
) {
    var attribution by remember { mutableStateOf(settings.showAttribution) }
    var rotation by remember { mutableStateOf(settings.mapRotationEnabled) }
    var mapStyle by remember { mutableStateOf(settings.mapStyle) }
    var verifiedYears by remember { mutableStateOf(settings.verifiedFilterYears) }

    // The account row names the cached user, which is read off the main thread.
    // [reloadKey] lets a host re-read it after a sign-in, which the page cannot
    // observe on its own.
    var accountTitle by remember { mutableStateOf(labels.account) }
    var accountSecondary by remember { mutableStateOf(labels.logIn) }
    LaunchedEffect(reloadKey) {
        val username = withContext(Dispatchers.IO) {
            if (!settings.authorized) {
                null
            } else {
                db.user.select()?.name ?: run {
                    // A token without a cached account is not a usable session,
                    // so clear it instead of pointing at a profile that will be
                    // dropped.
                    settings.clearSession(db)
                    null
                }
            }
        }
        accountTitle = if (username != null) labels.loggedInAs(username) else labels.account
        accountSecondary = if (username != null) labels.openProfile else labels.logIn
    }

    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    SettingsScreen(
        items = settingsItems(
            strings = SettingsStrings(
                accountTitle = accountTitle,
                accountSecondary = accountSecondary,
                mapStyle = labels.mapStyle,
                mapStyleValue = labels.mapStyleValue(mapStyle),
                customizeColors = labels.customizeColors,
                customizeColorsSecondary = labels.customizeColorsSecondary,
                verifiedFilter = labels.verifiedFilter,
                verifiedFilterValue = labels.verifiedFilterValue(verifiedYears),
                showAttribution = labels.showAttribution,
                showAttributionSecondary = labels.showAttributionSecondary,
                mapRotation = labels.mapRotation,
                mapRotationSecondary = labels.mapRotationSecondary,
                dbStats = labels.dbStats,
                dbStatsSecondary = labels.dbStatsSecondary,
                imageStats = labels.imageStats,
                imageStatsSecondary = labels.imageStatsSecondary,
            ),
            showAttribution = attribution,
            mapRotationEnabled = rotation,
            includeImageStats = includeImageStats,
        ),
        onItemClick = { key ->
            when (key) {
                "account" -> onOpenAccount()
                "mapStyle" -> dialog = SettingsDialog.MapStyle
                "customizeColors" -> onOpenColors()
                "verifiedFilter" -> dialog = SettingsDialog.VerifiedFilter
                "dbStats" -> onOpenDbStats()
                "imageStats" -> onOpenImageStats()
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
            title = labels.mapStyleDialogTitle,
            options = MapStyle.entries.map { RadioOption(it.name, labels.mapStyleValue(it)) },
            selectedKey = mapStyle.name,
            close = labels.close,
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
            title = labels.verifiedFilterDialogTitle,
            options = labels.verifiedFilterYears.map { years ->
                RadioOption(years.toString(), labels.verifiedFilterValue(years))
            },
            selectedKey = verifiedYears.toString(),
            close = labels.close,
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

/** The colour screen's strings and the desktop picker's labels. */
data class ColorsPageLabels(
    val colorTitle: (MapColor) -> String,
    val red: String,
    val green: String,
    val blue: String,
    val alpha: String,
    val ok: String,
    val reset: String,
    val cancel: String,
)

/**
 * The colour screen: the shared list plus the host's colour picker. A chosen
 * colour is written straight to the shared settings and the list re-read, so the
 * map picks it up when it returns.
 */
@Composable
fun ColorsPage(
    settings: Settings,
    labels: ColorsPageLabels,
) {
    var items by remember { mutableStateOf(mapColorItems(settings, labels.colorTitle)) }
    var picking by remember { mutableStateOf<MapColor?>(null) }

    ColorSettingsScreen(
        items = items,
        onItemClick = { key -> picking = MapColor.fromKey(key) },
    )

    picking?.let { color ->
        ColorPickerDialog(
            title = labels.colorTitle(color),
            initial = settings.mapColor(color),
            resettable = color.resettable,
            labels = labels,
            onPick = { argb ->
                settings.setMapColor(color, argb)
                items = mapColorItems(settings, labels.colorTitle)
                picking = null
            },
            onReset = {
                settings.setMapColor(color, null)
                items = mapColorItems(settings, labels.colorTitle)
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

/** The database stats page's strings beyond the shared [DbStatsLabels]. */
data class DbStatsPageLabels(
    val dbStats: DbStatsLabels,
    val sync: String,
    val source: String,
    val state: String,
    val syncNow: String,
    val syncStateLabel: (SyncState) -> String,
)

/**
 * The database stats page: the shared [StatsScreen] over the shared
 * [dbStatsSections], with the host's own sync card and, optionally, a "sync
 * now" button. The Android host reads the bundled snapshots and hides the
 * button, since its toolbar carries the sync action.
 */
@Composable
fun DbStatsPage(
    db: Database,
    settings: Settings,
    syncState: SyncState,
    labels: DbStatsPageLabels,
    onSync: () -> Unit,
    bundles: Map<String, BundleStats> = emptyMap(),
    showSyncButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var sections by remember { mutableStateOf<List<StatsSection>>(emptyList()) }
    LaunchedEffect(syncState, bundles) {
        sections = withContext(Dispatchers.IO) {
            loadDbStatsSections(db, settings.apiUrl.toString(), syncState, labels, bundles)
        }
    }

    if (showSyncButton) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Button(
                onClick = onSync,
                enabled = syncState == SyncState.Idle,
            ) {
                Text(text = labels.syncNow)
            }
        }
    }
    StatsScreen(sections = sections, modifier = modifier)
}

private fun loadDbStatsSections(
    db: Database,
    apiUrl: String,
    syncState: SyncState,
    labels: DbStatsPageLabels,
    bundles: Map<String, BundleStats>,
): List<StatsSection> {
    val reader = DbStatsReader(db.conn)
    val file = DatabaseFile.read(db.path)
    val (version, tables) = runDbBlocking { reader.readUserVersion() to reader.readTables() }

    return buildList {
        addAll(
            dbStatsSections(
                file = file,
                version = version,
                tables = tables,
                bundles = bundles,
                labels = labels.dbStats,
                formatBytes = ::formatBytes,
            ),
        )
        add(
            StatsSection(
                key = "sync",
                title = labels.sync,
                icon = "sync",
                entries = listOf(
                    StatsEntry(labels.source, apiUrl),
                    StatsEntry(labels.state, labels.syncStateLabel(syncState)),
                ),
            )
        )
    }
}

/** A titled radio list in a dialog, the shape Android shows as picker dialogs. */
@Composable
private fun RadioPickerDialog(
    title: String,
    options: List<RadioOption>,
    selectedKey: String?,
    close: String,
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
        // Applying a choice already closes the dialog, so the only action is a
        // dismissal; M3 puts that in the dismiss slot rather than the confirm
        // one. With an empty confirm it still renders right-aligned.
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = close) }
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
    labels: ColorsPageLabels,
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
                ChannelSlider(label = labels.red, value = red) { red = it }
                ChannelSlider(label = labels.green, value = green) { green = it }
                ChannelSlider(label = labels.blue, value = blue) { blue = it }
                ChannelSlider(label = labels.alpha, value = alpha) { alpha = it }
                Text(
                    text = "#" + argb.toUInt().toString(16).uppercase().padStart(8, '0'),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(argb) }) { Text(text = labels.ok) }
        },
        dismissButton = {
            Row {
                if (resettable) {
                    TextButton(onClick = onReset) { Text(text = labels.reset) }
                }
                TextButton(onClick = onDismiss) { Text(text = labels.cancel) }
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
fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    if (unit == 0) return "$bytes ${units[unit]}"
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return "$rounded ${units[unit]}"
}
