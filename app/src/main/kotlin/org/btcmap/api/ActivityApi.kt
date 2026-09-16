package org.btcmap.api

import okhttp3.Request
import org.btcmap.auth.withoutAuth
import org.btcmap.util.toJsonArray
import java.io.InputStream

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
)

suspend fun Api.getActivity(areaIds: List<String>, days: Int = 7): List<ActivityFeedItem> {
    if (areaIds.isEmpty()) {
        return emptyList()
    }

    val url = buildUrl("v4", "activity") {
        addQueryParameter("areas", areaIds.joinToString(","))
        addQueryParameter("days", "$days")
    }

    return call(Request.Builder().withoutAuth().url(url).build()) { it.toActivityFeedItems() }
}

private fun InputStream.toActivityFeedItems(): List<ActivityFeedItem> {
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
