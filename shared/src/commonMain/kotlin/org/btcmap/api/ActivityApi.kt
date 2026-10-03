package org.btcmap.api

import io.ktor.http.HttpMethod
import org.btcmap.util.toJsonArray

data class ActivityFeedItem(
    val type: String,
    val placeId: Long,
    val placeName: String?,
    val osmUserId: Long?,
    val osmUserName: String?,
    val osmUserTip: String?,
    val image: String?,
    val date: String,
    val durationDays: Long?,
    val comment: String?,
) {
    companion object {
        const val TYPE_PLACE_ADDED = "place_added"
        const val TYPE_PLACE_UPDATED = "place_updated"
        const val TYPE_PLACE_DELETED = "place_deleted"
        const val TYPE_PLACE_COMMENTED = "place_commented"
        const val TYPE_PLACE_BOOSTED = "place_boosted"
    }
}

/**
 * Fetches the merged activity feed. [areaIds] accepts numeric area ids or url
 * aliases; [placeIds] are integer place ids. When both are given the endpoint
 * returns the union, deduplicated server-side.
 */
suspend fun Api.getActivity(
    areaIds: List<String>,
    placeIds: List<String> = emptyList(),
    days: Int = 7,
): List<ActivityFeedItem> {
    if (areaIds.isEmpty() && placeIds.isEmpty()) {
        return emptyList()
    }

    val url = buildUrl("v4", "activity") {
        if (areaIds.isNotEmpty()) {
            parameters.append("areas", areaIds.joinToString(","))
        }
        if (placeIds.isNotEmpty()) {
            parameters.append("places", placeIds.joinToString(","))
        }
        parameters.append("days", "$days")
    }

    return call(HttpMethod.Get, url, withoutAuth = true) { it.toActivityFeedItems() }
}

private fun String.toActivityFeedItems(): List<ActivityFeedItem> {
    return toJsonArray().map {
        ActivityFeedItem(
            type = it.string("type"),
            placeId = it.long("place_id"),
            placeName = it.nonBlankStringOrNull("place_name"),
            osmUserId = it.longOrNull("osm_user_id"),
            osmUserName = it.nonBlankStringOrNull("osm_user_name"),
            osmUserTip = it.nonBlankStringOrNull("osm_user_tip"),
            image = it.nonBlankStringOrNull("image"),
            date = it.string("date"),
            durationDays = it.longOrNull("duration_days"),
            comment = it.nonBlankStringOrNull("comment"),
        )
    }
}
