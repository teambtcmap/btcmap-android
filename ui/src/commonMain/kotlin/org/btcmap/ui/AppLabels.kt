package org.btcmap.ui

import androidx.compose.ui.graphics.Color
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
    val profileTitle: String,
    val uploadedImagesTitle: String,
    val myEventsTitle: String,
    val userProfile: UserProfileLabels,
    val profileForm: ProfileFormLabels,
    val uploadedImages: UploadedImagesLabels,
    val myEvents: MyEventsLabels,
    val account: AccountLabels,
    val placeStrings: PlaceSheetStrings,
    val addLocation: org.btcmap.ui.map.AddLocationLabels,
    /** The map's OpenStreetMap attribution line. */
    val osmAttribution: String,
    val osmAttributionColor: Color,
    /** Formats a distance in metres for the map's search results. */
    val formatDistance: (Double) -> String,
)
