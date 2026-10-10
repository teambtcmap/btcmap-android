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
            MapStyle.Auto -> "Auto (system)"
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
    mapTilt = "Allow map tilt",
    mapTiltSecondary = "Tilt the map for a bird's-eye view",
    dbStats = "Database",
    dbStatsSecondary = "Database metadata and table management",
    imageStats = "Image cache",
    imageStatsSecondary = "Memory and disk caches metadata plus load stats",
    manageUsers = "Manage users",
    manageUsersSecondary = "Look up accounts by name",
    manageAreas = "Manage areas",
    manageAreasSecondary = "View and edit area data",
    managePlaceImages = "Manage place images",
    managePlaceImagesSecondary = "Review recent uploads and remove spam",
    sectionMap = "Map",
    sectionData = "Data",
    sectionAdmin = "Admin",
    sectionGeneral = "General",
    language = "Language",
    languageDialogTitle = "Language",
    languageSystemDefault = "System default",
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
    uploadedImages = "Uploaded images",
    myEvents = "My events",
    myNotes = "My notes",
)

internal val TEST_UPLOADED_IMAGES_LABELS = UploadedImagesLabels(
    empty = "You haven't uploaded any images yet.",
    delete = "Delete",
    failed = "Couldn't delete the image.",
    retry = "Retry",
    unknownPlace = { "Place #$it" },
)

internal val TEST_MY_EVENTS_LABELS = MyEventsLabels(
    empty = "You haven't submitted any events yet.",
    failed = "Couldn't load your events",
    retry = "Retry",
    duplicate = "Duplicate",
    revoke = "Revoke",
    revokeFailed = "Couldn't revoke the event",
    statusPending = "Pending review",
    statusLive = "Live",
    statusRejected = "Rejected",
    dateRange = { date, start, end -> "$date, $start - $end" },
)

internal val TEST_MY_NOTES_LABELS = MyNotesLabels(
    empty = "You haven't added any notes yet.",
    failed = "Couldn't load your notes",
    retry = "Retry",
    public = "Public",
    private = "Private",
    delete = "Delete",
    actionFailed = "Couldn't update the note",
    openOnMap = "Show on map",
)

internal val TEST_ADD_NOTE_LABELS = AddNoteLabels(
    title = "Add a note",
    back = "Navigate up",
    text = "Note",
    textPlaceholder = "e.g. ATM is inside, ask at the bar",
    icon = "Icon",
    iconSearchHint = "Search icons",
    private = "Private",
    public = "Public",
    privateDescription = "Only you can see your private notes",
    publicDescription = "Public notes can be seen by anyone",
    dragMap = "Drag the map to set the exact location",
    required = "Required",
    submit = "Add note",
    submitted = "Note added",
    backToMap = "Back to the map",
    error = ErrorDialogLabels(title = "Error", ok = "OK"),
)

internal val TEST_EVENT_REVIEW_LABELS = EventReviewLabels(
    back = "Navigate up",
    empty = "No events are waiting for review",
    failed = "Couldn't load the events",
    retry = "Retry",
    approve = "Approve",
    reject = "Reject",
    actionFailed = "Couldn't update the event",
    dateRange = { date, start, end -> "$date, $start - $end" },
)

internal val TEST_EVENT_SCREEN_LABELS = EventScreenLabels(
    dateRange = { date, start, end -> "$date, $start - $end" },
    zoomIn = "Zoom in",
    zoomOut = "Zoom out",
    directions = "Directions",
    delete = "Delete event",
    deleteConfirmTitle = "Delete this event?",
    deleteConfirmMessage = "This event will be removed for everyone.",
    deleteFailed = "Couldn't delete the event",
    cancel = "Cancel",
)
