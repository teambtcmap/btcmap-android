package org.btcmap.feed

import android.content.Context
import android.text.format.DateFormat
import org.btcmap.R
import org.btcmap.api.ActivityFeedItem
import org.btcmap.ui.ActivityFeedRow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.concurrent.TimeUnit

/** Identity of a feed row, so a click can be mapped back to its item. */
internal fun ActivityFeedItem.feedKey(): String = "$type:$placeId:$date"

/**
 * Renders a feed item's icon (a Material Symbols ligature), subtitle and
 * relative date, resolving the Android resources here so the shared screen
 * stays resource-free.
 */
internal fun ActivityFeedItem.toRow(context: Context): ActivityFeedRow {
    val icon = when (type) {
        ActivityFeedItem.TYPE_PLACE_ADDED -> "add_location"
        ActivityFeedItem.TYPE_PLACE_UPDATED -> "edit"
        ActivityFeedItem.TYPE_PLACE_BOOSTED -> "rocket_launch"
        ActivityFeedItem.TYPE_PLACE_COMMENTED -> "comment"
        ActivityFeedItem.TYPE_PLACE_DELETED -> "delete"
        else -> "place"
    }

    val subtitle = when (type) {
        ActivityFeedItem.TYPE_PLACE_BOOSTED -> durationDays?.let {
            context.resources.getQuantityString(
                R.plurals.activity_boosted_for_days,
                it.toInt(),
                it.toInt(),
            )
        }.orEmpty()

        ActivityFeedItem.TYPE_PLACE_COMMENTED -> comment.orEmpty()
        ActivityFeedItem.TYPE_PLACE_ADDED -> byUser(context, R.string.activity_added_by)
        ActivityFeedItem.TYPE_PLACE_UPDATED -> byUser(context, R.string.activity_updated_by)
        ActivityFeedItem.TYPE_PLACE_DELETED -> byUser(context, R.string.activity_deleted_by)
        else -> byUser(context, R.string.activity_by_user)
    }

    return ActivityFeedRow(
        key = feedKey(),
        icon = icon,
        placeName = placeName.orEmpty(),
        subtitle = subtitle,
        date = relativeTime(context, date),
    )
}

private fun ActivityFeedItem.byUser(context: Context, resId: Int): String =
    osmUserName?.let { context.getString(resId, it) }.orEmpty()

private fun relativeTime(context: Context, dateString: String): String {
    val date = try {
        ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: DateTimeParseException) {
        return dateString
    }
    val now = ZonedDateTime.now()
    val diffMillis = now.toInstant().toEpochMilli() - date.toInstant().toEpochMilli()

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

    return when {
        minutes < 1 -> context.getString(R.string.activity_just_now)
        minutes < 60 -> context.resources.getQuantityString(
            R.plurals.activity_minutes_ago,
            minutes.toInt(),
            minutes.toInt(),
        )

        hours < 24 -> context.resources.getQuantityString(
            R.plurals.activity_hours_ago,
            hours.toInt(),
            hours.toInt(),
        )

        days < 7 -> context.resources.getQuantityString(
            R.plurals.activity_days_ago,
            days.toInt(),
            days.toInt(),
        )

        else -> DateFormat.getMediumDateFormat(context).format(Date.from(date.toInstant()))
    }
}
