package org.btcmap.nav

import android.content.Context
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import org.btcmap.R
import org.btcmap.area.areaStrings
import org.btcmap.boost.BoostPlan
import org.btcmap.dbstats.DbStatsLabels
import org.btcmap.feed.toRow
import org.btcmap.imagestats.ImageStatsLabels
import org.btcmap.place.ReportType
import org.btcmap.place.placeSheetStrings
import org.btcmap.settings.ActivityInterval
import org.btcmap.settings.MapColor
import org.btcmap.settings.mapStyle
import org.btcmap.settings.name as styleName
import org.btcmap.settings.prefs
import org.btcmap.settings.toVerifiedFilterYears
import org.btcmap.sync.SyncState
import org.btcmap.ui.AccountLabels
import org.btcmap.ui.AddCommentLabels
import org.btcmap.ui.AddEventLabels
import org.btcmap.ui.AddPlaceLabels
import org.btcmap.ui.AppLabels
import org.btcmap.ui.BoostScreenLabels
import org.btcmap.ui.ColorsPageLabels
import org.btcmap.ui.CommentScreenLabels
import org.btcmap.ui.DbStatsPageLabels
import org.btcmap.ui.EventReviewLabels
import org.btcmap.ui.EventScreenLabels
import org.btcmap.ui.InvoicePaymentLabels
import org.btcmap.ui.InvoicePaymentSectionLabels
import org.btcmap.ui.MyEventsLabels
import org.btcmap.ui.ProfileFormLabels
import org.btcmap.ui.ReportPlaceLabels
import org.btcmap.ui.SettingsPageLabels
import org.btcmap.ui.UploadedImagesLabels
import org.btcmap.ui.UserProfileLabels
import org.btcmap.ui.map.AddLocationLabels
import java.text.NumberFormat

/**
 * Builds the shared [AppLabels] from the app's resources, so the Compose root
 * stays free of `R.string`. Each screen that moves into the root adds its labels
 * here; the label construction is the same code the fragment used to hold.
 */
internal fun Context.androidAppLabels(): AppLabels = AppLabels(
    back = getString(R.string.navigate_up),
    eventReviewTitle = getString(R.string.event_review_title),
    eventReview = EventReviewLabels(
        back = getString(R.string.navigate_up),
        empty = getString(R.string.event_review_empty),
        failed = getString(R.string.event_review_failed),
        retry = getString(R.string.retry),
        approve = getString(R.string.event_review_approve),
        reject = getString(R.string.event_review_reject),
        actionFailed = getString(R.string.event_review_action_failed),
        dateRange = { date, start, end ->
            getString(R.string.event_date_time_range, date, start, end)
        },
    ),
    infraTitle = getString(R.string.infra_dashboard),
    refresh = getString(R.string.refresh),
    boostTitle = getString(R.string.boost_merchant),
    boost = BoostScreenLabels(
        active = getString(R.string.your_boost_is_active),
        backToMap = getString(R.string.back_to_map),
        planLabel = { getString(it.labelRes()) },
        description = getString(R.string.boost_description),
        durationTitle = getString(R.string.boost_duration),
        continueLabel = getString(R.string.btn_continue),
        invoice = invoiceLabels(),
    ),
    boostPaymentRequest = getString(R.string.btc_map_boost_payment_request),
    addCommentTitle = getString(R.string.add_comment),
    addComment = CommentScreenLabels(
        posted = getString(R.string.your_comment_has_been_posted),
        backToMap = getString(R.string.back_to_map),
        form = AddCommentLabels(
            disclosure = getString(R.string.add_element_comment_disclosure_1),
            currentFee = getString(R.string.current_fee),
            comment = getString(R.string.comment),
            placeholder = getString(R.string.comment_placeholder),
            continueLabel = getString(R.string.btn_continue),
            emptyComment = getString(R.string.comment_cannot_be_empty),
            failedToLoad = getString(R.string.failed_to_load),
            tapToRetry = getString(R.string.tap_to_retry),
        ),
        invoice = invoiceLabels(),
    ),
    commentPaymentRequest = getString(R.string.btc_map_comment_payment_request),
    eventScreen = EventScreenLabels(
        dateRange = { date, start, end ->
            getString(R.string.event_date_time_range, date, start, end)
        },
    ),
    directions = getString(R.string.directions),
    addPlace = AddPlaceLabels(
        title = getString(R.string.add_place_title),
        back = getString(R.string.navigate_up),
        name = getString(R.string.name),
        namePlaceholder = getString(R.string.name_placeholder),
        category = getString(R.string.category),
        categoryPlaceholder = getString(R.string.category_placeholder),
        address = getString(R.string.address),
        addressPlaceholder = getString(R.string.address_placeholder),
        website = getString(R.string.website_optional),
        websitePlaceholder = getString(R.string.website_placeholder),
        description = getString(R.string.description_optional),
        descriptionPlaceholder = getString(R.string.description_placeholder),
        dragMap = getString(R.string.add_location_drag_to_adjust),
        required = getString(R.string.field_required),
        submit = getString(R.string.submit_place),
        submitted = getString(R.string.place_submitted),
        backToMap = getString(R.string.back_to_map),
    ),
    addEvent = AddEventLabels(
        title = getString(R.string.add_event_title),
        back = getString(R.string.navigate_up),
        name = getString(R.string.name),
        namePlaceholder = getString(R.string.event_name_placeholder),
        website = getString(R.string.event_website),
        websitePlaceholder = getString(R.string.website_placeholder),
        startsAt = getString(R.string.event_starts_at),
        endsAt = getString(R.string.event_ends_at),
        selectDateTime = getString(R.string.event_select_date_time),
        clearEnd = getString(R.string.event_clear_end),
        dragMap = getString(R.string.add_location_drag_to_adjust),
        required = getString(R.string.field_required),
        submit = getString(R.string.submit_event),
        submitted = getString(R.string.event_submitted),
        backToMap = getString(R.string.back_to_map),
        ok = getString(R.string.ok),
        cancel = getString(R.string.cancel),
    ),
    report = ReportPlaceLabels(
        intro = getString(R.string.verify_or_report_description),
        reasonLabel = { getString(it.labelRes()) },
        reasonDescription = { getString(it.descriptionRes()) },
        noteHint = getString(R.string.report_comment_optional),
        addPhoto = { _, _ -> getString(R.string.add_photo) },
        removePhoto = getString(R.string.delete),
        submit = getString(R.string.btn_submit),
        submitted = getString(R.string.report_submitted),
        backToMap = getString(R.string.back_to_map),
    ),
    colorsTitle = getString(R.string.customize_colors),
    colors = ColorsPageLabels(
        colorTitle = { getString(it.titleRes()) },
        red = getString(R.string.color_picker_red),
        green = getString(R.string.color_picker_green),
        blue = getString(R.string.color_picker_blue),
        alpha = getString(R.string.color_picker_alpha),
        ok = getString(android.R.string.ok),
        reset = getString(R.string.reset),
        cancel = getString(android.R.string.cancel),
    ),
    dbStatsTitle = getString(R.string.database_stats),
    dbStats = DbStatsPageLabels(
        dbStats = DbStatsLabels(
            database = getString(R.string.db_stats_database),
            file = getString(R.string.db_stats_file_name),
            version = getString(R.string.db_stats_version),
            size = getString(R.string.db_stats_size),
            table = { getString(R.string.db_stats_table, it) },
            bundle = { getString(R.string.db_stats_bundle, it) },
            location = getString(R.string.db_stats_location),
            visibleRows = getString(R.string.db_stats_visible_rows),
            deletedRows = getString(R.string.db_stats_deleted_rows),
            futureRows = getString(R.string.db_stats_future_rows),
            rows = getString(R.string.db_stats_rows),
            newestUpdate = getString(R.string.db_stats_max_updated_at),
        ),
        sync = getString(R.string.db_stats_sync),
        source = getString(R.string.db_stats_sync_source),
        state = getString(R.string.db_stats_sync_state),
        syncNow = getString(R.string.sync),
        syncStateLabel = { getString(it.labelRes()) },
    ),
    imageStatsTitle = getString(R.string.image_stats),
    imageStats = ImageStatsLabels(
        memoryCache = getString(R.string.image_stats_memory_cache),
        diskCache = getString(R.string.image_stats_disk_cache),
        loads = getString(R.string.image_stats_loads),
        entries = getString(R.string.image_stats_entries),
        size = getString(R.string.image_stats_size),
        location = getString(R.string.image_stats_location),
        requests = getString(R.string.image_stats_requests),
        memoryHits = getString(R.string.image_stats_memory_hits),
        diskHits = getString(R.string.image_stats_disk_hits),
        networkLoads = getString(R.string.image_stats_network_loads),
        cacheHitRate = getString(R.string.image_stats_cache_hit_rate),
        errors = getString(R.string.image_stats_errors),
        cancels = getString(R.string.image_stats_cancels),
        averageLoad = getString(R.string.image_stats_average_load),
        percent = { getString(R.string.image_stats_percent, it) },
        millis = { getString(R.string.image_stats_millis, it) },
        sizeOf = { used, max ->
            getString(
                R.string.image_stats_size_of,
                Formatter.formatFileSize(this@androidAppLabels, used),
                Formatter.formatFileSize(this@androidAppLabels, max),
            )
        },
    ),
    feedTitle = getString(R.string.activity_feed_title),
    feedLocalTab = getString(R.string.activity_tab_local),
    feedSavedTab = getString(R.string.activity_tab_saved),
    feedFilter = getString(R.string.filter),
    feedAreasLabel = getString(R.string.activity_filter_areas),
    feedIntervalLabel = getString(R.string.activity_interval),
    feedIntervalName = { interval ->
        when (interval) {
            ActivityInterval.Day -> getString(R.string.activity_interval_day)
            ActivityInterval.Week -> getString(R.string.activity_interval_week)
            ActivityInterval.Month -> getString(R.string.activity_interval_month)
            ActivityInterval.HalfYear -> getString(R.string.activity_interval_half_year)
            ActivityInterval.Year -> getString(R.string.activity_interval_year)
        }
    },
    feedEmptyLocal = getString(R.string.activity_empty_local),
    feedEmptySavedSignedOut = getString(R.string.activity_empty_saved_signed_out),
    feedEmptySavedNoItems = getString(R.string.activity_empty_saved_no_items),
    feedEmptySavedNoActivity = getString(R.string.activity_empty_saved_no_activity),
    feedError = getString(
        R.string.failed_to_load_tap_to_retry,
        getString(R.string.failed_to_load),
        getString(R.string.tap_to_retry),
    ),
    feedRow = { item -> item.toRow(this@androidAppLabels) },
    ok = getString(android.R.string.ok),
    area = areaStrings(),
    offlineStyleName = prefs.mapStyle.styleName(this@androidAppLabels),
    save = getString(R.string.save),
    offlineDeleteTitle = getString(R.string.offline_map_delete_title),
    offlineDeleteMessage = getString(R.string.offline_map_delete_message),
    errorTitle = getString(R.string.error),
    errorMessage = getString(R.string.error),
    settingsTitle = getString(R.string.settings),
    settings = SettingsPageLabels(
        account = getString(R.string.not_logged_in),
        logIn = getString(R.string.create_account),
        loggedInAs = { getString(R.string.logged_in_as, it) },
        openProfile = getString(R.string.click_to_see_your_profile),
        mapStyle = getString(R.string.map_style),
        mapStyleValue = { it.styleName(this@androidAppLabels) },
        customizeColors = getString(R.string.customize_colors),
        customizeColorsSecondary = getString(R.string.customize_colors_secondary),
        verifiedFilter = getString(R.string.verified_filter),
        verifiedFilterValue = { it.toVerifiedFilterYears(this@androidAppLabels) },
        verifiedFilterYears = listOf(1, 2, 3),
        showAttribution = getString(R.string.show_attribution),
        showAttributionSecondary = getString(R.string.show_attribution_secondary),
        mapRotation = getString(R.string.map_rotation),
        mapRotationSecondary = getString(R.string.map_rotation_secondary),
        dbStats = getString(R.string.database_stats),
        dbStatsSecondary = getString(R.string.database_stats_secondary),
        imageStats = getString(R.string.image_stats),
        imageStatsSecondary = getString(R.string.image_stats_secondary),
        mapStyleDialogTitle = getString(R.string.map_style),
        verifiedFilterDialogTitle = getString(R.string.verified_filter),
        close = getString(R.string.close),
    ),
    profileTitle = getString(R.string.profile),
    uploadedImagesTitle = getString(R.string.uploaded_images),
    myEventsTitle = getString(R.string.my_events),
    userProfile = UserProfileLabels(
        username = getString(R.string.username),
        password = getString(R.string.password),
        savedPlaces = getString(R.string.saved_places),
        savedAreas = getString(R.string.saved_areas),
        noSavedPlaces = getString(R.string.no_saved_places),
        noSavedAreas = getString(R.string.no_saved_areas),
        logOut = getString(R.string.logout),
        editUsername = getString(R.string.change_username),
        editPassword = getString(R.string.change_password),
        delete = getString(R.string.delete),
        uploadedImages = getString(R.string.uploaded_images),
        myEvents = getString(R.string.my_events),
    ),
    profileForm = ProfileFormLabels(
        passwordMask = getString(R.string.password_mask),
        required = getString(R.string.field_required),
        changeUsernameTitle = getString(R.string.change_username),
        changePasswordTitle = getString(R.string.change_password),
        username = getString(R.string.username),
        currentPassword = getString(R.string.current_password),
        newPassword = getString(R.string.new_password),
        confirmPassword = getString(R.string.confirm_password),
        passwordsDoNotMatch = getString(R.string.passwords_do_not_match),
        passwordTooShort = { getString(R.string.password_min_length, it) },
        save = getString(R.string.save),
        cancel = getString(android.R.string.cancel),
        usernameChanged = getString(R.string.username_changed),
        passwordChanged = getString(R.string.password_changed),
    ),
    uploadedImages = UploadedImagesLabels(
        empty = getString(R.string.uploaded_images_empty),
        delete = getString(R.string.delete),
        failed = getString(R.string.uploaded_images_failed),
        retry = getString(R.string.retry),
        unknownPlace = { getString(R.string.uploaded_images_place, it) },
    ),
    myEvents = MyEventsLabels(
        empty = getString(R.string.my_events_empty),
        failed = getString(R.string.my_events_failed),
        retry = getString(R.string.retry),
        statusPending = getString(R.string.event_status_pending),
        statusLive = getString(R.string.event_status_live),
        statusRejected = getString(R.string.event_status_rejected),
        revoke = getString(R.string.event_revoke),
        revokeFailed = getString(R.string.event_revoke_failed),
        duplicate = getString(R.string.event_duplicate),
        dateRange = { date, start, end ->
            getString(R.string.event_date_time_range, date, start, end)
        },
    ),
    account = AccountLabels(
        username = getString(R.string.username),
        password = getString(R.string.password),
        confirmPassword = getString(R.string.confirm_password),
        required = getString(R.string.field_required),
        passwordTooShort = { getString(R.string.password_min_length, it) },
        passwordsDoNotMatch = getString(R.string.passwords_do_not_match),
        signIn = getString(R.string.login),
        createAccount = getString(R.string.sign_up),
        alreadyHaveAccount = getString(R.string.log_in_with_existing_account),
        createAnAccount = getString(R.string.i_don_t_have_an_account),
        accountCreated = getString(R.string.account_created_sign_in_failed),
    ),
    placeStrings = placeSheetStrings(),
    addLocation = AddLocationLabels(
        addPlace = getString(R.string.add_place_title),
        addEvent = getString(R.string.add_event),
    ),
    osmAttribution = getString(R.string.osm_attribution),
    osmAttributionColor = Color(
        ContextCompat.getColor(this@androidAppLabels, R.color.osm_attribution_text),
    ),
    formatDistance = { meters ->
        val format = NumberFormat.getNumberInstance().apply {
            // Beyond 10 km the fraction is noise.
            maximumFractionDigits = if (meters > 10_000) 0 else 1
        }
        if (meters < 1_000) {
            getString(R.string.s_m, format.format(meters))
        } else {
            getString(R.string.s_km, format.format(meters / 1_000))
        }
    },
)

/** The Lightning invoice strings shared by the boost and comment screens. */
private fun Context.invoiceLabels(): InvoicePaymentSectionLabels = InvoicePaymentSectionLabels(
    invoice = InvoicePaymentLabels(
        qrDescription = getString(R.string.qr_code),
        pay = getString(R.string.pay),
        copy = getString(android.R.string.copy),
        startOver = getString(R.string.start_over),
    ),
    discardMessage = getString(R.string.discard_invoice_confirmation),
    discard = getString(R.string.start_over),
    cancel = getString(android.R.string.cancel),
)

/** The Android label resource for each shared boost plan. */
@StringRes
private fun BoostPlan.labelRes(): Int = when (this) {
    BoostPlan.ONE_MONTH -> R.string.months_1
    BoostPlan.THREE_MONTHS -> R.string.months_3
    BoostPlan.TWELVE_MONTHS -> R.string.months_12
}

/** The Android label resource for each report reason. */
@StringRes
private fun ReportType.labelRes(): Int = when (this) {
    ReportType.Verified -> R.string.report_type_verified
    ReportType.RefusedSats -> R.string.report_type_refused_sats
    ReportType.OutOfBusiness -> R.string.report_type_out_of_business
}

/** The Android description resource for each report reason. */
@StringRes
private fun ReportType.descriptionRes(): Int = when (this) {
    ReportType.Verified -> R.string.report_type_verified_description
    ReportType.RefusedSats -> R.string.report_type_refused_sats_description
    ReportType.OutOfBusiness -> R.string.report_type_out_of_business_description
}

/** The Android label resource for each customizable colour. */
@StringRes
private fun MapColor.titleRes(): Int = when (this) {
    MapColor.MarkerBackground -> R.string.marker_background_color
    MapColor.MarkerIcon -> R.string.marker_icon_color
    MapColor.BoostedMarkerBackground -> R.string.boosted_marker_background
    MapColor.BoostedMarkerIcon -> R.string.boosted_marker_icon
    MapColor.BadgeBackground -> R.string.badge_background
    MapColor.BadgeText -> R.string.badge_text
    MapColor.ButtonBackground -> R.string.button_background
    MapColor.ButtonIcon -> R.string.button_icon
    MapColor.ButtonBorder -> R.string.button_border
}

/** The Android label resource for each sync state. */
@StringRes
private fun SyncState.labelRes(): Int = when (this) {
    SyncState.Idle -> R.string.db_stats_sync_state_idle
    SyncState.UnbundlingPlaces -> R.string.db_stats_sync_state_unbundling_places
    SyncState.SyncingPlaces -> R.string.db_stats_sync_state_syncing_places
    SyncState.UnbundlingEvents -> R.string.db_stats_sync_state_unbundling_events
    SyncState.SyncingEvents -> R.string.db_stats_sync_state_syncing_events
    SyncState.UnbundlingComments -> R.string.db_stats_sync_state_unbundling_comments
    SyncState.SyncingComments -> R.string.db_stats_sync_state_syncing_comments
    SyncState.UnbundlingAreas -> R.string.db_stats_sync_state_unbundling_areas
    SyncState.SyncingAreas -> R.string.db_stats_sync_state_syncing_areas
}
