package org.btcmap.ui

import androidx.compose.ui.text.font.FontFamily
import org.btcmap.api.ActivityFeedItem
import org.btcmap.imagestats.ImageStatsLabels
import org.btcmap.settings.ActivityInterval

/**
 * The host-resolved strings the shared root's screens draw. The Android host
 * builds it from `R.string`, the desktop from its own literals, so the screens
 * stay free of platform resources.
 *
 * Fields are added per screen as that screen moves into the root.
 */
data class AppLabels(
    /** A generic back affordance label, for [ScreenPage]. */
    val back: String,
    val eventReviewTitle: String,
    val eventReview: EventReviewLabels,
    val infraTitle: String,
    /** The infrastructure dashboard's refresh action. */
    val refresh: String,
    /** The boost screen's title, used when the place has no name. */
    val boostTitle: String,
    val boost: BoostScreenLabels,
    /** The clipboard label for a boost payment request. */
    val boostPaymentRequest: String,
    /** The add-comment screen's title, used when the place has no name. */
    val addCommentTitle: String,
    val addComment: CommentScreenLabels,
    /** The clipboard label for a comment payment request. */
    val commentPaymentRequest: String,
    /** The event screen's date formatting (see `EventScreen`). */
    val eventScreen: EventScreenLabels,
    /** The event screen's directions action. */
    val directions: String,
    val addPlace: AddPlaceLabels,
    val addEvent: AddEventLabels,
    val addNote: AddNoteLabels,
    val report: ReportPlaceLabels,
    val colorsTitle: String,
    val colors: ColorsPageLabels,
    val dbStatsTitle: String,
    val dbStats: DbStatsPageLabels,
    val imageStatsTitle: String,
    val imageStats: ImageStatsLabels,
    val feedTitle: String,
    val feedLocalTab: String,
    val feedSavedTab: String,
    val feedFilter: String,
    val feedAreasLabel: String,
    val feedIntervalLabel: String,
    val feedIntervalName: (ActivityInterval) -> String,
    val feedEmptyLocal: String,
    val feedEmptySavedSignedOut: String,
    val feedEmptySavedNoItems: String,
    val feedEmptySavedNoActivity: String,
    val feedError: String,
    /** Renders a feed item as a row, resolving the host's strings and date format. */
    val feedRow: (ActivityFeedItem) -> ActivityFeedRow,
    /** A generic confirmation label, for the feed filter's OK button. */
    val ok: String,
    /** The strings of the sheet a note pin opens (see `NoteSheet`). */
    val noteSheet: NoteSheetLabels,
    /** The strings of the sheet a tapped basemap POI opens (see `PoiSheet`). */
    val poiSheet: PoiSheetLabels,
    val area: AreaStrings,
    /** The current map style's display name, for the offline download dialog. */
    val offlineStyleName: String,
    /** The area screen's save action. */
    val save: String,
    val offlineDeleteTitle: String,
    val offlineDeleteMessage: String,
    /** The area screen's load-failure dialog. */
    val errorTitle: String,
    val errorMessage: String,
    val settingsTitle: String,
    val settings: SettingsPageLabels,
    /** The manage-users screen's title, shared by both hosts. */
    val manageUsersTitle: String,
    val manageUsers: ManageUsersLabels,
    val manageAreasTitle: String,
    val manageAreas: ManageAreasLabels,
    /** The manage-place-images screen's title, shared by both hosts. */
    val managePlaceImagesTitle: String,
    val managePlaceImages: ManagePlaceImagesLabels,
    /** The user admin screen's edit-roles action and dialog title. */
    val editRoles: String,
    /** Formats a role id (e.g. "event_manager") for the roles dialog. */
    val roleName: (String) -> String,
    /** The user admin screen's edit-geofence action and dialog title. */
    val editGeofence: String,
    /** The geofence editor's search field placeholder. */
    val geofenceSearch: String,
    /** Shown when the geofence search matches no area. */
    val geofenceNoMatches: String,
    /** The area admin screen's verify action. */
    val verifyArea: String,
    /** The area admin screen's name edit action and dialog title. */
    val editName: String,
    /** The area name field's label, in the edit dialog. */
    val nameField: String,
    /** The area admin screen's description edit action and dialog title. */
    val editDescription: String,
    /** The area description field's label, in the edit dialog. */
    val descriptionField: String,
    /** A generic cancel label, for the name edit dialog. */
    val cancel: String,
    val profileTitle: String,
    val uploadedImagesTitle: String,
    val myEventsTitle: String,
    val myNotesTitle: String,
    val savedPlacesTitle: String,
    val savedAreasTitle: String,
    val userProfile: UserProfileLabels,
    val profileForm: ProfileFormLabels,
    val uploadedImages: UploadedImagesLabels,
    val myEvents: MyEventsLabels,
    val myNotes: MyNotesLabels,
    val savedPlaces: SavedItemsLabels,
    val savedAreas: SavedItemsLabels,
    val account: AccountLabels,
    val placeStrings: PlaceSheetStrings,
    val addLocation: org.btcmap.ui.map.AddLocationLabels,
    /** The heading above the map search's OpenStreetMap results group. */
    val searchOpenStreetMap: String,
    /** The map's OpenStreetMap attribution line. */
    val osmAttribution: String,
    /** Formats a distance in metres for the map's search results. */
    val formatDistance: (Double) -> String,
)
