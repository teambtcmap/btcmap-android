package org.btcmap.ui

/**
 * The resolved labels the settings list renders. Both hosts build one from their
 * own resources (Android from `R.string`, the desktop from literals) and hand it
 * to [settingsItems], so the rows and their order cannot drift apart.
 *
 * The labels are already-resolved strings; the screen never touches platform
 * resources.
 */
data class SettingsStrings(
    val accountTitle: String,
    val accountSecondary: String,
    val mapStyle: String,
    val mapStyleValue: String,
    val customizeColors: String,
    val customizeColorsSecondary: String,
    val verifiedFilter: String,
    val verifiedFilterValue: String,
    val showAttribution: String,
    val showAttributionSecondary: String,
    val mapRotation: String,
    val mapRotationSecondary: String,
    val dbStats: String,
    val dbStatsSecondary: String,
    val imageStats: String,
    val imageStatsSecondary: String,
)

/**
 * The settings rows, in the order both hosts show them.
 *
 * The keys are stable and name the action a row triggers, so the Android
 * fragment and the desktop window dispatch on the same strings. The account row
 * is passed in already resolved because its text depends on the cached user,
 * which is read off the main thread.
 *
 * [includeImageStats] is false on the desktop, which has no image-cache
 * telemetry to show.
 */
fun settingsItems(
    strings: SettingsStrings,
    showAttribution: Boolean,
    mapRotationEnabled: Boolean,
    includeImageStats: Boolean = true,
): List<SettingsItem> = buildList {
    add(SettingsItem.Action("account", strings.accountTitle, strings.accountSecondary))
    add(SettingsItem.Action("mapStyle", strings.mapStyle, strings.mapStyleValue))
    add(
        SettingsItem.Action(
            "customizeColors",
            strings.customizeColors,
            strings.customizeColorsSecondary,
        )
    )
    add(
        SettingsItem.Action(
            "verifiedFilter",
            strings.verifiedFilter,
            strings.verifiedFilterValue,
        )
    )
    add(
        SettingsItem.Toggle(
            "showAttribution",
            strings.showAttribution,
            strings.showAttributionSecondary,
            showAttribution,
        )
    )
    add(
        SettingsItem.Toggle(
            "mapRotation",
            strings.mapRotation,
            strings.mapRotationSecondary,
            mapRotationEnabled,
        )
    )
    add(SettingsItem.Action("dbStats", strings.dbStats, strings.dbStatsSecondary))
    if (includeImageStats) {
        add(SettingsItem.Action("imageStats", strings.imageStats, strings.imageStatsSecondary))
    }
}
