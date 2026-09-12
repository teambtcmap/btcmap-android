package org.btcmap.api

import okhttp3.Request
import org.btcmap.util.toJsonArray
import java.io.InputStream

data class ActivityFeedItem(
    val type: String,
    val placeId: Long,
    val placeName: String,
    val osmUserId: Long?,
    val osmUserName: String?,
    val osmUserTip: String?,
    val image: String?,
    val date: String,
    val durationDays: Long?,
    val comment: String?,
)

suspend fun Api.getActivity(areaIds: List<String>, days: Int = 7): List<ActivityFeedItem> {
    val url = url.newBuilder().addPathSegments("v4/activity").apply {
        addQueryParameter("areas", areaIds.joinToString(","))
        addQueryParameter("days", "$days")
    }.build()

    return call(Request.Builder().url(url).build()) { it.toActivityFeedItems() }
}

private fun InputStream.toActivityFeedItems(): List<ActivityFeedItem> {
    return toJsonArray().map {
        ActivityFeedItem(
            type = it.get("type").asString,
            placeId = it.get("place_id").asLong,
            placeName = it.get("place_name").asString,
            osmUserId = if (!it.has("osm_user_id") || it.get("osm_user_id").isJsonNull) null else it.get(
                "osm_user_id"
            ).asLong,
            osmUserName = if (!it.has("osm_user_name") || it.get("osm_user_name").isJsonNull) null else it.get(
                "osm_user_name"
            ).asString.ifBlank { null },
            osmUserTip = if (!it.has("osm_user_tip") || it.get("osm_user_tip").isJsonNull) null else it.get(
                "osm_user_tip"
            ).asString.ifBlank { null },
            image = if (!it.has("image") || it.get("image").isJsonNull) null else it.get("image").asString.ifBlank { null },
            date = it.get("date").asString,
            durationDays = if (!it.has("duration_days") || it.get("duration_days").isJsonNull) null else it.get(
                "duration_days"
            ).asLong,
            comment = if (!it.has("comment") || it.get("comment").isJsonNull) null else it.get("comment").asString.ifBlank { null },
        )
    }
}
