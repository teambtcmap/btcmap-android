package org.btcmap.feed

import org.btcmap.api.ActivityFeedItem

/**
 * The pieces of an activity-feed row that do not depend on a host's language:
 * its identity and its icon. Android and the desktop both render the shared
 * `ActivityFeedScreen`, so the icon and the row key must not drift apart.
 */

/** Identity of a feed row, so a click can be mapped back to its item. */
fun ActivityFeedItem.feedKey(): String = "$type:$placeId:$date"

/** The Material Symbols glyph for [this] item's kind. */
fun ActivityFeedItem.iconGlyph(): String = when (type) {
    ActivityFeedItem.TYPE_PLACE_ADDED -> "add_location"
    ActivityFeedItem.TYPE_PLACE_UPDATED -> "edit"
    ActivityFeedItem.TYPE_PLACE_BOOSTED -> "rocket_launch"
    ActivityFeedItem.TYPE_PLACE_COMMENTED -> "comment"
    ActivityFeedItem.TYPE_PLACE_DELETED -> "delete"
    else -> "place"
}
