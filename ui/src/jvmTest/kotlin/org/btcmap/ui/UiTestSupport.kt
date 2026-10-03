package org.btcmap.ui

import kotlinx.coroutines.runBlocking
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.btcmap.api.Api
import org.btcmap.api.apiHttpClient
import org.btcmap.db.Database
import org.btcmap.settings.MapStyle
import org.btcmap.settings.Settings
import org.btcmap.util.toUrl
import java.nio.file.Files

/**
 * Test doubles for the shared screens: an isolated database, its settings, and
 * an [Api] that is never called. Rendering a screen only reads the settings,
 * so the screens can be clicked through without a server or a window.
 */

internal fun testDatabase(): Database = runBlocking {
    Database(BundledSQLiteDriver(), Files.createTempFile("ui-test", ".db").toString()).apply { connect() }
}

internal fun testSettings(db: Database): Settings = runBlocking {
    Settings(dbProvider = { db }, legacyValues = { emptyMap() }).apply { preload() }
}

internal fun testApi(): Api = Api(
    httpClient = apiHttpClient("btcmap-ui-test"),
    baseUrl = { "https://api.example".toUrl() },
    token = { null },
    userAgent = "btcmap-ui-test",
)

internal val TEST_PROFILE_FORM_LABELS = ProfileFormLabels(
    passwordMask = "••••••••",
    required = "Required",
    changeUsernameTitle = "Change username",
    changePasswordTitle = "Change password",
    username = "Username",
    currentPassword = "Current password",
    newPassword = "New password",
    confirmPassword = "Confirm password",
    passwordsDoNotMatch = "Passwords do not match",
    passwordTooShort = { "At least $it characters" },
    save = "Save",
    cancel = "Cancel",
    usernameChanged = "Username changed.",
    passwordChanged = "Password changed.",
)

internal val TEST_INVOICE_SECTION_LABELS = InvoicePaymentSectionLabels(
    invoice = InvoicePaymentLabels(
        qrDescription = "Lightning invoice QR code",
        pay = "Pay",
        copy = "Copy",
        startOver = "Start over",
    ),
    discardMessage = "Discard this invoice?",
    discard = "Discard",
    cancel = "Cancel",
)

internal val TEST_SETTINGS_PAGE_LABELS = SettingsPageLabels(
    account = "Account",
    logIn = "Log in",
    loggedInAs = { "Logged in as $it" },
    openProfile = "Click to see your profile",
    mapStyle = "Map style",
    mapStyleValue = {
        when (it) {
            MapStyle.Auto -> "Auto"
            MapStyle.Liberty -> "OpenFreeMap Liberty"
            MapStyle.Positron -> "OpenFreeMap Positron"
            MapStyle.Bright -> "OpenFreeMap Bright"
            MapStyle.Dark -> "OpenFreeMap Dark"
            MapStyle.DarkMatter -> "OpenFreeMap Dark Matter"
        }
    },
    customizeColors = "Customize colors",
    customizeColorsSecondary = "Marker, badge and button colors",
    verifiedFilter = "Only show places",
    verifiedFilterValue = { "Verified within $it years" },
    verifiedFilterYears = listOf(1, 2, 3),
    showAttribution = "Show attribution",
    showAttributionSecondary = "Visible by default per OSM policy",
    mapRotation = "Allow map rotation",
    mapRotationSecondary = "Compass appears when not facing north",
    dbStats = "Database",
    dbStatsSecondary = "Database metadata and table management",
    imageStats = "Image cache",
    imageStatsSecondary = "Memory and disk caches metadata plus load stats",
    mapStyleDialogTitle = "Map style",
    verifiedFilterDialogTitle = "Only show places",
    close = "Close",
)

internal val TEST_PROFILE_LABELS = UserProfileLabels(
    username = "Username",
    password = "Password",
    savedPlaces = "Saved places",
    savedAreas = "Saved areas",
    noSavedPlaces = "No saved places.",
    noSavedAreas = "No saved areas.",
    logOut = "Log out",
    editUsername = "Change username",
    editPassword = "Change password",
    delete = "Delete",
)
