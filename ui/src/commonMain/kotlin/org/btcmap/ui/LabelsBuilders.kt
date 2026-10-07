package org.btcmap.ui

import org.btcmap.api.ActivityFeedItem
import org.btcmap.feed.feedKey
import org.btcmap.feed.iconGlyph
import org.btcmap.i18n.Strings
import kotlin.time.Clock
import kotlin.time.Instant

/** The shared strings for the area screen, with byte sizes left to the host. */
fun areaStrings(strings: Strings, formatBytes: (Long) -> String): AreaStrings = AreaStrings(
    readMore = strings["read_more"],
    collapse = strings["collapse"],
    boostedMerchants = strings["boosted_merchants"],
    events = strings["events"],
    howToHelp = strings["how_to_help"],
    offlineMap = strings["offline_map"],
    offlineDownload = strings["offline_map_download"],
    offlineDownloadAgain = strings["offline_map_download_again"],
    offlineDelete = strings["delete"],
    cancel = strings["cancel"],
    boosted = strings["boosted"],
    boostedUntil = { date -> strings.format("boosted_until_s", date) },
    issues = { shown, total ->
        if (shown < total) {
            strings.format("issues_d_of_d", shown, total)
        } else {
            strings.format("issues_d", total)
        }
    },
    issueDescription = { code -> describeIssue(strings, code) },
    offlineStatusDownloading = { size -> strings.format("offline_map_status_downloading", size) },
    offlineStatusProgress = { percent, size ->
        strings.format("offline_map_status_downloading_progress", percent, size)
    },
    offlineStatusDownloaded = { size, minZoom, maxZoom ->
        strings.format("offline_map_status_downloaded", size, minZoom, maxZoom)
    },
    offlineStyleMismatch = strings["offline_map_style_mismatch"],
    offlineStatusFailed = { message -> strings.format("offline_map_status_failed", message) },
    offlineDialogDescription = { areaName ->
        strings.format("offline_map_description", areaName)
    },
    offlineDialogStyle = { styleName -> strings.format("offline_map_style", styleName) },
    offlineDialogMaxZoom = { zoom -> strings.format("offline_map_max_zoom", zoom) },
    offlineDialogEstimatedSize = { size -> strings.format("offline_map_estimated_size", size) },
    offlineDialogTooLarge = { size -> strings.format("offline_map_too_large", size) },
    offlineDialogEstimateNote = strings["offline_map_estimate_note"],
    formatBytes = formatBytes,
)

/** The place-issue description for a data-quality code. */
private fun describeIssue(strings: Strings, code: String): String {
    val (key, arg) = when {
        code == "outdated" -> "issue_outdated" to null
        code == "outdated_soon" -> "issue_outdated_soon" to null
        code == "not_verified" -> "not_verified" to null
        code == "missing_icon" -> "issue_missing_icon" to null
        code.startsWith(INVALID_TAG_VALUE_PREFIX) ->
            "issue_invalid_tag_value" to code.substringAfter(INVALID_TAG_VALUE_PREFIX)

        code.startsWith(MISSPELLED_TAG_NAME_PREFIX) ->
            "issue_misspelled_tag_name" to code.substringAfter(MISSPELLED_TAG_NAME_PREFIX)

        else -> "issue_unknown" to null
    }
    return if (arg != null) strings.format(key, arg) else strings[key]
}

private const val INVALID_TAG_VALUE_PREFIX = "invalid_tag_value:"
private const val MISSPELLED_TAG_NAME_PREFIX = "misspelled_tag_name:"

/** The shared strings for the place sheet and standalone place body. */
fun placeSheetStrings(strings: Strings): PlaceSheetStrings = PlaceSheetStrings(
    directions = strings["directions"],
    share = strings["share"],
    viewOnBtcmap = strings["view_on_btcmap"],
    viewOnOsm = strings["view_on_osm"],
    editOnOsm = strings["edit_on_osm"],
    notVerified = strings["not_verified"],
    verificationWarningTitle = strings["verification_warning_title"],
    verificationWarningOutdated = strings["verification_warning_outdated"],
    verificationWarningNotVerified = strings["verification_warning_not_verified"],
    ok = strings["ok"],
    companionWarning = { strings.format("companion_warning", it) },
    verify = strings["btn_verify"],
    report = strings["btn_report"],
    boost = strings["boost"],
    commentsTitle = { count -> strings.format("comments_d", count) },
    addComment = strings["add_comment"],
    watch = strings["watch"],
    unwatch = strings["unwatch"],
    addPhoto = strings["add_photo"],
    uploadedBy = { name -> strings.format("uploaded_by", name) },
    deletePhoto = strings["delete_photo"],
    openingHoursClosed = strings["opening_hours_closed"],
    openingHoursOpen24_7 = strings["opening_hours_open_24_7"],
)

/**
 * One rendered activity feed row: the icon and key come from `:shared`, the
 * subtitle and relative date are resolved here from [strings]. [formatDate] is
 * the host's medium date format, used once a row is older than a week.
 */
fun activityFeedRow(
    strings: Strings,
    item: ActivityFeedItem,
    formatDate: (String) -> String,
): ActivityFeedRow {
    val subtitle = when (item.type) {
        ActivityFeedItem.TYPE_PLACE_BOOSTED -> item.durationDays
            ?.let { strings.plural("activity_boosted_for_days", it.toInt()) }
            .orEmpty()

        ActivityFeedItem.TYPE_PLACE_COMMENTED -> item.comment.orEmpty()
        ActivityFeedItem.TYPE_PLACE_ADDED -> item.byUser(strings, "activity_added_by")
        ActivityFeedItem.TYPE_PLACE_UPDATED -> item.byUser(strings, "activity_updated_by")
        ActivityFeedItem.TYPE_PLACE_DELETED -> item.byUser(strings, "activity_deleted_by")
        else -> item.byUser(strings, "activity_by_user")
    }

    return ActivityFeedRow(
        key = item.feedKey(),
        icon = item.iconGlyph(),
        placeName = item.placeName.orEmpty(),
        subtitle = subtitle,
        date = relativeTime(strings, item.date, formatDate),
    )
}

private fun ActivityFeedItem.byUser(strings: Strings, key: String): String =
    osmUserName?.let { strings.format(key, it) }.orEmpty()

private fun relativeTime(strings: Strings, dateString: String, formatDate: (String) -> String): String {
    val date = try {
        Instant.parse(dateString)
    } catch (_: Exception) {
        return dateString
    }
    val diff = Clock.System.now() - date
    val minutes = diff.inWholeMinutes
    val hours = diff.inWholeHours
    val days = diff.inWholeDays
    return when {
        minutes < 1 -> strings["activity_just_now"]
        minutes < 60 -> strings.plural("activity_minutes_ago", minutes.toInt())
        hours < 24 -> strings.plural("activity_hours_ago", hours.toInt())
        days < 7 -> strings.plural("activity_days_ago", days.toInt())
        else -> formatDate(dateString)
    }
}
