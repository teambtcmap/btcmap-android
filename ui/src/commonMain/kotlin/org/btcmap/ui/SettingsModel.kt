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
    val mapTilt: String,
    val mapTiltSecondary: String,
    val dbStats: String,
    val dbStatsSecondary: String,
    val imageStats: String,
    val imageStatsSecondary: String,
    val manageAreas: String,
    val manageAreasSecondary: String,
    val sectionMap: String,
    val sectionData: String,
    val sectionAdmin: String,
)

/**
 * The settings entries, in the order both hosts show them: a group
 * [SettingsItem.Header] followed by the rows under it.
 *
 * The keys are stable and name the action a row triggers, so the Android
 * fragment and the desktop window dispatch on the same strings; a header key is
 * its identity only. Each row's icon is a Material Symbols glyph name. The
 * account row is passed in already resolved because its text depends on the
 * cached user, which is read off the main thread.
 *
 * [includeImageStats] is false on the desktop, which has no image-cache
 * telemetry to show. [includeManageAreas] is true only for the area-admins the
 * page resolves the cached user to; its header is added only alongside the row.
 */
fun settingsItems(
    strings: SettingsStrings,
    showAttribution: Boolean,
    mapRotationEnabled: Boolean,
    mapTiltEnabled: Boolean,
    includeImageStats: Boolean = true,
    includeManageAreas: Boolean = false,
): List<SettingsItem> = buildList {
    add(
        SettingsItem.Action(
            "account",
            strings.accountTitle,
            strings.accountSecondary,
            icon = "account_circle",
        )
    )
    add(SettingsItem.Header("headerMap", strings.sectionMap))
    add(
        SettingsItem.Action(
            "mapStyle",
            strings.mapStyle,
            strings.mapStyleValue,
            icon = "map",
        )
    )
    add(
        SettingsItem.Action(
            "customizeColors",
            strings.customizeColors,
            strings.customizeColorsSecondary,
            icon = "palette",
        )
    )
    add(
        SettingsItem.Action(
            "verifiedFilter",
            strings.verifiedFilter,
            strings.verifiedFilterValue,
            icon = "verified",
        )
    )
    add(
        SettingsItem.Toggle(
            "showAttribution",
            strings.showAttribution,
            strings.showAttributionSecondary,
            showAttribution,
            icon = "copyright",
        )
    )
    add(
        SettingsItem.Toggle(
            "mapRotation",
            strings.mapRotation,
            strings.mapRotationSecondary,
            mapRotationEnabled,
            icon = "explore",
        )
    )
    add(
        SettingsItem.Toggle(
            "mapTilt",
            strings.mapTilt,
            strings.mapTiltSecondary,
            mapTiltEnabled,
            icon = "view_in_ar",
        )
    )
    add(SettingsItem.Header("headerData", strings.sectionData))
    add(
        SettingsItem.Action(
            "dbStats",
            strings.dbStats,
            strings.dbStatsSecondary,
            icon = "database",
        )
    )
    if (includeImageStats) {
        add(
            SettingsItem.Action(
                "imageStats",
                strings.imageStats,
                strings.imageStatsSecondary,
                icon = "image",
            )
        )
    }
    if (includeManageAreas) {
        add(SettingsItem.Header("headerAdmin", strings.sectionAdmin))
        add(
            SettingsItem.Action(
                "manageAreas",
                strings.manageAreas,
                strings.manageAreasSecondary,
                icon = "travel_explore",
            )
        )
    }
}
