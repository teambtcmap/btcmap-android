package org.btcmap.ui

import org.btcmap.boost.BoostPlan
import org.btcmap.dbstats.DbStatsLabels
import org.btcmap.i18n.Strings
import org.btcmap.imagestats.ImageStatsLabels
import org.btcmap.place.ReportType
import org.btcmap.settings.ActivityInterval
import org.btcmap.settings.MapColor
import org.btcmap.settings.MapStyle
import org.btcmap.sync.SyncState
import org.btcmap.ui.map.AddLocationLabels

/**
 * Builds the shared [AppLabels] from the cross-platform [strings], so the Android
 * and desktop hosts render identical text. Only true formatting (byte sizes,
 * distances, dates) is delegated back to the host, since the two platforms
 * format those differently.
 */
fun appLabels(
    strings: Strings,
    currentStyle: MapStyle,
    formatNumber: (value: Double, maximumFractionDigits: Int) -> String,
    formatBytes: (Long) -> String,
    formatFeedDate: (String) -> String,
): AppLabels = AppLabels(
    back = strings["navigate_up"],
    eventReviewTitle = strings["event_review_title"],
    eventReview = EventReviewLabels(
        back = strings["navigate_up"],
        empty = strings["event_review_empty"],
        failed = strings["event_review_failed"],
        retry = strings["retry"],
        approve = strings["event_review_approve"],
        reject = strings["event_review_reject"],
        actionFailed = strings["event_review_action_failed"],
        dateRange = { date, start, end ->
            strings.format("event_date_time_range", date, start, end)
        },
    ),
    infraTitle = strings["infra_dashboard"],
    refresh = strings["refresh"],
    boostTitle = strings["boost_merchant"],
    boost = BoostScreenLabels(
        active = strings["your_boost_is_active"],
        backToMap = strings["back_to_map"],
        planLabel = { strings[it.labelKey()] },
        description = strings["boost_description"],
        durationTitle = strings["boost_duration"],
        continueLabel = strings["btn_continue"],
        invoice = invoiceLabels(strings),
    ),
    boostPaymentRequest = strings["btc_map_boost_payment_request"],
    addCommentTitle = strings["add_comment"],
    addComment = CommentScreenLabels(
        posted = strings["your_comment_has_been_posted"],
        backToMap = strings["back_to_map"],
        form = AddCommentLabels(
            disclosure = strings["add_element_comment_disclosure_1"],
            currentFee = strings["current_fee"],
            comment = strings["comment"],
            placeholder = strings["comment_placeholder"],
            continueLabel = strings["btn_continue"],
            emptyComment = strings["comment_cannot_be_empty"],
            failedToLoad = strings["failed_to_load"],
            tapToRetry = strings["tap_to_retry"],
        ),
        invoice = invoiceLabels(strings),
    ),
    commentPaymentRequest = strings["btc_map_comment_payment_request"],
    eventScreen = EventScreenLabels(
        dateRange = { date, start, end ->
            strings.format("event_date_time_range", date, start, end)
        },
        zoomIn = strings["zoom_in"],
        zoomOut = strings["zoom_out"],
    ),
    directions = strings["directions"],
    addPlace = AddPlaceLabels(
        title = strings["add_place_title"],
        back = strings["navigate_up"],
        name = strings["name"],
        namePlaceholder = strings["name_placeholder"],
        category = strings["category"],
        categoryPlaceholder = strings["category_placeholder"],
        address = strings["address"],
        addressPlaceholder = strings["address_placeholder"],
        website = strings["website_optional"],
        websitePlaceholder = strings["website_placeholder"],
        description = strings["description_optional"],
        descriptionPlaceholder = strings["description_placeholder"],
        dragMap = strings["add_location_drag_to_adjust"],
        required = strings["field_required"],
        submit = strings["submit_place"],
        submitted = strings["place_submitted"],
        backToMap = strings["back_to_map"],
    ),
    addEvent = AddEventLabels(
        title = strings["add_event_title"],
        back = strings["navigate_up"],
        name = strings["name"],
        namePlaceholder = strings["event_name_placeholder"],
        website = strings["event_website"],
        websitePlaceholder = strings["website_placeholder"],
        startsAt = strings["event_starts_at"],
        endsAt = strings["event_ends_at"],
        selectDateTime = strings["event_select_date_time"],
        clearEnd = strings["event_clear_end"],
        dragMap = strings["add_location_drag_to_adjust"],
        required = strings["field_required"],
        submit = strings["submit_event"],
        submitted = strings["event_submitted"],
        backToMap = strings["back_to_map"],
        ok = strings["ok"],
        cancel = strings["cancel"],
    ),
    addNote = AddNoteLabels(
        title = strings["add_note_title"],
        back = strings["navigate_up"],
        text = strings["note_text"],
        textPlaceholder = strings["note_text_placeholder"],
        private = strings["note_private"],
        public = strings["note_public"],
        privateDescription = strings["note_private_description"],
        publicDescription = strings["note_public_description"],
        dragMap = strings["add_location_drag_to_adjust"],
        required = strings["field_required"],
        submit = strings["submit_note"],
        submitted = strings["note_submitted"],
        backToMap = strings["back_to_map"],
    ),
    report = ReportPlaceLabels(
        intro = strings["verify_or_report_description"],
        reasonLabel = { strings[it.labelKey()] },
        reasonDescription = { strings[it.descriptionKey()] },
        noteHint = strings["report_comment_optional"],
        addPhoto = { _, _ -> strings["add_photo"] },
        removePhoto = strings["delete"],
        submit = strings["btn_submit"],
        submitted = strings["report_submitted"],
        backToMap = strings["back_to_map"],
    ),
    colorsTitle = strings["customize_colors"],
    colors = ColorsPageLabels(
        colorTitle = { strings[it.titleKey()] },
        red = strings["color_picker_red"],
        green = strings["color_picker_green"],
        blue = strings["color_picker_blue"],
        alpha = strings["color_picker_alpha"],
        ok = strings["ok"],
        reset = strings["reset"],
        cancel = strings["cancel"],
    ),
    dbStatsTitle = strings["database_stats"],
    dbStats = DbStatsPageLabels(
        dbStats = DbStatsLabels(
            database = strings["db_stats_database"],
            file = strings["db_stats_file_name"],
            version = strings["db_stats_version"],
            size = strings["db_stats_size"],
            table = { strings.format("db_stats_table", it) },
            bundle = { strings.format("db_stats_bundle", it) },
            location = strings["db_stats_location"],
            visibleRows = strings["db_stats_visible_rows"],
            deletedRows = strings["db_stats_deleted_rows"],
            futureRows = strings["db_stats_future_rows"],
            rows = strings["db_stats_rows"],
            newestUpdate = strings["db_stats_max_updated_at"],
        ),
        sync = strings["db_stats_sync"],
        source = strings["db_stats_sync_source"],
        state = strings["db_stats_sync_state"],
        syncNow = strings["sync"],
        syncStateLabel = { strings[it.labelKey()] },
    ),
    imageStatsTitle = strings["image_stats"],
    imageStats = ImageStatsLabels(
        memoryCache = strings["image_stats_memory_cache"],
        diskCache = strings["image_stats_disk_cache"],
        loads = strings["image_stats_loads"],
        entries = strings["image_stats_entries"],
        size = strings["image_stats_size"],
        location = strings["image_stats_location"],
        requests = strings["image_stats_requests"],
        memoryHits = strings["image_stats_memory_hits"],
        diskHits = strings["image_stats_disk_hits"],
        networkLoads = strings["image_stats_network_loads"],
        cacheHitRate = strings["image_stats_cache_hit_rate"],
        errors = strings["image_stats_errors"],
        cancels = strings["image_stats_cancels"],
        averageLoad = strings["image_stats_average_load"],
        percent = { strings.format("image_stats_percent", it) },
        millis = { strings.format("image_stats_millis", it) },
        sizeOf = { used, max ->
            strings.format("image_stats_size_of", formatBytes(used), formatBytes(max))
        },
    ),
    feedTitle = strings["activity_feed_title"],
    feedLocalTab = strings["activity_tab_local"],
    feedSavedTab = strings["activity_tab_saved"],
    feedFilter = strings["filter"],
    feedAreasLabel = strings["activity_filter_areas"],
    feedIntervalLabel = strings["activity_interval"],
    feedIntervalName = { strings[it.labelKey()] },
    feedEmptyLocal = strings["activity_empty_local"],
    feedEmptySavedSignedOut = strings["activity_empty_saved_signed_out"],
    feedEmptySavedNoItems = strings["activity_empty_saved_no_items"],
    feedEmptySavedNoActivity = strings["activity_empty_saved_no_activity"],
    feedError = strings.format(
        "failed_to_load_tap_to_retry",
        strings["failed_to_load"],
        strings["tap_to_retry"],
    ),
    feedRow = { item -> activityFeedRow(strings, item, formatFeedDate) },
    ok = strings["ok"],
    noteDialog = NoteDialogLabels(
        title = strings["note_text"],
        ok = strings["ok"],
    ),
    area = areaStrings(strings, formatBytes),
    offlineStyleName = strings[currentStyle.offlineNameKey()],
    save = strings["save"],
    offlineDeleteTitle = strings["offline_map_delete_title"],
    offlineDeleteMessage = strings["offline_map_delete_message"],
    errorTitle = strings["error"],
    errorMessage = strings["error"],
    settingsTitle = strings["settings"],
    settings = SettingsPageLabels(
        account = strings["not_logged_in"],
        logIn = strings["create_account"],
        loggedInAs = { strings.format("logged_in_as", it) },
        openProfile = strings["click_to_see_your_profile"],
        mapStyle = strings["map_style"],
        mapStyleValue = { strings[it.nameKey()] },
        customizeColors = strings["customize_colors"],
        customizeColorsSecondary = strings["customize_colors_secondary"],
        verifiedFilter = strings["verified_filter"],
        verifiedFilterValue = { strings[it.verifiedFilterKey()] },
        verifiedFilterYears = listOf(1, 2, 3),
        showAttribution = strings["show_attribution"],
        showAttributionSecondary = strings["show_attribution_secondary"],
        mapRotation = strings["map_rotation"],
        mapRotationSecondary = strings["map_rotation_secondary"],
        mapTilt = strings["map_tilt"],
        mapTiltSecondary = strings["map_tilt_secondary"],
        dbStats = strings["database_stats"],
        dbStatsSecondary = strings["database_stats_secondary"],
        imageStats = strings["image_stats"],
        imageStatsSecondary = strings["image_stats_secondary"],
        manageAreas = strings["manage_areas"],
        manageAreasSecondary = strings["manage_areas_secondary"],
        sectionMap = strings["settings_section_map"],
        sectionData = strings["settings_section_data"],
        sectionAdmin = strings["settings_section_admin"],
        sectionGeneral = strings["settings_section_general"],
        language = strings["language"],
        languageDialogTitle = strings["language"],
        languageSystemDefault = strings["language_system_default"],
        mapStyleDialogTitle = strings["map_style"],
        verifiedFilterDialogTitle = strings["verified_filter"],
        close = strings["close"],
    ),
    manageAreasTitle = strings["manage_areas"],
    manageAreas = ManageAreasLabels(
        search = strings["manage_areas_search"],
        clear = strings["manage_areas_clear"],
        empty = strings["manage_areas_empty"],
        noMatches = strings["manage_areas_no_matches"],
        failed = strings["manage_areas_failed"],
        retry = strings["retry"],
        notVerified = strings["not_verified"],
    ),
    verifyArea = strings["btn_verify"],
    editName = strings["edit_name"],
    nameField = strings["name"],
    editDescription = strings["edit_description"],
    descriptionField = strings["description"],
    cancel = strings["cancel"],
    profileTitle = strings["profile"],
    uploadedImagesTitle = strings["uploaded_images"],
    myEventsTitle = strings["my_events"],
    myNotesTitle = strings["my_notes"],
    userProfile = UserProfileLabels(
        username = strings["username"],
        password = strings["password"],
        savedPlaces = strings["saved_places"],
        savedAreas = strings["saved_areas"],
        noSavedPlaces = strings["no_saved_places"],
        noSavedAreas = strings["no_saved_areas"],
        logOut = strings["logout"],
        editUsername = strings["change_username"],
        editPassword = strings["change_password"],
        delete = strings["delete"],
        uploadedImages = strings["uploaded_images"],
        myEvents = strings["my_events"],
        myNotes = strings["my_notes"],
    ),
    profileForm = ProfileFormLabels(
        passwordMask = strings["password_mask"],
        required = strings["field_required"],
        changeUsernameTitle = strings["change_username"],
        changePasswordTitle = strings["change_password"],
        username = strings["username"],
        currentPassword = strings["current_password"],
        newPassword = strings["new_password"],
        confirmPassword = strings["confirm_password"],
        passwordsDoNotMatch = strings["passwords_do_not_match"],
        passwordTooShort = { strings.format("password_min_length", it) },
        save = strings["save"],
        cancel = strings["cancel"],
        usernameChanged = strings["username_changed"],
        passwordChanged = strings["password_changed"],
    ),
    uploadedImages = UploadedImagesLabels(
        empty = strings["uploaded_images_empty"],
        delete = strings["delete"],
        failed = strings["uploaded_images_failed"],
        retry = strings["retry"],
        unknownPlace = { strings.format("uploaded_images_place", it) },
    ),
    myEvents = MyEventsLabels(
        empty = strings["my_events_empty"],
        failed = strings["my_events_failed"],
        retry = strings["retry"],
        statusPending = strings["event_status_pending"],
        statusLive = strings["event_status_live"],
        statusRejected = strings["event_status_rejected"],
        revoke = strings["event_revoke"],
        revokeFailed = strings["event_revoke_failed"],
        duplicate = strings["event_duplicate"],
        dateRange = { date, start, end ->
            strings.format("event_date_time_range", date, start, end)
        },
    ),
    myNotes = MyNotesLabels(
        empty = strings["my_notes_empty"],
        failed = strings["my_notes_failed"],
        retry = strings["retry"],
        public = strings["note_public"],
        private = strings["note_private"],
        delete = strings["delete"],
        actionFailed = strings["my_notes_action_failed"],
        openOnMap = strings["note_show_on_map"],
    ),
    account = AccountLabels(
        username = strings["username"],
        password = strings["password"],
        confirmPassword = strings["confirm_password"],
        required = strings["field_required"],
        passwordTooShort = { strings.format("password_min_length", it) },
        passwordsDoNotMatch = strings["passwords_do_not_match"],
        signIn = strings["login"],
        createAccount = strings["sign_up"],
        alreadyHaveAccount = strings["log_in_with_existing_account"],
        createAnAccount = strings["i_don_t_have_an_account"],
        accountCreated = strings["account_created_sign_in_failed"],
        showPassword = strings["show_password"],
        hidePassword = strings["hide_password"],
    ),
    placeStrings = placeSheetStrings(strings),
    addLocation = AddLocationLabels(
        addPlace = strings["add_place_title"],
        addEvent = strings["add_event"],
        addNote = strings["add_note"],
    ),
    osmAttribution = strings["osm_attribution"],
    formatDistance = { meters ->
        // Beyond 10 km the fraction is noise.
        val number = if (meters < 1_000) {
            formatNumber(meters, if (meters > 10_000) 0 else 1)
        } else {
            formatNumber(meters / 1_000, if (meters > 10_000) 0 else 1)
        }
        if (meters < 1_000) strings.format("s_m", number) else strings.format("s_km", number)
    },
)

/** The Lightning invoice strings shared by the boost and comment screens. */
private fun invoiceLabels(strings: Strings): InvoicePaymentSectionLabels =
    InvoicePaymentSectionLabels(
        invoice = InvoicePaymentLabels(
            qrDescription = strings["qr_code"],
            pay = strings["pay"],
            copy = strings["copy"],
            startOver = strings["start_over"],
        ),
        discardMessage = strings["discard_invoice_confirmation"],
        discard = strings["start_over"],
        cancel = strings["cancel"],
    )

/** The string key for each shared boost plan. */
private fun BoostPlan.labelKey(): String = when (this) {
    BoostPlan.ONE_MONTH -> "months_1"
    BoostPlan.THREE_MONTHS -> "months_3"
    BoostPlan.TWELVE_MONTHS -> "months_12"
}

/** The string key for each report reason. */
private fun ReportType.labelKey(): String = when (this) {
    ReportType.Verified -> "report_type_verified"
    ReportType.RefusedSats -> "report_type_refused_sats"
    ReportType.OutOfBusiness -> "report_type_out_of_business"
}

/** The description string key for each report reason. */
private fun ReportType.descriptionKey(): String = when (this) {
    ReportType.Verified -> "report_type_verified_description"
    ReportType.RefusedSats -> "report_type_refused_sats_description"
    ReportType.OutOfBusiness -> "report_type_out_of_business_description"
}

/** The string key for each customizable colour. */
private fun MapColor.titleKey(): String = when (this) {
    MapColor.MarkerBackground -> "marker_background_color"
    MapColor.MarkerIcon -> "marker_icon_color"
    MapColor.BoostedMarkerBackground -> "boosted_marker_background"
    MapColor.BoostedMarkerIcon -> "boosted_marker_icon"
    MapColor.BadgeBackground -> "badge_background"
    MapColor.BadgeText -> "badge_text"
    MapColor.ButtonBackground -> "button_background"
    MapColor.ButtonIcon -> "button_icon"
    MapColor.ButtonBorder -> "button_border"
}

/** The string key for each sync state. */
private fun SyncState.labelKey(): String = when (this) {
    SyncState.Idle -> "db_stats_sync_state_idle"
    SyncState.UnbundlingPlaces -> "db_stats_sync_state_unbundling_places"
    SyncState.SyncingPlaces -> "db_stats_sync_state_syncing_places"
    SyncState.UnbundlingEvents -> "db_stats_sync_state_unbundling_events"
    SyncState.SyncingEvents -> "db_stats_sync_state_syncing_events"
    SyncState.UnbundlingComments -> "db_stats_sync_state_unbundling_comments"
    SyncState.SyncingComments -> "db_stats_sync_state_syncing_comments"
    SyncState.UnbundlingAreas -> "db_stats_sync_state_unbundling_areas"
    SyncState.SyncingAreas -> "db_stats_sync_state_syncing_areas"
}

/** The string key for each activity interval. */
private fun ActivityInterval.labelKey(): String = when (this) {
    ActivityInterval.Day -> "activity_interval_day"
    ActivityInterval.Week -> "activity_interval_week"
    ActivityInterval.Month -> "activity_interval_month"
    ActivityInterval.HalfYear -> "activity_interval_half_year"
    ActivityInterval.Year -> "activity_interval_year"
}

/** The string key for each map style's display name. */
private fun MapStyle.nameKey(): String = when (this) {
    MapStyle.Auto -> "style_auto"
    MapStyle.Liberty -> "style_liberty"
    MapStyle.Positron -> "style_positron"
    MapStyle.Bright -> "style_bright"
    MapStyle.Dark -> "style_dark"
    MapStyle.DarkMatter -> "style_dark_matter"
}

/** The string key for the offline download dialog's style name. */
private fun MapStyle.offlineNameKey(): String = nameKey()

/** The string key for a verification window. */
private fun Int.verifiedFilterKey(): String = when (this) {
    1 -> "verified_filter_1_year"
    2 -> "verified_filter_2_years"
    3 -> "verified_filter_3_years"
    else -> ""
}
